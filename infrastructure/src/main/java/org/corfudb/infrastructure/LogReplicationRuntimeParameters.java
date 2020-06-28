package org.corfudb.infrastructure;

import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import lombok.Data;
import org.corfudb.comm.ChannelImplementation;
import org.corfudb.infrastructure.logreplication.LogReplicationConfig;
import org.corfudb.protocols.wireprotocol.MsgHandlingFilter;

import org.corfudb.infrastructure.logreplication.infrastructure.ClusterDescriptor;
import org.corfudb.runtime.RuntimeParameters;
import org.corfudb.runtime.RuntimeParametersBuilder;
import org.corfudb.util.MetricsUtils;

import java.lang.Thread.UncaughtExceptionHandler;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Log Replication Runtime Parameters (a runtime is specific per remote cluster)
 */
@Data
public class LogReplicationRuntimeParameters extends RuntimeParameters {

    // Remote Cluster Descriptor
    private ClusterDescriptor remoteClusterDescriptor;

    // Local Corfu Endpoint (used for database access)
    private String localCorfuEndpoint;

    // Local Cluster Identifier
    private String localClusterId;

    // Log Replication Configuration (streams to replicate)
    private LogReplicationConfig replicationConfig;

    // Plugin File Path (file with plugin configurations - absolute paths of JAR and canonical name of  classes)
    private String pluginFilePath;

    // Topology Configuration Identifier (configuration epoch)
    private long topologyConfigId;

    public static LogReplicationRuntimeParametersBuilder builder() {
        return new LogReplicationRuntimeParametersBuilder();
    }

    public static class LogReplicationRuntimeParametersBuilder extends RuntimeParametersBuilder {
        /*boolean tlsEnabled = false;
        String keyStore;
        String ksPasswordFile;
        String trustStore;
        String tsPasswordFile;
        boolean saslPlainTextEnabled = false;
        String usernameFile;
        String passwordFile;
        int handshakeTimeout = 10;
        Duration requestTimeout = Duration.ofSeconds(5);
        int idleConnectionTimeout = 7;
        int keepAlivePeriod = 2;
        Duration connectionTimeout = Duration.ofMillis(500);
        Duration connectionRetryRate = Duration.ofSeconds(1);
        UUID clientId = UUID.randomUUID();
        ChannelImplementation socketType = ChannelImplementation.NIO;
        EventLoopGroup nettyEventLoop;
        String nettyEventLoopThreadFormat = "netty-%d";
        int nettyEventLoopThreads = 0;
        boolean shutdownNettyEventLoop = true;
        Map<ChannelOption, Object> customNettyChannelOptions;
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
        List<MsgHandlingFilter> nettyClientInboundMsgFilters = null;
        volatile Runnable systemDownHandler = () -> {
        };
        volatile Runnable beforeRpcHandler = () -> {
        };*/
        private String localCorfuEndpoint;
        private String localClusterId;
        private ClusterDescriptor remoteClusterDescriptor;
        private String pluginFilePath;
        private long topologyConfigId;
        private LogReplicationConfig replicationConfig;
        private int prometheusMetricsPort = MetricsUtils.NO_METRICS_PORT;

