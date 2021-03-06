/*
 * Cacheonix Systems licenses this file to You under the LGPL 2.1
 * (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.cacheonix.org/products/cacheonix/license-lgpl-2.1.htm
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cacheonix.impl.cache.store;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.cacheonix.cache.invalidator.Invalidateable;
import org.cacheonix.cache.subscriber.EntryModifiedEventContentFlag;
import org.cacheonix.cache.subscriber.EntryModifiedEventType;
import org.cacheonix.impl.cache.datasource.PrefetchCommand;
import org.cacheonix.impl.cache.item.Binary;
import org.cacheonix.impl.cache.storage.disk.StorageException;
import org.cacheonix.impl.cache.storage.disk.StoredObject;
import org.cacheonix.impl.cache.subscriber.BinaryEntryModifiedEvent;
import org.cacheonix.impl.clock.Clock;
import org.cacheonix.impl.clock.Time;
import org.cacheonix.impl.net.serializer.SerializerUtils;
import org.cacheonix.impl.net.serializer.Wireable;
import org.cacheonix.impl.net.serializer.WireableBuilder;
import org.cacheonix.impl.util.Assert;
import org.cacheonix.impl.util.exception.ExceptionUtils;
import org.cacheonix.impl.util.logging.Logger;

import static org.cacheonix.cache.subscriber.EntryModifiedEventContentFlag.NEED_ALL;
import static org.cacheonix.cache.subscriber.EntryModifiedEventContentFlag.NEED_KEY;
import static org.cacheonix.cache.subscriber.EntryModifiedEventContentFlag.NEED_NEW_VALUE;
import static org.cacheonix.cache.subscriber.EntryModifiedEventContentFlag.NEED_PREVIOUS_VALUE;
import static org.cacheonix.cache.subscriber.EntryModifiedEventType.EVICT;
import static org.cacheonix.cache.subscriber.EntryModifiedEventType.REMOVE;
import static org.cacheonix.impl.cache.util.StandardObjectSizeCalculator.SIZE_OBJECT_REF;

/**
 * Cache element used in local caches to store cached values. In addition to holding a cache value itself, the cache
 * element holds information about secondary storage, if any.
 */
@SuppressWarnings({"JavaDoc", "RedundantIfStatement"})
public final class BinaryStoreElement implements Invalidateable, Wireable, ReadableElement {

   /**
    * Logger.
    *
    * @noinspection UNUSED_SYMBOL, UnusedDeclaration
    */
   private static final Logger LOG = Logger.getLogger(BinaryStoreElement.class); // NOPMD

   private static final byte FLAG_HAS_ELEMENT_SIZE_BYTES = 1;

   private static final byte FLAG_HAS_KEY_SIZE_BYTES = 2;

   private static final byte FLAG_HAS_VALUE_SIZE_BYTES = 4;

   private static final byte FLAG_VALID = 8;

   /**
    * Builder used by WireableFactory.
    */
   public static final WireableBuilder BUILDER = new Builder();

   /**
    * Shallow size of empty BinaryStoreElement used to calculate total byte size in memory of BinaryStoreElement. See
    * <code>BinaryStoreElementTest.testWriteReadWire()</code> for calculation.
    */
   static final int SIZE_CACHE_ELEMENT_OVERHEAD = 136;


   /**
    * Key.
    */
   private Binary key = null;

   /**
    * Cache value or StoredObject is the value of the element is stored in the second storage.
    */
   private Binary value = null;


   private transient StoredObject storedValue = null;

   /**
    * Time the element was created.
    */
   private Time createdTime = null;

   /**
    * Time to expire.
    */
   private Time expirationTime = null;

   /**
    * Time to idle
    */
   private Time idleTime = null;

   /**
    * Element before.
    */
   private transient BinaryStoreElement before = null;

   /**
    * Element after.
    */
   private transient BinaryStoreElement after = null;

   /**
    * Recorder size in bytes.
    */
   private long elementSizeBytes = 0L;

   /**
    * Key size.
    */
   private long keySizeBytes = 0L;

   /**
    * Value size.
    */
   private long valueSizeBytes = 0L;

   /**
    * Update counter.
    */
   private long updateCounter = 0L;

   /**
    * A collection of entry modified subscribers.
    */
   private transient List<BinaryEntryModifiedSubscriber> entryModifiedSubscribers = null;

   /**
    * A prefetch order.
    */
   private transient PrefetchCommand prefetchCommand = null;

   /**
    * Boolean flags.
    */
   private byte flags = (byte) 0;

   private BinaryStoreElementContext context = null;


