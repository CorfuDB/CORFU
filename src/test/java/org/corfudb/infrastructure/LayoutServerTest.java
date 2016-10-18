package org.corfudb.infrastructure;

import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import org.corfudb.protocols.wireprotocol.*;
import com.google.common.util.concurrent.ExecutionError;
import org.corfudb.runtime.view.Layout;
import org.junit.Test;

import java.io.File;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.corfudb.infrastructure.LayoutServerAssertions.assertThat;

/**
 * Created by mwei on 12/14/15.
 */
@Slf4j
public class LayoutServerTest extends AbstractServerTest {

    @Override
    public LayoutServer getDefaultServer() {
        String serviceDir = getTempDir();
        return getDefaultServer(serviceDir);
    }

    private LayoutServer getDefaultServer(String serviceDir) {
        LayoutServer s1 = new LayoutServer(new ServerContextBuilder().setSingle(false).setMemory(false).setLogPath(serviceDir).setServerRouter(getRouter()).build());
        setServer(s1);
        return s1;
    }

    private void bootstrapServer(Layout l) {
        sendMessage(new LayoutMsg(l, CorfuMsgType.LAYOUT_BOOTSTRAP));
    }

    // memoryLayoutServerReadsLayout() test is no longer valid.

    @Test
    public void nonBootstrappedServerNoLayout() {
        sendMessage(new CorfuMsg(CorfuMsgType.LAYOUT_REQUEST));
        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.LAYOUT_NOBOOTSTRAP);
    }

    @Test
    public void bootstrapServerInstallsNewLayout() {
        Layout testLayout = TestLayoutBuilder.single(9000);
        sendMessage(new LayoutMsg(testLayout, CorfuMsgType.LAYOUT_BOOTSTRAP));
        sendMessage(new CorfuMsg(CorfuMsgType.LAYOUT_REQUEST));
        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.LAYOUT_RESPONSE);
        assertThat(((LayoutMsg) getLastMessage()).getLayout())
                .isEqualTo(testLayout);
    }

    @Test
    public void cannotBootstrapServerTwice() {
        Layout testLayout = TestLayoutBuilder.single(9000);
        sendMessage(new LayoutMsg(testLayout, CorfuMsgType.LAYOUT_BOOTSTRAP));
        sendMessage(new LayoutMsg(testLayout, CorfuMsgType.LAYOUT_BOOTSTRAP));
        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.LAYOUT_ALREADY_BOOTSTRAP);
    }


    @Test
    public void prepareRejectsLowerRanks() {
        bootstrapServer(TestLayoutBuilder.single(9000));
        sendMessage(new LayoutRankMsg(TestLayoutBuilder.single(9000), 100, CorfuMsgType.LAYOUT_PREPARE));
        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.LAYOUT_PREPARE_ACK);
        sendMessage(new LayoutRankMsg(TestLayoutBuilder.single(9000), 10, CorfuMsgType.LAYOUT_PREPARE));
        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.LAYOUT_PREPARE_REJECT);
    }

    @Test
    public void prepareRejectsLowerRanksWithLastProposal() {

    }

    @Test
    public void proposeRejectsLowerRanks() {
        bootstrapServer(TestLayoutBuilder.single(9000));
        sendMessage(new LayoutRankMsg(TestLayoutBuilder.single(9000), 100, CorfuMsgType.LAYOUT_PREPARE));
        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.LAYOUT_PREPARE_ACK);
        sendMessage(new LayoutRankMsg(TestLayoutBuilder.single(9000), 10, CorfuMsgType.LAYOUT_PROPOSE));
        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.LAYOUT_PROPOSE_REJECT);
    }

    @Test
    public void proposeRejectsAlreadyProposed() {
        bootstrapServer(TestLayoutBuilder.single(9000));
        sendMessage(new LayoutRankMsg(TestLayoutBuilder.single(9000), 10, CorfuMsgType.LAYOUT_PREPARE));
        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.LAYOUT_PREPARE_ACK);
        sendMessage(new LayoutRankMsg(TestLayoutBuilder.single(9000), 10, CorfuMsgType.LAYOUT_PROPOSE));
        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.ACK);
        sendMessage(new LayoutRankMsg(TestLayoutBuilder.single(9000), 10, CorfuMsgType.LAYOUT_PROPOSE));
        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.LAYOUT_PROPOSE_REJECT);
    }

    @Test
    public void commitReturnsAck() {
        Layout layout = TestLayoutBuilder.single(9000);


        bootstrapServer(layout);
        layout.setEpoch(layout.getEpoch() + 1);

        sendMessage(new CorfuPayloadMsg<>(CorfuMsgType.SET_EPOCH, layout.getEpoch()));

        sendMessage(new LayoutRankMsg(layout, 100, CorfuMsgType.LAYOUT_PREPARE));

        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.LAYOUT_PREPARE_ACK);
        sendMessage(new LayoutRankMsg(layout, 100, CorfuMsgType.LAYOUT_PROPOSE));
        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.ACK);
        sendMessage(new LayoutRankMsg(layout, 1000, CorfuMsgType.LAYOUT_COMMITTED));
        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.ACK);
    }

    @Test
    public void checkServerEpochPersisted() {
        String serviceDir = getTempDir();
        LayoutServer s1 = getDefaultServer(serviceDir);

        bootstrapServer(TestLayoutBuilder.single(9000));
        sendMessage(new CorfuPayloadMsg<>(CorfuMsgType.SET_EPOCH, 2L));
        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.ACK);
        sendMessage(new CorfuMsg(CorfuMsgType.LAYOUT_REQUEST));
        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.LAYOUT_RESPONSE);
        assertThat(getLastMessage().getEpoch()).isEqualTo(2);
        sendMessage(new CorfuPayloadMsg<>(CorfuMsgType.SET_EPOCH, 1L));
        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.WRONG_EPOCH);

    }

    @Test
    public void checkLayoutPersisted()
            throws Exception {
        String serviceDir = getTempDir();

        LayoutServer s1 = getDefaultServer(serviceDir);
        bootstrapServer(TestLayoutBuilder.single(9000));
        Layout l100 = TestLayoutBuilder.single(9000);
        //l100.setEpoch(100);
        sendMessage(new LayoutRankMsg(l100, 100, CorfuMsgType.LAYOUT_PREPARE));
        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.LAYOUT_PREPARE_ACK);
        sendMessage(new LayoutRankMsg(l100, 100, CorfuMsgType.LAYOUT_PROPOSE));

        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.ACK);
        assertThat(s1)
                .isInEpoch(0);
        assertThat(s1)
                .isPhase1Rank(new Rank(100L, AbstractServerTest.testClientId));
        assertThat(s1)
                .isPhase2Rank(new Rank(100L, AbstractServerTest.testClientId));
        s1.shutdown();

        LayoutServer s2 = getDefaultServer(serviceDir);
        this.router.reset();
        this.router.addServer(s2);

        assertThat(s2)
                .isInEpoch(0);  // SLF: TODO: rebase conflict: new is 0, old was 100
        assertThat(s2)
                .isPhase1Rank(new Rank(100L, AbstractServerTest.testClientId));
        assertThat(s2)
                .isPhase2Rank(new Rank(100L, AbstractServerTest.testClientId));

        sendMessage(new CorfuMsg(CorfuMsgType.LAYOUT_REQUEST));
        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.LAYOUT_RESPONSE);
        assertThat(((LayoutMsg) getLastMessage()).getLayout().getEpoch())
                .isEqualTo(0);
    }

    /**
     * The test validates that the data in accepted phase1 and phase2 messages
     * is persisted to disk and survives layout server restarts.
     *
     * @throws Exception
     */
    @Test
    public void checkPaxosPhasesPersisted() throws Exception {
        String serviceDir = getTempDir();

        LayoutServer s1 = getDefaultServer(serviceDir);
        Layout l100 = TestLayoutBuilder.single(9000);
        bootstrapServer(l100);

        //l100.setEpoch(100);

        // validate phase 1
        sendMessage(new LayoutRankMsg(l100, 100, CorfuMsgType.LAYOUT_PREPARE));
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.LAYOUT_PREPARE_ACK);

