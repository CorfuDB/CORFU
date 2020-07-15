package org.corfudb.infrastructure.logreplication.replication.receive;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.corfudb.infrastructure.logreplication.proto.LogReplicationMetadata;
import org.corfudb.infrastructure.logreplication.proto.LogReplicationMetadata.LogReplicationMetadataKey;
import org.corfudb.infrastructure.logreplication.proto.LogReplicationMetadata.LogReplicationMetadataVal;
import org.corfudb.protocols.wireprotocol.logreplication.LogReplicationEntry;
import org.corfudb.runtime.CorfuRuntime;
import org.corfudb.runtime.CorfuStoreMetadata.Timestamp;
import org.corfudb.runtime.collections.CorfuRecord;
import org.corfudb.runtime.collections.CorfuStore;
import org.corfudb.runtime.collections.Table;
import org.corfudb.runtime.collections.TableOptions;
import org.corfudb.runtime.collections.TxBuilder;
import org.corfudb.runtime.exceptions.TransactionAbortedException;
import org.corfudb.runtime.view.Address;

/**
 * The class manages the metadata table persisted at the replication cluster.
 * It records the cluster's topologyConfigId, version, and log replication status.
 * For now, this table has only one entry with key as "CORFU-REPLICATION-CURRENT-STATUS",
 * value is the LogReplicationMetadataVal defined in the log_replication_metadata.proto.
 *
 * The log replication status (used by standby side) contains following information:
 * 1. full snapshot transfer status: base snapshot timestamp, transfer phase timestamp, apply phase timestamp, and the snapshot
 *    message's sequence number has been received and has been applied.
 * 2. delta sync status: the last log entry timestamp has been applied.
 *
 */
@Slf4j
public class LogReplicationMetadataManager {

    private static final String namespace = "CORFU_SYSTEM";
    private static final String TABLE_PREFIX_NAME = "CORFU-REPLICATION-WRITER-";
    private static final String CURRENT_METADATA_KEY = "CORFU-REPLICATION-CURRENT-STATUS";
    private CorfuStore corfuStore;

    private LogReplicationMetadataKey currentMetadataKey = LogReplicationMetadataKey.newBuilder().setKey(CURRENT_METADATA_KEY).build();

    private String metadataTableName;

    private Table<LogReplicationMetadataKey, LogReplicationMetadataVal, LogReplicationMetadataKey> metadataTable;

    private CorfuRuntime runtime;

    public LogReplicationMetadataManager(CorfuRuntime rt, long topologyConfigId, String localClusterId) {
        this.runtime = rt;
        this.corfuStore = new CorfuStore(runtime);
        metadataTableName = getPersistedWriterMetadataTableName(localClusterId);
        try {
            metadataTable = this.corfuStore.openTable(namespace,
                    metadataTableName,
                    LogReplicationMetadata.LogReplicationMetadataKey.class,
                    LogReplicationMetadata.LogReplicationMetadataVal.class,
                    null,
                    TableOptions.builder().build());
        } catch (Exception e) {
            log.error("Caught an exception while opening the table namespace={}, name={}", namespace, metadataTableName);
            throw new ReplicationWriterException(e);
        }
        setupTopologyConfigId(topologyConfigId);
    }

    /**
     * Get the latest logical timestamp (global tail) in Corfu's distributed log.
     * @return
     */
    public Timestamp getTimestamp() {
        return corfuStore.getTimestamp();
    }

    /**
     * Get a txBuilder.
     * @return
     */
    public TxBuilder getTxBuilder() {
        return corfuStore.tx(namespace);
    }