        private LogReplicationRuntimeParametersBuilder() {
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder localCorfuEndpoint(String localCorfuEndpoint) {
            this.localCorfuEndpoint = localCorfuEndpoint;
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder localClusterId(String localClusterId) {
            this.localClusterId = localClusterId;
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder remoteClusterDescriptor(ClusterDescriptor remoteLogReplicationCluster) {
            this.remoteClusterDescriptor = remoteLogReplicationCluster;
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder pluginFilePath(String pluginFilePath) {
            this.pluginFilePath = pluginFilePath;
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder topologyConfigId(long topologyConfigId) {
            this.topologyConfigId = topologyConfigId;
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder replicationConfig(LogReplicationConfig replicationConfig) {
            this.replicationConfig = replicationConfig;
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder tlsEnabled(boolean tlsEnabled) {
            super.tlsEnabled(tlsEnabled);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder keyStore(String keyStore) {
            super.keyStore(keyStore);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder ksPasswordFile(String ksPasswordFile) {
            super.ksPasswordFile(ksPasswordFile);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder trustStore(String trustStore) {
            super.trustStore(trustStore);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder tsPasswordFile(String tsPasswordFile) {
            super.tsPasswordFile(tsPasswordFile);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder saslPlainTextEnabled(boolean saslPlainTextEnabled) {
            super.saslPlainTextEnabled(saslPlainTextEnabled);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder usernameFile(String usernameFile) {
            super.usernameFile(usernameFile);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder passwordFile(String passwordFile) {
            super.passwordFile(passwordFile);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder handshakeTimeout(int handshakeTimeout) {
            super.handshakeTimeout(handshakeTimeout);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder requestTimeout(Duration requestTimeout) {
            super.requestTimeout(requestTimeout);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder idleConnectionTimeout(int idleConnectionTimeout) {
            super.idleConnectionTimeout(idleConnectionTimeout);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder keepAlivePeriod(int keepAlivePeriod) {
            super.keepAlivePeriod(keepAlivePeriod);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder connectionTimeout(Duration connectionTimeout) {
            super.connectionTimeout(connectionTimeout);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder connectionRetryRate(Duration connectionRetryRate) {
            super.connectionRetryRate(connectionRetryRate);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder clientId(UUID clientId) {
            super.clientId(clientId);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder socketType(ChannelImplementation socketType) {
            super.socketType(socketType);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder nettyEventLoop(EventLoopGroup nettyEventLoop) {
            super.nettyEventLoop(nettyEventLoop);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder nettyEventLoopThreadFormat(String nettyEventLoopThreadFormat) {
            super.nettyEventLoopThreadFormat(nettyEventLoopThreadFormat);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder nettyEventLoopThreads(int nettyEventLoopThreads) {
            super.nettyEventLoopThreads(nettyEventLoopThreads);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder shutdownNettyEventLoop(boolean shutdownNettyEventLoop) {
            super.shutdownNettyEventLoop(shutdownNettyEventLoop);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder customNettyChannelOptions(Map<ChannelOption, Object> customNettyChannelOptions) {
            super.customNettyChannelOptions(customNettyChannelOptions);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder uncaughtExceptionHandler(UncaughtExceptionHandler uncaughtExceptionHandler) {
            super.uncaughtExceptionHandler(uncaughtExceptionHandler);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder nettyClientInboundMsgFilters(List<MsgHandlingFilter> nettyClientInboundMsgFilters) {
            super.nettyClientInboundMsgFilters(nettyClientInboundMsgFilters);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder prometheusMetricsPort(int prometheusMetricsPort) {
            super.prometheusMetricsPort(prometheusMetricsPort);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder systemDownHandler(Runnable systemDownHandler) {
            super.systemDownHandler(systemDownHandler);
            return this;
        }

        public LogReplicationRuntimeParameters.LogReplicationRuntimeParametersBuilder beforeRpcHandler(Runnable beforeRpcHandler) {
            super.beforeRpcHandler(beforeRpcHandler);
            return this;
        }

        public LogReplicationRuntimeParameters build() {
            LogReplicationRuntimeParameters runtimeParameters = new LogReplicationRuntimeParameters();
            runtimeParameters.setTlsEnabled(tlsEnabled);
            runtimeParameters.setKeyStore(keyStore);
            runtimeParameters.setKsPasswordFile(ksPasswordFile);
            runtimeParameters.setTrustStore(trustStore);
            runtimeParameters.setTsPasswordFile(tsPasswordFile);
            runtimeParameters.setSaslPlainTextEnabled(saslPlainTextEnabled);
            runtimeParameters.setUsernameFile(usernameFile);
            runtimeParameters.setPasswordFile(passwordFile);
            runtimeParameters.setHandshakeTimeout(handshakeTimeout);
            runtimeParameters.setRequestTimeout(requestTimeout);
            runtimeParameters.setIdleConnectionTimeout(idleConnectionTimeout);
            runtimeParameters.setKeepAlivePeriod(keepAlivePeriod);
            runtimeParameters.setConnectionTimeout(connectionTimeout);
            runtimeParameters.setConnectionRetryRate(connectionRetryRate);
            runtimeParameters.setClientId(clientId);
            runtimeParameters.setSocketType(socketType);
            runtimeParameters.setNettyEventLoop(nettyEventLoop);
            runtimeParameters.setNettyEventLoopThreadFormat(nettyEventLoopThreadFormat);
            runtimeParameters.setNettyEventLoopThreads(nettyEventLoopThreads);
            runtimeParameters.setShutdownNettyEventLoop(shutdownNettyEventLoop);
            runtimeParameters.setCustomNettyChannelOptions(customNettyChannelOptions);
            runtimeParameters.setUncaughtExceptionHandler(uncaughtExceptionHandler);
            runtimeParameters.setNettyClientInboundMsgFilters(nettyClientInboundMsgFilters);
            runtimeParameters.setPrometheusMetricsPort(prometheusMetricsPort);
            runtimeParameters.setSystemDownHandler(systemDownHandler);
            runtimeParameters.setBeforeRpcHandler(beforeRpcHandler);
            runtimeParameters.setLocalCorfuEndpoint(localCorfuEndpoint);
            runtimeParameters.setLocalClusterId(localClusterId);
            runtimeParameters.setRemoteClusterDescriptor(remoteClusterDescriptor);
            runtimeParameters.setTopologyConfigId(topologyConfigId);
            runtimeParameters.setPluginFilePath(pluginFilePath);
            runtimeParameters.setReplicationConfig(replicationConfig);
            return runtimeParameters;
        }
    }
}
