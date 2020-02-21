package org.corfudb.logreplication.fsm;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.corfudb.logreplication.DataControl;

/**
 * This class represents the InRequireSnapshotSync state of the Log Replication State Machine.
 *
 * This state is entered after a cancel or error (trim), and awaits for a signal to start snapshot sync again.
 */
@Slf4j
public class InRequireSnapshotSyncState implements LogReplicationState {

    /*
     * Log Replication Finite State Machine Instance
     */
    private final LogReplicationFSM fsm;

    /*
     * Data Control Implementation
     */
    private final DataControl dataControl;

    public InRequireSnapshotSyncState(@NonNull LogReplicationFSM logReplicationFSM, @NonNull DataControl dataControl) {
        this.fsm = logReplicationFSM;
        this.dataControl = dataControl;
    }

    @Override
    public LogReplicationState processEvent(LogReplicationEvent event) throws IllegalTransitionException {
        switch (event.getType()) {
            case SNAPSHOT_SYNC_REQUEST:
                LogReplicationState snapshotSyncState = fsm.getStates().get(LogReplicationStateType.IN_SNAPSHOT_SYNC);
                snapshotSyncState.setTransitionEventId(event.getEventID());
                return snapshotSyncState;
            case REPLICATION_STOP:
                return fsm.getStates().get(LogReplicationStateType.INITIALIZED);
            case REPLICATION_SHUTDOWN:
                return fsm.getStates().get(LogReplicationStateType.STOPPED);
            default: {
                log.warn("Unexpected log replication event {} when in require snapshot send state.", event.getType());
                throw new IllegalTransitionException(event.getType(), getType());
            }
        }
    }

    @Override
    public void onEntry(LogReplicationState from) {
        // TODO: since a SNAPSHOT_SYNC_REQUEST is the only event that can take us out of this state,
        //  we need a scheduler to re-notify the remote site of the error, in case the request was lost.
        dataControl.requestSnapshotSync();
    }

    @Override
    public LogReplicationStateType getType() {
        return LogReplicationStateType.IN_REQUIRE_SNAPSHOT_SYNC;
    }
}