    /**
     * Given a metadataName, return its value at the persisted corfu table.
     * This is used for metadata that has int64 defined in the proto.
     * @param metadataName
     * @return
     */
    public long query(LogReplicationMetadataName metadataName) {
        CorfuRecord<LogReplicationMetadataVal, LogReplicationMetadataKey> record = metadataTable.get(currentMetadataKey);

        if (record == null) {
            log.warn(" The log replication metadata doesn't exist at corefuTable, the operation could not succeed.");
            return Address.NON_ADDRESS;
        }

        switch (metadataName) {
            case TOPOLOGY_CONFIG_ID:
                return record.getPayload().getTopologyConfigId();

            case LAST_SNAPSHOT_STARTED:
                return record.getPayload().getSnapshotStartTimestamp();

            case LAST_SNAPSHOT_TRANSFERRED:
                return record.getPayload().getSnapshotTransferredTimestamp();

            case LAST_SNAPSHOT_APPLIED:
                return record.getPayload().getSnapshotAppliedTimestamp();

            case LAST_SNAPSHOT_MSG_RECEIVED_SEQ_NUM:
                return record.getPayload().getSnapshotMessageReceivedSeqNum();

            case LAST_SNAPSHOT_MSG_APPLIED_SEQ_NUM:
                return record.getPayload().getSnapshotMessageAppliedSeqNum();

            case LAST_LOG_PROCESSED:
                return record.getPayload().getLastLogEntryProcessedTimestamp();

            default:
                log.warn("there is no metadata name for {}", metadataName);
                return Address.NON_ADDRESS;
        }
    }


    public LogReplicationMetadataVal queryPersistedMetadata() {
        CorfuRecord<LogReplicationMetadataVal, LogReplicationMetadataKey> record = metadataTable.get(currentMetadataKey);

        if (record == null) {
            log.warn(" The log replication metadata doesn't exist at corefuTable, the operation could not succeed.");
            return LogReplicationMetadataVal.newBuilder().build();
        }

        return record.getPayload();
    }

    /**
     * This API is used to append the metadata update to the transaction that updates the shadow stream or real stream.
     *
     * @param txBuilder
     * @param metadataName
     * @param val
     */
    public void appendUpdate(TxBuilder txBuilder, LogReplicationMetadataName metadataName, long val) {
        CorfuRecord<LogReplicationMetadataVal, LogReplicationMetadataKey> record = metadataTable.get(currentMetadataKey);
        LogReplicationMetadataVal metadataVal;

        if (record == null) {
            log.warn(" The log replication metadata doesn't exist at corefuTable, the operation could not succeed.");
            return;
        }

        switch (metadataName) {
            case TOPOLOGY_CONFIG_ID:
                metadataVal = LogReplicationMetadataVal.newBuilder(record.getPayload()).setTopologyConfigId(val).build();
                break;

            case LAST_SNAPSHOT_STARTED:
                metadataVal = LogReplicationMetadataVal.newBuilder(record.getPayload()).
                        setSnapshotStartTimestamp(val).
                        setSnapshotMessageReceivedSeqNum(Address.NON_ADDRESS).
                        setSnapshotMessageAppliedSeqNum(Address.NON_ADDRESS).
                        build();
                break;

            case LAST_SNAPSHOT_TRANSFERRED:
                metadataVal = LogReplicationMetadataVal.newBuilder(record.getPayload()).setSnapshotTransferredTimestamp(val).build();
                break;

            case LAST_SNAPSHOT_APPLIED:
                metadataVal = LogReplicationMetadataVal.newBuilder(record.getPayload()).setSnapshotAppliedTimestamp(val).build();
                break;

            case LAST_SNAPSHOT_MSG_RECEIVED_SEQ_NUM:
                metadataVal = LogReplicationMetadataVal.newBuilder(record.getPayload()).setSnapshotMessageReceivedSeqNum(val).build();
                break;

            case LAST_SNAPSHOT_MSG_APPLIED_SEQ_NUM:
                metadataVal = LogReplicationMetadataVal.newBuilder(record.getPayload()).setSnapshotMessageAppliedSeqNum(val).build();
                break;

            case LAST_LOG_PROCESSED:
                metadataVal = LogReplicationMetadataVal.newBuilder(record.getPayload()).setLastLogEntryProcessedTimestamp(val).build();
                break;

            default:
                log.warn("there is no metadata name for {}", metadataName);
                return;
        }

        txBuilder.update(metadataTableName, currentMetadataKey, metadataVal, null);
    }