//        assertThat(s1).isInEpoch(0);
        assertThat(s1).isPhase1Rank(new Rank(100L, AbstractServerTest.testClientId));
        s1.shutdown();

        LayoutServer s2 = getDefaultServer(serviceDir);

        assertThat(s2).isInEpoch(0);
        assertThat(s2).isPhase1Rank(new Rank(100L, AbstractServerTest.testClientId));

        // validate phase2 data persistence

        sendMessage(new LayoutRankMsg(l100, 100, CorfuMsgType.LAYOUT_PROPOSE));
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.ACK);
        s2.shutdown();

        LayoutServer s3 = getDefaultServer(serviceDir);

        assertThat(s3).isInEpoch(0);
        assertThat(s3).isPhase1Rank(new Rank(100L, AbstractServerTest.testClientId));
        assertThat(s3).isPhase2Rank(new Rank(100L, AbstractServerTest.testClientId));
        assertThat(s3).isProposedLayout(l100);

    }

    /**
     * Validates that the layout server accept or rejects incoming phase1 messages based on
     * the last persisted phase1 rank.
     *
     * @throws Exception
     */
    @Test
    public void checkMessagesValidatedAgainstPhase1PersistedData() throws Exception {
        String serviceDir = getTempDir();

        LayoutServer s1 = getDefaultServer(serviceDir);
        Layout l100 = TestLayoutBuilder.single(9000);
        bootstrapServer(l100);
        l100.setEpoch(0);

        // validate phase 1
        sendMessage(new LayoutRankMsg(l100, 100, CorfuMsgType.LAYOUT_PREPARE));
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.LAYOUT_PREPARE_ACK);

        assertThat(s1).isInEpoch(0);
        assertThat(s1).isPhase1Rank(new Rank(100L, AbstractServerTest.testClientId));
        s1.shutdown();

        LayoutServer s2 = getDefaultServer(serviceDir);

        assertThat(s2).isInEpoch(0);
        assertThat(s2).isPhase1Rank(new Rank(100L, AbstractServerTest.testClientId));

        //new LAYOUT_PREPARE message with a lower phase1 rank should be rejected
        sendMessage(new LayoutRankMsg(l100, 99, CorfuMsgType.LAYOUT_PREPARE));
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.LAYOUT_PREPARE_REJECT);


        //new LAYOUT_PREPARE message with a higher phase1 rank should be accepted
        sendMessage(new LayoutRankMsg(l100, 101, CorfuMsgType.LAYOUT_PREPARE));
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.LAYOUT_PREPARE_ACK);
    }

    /**
     * Validates that the layout server accept or rejects incoming phase2 messages based on
     * the last persisted phase1 and phase2 data.
     * If persisted phase1 rank does not match the LAYOUT_PROPOSE message then the server did not
     * take part in the prepare phase. It should reject this message.
     * If the persisted phase2 rank is the same as incoming message, it will be rejected as it is a
     * duplicate message.
     *
     * @throws Exception
     */
    @Test
    public void checkMessagesValidatedAgainstPhase2PersistedData() throws Exception {
        String serviceDir = getTempDir();

        LayoutServer s1 = getDefaultServer(serviceDir);
        Layout l100 = TestLayoutBuilder.single(9000);
        bootstrapServer(l100);
        l100.setEpoch(0);
        // validate phase 1
        sendMessage(new LayoutRankMsg(l100, 100, CorfuMsgType.LAYOUT_PREPARE));
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.LAYOUT_PREPARE_ACK);

        // the epoch should not change yet.
