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
package org.cacheonix.impl.net.processor;

import org.cacheonix.ShutdownException;
import org.cacheonix.impl.util.Assert;
import org.cacheonix.impl.util.Shutdownable;
import org.cacheonix.impl.util.logging.Logger;
import org.cacheonix.impl.util.thread.UserThreadFactory;

/**
 * Abstract command processor.
 */
public abstract class Processor implements Shutdownable {

   /**
    * Logger.
    *
    * @noinspection UNUSED_SYMBOL, UnusedDeclaration
    */
   private static final Logger LOG = Logger.getLogger(Processor.class); // NOPMD


   private static final long DEFAULT_PROCESSOR_SHUTDOWN_TIMEOUT_MILLIS = 10000L;

   /**
    * Exec thread.
    */
   protected Thread thread;

   /**
    * Time the processor started.
    */
   protected long startTimeMillis = 0L;

   /**
    * The processor name.
    */
   private final String threadFactoryName;


   /**
    * Creates a processor.
    *
    * @param threadFactoryName a processor's thread factory name. It is used to create a name of the executor thread.
    */
   public Processor(final String threadFactoryName) {

      this.threadFactoryName = threadFactoryName;
   }


   /**
    * Enqueues the command for execution by this processor.
    *
    * @param command the command to enqueue.
    * @throws InterruptedException if the caller thread has been interrupted.
    * @throws ShutdownException    if the processor is shutdown.
    */
   public abstract void enqueue(Command command) throws InterruptedException, ShutdownException;


   /**
    * Starts
    */
   public void startup() {

      // Set start time
      startTimeMillis = System.currentTimeMillis();

      // Create worker object
      final Runnable worker = createWorker();

      // Create worker thread
      thread = new UserThreadFactory(threadFactoryName).newThread(worker);

      // Start thread
      thread.start();
   }


   /**
    * Shuts down this processor by sending interrupt to its worker thread. This method must be called before discarding
    * this instance of the Processor. Failure to call shutdown may lead to resources such as open files left hanging
    * around.
    */
   public void shutdown() {

      validateStarted();

      // ... Close input queue for puts

      // ... Drain input queue

      // Terminate exec thread
      thread.interrupt();
      try {

         // Init shutdown watch
         final long shutdownWaitStarted = System.currentTimeMillis();

         // Wait for the thread to finish
         thread.join(DEFAULT_PROCESSOR_SHUTDOWN_TIMEOUT_MILLIS);

         // Calculate time to shutdown
         final long shutdownWaitDuration = System.currentTimeMillis() - shutdownWaitStarted;

         // Print result
         if (thread.isAlive()) {
            warnHasNotStoppedAfter(shutdownWaitDuration);
         } else {
            debugShutdownTime(shutdownWaitDuration);
         }
      } catch (final InterruptedException e) {
         Thread.currentThread().interrupt();
      }
   }


   /**
    * Returns processor name.
    *
    * @return the processor name.
    */
   final String getProcessorName() {

      validateStarted();

      return thread.getName();
   }


   /**
    * Returns <code>true</code> if the current thread is a processor thread. Returns <code>false</code> if the current
    * thread is not a processor thread.
    *
    * @return <code>true</code> if the current thread is a processor thread. <code>false</code> if the current thread is
    *         not a processor thread.
    */
   public final boolean isProcessorThread() {

      validateStarted();

      //noinspection ObjectEquality
      return Thread.currentThread() == thread;
   }


   /**
    * Returns <code>true</code> if the connection was closed.
    *
    * @return <code>true</code> if the connection was closed.
    */
   public boolean isShutdown() {

      validateStarted();

      return !thread.isAlive();
   }


   /**
    * Returns <code>true</code> if the processor is alive and ready to process commands. Returns <code>false</code> if
    * the processor has not been started yet or if the processor has been shutdown.
    *
    * @return <code>true</code> if the processor is alive and ready to process commands. Returns <code>false</code> if
    *         the processor has not been started yet or if the processor has been shutdown.
    */
   public boolean isAlive() {

      validateStarted();

      return thread.isAlive();
   }


   /**
    * Waits for this processor to shutdown.
    *
    * @param timeoutMillis time to wait.
    * @return <code>true</code> if the processor was shutdown when this method returned control to the caller.
    */
   public boolean waitForShutdown(final long timeoutMillis) {

      validateStarted();

      try {

         Assert.assertFalse(isProcessorThread(), "This method cannot be called from the processor thread because it blocks");
         thread.join(timeoutMillis);
      } catch (final InterruptedException e) {
         Thread.currentThread().interrupt();
      }
      return isShutdown();
   }


   /**
    * Creates a runnable executed by the worker thread.
    *
    * @return Runnable
    */
   protected abstract Runnable createWorker();


   /**
    * Prints a warning that a given processor has not been shutdown after a given timeout.
    *
    * @param shutdownWaitDuration the timeout.
    */
   private void warnHasNotStoppedAfter(final long shutdownWaitDuration) {

      LOG.warn(getProcessorName() + " was shutdown but the processor thread did not stop in " + shutdownWaitDuration + " millis after shutdown was initiated");
   }


   private void debugShutdownTime(final long timeToShutdown) {

      LOG.debug(getProcessorName() + " was successfully shutdown. Time to shutdown was " + timeToShutdown + " millis");
   }


   /**
    * Validates the processor has started.
    */
   private void validateStarted() {

      if (thread == null) {
         throw new IllegalStateException("Processor has not started yet");
      }
   }


   public String toString() {

      return "Processor{" +
              "thread=" + thread +
              ", startTimeMillis=" + startTimeMillis +
              '}';
   }
}
