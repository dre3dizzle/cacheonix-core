/*
 * Cacheonix systems licenses this file to You under the LGPL 2.1
 * (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.cacheonix.com/products/cacheonix/license-lgpl-2.1.htm
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cacheonix.impl.cache.local;

import org.cacheonix.CacheonixTestCase;
import org.cacheonix.impl.cache.datasource.DummyBinaryStoreDataSource;
import org.cacheonix.impl.cache.datastore.DummyDataStore;
import org.cacheonix.impl.cache.invalidator.DummyCacheInvalidator;
import org.cacheonix.impl.cache.loader.DummyCacheLoader;
import org.cacheonix.impl.clock.Time;
import org.cacheonix.impl.configuration.ElementEventNotification;
import org.cacheonix.impl.storage.disk.DummyDiskStorage;
import org.cacheonix.impl.util.cache.DummyObjectSizeCalculator;
import org.cacheonix.impl.util.logging.Logger;

/**
 * A tester for LocalCache handling expiration.
 * <p/>
 *
 * @author <a href="mailto:simeshev@cacheonix.com">Slava Imeshev</a>
 * @since May 26, 2008 10:40:58 PM
 */
public final class LocalCacheSynchronousExpirationTest extends CacheonixTestCase {

   /**
    * Logger.
    *
    * @noinspection UNUSED_SYMBOL, UnusedDeclaration
    */
   private static final Logger LOG = Logger.getLogger(LocalCacheSynchronousExpirationTest.class); // NOPMD

   private static final int EXPIRATION_TIME_MILLIS = 2000;

   private static final long SLEEP_TIME_TO_EXPIRE_MILLIS = (long) (EXPIRATION_TIME_MILLIS * 1.1);

   private static final int IDLE_TIME_MILLIS = 0;

   private static final int MAX_SIZE_M_BYTES = 0;

   private static final int MAX_SIZE = 2;

   private static final String TEST_CACHE = "test-cache";

   private static final String VALUE = "value";

   private static final String KEY = "key";

   private LocalCache<String, String> cache;


   public void testValues() throws InterruptedException {

      assertEquals(1, cache.values().size());
      Thread.sleep(SLEEP_TIME_TO_EXPIRE_MILLIS);
      assertEquals(0, cache.values().size());
   }


   @SuppressWarnings("PointlessArithmeticExpression")
   public void testGetExpirationTimeMillis() {

      assertEquals(new Time(EXPIRATION_TIME_MILLIS, 0), cache.getExpirationInterval());
   }


   public void testEntrySet() throws InterruptedException {

      assertEquals(1, cache.entrySet().size());
      Thread.sleep(SLEEP_TIME_TO_EXPIRE_MILLIS);
      assertEquals(0, cache.entrySet().size());
   }


   public void testKeySet() throws InterruptedException {

      assertEquals(1, cache.keySet().size());
      Thread.sleep(SLEEP_TIME_TO_EXPIRE_MILLIS);
      assertEquals(0, cache.keySet().size());
   }


   public void testNotifiesExpirationSubscriber() throws InterruptedException {

      // Subscribe to eviction notifications
      final SynchronousExpirationSubscriber subscriber = new SynchronousExpirationSubscriber();
      cache.addEventSubscriber(KEY, subscriber);

      // Evict
      Thread.sleep(SLEEP_TIME_TO_EXPIRE_MILLIS);
      cache.get(KEY);

      // Assert
      assertFalse(subscriber.getEvents().isEmpty());
   }


   public void testGet() throws InterruptedException {

      assertEquals(VALUE, cache.get(KEY));
      Thread.sleep(SLEEP_TIME_TO_EXPIRE_MILLIS);
      assertNull(cache.get(KEY));
   }


   /**
    * Sets up the fixture, for example, open a network connection. This method is called before a test is executed.
    */
   protected void setUp() throws Exception {

      super.setUp();

      cache = new LocalCache<String, String>(TEST_CACHE, MAX_SIZE, MAX_SIZE_M_BYTES, EXPIRATION_TIME_MILLIS,
              IDLE_TIME_MILLIS, getClock(), getEventNotificationExecutor(),
              new DummyDiskStorage(TEST_CACHE), new DummyObjectSizeCalculator(),
              new DummyBinaryStoreDataSource(), new DummyDataStore(),
              new DummyCacheInvalidator(), new DummyCacheLoader(), ElementEventNotification.SYNCHRONOUS);
      cache.put(KEY, VALUE);
   }


   public String toString() {

      return "LocalCacheSynchronousExpirationTest{" +
              "cache=" + cache +
              "} " + super.toString();
   }
}