//        assertThat(s1).isInEpoch(0);
        assertThat(s1).isPhase1Rank(new Rank(100L, AbstractServerTest.testClientId));
        s1.shutdown();

        LayoutServer s2 = getDefaultServer(serviceDir);

        assertThat(s2).isInEpoch(0);
        assertThat(s2).isPhase1Rank(new Rank(100L, AbstractServerTest.testClientId));

        //new LAYOUT_PROPOSE message with a lower phase2 rank should be rejected
        sendMessage(new LayoutRankMsg(l100, 99, CorfuMsgType.LAYOUT_PROPOSE));
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.LAYOUT_PROPOSE_REJECT);


        //new LAYOUT_PREPARE message with a higher phase2 rank should be rejected
        sendMessage(new LayoutRankMsg(l100, 101, CorfuMsgType.LAYOUT_PROPOSE));
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.LAYOUT_PROPOSE_REJECT);

        //new LAYOUT_PREPARE message with same phase2 rank should be accepted
        sendMessage(new LayoutRankMsg(l100, 100, CorfuMsgType.LAYOUT_PROPOSE));
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.ACK);
        s2.shutdown();

        LayoutServer s3 = getDefaultServer(serviceDir);

        // the epoch should have changed by now.
        assertThat(s3).isInEpoch(0);
        assertThat(s3).isPhase1Rank(new Rank(100L, AbstractServerTest.testClientId));
        assertThat(s3).isProposedLayout(l100);
    }

    /**
     * Validates that the layout server accept or rejects incoming phase1 and phase2 messages from multiple
     * clients based on current state {Phease1Rank [rank, clientID], Phase2Rank [rank, clientID] }
     * If LayoutServer has accepted a phase1 message from a client , it can only accept a higher ranked phase1 message
     * from another client.
     * A phase2 message can only be accepted if the last accepted phase1 message is from the same client and has the
     * same rank.
     *
     * @throws Exception
     */
    // SLF TODO: put me back: @Test
    public void checkPhase1AndPhase2MessagesFromMultipleClients() throws Exception {
        String serviceDir = getTempDir();

        LayoutServer s1 = getDefaultServer(serviceDir);
        Layout l100 = TestLayoutBuilder.single(9000);
        bootstrapServer(l100);
        //l100.setEpoch(100);
        /* validate phase 1 */
        sendMessage(new LayoutRankMsg(l100, 100, CorfuMsgType.LAYOUT_PREPARE));
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.LAYOUT_PREPARE_ACK);

        // the epoch should not change yet.