    /**
     * Given topologyConfigId and version number, build a metadataVal with log replication status with init values.
     *
     * @param topologyConfigId
     * @param version
     * @return
     */
    private LogReplicationMetadataVal buildMetadataValInstance(long topologyConfigId, String version) {
            LogReplicationMetadataVal metadataVal = LogReplicationMetadataVal.newBuilder().
                setTopologyConfigId(topologyConfigId).
                setVersion(version).
                setSnapshotStartTimestamp(Address.NON_ADDRESS).
                setSnapshotTransferredTimestamp(Address.NON_ADDRESS).
                setSnapshotAppliedTimestamp(Address.NON_ADDRESS).
                setSnapshotMessageReceivedSeqNum(Address.NON_ADDRESS).
                setSnapshotMessageAppliedSeqNum(Address.NON_ADDRESS).
                setLastLogEntryProcessedTimestamp(Address.NON_ADDRESS).build();
            return metadataVal;
    }

    /**
     * Persist the new metadata to the corfu table.
     * @param timestamp
     * @param metadata
     */
    private void persistMetadata (Timestamp timestamp, LogReplicationMetadataVal metadata) {
        TxBuilder txBuilder = corfuStore.tx(namespace);

        txBuilder.update(metadataTableName, currentMetadataKey, metadata, null);

        txBuilder.commit(timestamp);

        log.debug("Updated metadata with new metadata {}", metadata);
    }

    /**
     * Update the topologyConfigId with a transaction.
     * It will set the log replications status with init values.
     * @param topologyConfigId
     */
    public void setupTopologyConfigId(long topologyConfigId) {
        Timestamp timestamp = corfuStore.getTimestamp();

        CorfuRecord<LogReplicationMetadataVal, LogReplicationMetadataKey> record = metadataTable.get(currentMetadataKey);
        long persistedTopologyConfigId = Address.NON_ADDRESS;
        String persistedVersion = "NO VERSION";

        if (record != null) {
            persistedTopologyConfigId = record.getPayload().getTopologyConfigId();
            persistedVersion = record.getPayload().getVersion();
        }

        if (topologyConfigId <= persistedTopologyConfigId) {
            log.warn("Skip setupTopologyConfigId. the current topologyConfigId {} is smaller than the persistedTopologyConfigID {}.",
                    topologyConfigId, persistedTopologyConfigId);
            return;
        }


        LogReplicationMetadataVal metadataVal = buildMetadataValInstance(topologyConfigId, persistedVersion);

        try {
            persistMetadata(timestamp, metadataVal);
        } catch (TransactionAbortedException e) {
            log.warn("Transaction about when updating with the new topologyConfigId {} ", topologyConfigId, e);
        }
    }

