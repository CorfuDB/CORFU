package org.corfudb.recovery;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import org.corfudb.protocols.logprotocol.CheckpointEntry;
import org.corfudb.protocols.logprotocol.LogEntry;
import org.corfudb.protocols.wireprotocol.ILogData;
import org.corfudb.runtime.CorfuRuntime;
import org.corfudb.runtime.collections.SMRMap;
import org.corfudb.runtime.object.CorfuCompileProxy;
import org.corfudb.runtime.object.ICorfuSMR;
import org.corfudb.runtime.view.AddressSpaceOptions;
import org.corfudb.runtime.view.ObjectsView;
import org.corfudb.util.serializer.ISerializer;

import java.util.*;
import java.util.stream.Collectors;

import static org.corfudb.protocols.logprotocol.CheckpointEntry.CheckpointDictKey.SNAPSHOT_ADDRESS;

/**
 * Created by rmichoud on 6/22/17.
 */
public class RecoveryUtils {

    static ObjectsView.ObjectID getObjectIdFromStreamId(UUID streamId) {
        return new ObjectsView.ObjectID(streamId, SMRMap.class);
    }
    static boolean isCheckPointEntry(ILogData logData) {
        return logData.hasCheckpointMetadata();
    }

    static long getSnapShotAddressOfCheckPoint(CheckpointEntry logEntry) {
        return Long.parseLong(logEntry.getDict().get(SNAPSHOT_ADDRESS));
    }

    static long getStartAddressOfCheckPoint(ILogData logData) {
        return logData.getCheckpointedStreamStartLogAddress();
    }

    /** Create a new object SMRMap as recipient of SMRUpdates (if doesn't exist yet)
     *
     * @param streamId
     */
    static void createSmrMapIfNotExist(CorfuRuntime runtime, UUID streamId, ISerializer serializer) {
        if (!runtime.getObjectsView().getObjectCache()
                .containsKey(RecoveryUtils.getObjectIdFromStreamId(streamId))) {
            runtime.getObjectsView().build()
                    .setStreamID(streamId)
                    .setType(SMRMap.class)
                    .setSerializer(serializer)
                    .open();
        }
    }

    static final AddressSpaceOptions addressSpaceOptions = new AddressSpaceOptions();

    /**
     * Fetch LogData from Corfu server
     *
     * @param address address to be fetched
     * @return LogData at address
     */
    static ILogData getLogData(CorfuRuntime runtime, boolean loadInCache, long address) {
        addressSpaceOptions.setDoCache(false);
        return runtime.getAddressSpaceView().read(address, addressSpaceOptions);
    }

    /**
     * Get a range of LogData from the server
     *
     * This is using the underlying bulk read implementation for
     * fetching a range of addresses. This read will return
     * a map ordered by address.
     *
     * It uses a ClosedOpen range : [start, end)
     * (e.g [0, 5) == (0,1,2,3,4))
     *
     * @param start start address for the bulk read
     * @param end end address for the bulk read
     * @return logData map ordered by addresses (increasing)
     */
    static Map<Long, ILogData> getLogData(CorfuRuntime runtime, long start, long end) {
        addressSpaceOptions.setDoCache(false);
        Set<Long> addresses = ContiguousSet.create(Range.closedOpen(start, end), DiscreteDomain
                .longs());

        Map<Long, ILogData> dataMap =
                runtime.getAddressSpaceView()
                        .bulkFetchNoBatch(addresses, addressSpaceOptions);

        return dataMap;
    }

    /** Deserialize a logData by getting the logEntry
     *
     * Getting the underlying logEntry should trigger deserialization only once.
     * Next access should just returned the logEntry direclty.
     *
     * @param logData
     * @return
     * @throws Exception
     */
    public static LogEntry deserializeLogData(CorfuRuntime runtime, ILogData logData) throws Exception{
        return logData.getLogEntry(runtime);
    }

    /**
     * Look in the objectCache for the corresponding CorfuCompileProxy
     *
     * @param runtime
     * @param streamId
     * @return
     */
    static CorfuCompileProxy getCorfuCompileProxy(CorfuRuntime runtime, UUID streamId) {
        ObjectsView.ObjectID thisObjectId = new ObjectsView.ObjectID(streamId, SMRMap.class);
        return ((CorfuCompileProxy) ((ICorfuSMR) runtime.getObjectsView().getObjectCache().get(thisObjectId)).
                getCorfuSMRProxy());
    }
}
