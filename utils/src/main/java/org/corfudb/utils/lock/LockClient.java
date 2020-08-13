/*
 ***********************************************************************
 * Copyright 2019 VMware, Inc.  All rights reserved. VMware Confidential
 ***********************************************************************
 */

package org.corfudb.utils.lock;


import com.google.common.annotations.VisibleForTesting;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.corfudb.runtime.CorfuRuntime;
import org.corfudb.util.concurrent.SingletonResource;
import org.corfudb.utils.lock.LockDataTypes.LockData;
import org.corfudb.utils.lock.LockDataTypes.LockId;
import org.corfudb.utils.lock.persistence.LockStore;
import org.corfudb.utils.lock.persistence.LockStoreException;
import org.corfudb.utils.lock.states.LockEvent;
import org.corfudb.utils.lock.states.LockStateType;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Applications can register interest for a lock using the LockClient. When a lock is acquired on behalf of an instance
 * of an application it is notified through registered callbacks. Similarly if a lock is lost/revoked the corresponding
 * application instance is notified through callbacks.
 * <p>
 * The client also monitors the registered locks. If a lock has an expired lease, it generates a LEASE_REVOKED
 * event on that lock.
 *
 * @author mdhawan
 * @since 04/17/2020
 */
// TODO Figure out if the lock client should be a singleton
@Slf4j
public class LockClient {

    // all the locks that the applications are interested in.
    @VisibleForTesting
    @Getter
    private final Map<LockId, Lock> locks = new ConcurrentHashMap<>();

    // Lock data store
    private final LockStore lockStore;

    // Single threaded scheduler to monitor locks
    private final ScheduledExecutorService lockMonitorScheduler;

    private final ScheduledExecutorService taskScheduler;

    // Single threaded scheduler to monitor the acquired locks (lease)
    // A dedicated scheduler is required in case the task scheduler is stuck in some database operation
    // and the previous lock owner can effectively expire the lock.
    private final ScheduledExecutorService leaseMonitorScheduler;

    private final ExecutorService lockListenerExecutor;

    // duration between monitoring runs
    @Setter
    private static int DurationBetweenLockMonitorRuns = 10;

    // The context contains objects that are shared across the locks in this client.
    @Getter
    private final ClientContext clientContext;

    // Handle for the periodic lock monitoring task
    private Optional<ScheduledFuture<?>> lockMonitorFuture = Optional.empty();

    @Getter
    private UUID clientId;

    @Getter
    private final LockConfig lockConfig;