    /**
     * If the current topologyConfigId is not the same as the persisted topologyConfigId, ignore the operation.
     * If the current ts is smaller than the persisted snapStart, it is an old operation, ignore the operation.
     * Otherwise, update the snapStart.
     *
     * @param topologyConfigId the current operation's topologyConfigId
     * @param ts the snapshotStart snapshot time for the topologyConfigId.
     * @return if the operation succeeds or not.
     */
    public boolean setSrcBaseSnapshotStart(long topologyConfigId, long ts) {
        Timestamp timestamp = corfuStore.getTimestamp();
        CorfuRecord<LogReplicationMetadataVal, LogReplicationMetadataKey> record = metadataTable.get(currentMetadataKey);

        if (record == null) {
            log.warn(" The log replication metadata doesn't exist at corefuTable, the operation could not succeed.");
            return false;
        }

        long persistedTopologyConfigID = record.getPayload().getTopologyConfigId();
        long persistedSnapStart = record.getPayload().getSnapshotStartTimestamp();

        log.debug("Set snapshotStart topologyConfigId={}, ts={}, persistedTopologyConfigID={}, persistedSnapshotStart={}",
                topologyConfigId, ts, persistedTopologyConfigID, persistedSnapStart);

        // It means the cluster config has changed, ignore the update operation.
        if (topologyConfigId != persistedTopologyConfigID || ts < persistedSnapStart) {
            log.warn("The metadata is older than the persisted one. Set snapshotStart topologyConfigId={}, ts={}," +
                    " persistedTopologyConfigId={}, persistedSnapshotStart={}", topologyConfigId, ts,
                    persistedTopologyConfigID, persistedSnapStart);
            return false;
        }

        LogReplicationMetadataVal metadataVal = LogReplicationMetadataVal.newBuilder(record.getPayload()).
                setSnapshotStartTimestamp(ts).
                setSnapshotMessageReceivedSeqNum(Address.NON_ADDRESS).
                setSnapshotMessageAppliedSeqNum(Address.NON_ADDRESS).
                build();

        try {
            persistMetadata(timestamp, metadataVal);
            return true;
        } catch (TransactionAbortedException e) {
            log.warn("Transaction Aborted while updating the SnapshotStartTimestamp {} with the topologyConfigID {}", ts, topologyConfigId);
            return false;
        }
    }


    /**
     * This call should be done in a transaction after a transfer is done and before applying the snapshot.
     * @param ts
     */
    public void setLastSnapTransferDoneTimestamp(long topologyConfigId, long ts) {
        Timestamp timestamp = corfuStore.getTimestamp();
        CorfuRecord<LogReplicationMetadataVal, LogReplicationMetadataKey> record = metadataTable.get(currentMetadataKey);

        if (record == null) {
            log.warn(" The log replication metadata doesn't exist at corefuTable, the operation could not succeed.");
            return;
        }

        long persistedTopologyConfigId = record.getPayload().getTopologyConfigId();
        long persistedSnapStart = record.getPayload().getSnapshotStartTimestamp();
        long persistedSnapTransferredTime = record.getPayload().getSnapshotTransferredTimestamp();

        /**
         * If the cluster config has changed, ignore the update operation.
         * If the timestamp is not consistent with the logReplicationStatus, ignore it.
         */
        if (topologyConfigId != persistedTopologyConfigId || ts != persistedSnapStart || ts <= persistedSnapTransferredTime) {
            log.warn("The metadata topologyConfigId {} and new SnapshotTransferDoneTimestamp ts {} are older than the persisted ones {}", getPersistedMetadataStr());
            return;
        }

        LogReplicationMetadataVal metadataVal = LogReplicationMetadataVal.newBuilder(record.getPayload()).
                setSnapshotTransferredTimestamp(ts).build();

        try {
            persistMetadata(timestamp, metadataVal);
        } catch (TransactionAbortedException e) {
            log.warn("Caught a transaction exception while updating the snapshotTransferredTimestamp {} ", metadataVal, e);
        }
    }

    public void setSnapshotApplied(LogReplicationEntry entry) {
        Timestamp timestamp = corfuStore.getTimestamp();
        CorfuRecord<LogReplicationMetadataVal, LogReplicationMetadataKey> record = metadataTable.get(currentMetadataKey);

        if (record == null) {
            log.warn(" The log replication metadata doesn't exist at corefuTable, the operation could not succeed.");
            return;
        }

        long persistedTopologyConfigId = record.getPayload().getTopologyConfigId();
        long persistedSnapStart = record.getPayload().getSnapshotStartTimestamp();
        long persistedSnapTranferDone = record.getPayload().getSnapshotTransferredTimestamp();
        long topologyConfigID = entry.getMetadata().getTopologyConfigId();
        long ts = entry.getMetadata().getSnapshotTimestamp();

        if (topologyConfigID != persistedTopologyConfigId || ts != persistedSnapStart || ts != persistedSnapTranferDone) {
            log.warn("The update is not valid, the entry's snapshotTimestamp {} is wrong according to the persistedMetadata {}", ts, record.getPayload());
            return;
        }

        LogReplicationMetadataVal metadataVal = LogReplicationMetadataVal.newBuilder(record.getPayload()).
                setSnapshotAppliedTimestamp(ts).
                setLastLogEntryProcessedTimestamp(ts).
                build();

        try {
            persistMetadata(timestamp, metadataVal);
        } catch (TransactionAbortedException e) {
            log.warn("Transaction aborted while updating the log replication metadata with the new value {}", metadataVal);
        }

        return;
    }

