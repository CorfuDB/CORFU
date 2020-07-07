package org.corfudb.infrastructure.logreplication.infrastructure.plugins;

import org.corfudb.infrastructure.logreplication.infrastructure.CorfuReplicationDiscoveryServiceAdapter;
import org.corfudb.infrastructure.logreplication.proto.LogReplicationClusterInfo.TopologyConfigurationMsg;

/**
 * This is the interface for CorfuReplicationClusterManager.
 * Implementation of this should have following members:
 * 1. corfuReplicationDiscoveryService that is needed to notify the cluster configuration change.
 * 2. localEndpoint that has the local node information.
 *
 */
public interface CorfuReplicationClusterManagerAdapter {

    /**
     *   Register the discovery service and start the cluster manager callback service.
     *   Implementation of this function should call start(), start this ClusterManager at the upper
     *   layer to get the callback for topology change.
     */
     void register(CorfuReplicationDiscoveryServiceAdapter corfuReplicationDiscoveryService);

     /**
     * Set the localEndpoint
     */
    void setLocalEndpoint(String endpoint);


    /**
     * Query the topology information.
     * @param useCached if it is true, used the cached topology, otherwise do a query to get the most
     *                  recent topology from the real Cluster Manager.
     * @return
     */
    TopologyConfigurationMsg queryTopologyConfig(boolean useCached);

    // This is called when get a notification of cluster config change.
    void updateTopologyConfig(TopologyConfigurationMsg newClusterConfigMsg);

    // start to talk to the upper layer to get cluster topology information
    void start();

    // Stop the ClusterManager service
    void shutdown();


    /**
     * While doing a cluster role type flip, it is the API used to notify the current log
     * replication node to prepare a cluster role type change. It will do some
     * bookkeeping to calculate the number of log entries to be sent over
     *
     */
    void prepareToBecomeStandby();


    /**
     * This API is used to query the log replication status when it is preparing a role type flip and
     * the replicated tables should be in read-only mode.
     *
     * @return
     */
    int queryReplicationStatus();
}
