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
package org.cacheonix.impl.cache.distributed.partitioned;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;

import org.cacheonix.impl.net.processor.InvalidMessageException;
import org.cacheonix.impl.util.Assert;
import org.cacheonix.impl.util.AssertionException;

/**
 * A CacheRequest that can only be sent to a local CacheProcessor. It cannot be sent over the network.
 */
public abstract class LocalCacheRequest extends CacheRequest {

   public LocalCacheRequest(final int wireableType, final String cacheName) {

      super(wireableType, cacheName);
   }


   public LocalCacheRequest() {

   }


   /**
    * {@inheritDoc}
    *
    * @throws InvalidMessageException
    */
   public void validate() throws InvalidMessageException {

      super.validate();

      try {

         Assert.assertTrue(getReceiver().isAddressOf(getSender()), "Sender and receiver should be the same, sender: {0}, receiver: {1}");
      } catch (final AssertionException e) {

         throw new InvalidMessageException(e);
      }
   }


   public final void readWire(final DataInputStream in) throws IOException {

      throw new NotSerializableException(this.getClass() + " is a strictly local object");
   }


   public final void writeWire(final DataOutputStream out) throws IOException {

      throw new NotSerializableException(this.getClass() + " is a strictly local object");
   }
}
