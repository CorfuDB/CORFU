package org.corfudb.infrastructure.management;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.corfudb.protocols.wireprotocol.ClusterState;
import org.corfudb.runtime.exceptions.WrongEpochException;
import org.corfudb.runtime.view.Layout;

import java.util.List;
import java.util.Set;

/**
 * Poll Report generated by the detectors that poll to detect failed or healed nodes.
 * This is consumed and analyzed by the Management Server to detect changes in the cluster and
 * take appropriate action.
 * Created by zlokhandwala on 3/21/17.
 */
@Data
@Builder
@Slf4j
public class PollReport {

    @Default
    private final long pollEpoch = Layout.INVALID_EPOCH;

    /**
     * A map of nodes answered with {@link WrongEpochException}.
     */
    @Default
    private final ImmutableMap<String, Long> wrongEpochs = ImmutableMap.of();

    /**
     * Current cluster state, collected by a failure detector
     */
    @NonNull
    private final ClusterState clusterState;

    /**
     * List of responsive servers in a current layout
     */
    @NonNull
    private final List<String> responsiveServers;

    /**
     * Returns all connected nodes to the current node including nodes with higher epoch.
     *
     * @return set of all reachable nodes
     */
    public ImmutableSet<String> getAllReachableNodes() {
        return Sets.union(getReachableNodes(), wrongEpochs.keySet()).immutableCopy();
    }

    /**
     * Contains list of servers successfully connected with current node.
     * It doesn't contain nodes answered with WrongEpochException.
     */
    public Set<String> getReachableNodes() {
        Set<String> connectedNodes =  clusterState.getLocalNodeConnectivity().getConnectedNodes();
        return Sets.difference(connectedNodes, wrongEpochs.keySet());
    }

    /**
     * Contains list of servers disconnected from this node. If the node A can't ping node B then node B will be added
     * to failedNodes list.
     */
    public Set<String> getFailedNodes() {
        Set<String> failedNodes = clusterState.getLocalNodeConnectivity().getFailedNodes();

        return Sets.difference(failedNodes, wrongEpochs.keySet());
    }

    /**
     * All active Layout servers have been sealed but there is no client to take this forward and
     * fill the slot by proposing a new layout. This is determined by the outOfPhaseEpochNodes map.
     *
     * @return True if latest layout slot is vacant.
     */
    public boolean isCurrentLayoutSlotUnFilled() {
        log.trace("isCurrentLayoutSlotUnFilled. Wrong epochs: {}, active servers: {}",
                wrongEpochs, responsiveServers
        );

        // Check if all active layout servers are present in the outOfPhaseEpochNodes map.
        boolean result = wrongEpochs.keySet().containsAll(responsiveServers);

        if (result) {
            log.info("Current layout slot is empty. Filling slot with current layout. " +
                    "Wrong epochs: {}, active layout servers: {}", wrongEpochs, responsiveServers);
        }
        return result;
    }
}
