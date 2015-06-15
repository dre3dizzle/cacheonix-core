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
package org.cacheonix.impl.net.multicast.sender;

import java.io.IOException;

import org.cacheonix.impl.net.processor.Frame;
import org.cacheonix.impl.net.processor.Router;

/**
 * Low-level multicast message sender that takes in account multiple interfaces.
 * <p/>
 *
 * @author <a href="mailto:simeshev@cacheonix.com">Slava Imeshev</a>
 * @since Mar 28, 2008 4:04:18 PM
 */
public interface MulticastSender {

   /**
    * Sends message.
    *
    * @param frame
    * @throws IOException
    */
   void sendFrame(final Frame frame) throws IOException;

   /**
    * Sets the router. The router must be set before the sender can be used.
    *
    * @param router the router to set.
    */
   void setRouter(final Router router);
}
