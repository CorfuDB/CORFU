package org.corfudb.infrastructure.logreplication.infrastructure;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.corfudb.infrastructure.logreplication.proto.LogReplicationClusterInfo.TopologyConfigurationMsg;
import org.corfudb.infrastructure.logreplication.proto.LogReplicationClusterInfo.ClusterRole;
import org.corfudb.infrastructure.logreplication.proto.LogReplicationClusterInfo.ClusterConfigurationMsg;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class represents a view of a Multi-Cluster Topology,
 *
 * Ideally, in a given topology, one cluster represents the active cluster (source of data)
 * while n others are standby clusters (backup's). However, because the topology info is provided by an
 * external adapter which can be specific to the use cases of the user, a topology might be initialized
 * with multiple active clusters and multiple standby clusters.
 *
 */
@Slf4j
public class TopologyDescriptor {

    // Represents a state of the topology configuration (a topology epoch)
    @Getter
    @Setter
    private long topologyConfigId;

    @Getter
    private Map<String, ClusterDescriptor> activeClusters;

    @Getter
    private Map<String, ClusterDescriptor> standbyClusters;

    /**
     * Constructor
     *
     * @param topologyMessage proto definition of the topology
     */
    public TopologyDescriptor(TopologyConfigurationMsg topologyMessage) {
        this.topologyConfigId = topologyMessage.getTopologyConfigID();
        this.standbyClusters = new HashMap<>();
        this.activeClusters = new HashMap<>();
        for (ClusterConfigurationMsg clusterConfig : topologyMessage.getClustersList()) {
            ClusterDescriptor cluster = new ClusterDescriptor(clusterConfig);
            if (clusterConfig.getRole() == ClusterRole.ACTIVE) {
                activeClusters.put(cluster.getClusterId(), cluster);
            } else if (clusterConfig.getRole() == ClusterRole.STANDBY) {
                addStandbyCluster(cluster);
            }
        }
    }

    /**
     * Constructor
     *
     * @param topologyConfigId topology configuration identifier (epoch)
     * @param activeCluster active cluster
     * @param standbyClusters standby cluster's
     */
    public TopologyDescriptor(long topologyConfigId, @NonNull ClusterDescriptor activeCluster,
                              @NonNull List<ClusterDescriptor> standbyClusters) {
        this(topologyConfigId, Arrays.asList(activeCluster), standbyClusters);
    }

    /**
     * Constructor
     *
     * @param topologyConfigId topology configuration identifier (epoch)
     * @param activeClusters active cluster's
     * @param standbyClusters standby cluster's
     */
    public TopologyDescriptor(long topologyConfigId, @NonNull List<ClusterDescriptor> activeClusters,
                              @NonNull List<ClusterDescriptor> standbyClusters) {
        this.topologyConfigId = topologyConfigId;
        this.activeClusters = new HashMap<>();
        this.standbyClusters = new HashMap<>();

        if(activeClusters != null) {
            activeClusters.forEach(activeCluster -> this.activeClusters.put(activeCluster.getClusterId(), activeCluster));
        }

        if(standbyClusters != null) {
            standbyClusters.forEach(activeCluster -> this.standbyClusters.put(activeCluster.getClusterId(), activeCluster));
        }
    }

    /**
     * Convert Topology Descriptor to ProtoBuf Definition
     *
     * @return topology protoBuf
     */
    public TopologyConfigurationMsg convertToMessage() {

        List<ClusterConfigurationMsg> clusterConfigurationMsgs = Stream.of(activeClusters.values(), standbyClusters.values())
                .flatMap(x -> x.stream())
                .map(cluster -> cluster.convertToMessage())
                .collect(Collectors.toList());

        TopologyConfigurationMsg topologyConfig = TopologyConfigurationMsg.newBuilder()
                .setTopologyConfigID(topologyConfigId)
                .addAllClusters(clusterConfigurationMsgs).build();

        return topologyConfig;
    }

    /**
     * Add a standby cluster to the current topology
     *
     * @param cluster standby cluster to add
     */
    public void addStandbyCluster(ClusterDescriptor cluster) {
        standbyClusters.put(cluster.getClusterId(), cluster);
    }

    /**
     * Remove a standby cluster from the current topology
     *
     * @param clusterId unique identifier of the standby cluster to be removed from topology
     */
    public void removeStandbyCluster(String clusterId) {
        ClusterDescriptor removedCluster = standbyClusters.remove(clusterId);

        if (removedCluster == null) {
            log.warn("Cluster {} never present as a STANDBY cluster.", clusterId);
        }
    }

    /**
     * Get the Cluster Descriptor to which a given endpoint belongs to.
     *
     * @param endpoint
     * @return cluster descriptor to which endpoint belongs to.
     */
    public ClusterDescriptor getClusterDescriptor(String endpoint) {
        List<ClusterDescriptor> clusters = Stream.of(activeClusters.values(), standbyClusters.values())
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());

        for(ClusterDescriptor cluster : clusters) {
            for (NodeDescriptor node : cluster.getNodesDescriptors()) {
                if (node.getEndpoint().equals(endpoint)) {
                    return cluster;
                }
            }
        }

        log.warn("Endpoint {} does not belong to any cluster defined in {}", clusters);
        return null;
    }

    @Override
    public String toString() {
        return String.format("Topology[%s] :: Active Cluster=%s :: Standby Clusters=%s", topologyConfigId, activeClusters, standbyClusters);
    }
}