   /**
    * Constructor.
    *
    * @param value          cache element value.
    * @param createdTime
    * @param expirationTime expiration time millis.
    * @param idleTime       idle time millis.
    */
   public BinaryStoreElement(final Binary key, final Binary value, final Time createdTime, final Time expirationTime,
           final Time idleTime) {

      // Functional fields
      this.createdTime = createdTime;
      this.expirationTime = expirationTime;
      this.idleTime = idleTime;
      this.key = key;
      this.value = value;
      this.setValid(true);
   }


   /**
    * Required by Wireable.
    */
   public BinaryStoreElement() {

   }


   /**
    * {@inheritDoc}
    */
   public Binary getValue() throws StorageException {

      // Get cache value
      if (isStored()) {

         // Element contains stored object as value
         return (Binary) context.getDiskStorage().get(storedValue);
      } else {
         return value;
      }
   }


   /**
    * Returns <code>true</code> if the value of this cache element is stored in the secondary storage.
    * <p/>
    * For a stored element method getValue() return StoredObject instead of the actual value.
    *
    * @return <code>true</code> if the value of this cache element is stored in the secondary storage.
    */
   public boolean isStored() {

      return storedValue != null;
   }


   /**
    * Returns the time this element was created.
    *
    * @return the time this element was created.
    */
   public Time getCreatedTime() {

      return createdTime;
   }


   /**
    * Returns time when the element should expire.
    *
    * @return the time when the element should expire.
    */
   public Time getExpirationTime() {

      return expirationTime;
   }


   /**
    * Returns <code>true</code> if this cache element expired.
    *
    * @param clock
    * @return <code>true</code> if this cache element expired.
    */
   @SuppressWarnings("ReuseOfLocalVariable")
   public boolean isExpired(final Clock clock) {

      Time currentTime = null;
      if (expirationTime != null) {

         currentTime = clock.currentTime();
         if (currentTime.compareTo(expirationTime) > 0) {

            return true;
         }
      }

      if (idleTime != null) {

         if (currentTime == null) {

            currentTime = clock.currentTime();
         }

         if (currentTime.compareTo(idleTime) > 0) {

            return true;
         }
      }
      return false;
   }


   /**
    * Returns the absolute time the element should expire at due to inactivity.
    *
    * @return the absolute time the element should expire at due to inactivity.
    */
   public Time getIdleTime() {

      return idleTime;
   }


   public void setIdleTime(final Time idleTime) {

      this.idleTime = idleTime;
   }


   /**
    * @return cache element before.
    */
   public BinaryStoreElement getBefore() {

      return before;
   }


   /**
    * {@inheritDoc}
    *
    * @param before sets before element.
    */
   public void setBefore(final BinaryStoreElement before) {

      this.before = before;
   }


   /**
    * Returns cache element after.
    *
    * @return cache element after.
    */
   public BinaryStoreElement getAfter() {

      return after;
   }


   /**
    * Sets cache element after.
    *
    * @param after sets after element.
    */
   public void setAfter(final BinaryStoreElement after) {

      this.after = after;
   }


   /**
    * @return cache element key.
    */
   public Binary getKey() {

      return key;
   }


   /**
    * {@inheritDoc}
    */
   public void invalidate() {

      setValid(false);
   }


   /**
    * {@inheritDoc}
    */
   public boolean isValid() {

      // Do invalidation only once.
      if ((flags & FLAG_VALID) != 0) {

         return true;
      }

      // Call invalidator.
      context.getInvalidator().process(this);

      // Return result
      return false;
   }


   /**
    * {@inheritDoc}
    */
   public long getUpdateCounter() {

      return updateCounter;
   }


   /**
    * Sets update counter.
    */
   public void setUpdateCounter(final long counter) {

      this.updateCounter = counter;
   }


   /**
    * Sets a prefetch order.
    *
    * @param prefetchCommand the prefetch order to set.
    */
   public void setPrefetchCommand(final PrefetchCommand prefetchCommand) {

      this.prefetchCommand = prefetchCommand;
   }


   public void cancelPrefetch() {

      if (prefetchCommand != null) {

         prefetchCommand.cancelPrefetch();
         prefetchCommand = null;
      }
   }


   /**
    * Removes an element from the LRU list.
    *
    * @return a element that comes after the removed element as returned by {@link #getAfter()}. The returned result can
    * be used for iterative removal of elements.
    */
   public BinaryStoreElement removeFromLRUList() {

      if (before == null) {
         return null;
      }

      // Element is in the list, remove
      final BinaryStoreElement result = after;
      before.after = after;
      after.before = before;
      before = null;
      after = null;

      return result;
   }