    /**
     * Constructor
     *
     * @param clientId
     * @param lockConfig
     * @param corfuRuntime
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    //TODO need to determine if the application should provide a clientId or should it be internally generated.
    public LockClient(UUID clientId, LockConfig lockConfig, CorfuRuntime corfuRuntime) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        this.taskScheduler = Executors.newScheduledThreadPool(1, (r) ->
        {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setName("LockTaskThread");
            t.setDaemon(true);
            return t;
        });

        this.leaseMonitorScheduler = Executors.newScheduledThreadPool(1, (r) ->
        {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setName("LeaseMonitorThread");
            t.setDaemon(true);
            return t;
        });

        this.lockListenerExecutor = Executors.newFixedThreadPool(1, (r) ->
        {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setName("LockListenerThread");
            t.setDaemon(true);
            return t;
        });

        this.lockMonitorScheduler = Executors.newScheduledThreadPool(1, (r) ->
        {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setName("LockMonitorThread");
            t.setDaemon(true);
            return t;
        });
        this.lockConfig = lockConfig;
        this.clientId = clientId;
        this.lockStore = new LockStore(corfuRuntime, clientId, lockConfig);
        this.clientContext = new ClientContext(clientId, lockStore, taskScheduler, lockListenerExecutor, leaseMonitorScheduler);
    }

    /**
     * Create a new LockClient instance given the lock config, nodeId, lock listener, runtime and optional
     * init state for the lock. This method is used primarily in testing to achieve a specific desired behavior,
     * hence no lock monitoring is invoked, and no events are being input for the created lock.
     *
     * @param lockConfig            A configuration for a distributed lock.
     * @param logReplicationNodeId  Current log replication node id.
     * @param lockListener          Lock event listener.
     * @param runtime               Corfu runtime singleton resource.
     * @param initState             An optional init state.
     * @return                      A new instance of a lock client.
     * @throws Exception            In case we fail to create lock client.
     */
    @VisibleForTesting
    public static LockClient newInstance(LockConfig lockConfig, UUID logReplicationNodeId,
                                         LockListener lockListener,
                                         SingletonResource<CorfuRuntime> runtime, Optional<LockStateType> initState) throws Exception {
        LockClient lockClient =
                new LockClient(logReplicationNodeId,
                        lockConfig, runtime.get());

        LockDataTypes.LockId lockId = LockDataTypes.LockId.newBuilder()
                .setLockGroup(lockConfig.getLockGroup())
                .setLockName(lockConfig.getLockName())
                .build();

        lockClient.getLocks().computeIfAbsent(
                lockId,
                key -> initState.map(lockStateType ->
                        new Lock(lockId, lockListener, lockClient.getClientContext(), lockConfig,
                                lockStateType))
                        .orElseGet(() -> new Lock(lockId, lockListener, lockClient.getClientContext(), lockConfig)));

        return lockClient;
    }

    /**
     * Application registers interest for a lock [lockgroup, lockname]. The <class>Lock</class> will then
     * make periodic attempts to acquire lock. Lock is acquired when the <class>Lock</class> is able to write
     * a lease record in a common table that is being written to/read by all the registered <class>Lock</class>
     * instances. Once acquired, the lease for the lock needs to be renewed periodically or else it will be acquired by
     * another contending <class>Lock</class> instance. The application is notified if a lock is lost.
     *
     * @param lockListener
     */
    public void registerInterest(LockListener lockListener) {
        registerInterest(lockConfig.getLockGroup(), lockConfig.getLockName(),
                lockListener);
    }

    @VisibleForTesting
    public void deregisterInterest() {
        LockId lockId = LockDataTypes.LockId.newBuilder()
                .setLockGroup(lockConfig.getLockGroup())
                .setLockName(lockConfig.getLockName())
                .build();
        if (!locks.containsKey(lockId)) {
            throw new IllegalStateException("Can only deregister if the known " +
                    "lock is present in the map.");
        }
        Lock lock = locks.get(lockId);

        if (lock.getState().getType() != LockStateType.HAS_LEASE) {
            throw new IllegalStateException("Can only deregister if the lock " +
                    "is in HAS_LEASE state.");
        }

        lock.input(LockEvent.LEASE_EXPIRED);
    }

    @VisibleForTesting
    public void resumeInterest() {
        LockId lockId = LockDataTypes.LockId.newBuilder()
                .setLockGroup(lockConfig.getLockGroup())
                .setLockName(lockConfig.getLockName())
                .build();
        if (!locks.containsKey(lockId)) {
            throw new IllegalStateException("Can only resume interest if the known " +
                    "lock is present in the map.");
        }

        Lock lock = locks.get(lockId);

        if (lock.getState().getType() != LockStateType.NO_LEASE) {
            throw new IllegalStateException("Can only resume interest if the lock " +
                    "is in NO_LEASE state.");
        }

        monitorLocks();

        lock.input(LockEvent.LEASE_REVOKED);
    }

