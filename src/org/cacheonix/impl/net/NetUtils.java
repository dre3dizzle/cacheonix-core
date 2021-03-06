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
package org.cacheonix.impl.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import org.cacheonix.CacheonixException;
import org.cacheonix.impl.util.array.HashSet;

import static java.util.Collections.unmodifiableSet;

/**
 * Utility methods to support network operations.
 */
public final class NetUtils {

   private NetUtils() {

   }


   /**
    * Returns a list of local addresses on this machine.
    *
    * @return a list of local addresses on this machine.
    * @throws CacheonixException if there is problem getting local address
    * @see InetAddress
    */
   public static Set<InetAddress> getLocalInetAddresses() throws CacheonixException {

      try {

         final Enumeration<NetworkInterface> netIfs = NetworkInterface.getNetworkInterfaces();
         final Set<InetAddress> result = new HashSet<InetAddress>(11);
         while (netIfs.hasMoreElements()) {
            final NetworkInterface netIf = netIfs.nextElement();
            final Enumeration<InetAddress> address = netIf.getInetAddresses();
            while (address.hasMoreElements()) {
               result.add(address.nextElement());
            }
         }
         return unmodifiableSet(result);
      } catch (final SocketException e) {

         throw new CacheonixException("Failed to obtain a list of local InetAddresses: " + e, e);
      }
   }


   /**
    * Converts cluster node address to a list.
    *
    * @param address to covert
    * @return a new list that contains a single address.
    */
   public static List<ClusterNodeAddress> addressToList(final ClusterNodeAddress address) {

      final List<ClusterNodeAddress> result = new ArrayList<ClusterNodeAddress>(1);
      result.add(address);
      return result;
   }
}