//        assertThat(s1).isInEpoch(0);
        assertThat(s1).isPhase1Rank(new Rank(100L, AbstractServerTest.testClientId));

        // message from a different client with same rank should be rejected or accepted based on
        // whether the uuid is greater of smaller.
        sendMessage(UUID.nameUUIDFromBytes("OTHER_CLIENT".getBytes()), new LayoutRankMsg(l100, 100, CorfuMsgType.LAYOUT_PREPARE));
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.LAYOUT_PREPARE_REJECT);

        sendMessage(UUID.nameUUIDFromBytes("TEST_CLIENT_OTHER".getBytes()), new LayoutRankMsg(l100, 100, CorfuMsgType.LAYOUT_PREPARE));
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.LAYOUT_PREPARE_ACK);

        // message from a different client but with a higher rank gets accepted
        sendMessage(UUID.nameUUIDFromBytes("OTHER_CLIENT".getBytes()), new LayoutRankMsg(l100, 101, CorfuMsgType.LAYOUT_PREPARE));
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.LAYOUT_PREPARE_ACK);
        assertThat(s1).isPhase1Rank(new Rank(101L, UUID.nameUUIDFromBytes("OTHER_CLIENT".getBytes())));
//        assertThat(s1).isInEpoch(0);

        // testing behaviour after server restart
        s1.shutdown();
        LayoutServer s2 = getDefaultServer(serviceDir);
        assertThat(s2).isInEpoch(0);
        assertThat(s2).isPhase1Rank(new Rank(101L, UUID.nameUUIDFromBytes("OTHER_CLIENT".getBytes())));
        //duplicate message to be rejected
        sendMessage(UUID.nameUUIDFromBytes("OTHER_CLIENT".getBytes()), new LayoutRankMsg(l100, 101, CorfuMsgType.LAYOUT_PREPARE));
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.LAYOUT_PREPARE_REJECT);

        /* validate phase 2 */

        //phase2 message from a different client than the one whose phase1 was last accepted is rejected
        sendMessage(new LayoutRankMsg(l100, 101, CorfuMsgType.LAYOUT_PROPOSE));
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.LAYOUT_PROPOSE_REJECT);

        // phase2 from same client with same rank as in phase1 gets accepted
        sendMessage(UUID.nameUUIDFromBytes("OTHER_CLIENT".getBytes()), new LayoutRankMsg(l100, 101, CorfuMsgType.LAYOUT_PROPOSE));
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.ACK);

        assertThat(s2).isInEpoch(0);
        assertThat(s2).isPhase1Rank(new Rank(101L, UUID.nameUUIDFromBytes("OTHER_CLIENT".getBytes())));
        assertThat(s2).isPhase2Rank(new Rank(101L, UUID.nameUUIDFromBytes("OTHER_CLIENT".getBytes())));
        assertThat(s2).isProposedLayout(l100);

        s2.shutdown();
    }

    @Test
    public void testReboot() throws Exception {
        String serviceDir = getTempDir();

        LayoutServer s1 = getDefaultServer(serviceDir);

        setServer(s1);
        Layout l99 = TestLayoutBuilder.single(9000);
        l99.setEpoch(99);
        bootstrapServer(l99);

        // Reboot, then check that our epoch 100 layout is still there.
        s1.reboot();

        sendMessage(new CorfuMsg(CorfuMsgType.LAYOUT_REQUEST));
        assertThat(getLastMessage().getMsgType())
                .isEqualTo(CorfuMsgType.LAYOUT_RESPONSE);
        assertThat(((LayoutMsg) getLastMessage()).getLayout().getEpoch())
                .isEqualTo(99);
        s1.shutdown();

        for (int i = 0; i < 16; i++) {
            LayoutServer s2 = getDefaultServer(serviceDir);
            setServer(s2);
            commitReturnsAck(s2, i, 100);
            s2.shutdown();
        }
    }

    // Same as commitReturnsAck() test, but we perhaps make a .reboot() call
    // between each step.

    private void commitReturnsAck(LayoutServer s1, Integer reboot, long baseEpoch) {

        if ((reboot & 1) > 0) {
            s1.reboot();
        }
        long newEpoch = baseEpoch + reboot;
        sendMessage(new CorfuPayloadMsg<>(CorfuMsgType.SET_EPOCH, newEpoch));

        Layout layout = TestLayoutBuilder.single(9000);
        layout.setEpoch(newEpoch);

        LayoutRankMsg lrm1 = new LayoutRankMsg(layout, 100, CorfuMsgType.LAYOUT_PREPARE);
        sendMessage(lrm1);
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.LAYOUT_PREPARE_ACK);

        if ((reboot & 2) > 0) {
            log.debug("Rebooted server because reboot & 2 {}", reboot & 2);
            s1.reboot();
        }

        LayoutRankMsg lrm2 = new LayoutRankMsg(layout, 100, CorfuMsgType.LAYOUT_PROPOSE);
        lrm2.setEpoch(newEpoch);
        sendMessage(lrm2);
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.ACK);

        if ((reboot & 4) > 0) {
            s1.reboot();
        }

        LayoutRankMsg lrm3 = new LayoutRankMsg(layout, 1000, CorfuMsgType.LAYOUT_COMMITTED);
        sendMessage(lrm3);
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.ACK);

        if ((reboot & 8) > 0) {
            s1.reboot();
        }

        sendMessage(lrm3);
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.ACK);

        sendMessage(new CorfuMsg(CorfuMsgType.LAYOUT_REQUEST));
        assertThat(getLastMessage().getMsgType()).isEqualTo(CorfuMsgType.LAYOUT_RESPONSE);
        assertThat(((LayoutMsg) getLastMessage()).getLayout()).isEqualTo(layout);

    }

}
