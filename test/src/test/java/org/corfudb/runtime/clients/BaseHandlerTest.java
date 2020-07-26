package org.corfudb.runtime.clients;

import com.google.common.collect.ImmutableSet;
import org.corfudb.infrastructure.AbstractServer;
import org.corfudb.infrastructure.BaseServer;
import org.corfudb.infrastructure.ServerContextBuilder;
import org.corfudb.infrastructure.server.CorfuServerStateMachine;
import org.corfudb.util.CFUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Set;
import java.util.UUID;

/**
 * Created by mwei on 7/27/16.
 */
public class BaseHandlerTest extends AbstractClientTest {

    BaseClient client;

    @Override
    Set<AbstractServer> getServersForTest() {
        return new ImmutableSet.Builder<AbstractServer>()
                .add(new BaseServer(ServerContextBuilder.defaultTestContext(0), serverSm))
                .build();
    }

    @Override
    Set<IClient> getClientsForTest() {
        BaseHandler baseHandler = new BaseHandler();
        client = new BaseClient(router, 0L, UUID.fromString("00000000-0000-0000-0000-000000000000"));
        return new ImmutableSet.Builder<IClient>()
                .add(baseHandler)
                .build();
    }

    @Test
    public void canGetVersionInfo() {
        CFUtils.getUninterruptibly(client.getVersionInfo());
    }
}
