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
package org.cacheonix.impl.cluster;

import java.util.ArrayList;
import java.util.Collection;

import org.cacheonix.cluster.ClusterConfiguration;
import org.cacheonix.cluster.ClusterMember;
import org.cacheonix.cluster.ClusterMemberLeftEvent;

/**
 * An implementation of {@link ClusterMemberLeftEvent}.
 */
public final class ClusterMemberLeftEventImpl implements ClusterMemberLeftEvent {

   /**
    * A collection of left members.
    */
   private final Collection<ClusterMember> leftMembers;

   /**
    * A cluster configuration after the members left.
    */
   private final ClusterConfiguration clusterConfiguration;


   /**
    * Creates ClusterMemberLeftEventImpl.
    *
    * @param clusterConfiguration a cluster configuration after the members left.
    * @param leftMembers          a list of left members.
    */
   public ClusterMemberLeftEventImpl(final ClusterConfiguration clusterConfiguration,
           final Collection<ClusterMember> leftMembers) {

      this.leftMembers = new ArrayList<ClusterMember>(leftMembers);
      this.clusterConfiguration = clusterConfiguration;
   }


   /**
    * Returns the cluster configuration after the members left.
    *
    * @return the cluster configuration after the members left.
    */
   public ClusterConfiguration getClusterConfiguration() {

      return clusterConfiguration;
   }


   /**
    * Returns an unmodifiable collection containing ClusterMembers that left the cluster.
    *
    * @return the unmodifiable collection containing ClusterMembers that left the cluster.
    */
   public Collection<ClusterMember> getLeftMembers() {

      return new ArrayList<ClusterMember>(leftMembers);
   }


   public String toString() {

      return "ClusterMemberLeftEventImpl{" +
              "leftMembers=" + leftMembers +
              '}';
   }
}