    /**
     * Get the current metadata from corfu store in string format used by logging.
     * @return
     */
    private String getPersistedMetadataStr() {
        CorfuRecord<LogReplicationMetadataVal, LogReplicationMetadataKey> record = metadataTable.get(currentMetadataKey);
        if (record == null) {
            log.warn("No metadata available in the corfu store. The operation could not succeed");
            return "No LogReplicationMetadata";
        }

        return getPersistedMetadataStr(record.getPayload());
    }

    /**
     * Given a metadata, get the metadata in string format used by logging.
     * @param metadataVal
     * @return
     */
    static public String getPersistedMetadataStr(LogReplicationMetadataVal metadataVal) {
        String s = new String();

        s.concat(LogReplicationMetadataName.TOPOLOGY_CONFIG_ID.getVal() + " " + metadataVal.getTopologyConfigId() + " ");
        s.concat(LogReplicationMetadataName.LAST_SNAPSHOT_STARTED.getVal() + " " + metadataVal.getSnapshotStartTimestamp() +" ");
        s.concat(LogReplicationMetadataName.LAST_SNAPSHOT_TRANSFERRED.getVal() + " " + metadataVal.getSnapshotTransferredTimestamp() + " ");
        s.concat(LogReplicationMetadataName.LAST_SNAPSHOT_APPLIED.getVal() + " " + metadataVal.getSnapshotAppliedTimestamp() + " ");
        s.concat(LogReplicationMetadataName.LAST_SNAPSHOT_MSG_RECEIVED_SEQ_NUM.getVal() + " " + metadataVal.getSnapshotMessageAppliedSeqNum() + " ");
        s.concat(LogReplicationMetadataName.LAST_SNAPSHOT_MSG_APPLIED_SEQ_NUM.getVal() + " " + metadataVal.getSnapshotMessageAppliedSeqNum() + " ");
        s.concat(LogReplicationMetadataName.LAST_LOG_PROCESSED.getVal() + " " + metadataVal.getLastLogEntryProcessedTimestamp() + " ");

        return s;
    }

    public static String getPersistedWriterMetadataTableName(String localClusterId) {
        return TABLE_PREFIX_NAME + localClusterId;
    }

    public long getLogHead() {
        return runtime.getAddressSpaceView().getTrimMark().getSequence();
    }

    public enum LogReplicationMetadataName {
        TOPOLOGY_CONFIG_ID("topologyConfigId"),
        VERSION("version"),
        LAST_SNAPSHOT_STARTED("lastSnapStart"),
        LAST_SNAPSHOT_TRANSFERRED("lastSnapTransferred"),
        LAST_SNAPSHOT_APPLIED("lastSnapApplied"),
        LAST_SNAPSHOT_MSG_RECEIVED_SEQ_NUM("lastSnapMsgReceivedSeqNum"),
        LAST_SNAPSHOT_MSG_APPLIED_SEQ_NUM("lastSnapMsgAppliedSeqNum"),
        LAST_LOG_PROCESSED("lastLogProcessed");

        @Getter
        String val;
        LogReplicationMetadataName(String newVal) {
            val  = newVal;
        }
    }
}