    @VisibleForTesting
    public void forceAcquire() {
        LockId lockId = LockDataTypes.LockId.newBuilder()
                .setLockGroup(lockConfig.getLockGroup())
                .setLockName(lockConfig.getLockName())
                .build();
        if (!locks.containsKey(lockId)) {
            throw new IllegalStateException("Can only force acquire if the known " +
                    "lock is present in the map.");
        }

        Lock lock = locks.get(lockId);

        if (lock.getState().getType() != LockStateType.NO_LEASE) {
            throw new IllegalStateException("Can only force acquire if the lock " +
                    "is in NO_LEASE state.");
        }

        lock.input(LockEvent.FORCE_LEASE_ACQUIRED);
    }

    /**
     * Application registers interest for a lock [lockgroup, lockname]. The <class>Lock</class> will then
     * make periodic attempts to acquire lock. Lock is acquired when the <class>Lock</class> is able to write
     * a lease record in a common table that is being written to/read by all the registered <class>Lock</class>
     * instances. Once acquired, the lease for the lock needs to be renewed periodically or else it will be acquired by
     * another contending <class>Lock</class> instance. The application is notified if a lock is lost.
     *
     * @param lockGroup
     * @param lockName
     * @param lockListener
     */
    public void registerInterest(@NonNull String lockGroup, @NonNull String lockName,
                                 LockListener lockListener) {
        LockId lockId = LockDataTypes.LockId.newBuilder()
                .setLockGroup(lockGroup)
                .setLockName(lockName)
                .build();

        Lock lock = locks.computeIfAbsent(
                lockId,
                (key) -> new Lock(lockId, lockListener, clientContext, lockConfig));

        // Initialize the lease
        lock.input(LockEvent.LEASE_REVOKED);

        monitorLocks();
    }

    /**
     * Get current lock data.
     *
     * @param lockGroup
     * @param lockName
     * @return
     * @throws LockStoreException
     */
    public Optional<LockData> getCurrentLockData(@NonNull String lockGroup, @NonNull String lockName) throws LockStoreException {
        LockId lockId = LockDataTypes.LockId.newBuilder()
                .setLockGroup(lockGroup)
                .setLockName(lockName)
                .build();
        return lockStore.get(lockId);
    }

    /**
     * Monitor all the locks this client is interested in.
     * If a lock has an expired lease, the lock will be revoked.
     **/
    private void monitorLocks() {
        // find the expired leases.
        lockMonitorFuture = Optional.of(lockMonitorScheduler.scheduleWithFixedDelay(
                () -> {
                    try {
                        Collection<LockId> locksWithExpiredLeases = lockStore.filterLocksWithExpiredLeases(locks.keySet());
                        for (LockId lockId : locksWithExpiredLeases) {
                            log.debug("LockClient: lease revoked for lock {}", lockId.getLockName());
                            locks.get(lockId).input(LockEvent.LEASE_REVOKED);
                        }
                    } catch (Exception ex) {

                    }
                },
                lockConfig.getLockMonitorDurationInSeconds(),
                lockConfig.getLockMonitorDurationInSeconds(),
                TimeUnit.SECONDS

        ));
    }

    public void shutdown() {
        log.info("Shutdown Lock Client");
        this.lockMonitorScheduler.shutdown();
        this.taskScheduler.shutdown();
        this.lockListenerExecutor.shutdown();
    }

    /**
     * Context is used to provide access to common values and resources needed by objects implementing
     * the Lock functionality.
     */
    @Data
    public class ClientContext {

        private final UUID clientUuid;
        private final LockStore lockStore;
        private final ScheduledExecutorService taskScheduler;
        private final ScheduledExecutorService leaseMonitorScheduler;
        private final ExecutorService lockListenerExecutor;

        public ClientContext(UUID clientUuid, LockStore lockStore, ScheduledExecutorService taskScheduler,
                             ExecutorService lockListenerExecutor, ScheduledExecutorService leaseMonitorScheduler) {
            this.clientUuid = clientUuid;
            this.lockStore = lockStore;
            this.taskScheduler = taskScheduler;
            this.lockListenerExecutor = lockListenerExecutor;
            this.leaseMonitorScheduler = leaseMonitorScheduler;
        }
    }


}
