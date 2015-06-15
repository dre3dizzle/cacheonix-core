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

import org.cacheonix.Cacheonix;
import org.cacheonix.ShutdownException;
import org.cacheonix.ShutdownMode;
import org.cacheonix.TestUtils;
import org.cacheonix.cache.Cache;
import org.cacheonix.locks.Lock;
import org.cacheonix.impl.util.MutableBoolean;

/**
 */
public abstract class SinglePartitionedCacheTestDriver extends PartitionedCacheTestDriver {

   private final String configuration;

   private Cacheonix cacheonix;

   private Cache<String, String> cache;


   @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
   protected SinglePartitionedCacheTestDriver(final String configuration) {

      this.configuration = configuration;
   }


   protected Cache<String, String> cache() {

      return cache;
   }


   public void testWaitingForLockThrowsExceptionOnShutdown() throws InterruptedException {

      final Cache<String, String> cache0 = cache();

      final Lock writeLock = cache0.getReadWriteLock().writeLock();
      final MutableBoolean thrown = new MutableBoolean();
      writeLock.lock();
      try {


         final Thread thread = new Thread(new Runnable() {

            public void run() {

               try {
                  cache0.getReadWriteLock().writeLock().lock();
               } catch (final ShutdownException ignored) {
                  thrown.set(true);
               }
            }
         });
         thread.start();

         Thread.sleep(100L);
         cacheonix().shutdown(ShutdownMode.GRACEFUL_SHUTDOWN, true);
         thread.join();

      } finally {
         writeLock.unlock();
      }

      assertTrue(thrown);
   }


   protected Cacheonix cacheonix() {

      return cacheonix;
   }


   public void setUp() throws Exception {

      super.setUp();

      final String configurationPath = TestUtils.getTestFile(configuration).toString();
      cacheonix = Cacheonix.getInstance(configurationPath);
      cache = cacheonix.getCache(DISTRIBUTED_CACHE_NAME);
      assertNotNull("Cache should be not null", cache);
   }


   public void tearDown() throws Exception {

      cacheonix.shutdown(ShutdownMode.GRACEFUL_SHUTDOWN, true);

      super.tearDown();
   }
}
