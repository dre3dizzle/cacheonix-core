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
package org.cacheonix.impl.cache.distributed.partitioned;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.cacheonix.impl.cluster.node.state.ReplicatedState;
import org.cacheonix.impl.cluster.node.state.group.Group;
import org.cacheonix.impl.net.ClusterNodeAddress;
import org.cacheonix.impl.net.cluster.ClusterProcessor;
import org.cacheonix.impl.net.cluster.ClusterRequest;
import org.cacheonix.impl.net.processor.Response;
import org.cacheonix.impl.net.processor.SimpleWaiter;
import org.cacheonix.impl.net.processor.Waiter;
import org.cacheonix.impl.net.serializer.SerializerUtils;
import org.cacheonix.impl.net.serializer.Wireable;
import org.cacheonix.impl.net.serializer.WireableBuilder;
import org.cacheonix.impl.util.logging.Logger;

/**
 * A request to get key owners.
 */
public class GetKeyOwnerRequest extends ClusterRequest {

   /**
    * Logger.
    *
    * @noinspection UNUSED_SYMBOL, UnusedDeclaration
    */
   private static final Logger LOG = Logger.getLogger(GetKeyOwnerRequest.class); // NOPMD

   /**
    * Builder used by WireableFactory.
    */
   public static final WireableBuilder BUILDER = new Builder();

   private static final String CLUSTER_IS_RECONFIGURING = "Cluster is reconfiguring";

   private static final String CLUSTER_IS_BLOCKED = "Cluster is blocked";

   private String cacheName = null;

   private int storageNumber = 0;

   private int bucketNumber = 0;


   /**
    * Required by wireable.
    */
   @SuppressWarnings("UnusedDeclaration")
   public GetKeyOwnerRequest() {

   }


   public GetKeyOwnerRequest(final String cacheName, final int storageNumber, final int bucketNumber) {

      super(TYPE_CLUSTER_KEY_OWNER);
      this.cacheName = cacheName;
      this.storageNumber = storageNumber;
      this.bucketNumber = bucketNumber;
   }


   public String getCacheName() {

      return cacheName;
   }


   public int getStorageNumber() {

      return storageNumber;
   }


   public int getBucketNumber() {

      return bucketNumber;
   }


   protected void processNormal() throws IOException, InterruptedException {

      final ClusterProcessor processor = getClusterProcessor();

      final ReplicatedState state = processor.getProcessorState().getReplicatedState();
      final Group group = state.getGroup(Group.GROUP_TYPE_CACHE, cacheName);
      if (group == null) {
         postRetry("Cache " + cacheName + " is offline");
      } else {
         final Response response = createResponse(Response.RESULT_SUCCESS);
         final ClusterNodeAddress bucketOwner = group.getBucketOwner(storageNumber, bucketNumber);
         if (bucketOwner == null) {

            // NOTE: simeshev@cacheonix.org - 2016-02-16 - it's possible that RBOAT is empty
            // when the cluster node hasn't processes the group join yet, so we return retry.
            //
            // REVIEWME: simeshev@cacheonix.org -> 2016-03-01 - consider a a design where an
            // empty RBOAT is impossible such as instead of shared access to RBOAT from the
            // ClusterProcessor and CacheProcessor a CacheProcessor would receive RBOAT as
            // a first [replicated] message.
            processor.post(createResponse(Response.RESULT_RETRY));
         } else {

            response.setResult(bucketOwner);
            processor.post(response);
         }
      }
   }


   protected void processBlocked() throws IOException, InterruptedException {

      postRetry(CLUSTER_IS_BLOCKED);
   }


   protected void processRecovery() {

      postRetry(CLUSTER_IS_RECONFIGURING);
   }


   protected void processCleanup() throws IOException {

      postRetry(CLUSTER_IS_RECONFIGURING);
   }


   private void postRetry(final String message) {

      final Response response = createResponse(Response.RESULT_RETRY);
      response.setResult(message);
      getProcessor().post(response);
   }


   public void writeWire(final DataOutputStream out) throws IOException {

      super.writeWire(out);
      SerializerUtils.writeString(cacheName, out);
      out.writeInt(storageNumber);
      out.writeShort(bucketNumber);
   }


   public void readWire(final DataInputStream in) throws IOException, ClassNotFoundException {

      super.readWire(in);
      cacheName = SerializerUtils.readString(in);
      storageNumber = in.readInt();
      bucketNumber = in.readShort();
   }


   protected Waiter createWaiter() {

      return new SimpleWaiter(this);
   }


   public String toString() {

      return "GetKeyOwnersRequest{" +
              "cacheName='" + cacheName + '\'' +
              "} " + super.toString();
   }


   /**
    * A class factory.
    */
   private static final class Builder implements WireableBuilder {

      public Wireable create() {

         return new GetKeyOwnerRequest();
      }
   }
}