   /**
    * Stores the element in the secondary storage.
    *
    * @return <code>true</code> if an element was stored.
    * @throws IllegalStateException if the object is already stored.
    */
   public boolean store() throws StorageException {

      Assert.assertFalse(isStored(), "Object cannot be stored twice, key: {0}", key);

      final StoredObject storedObject = context.getDiskStorage().put(key, value);
      if (storedObject != null) {

         // Replace value
         storedValue = storedObject;
         value = null;
         setValueSizeBytes(SIZE_OBJECT_REF);
         setElementSizeBytes(context.getObjectSizeCalculator().sum(SIZE_CACHE_ELEMENT_OVERHEAD, getKeySizeBytes(),
                 getValueSizeBytes()));
      }
      return isStored();
   }


   /**
    * Loads value from storage.
    *
    * @return current size in bytes.
    * @throws StorageException
    */
   public long load() throws StorageException, IOException {

      if (isStored()) {

         // Load value
         value = (Binary) context.getDiskStorage().get(storedValue);

         // Discard
         context.getDiskStorage().remove(storedValue);

         // Mark as not stored
         storedValue = null;

         // Recalculate the size becuase it was set to reference length value at store()
         setValueSizeBytes(context.getObjectSizeCalculator().sizeOf(value));
         setElementSizeBytes(context.getObjectSizeCalculator().sum(SIZE_CACHE_ELEMENT_OVERHEAD, getKeySizeBytes(),
                 getValueSizeBytes()));
      }

      return getSizeBytes();
   }


   /**
    * {@inheritDoc}
    */
   public void discard() throws IOException {

      Assert.assertTrue(isStored(), "Cannot discard an object that is not stored, key: {0}", key);

      context.getDiskStorage().remove(storedValue);
      storedValue = null;
   }


   /**
    * {@inheritDoc}
    */
   public long getSizeBytes() {

      if (!hasElementSizeBytes()) {

         final long keySizeBytes = getKeySizeBytes();
         final long valueSizeBytes = getValueSizeBytes();
         setElementSizeBytes(
                 context.getObjectSizeCalculator().sum(SIZE_CACHE_ELEMENT_OVERHEAD, keySizeBytes, valueSizeBytes));
      }

      return elementSizeBytes;
   }


   private void setElementSizeBytes(final long elementSizeBytes) {

      this.elementSizeBytes = elementSizeBytes;
      this.setHasElementSizeBytes(true);
   }


   /**
    * Returns the key size in bytes. If the key size is null, calculates it.
    *
    * @return the key size in bytes.
    */
   private long getKeySizeBytes() {

      if (!hasKeySizeBytes()) {

         setKeySizeBytes(context.getObjectSizeCalculator().sizeOf(key));
      }

      return keySizeBytes;
   }


   private void setKeySizeBytes(final long keySizeBytes) {

      this.keySizeBytes = keySizeBytes;
      this.setHasKeySizeBytes(true);
   }


   /**
    * Returns the value size in bytes. If the value size is null, calculates it.
    *
    * @return the key size in bytes.
    */
   private long getValueSizeBytes() {

      if (!hasValueSizeBytes()) {

         if (isStored()) {

            setValueSizeBytes(SIZE_OBJECT_REF);
         } else {

            setValueSizeBytes(context.getObjectSizeCalculator().sizeOf(value));
         }
      }

      return valueSizeBytes;
   }


   private void setValueSizeBytes(final long valueSizeBytes) {

      this.valueSizeBytes = valueSizeBytes;
      this.setHasValueSizeBytes(true);
   }


   /**
    * Adds a list of subscribers to a list of entry modified subscribers. Does nothing if <code>subscribers</code> are
    * null or empty.
    *
    * @param subscribers the list of subscribers to add.
    */
   public void addEntryModifiedSubscribers(final List<BinaryEntryModifiedSubscriber> subscribers) {

      if (subscribers == null || subscribers.isEmpty()) {
         return;
      }

      entryModifiedSubscribers().addAll(subscribers);
   }


   /**
    * Adds a subscriber to a list of entry modified subscribers.
    *
    * @param subscriber the subscriber to add.
    */
   public void addEventSubscriber(final BinaryEntryModifiedSubscriber subscriber) {


      entryModifiedSubscribers().add(subscriber);
   }


   public void removeEntryModifiedSubscriber(final int subscriberIdentity) {

      // There is nothing to do
      if (entryModifiedSubscribers == null) {

         return;
      }

      // Remove subscriber
      for (final Iterator<BinaryEntryModifiedSubscriber> iterator = entryModifiedSubscribers.iterator(); iterator.hasNext(); ) {

         final BinaryEntryModifiedSubscriber subscriber = iterator.next();
         if (subscriber.getIdentity() == subscriberIdentity) {

            // Found, remove
            iterator.remove();

            break;
         }
      }

      // Discard empty subscriber registry
      if (entryModifiedSubscribers.isEmpty()) {

         entryModifiedSubscribers = null;
      }
   }


   /**
    * Sets subscribers this element owne to the parameter element's and sets own list of subscriber to null.
    *
    * @param receiver a receiver of entry modified subscribers.
    */
   public void transferEntryModifiedSubscribers(final BinaryStoreElement receiver) {

      // Pass to receiver
      receiver.entryModifiedSubscribers = entryModifiedSubscribers;

      // Set our own to null
      entryModifiedSubscribers = null;
   }


   /**
    * Returns a list of subscribers.
    *
    * @return a list of subscribers.
    */
   private List<BinaryEntryModifiedSubscriber> entryModifiedSubscribers() {

      if (entryModifiedSubscribers == null) {

         entryModifiedSubscribers = new ArrayList<BinaryEntryModifiedSubscriber>(1);
      }

      return entryModifiedSubscribers;
   }


   /**
    * Notifies modification subscribers.
    *
    * @param previousElement
    * @param updateType
    * @throws StorageException
    */
   public void notifyModificationSubscribers(final BinaryStoreElement previousElement,
           final EntryModifiedEventType updateType) throws StorageException {

      if (entryModifiedSubscribers == null || entryModifiedSubscribers.isEmpty()) {

         return;
      }

      for (final BinaryEntryModifiedSubscriber subscriber : entryModifiedSubscribers) {

         // Check interest
         if (!subscriber.getModificationTypes().contains(updateType)) {

            return;
         }

         // Calculate parameters based on flags
         Binary eventKey = null;
         Binary eventValue = null;
         Binary eventPreviousValue = null;
         for (final EntryModifiedEventContentFlag eventContentFlag : subscriber.getEventContentFlags()) {

            if (NEED_KEY.equals(eventContentFlag)) {

               eventKey = key;
            } else if (NEED_NEW_VALUE.equals(eventContentFlag)) {

               eventValue = updateType.equals(REMOVE) || updateType.equals(EVICT) ? null : getValue();
            } else if (NEED_PREVIOUS_VALUE.equals(eventContentFlag)) {

               eventPreviousValue = previousElement == null ? null : previousElement.getValue();
            } else if (NEED_ALL.equals(eventContentFlag)) {

               // A superposition of actions taken by individual flags

               eventKey = key;
               eventValue = updateType.equals(REMOVE) ? null : getValue();
               eventPreviousValue = previousElement == null ? null : previousElement.getValue();

               break; // No need to traverse through other flags
            }
         }

         // Send event
         final BinaryEntryModifiedEvent event = new BinaryEntryModifiedEvent(updateType, eventKey, eventValue,
                 eventPreviousValue, createdTime, updateCounter, null);
         subscriber.notifyKeysUpdated(Collections.singletonList(event));
      }
   }


   public final int getWireableType() {

      return TYPE_BINARY_STORE_ELEMENT;
   }


   /**
    * Returns the flag indicating that element size has been calculated.
    */
   private boolean hasElementSizeBytes() {

      return (flags & FLAG_HAS_ELEMENT_SIZE_BYTES) != 0;
   }


   /**
    * Sets the flag indicating that element size has been calculated.
    */
   private void setHasElementSizeBytes(final boolean hasElementSizeBytes) {

      if (hasElementSizeBytes) {

         flags |= FLAG_HAS_ELEMENT_SIZE_BYTES;
      } else {
         flags &= ~FLAG_HAS_ELEMENT_SIZE_BYTES;
      }
   }


   /**
    * Returns the flag indicating that key size has been calculated.
    */
   private boolean hasKeySizeBytes() {

      return (flags & FLAG_HAS_KEY_SIZE_BYTES) != 0;
   }


   /**
    * Sets the flag indicating that key size has been calculated.
    */
   private void setHasKeySizeBytes(final boolean hasKeySizeBytes) {

      if (hasKeySizeBytes) {

         flags |= FLAG_HAS_KEY_SIZE_BYTES;
      } else {
         flags &= ~FLAG_HAS_KEY_SIZE_BYTES;
      }
   }


   /**
    * Returns the flag indicating that value size has been calculated.
    */
   private boolean hasValueSizeBytes() {

      return (flags & FLAG_HAS_VALUE_SIZE_BYTES) != 0;
   }


   /**
    * Sets the flag indicating that value size has been calculated.
    */
   private void setHasValueSizeBytes(final boolean hasValueSizeBytes) {

      if (hasValueSizeBytes) {

         flags |= FLAG_HAS_VALUE_SIZE_BYTES;
      } else {
         flags &= ~FLAG_HAS_VALUE_SIZE_BYTES;
      }
   }


   /**
    * Sets the flag indicating that element is valid.
    */
   private void setValid(final boolean valid) {

      if (valid) {

         flags |= FLAG_VALID;
      } else {
         flags &= ~FLAG_VALID;
      }
   }


   @SuppressWarnings("SimplifiableConditionalExpression")
   public void writeWire(final DataOutputStream out) throws IOException {

      try {

         out.writeByte(flags);
         out.writeLong(updateCounter);
         out.writeLong(keySizeBytes);
         out.writeLong(valueSizeBytes);
         out.writeLong(elementSizeBytes);
         SerializerUtils.writeTime(idleTime, out);
         SerializerUtils.writeTime(createdTime, out);
         SerializerUtils.writeTime(expirationTime, out);
         SerializerUtils.writeBinary(out, key);
         SerializerUtils.writeBinary(out, getValue());
      } catch (final StorageException e) {

         throw ExceptionUtils.createIOException(e);
      }
   }


   public void readWire(final DataInputStream in) throws IOException, ClassNotFoundException {

      flags = in.readByte();
      updateCounter = in.readLong();
      keySizeBytes = in.readLong();
      valueSizeBytes = in.readLong();
      elementSizeBytes = in.readLong();
      idleTime = SerializerUtils.readTime(in);
      createdTime = SerializerUtils.readTime(in);
      expirationTime = SerializerUtils.readTime(in);
      key = SerializerUtils.readBinary(in);
      value = SerializerUtils.readBinary(in);
   }


   public boolean equals(final Object o) {

      if (this == o) {
         return true;
      }
      if (o == null || !o.getClass().equals(getClass())) {
         return false;
      }

      final BinaryStoreElement that = (BinaryStoreElement) o;

      if (elementSizeBytes != that.elementSizeBytes) {
         return false;
      }
      if (keySizeBytes != that.keySizeBytes) {
         return false;
      }
      if (valueSizeBytes != that.valueSizeBytes) {
         return false;
      }
      if (updateCounter != that.updateCounter) {
         return false;
      }
      if (flags != that.flags) {
         return false;
      }
      if (key != null ? !key.equals(that.key) : that.key != null) {
         return false;
      }
      if (value != null ? !value.equals(that.value) : that.value != null) {
         return false;
      }
      if (createdTime != null ? !createdTime.equals(that.createdTime) : that.createdTime != null) {
         return false;
      }
      if (expirationTime != null ? !expirationTime.equals(that.expirationTime) : that.expirationTime != null) {
         return false;
      }
      if (idleTime != null ? !idleTime.equals(that.idleTime) : that.idleTime != null) {
         return false;
      }

      return true;
   }


   public int hashCode() {

      int result = key != null ? key.hashCode() : 0;
      result = 31 * result + (value != null ? value.hashCode() : 0);
      result = 31 * result + (createdTime != null ? createdTime.hashCode() : 0);
      result = 31 * result + (expirationTime != null ? expirationTime.hashCode() : 0);
      result = 31 * result + (idleTime != null ? idleTime.hashCode() : 0);
      result = 31 * result + (int) (elementSizeBytes ^ elementSizeBytes >>> 32);
      result = 31 * result + (int) (keySizeBytes ^ keySizeBytes >>> 32);
      result = 31 * result + (int) (valueSizeBytes ^ valueSizeBytes >>> 32);
      result = 31 * result + (int) (updateCounter ^ updateCounter >>> 32);
      result = 31 * result + (int) flags;
      return result;
   }


   public String toString() {

      return "BinaryStoreElement{" +
              "key=" + key +
              ", value=" + value +
              ", storedValue=" + storedValue +
              ", createdTimeMillis=" + createdTime +
              ", expirationTimeMillis=" + expirationTime +
              ", idleTimeMillis=" + idleTime +
              ", sizeBytes=" + elementSizeBytes +
              ", keyByteSize=" + keySizeBytes +
              ", valueByteSize=" + valueSizeBytes +
              ", valid=" + isValid() +
              ", updateCounter=" + updateCounter +
              '}';
   }


   public void setContext(final BinaryStoreElementContext context) {

      this.context = context;
   }


   /**
    * A class factory.
    */
   private static final class Builder implements WireableBuilder {

      public Wireable create() {

         return new BinaryStoreElement();
      }
   }
}
