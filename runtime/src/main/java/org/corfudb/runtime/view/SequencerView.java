package org.corfudb.runtime.view;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.AccessLevel;
import org.corfudb.protocols.wireprotocol.TokenResponse;
import org.corfudb.protocols.wireprotocol.TxResolutionInfo;
import org.corfudb.runtime.CorfuRuntime;
import org.corfudb.util.CFUtils;

import java.util.Set;
import java.util.UUID;


/**
 * Created by mwei on 12/10/15.
 */

public class SequencerView extends AbstractView {

    // TODO the failover sequencer should be a separate class as specified
    // in https://github.com/CorfuDB/CorfuDB/wiki/Quorum-Replication
    @Setter(AccessLevel.PACKAGE)
    @Getter(AccessLevel.PACKAGE)
    private boolean failover = false;

    public SequencerView(CorfuRuntime runtime) {
        super(runtime);
    }


    /**
     * Return the next token in the sequence for a particular stream.
     *
     * If numTokens == 0, then the streamAddressesMap returned is the last handed out token for
     * each stream (if streamIDs is not empty). The token returned is the global address as
     * previously defined, namely, max global address across all the streams.
     *
     * @param streamIDs The stream IDs to retrieve from.
     * @param numTokens The number of tokens to reserve.
     * @return The first token retrieved.
     */
    public TokenResponse nextToken(Set<UUID> streamIDs, int numTokens) {
        return layoutHelper(l -> CFUtils.getUninterruptibly(l.getSequencer(0).nextToken(streamIDs, numTokens)));
    }


    public TokenResponse nextToken(Set<UUID> streamIDs, int numTokens, TxResolutionInfo conflictInfo) {
        return layoutHelper(l -> CFUtils.getUninterruptibly(l.getSequencer(0).nextToken(
                streamIDs, numTokens, conflictInfo)));
    }

    /**
     * Gives a hint whether this sequencer works in a steady state
     * @return false if most probably this sequencer will generate unique tokens, true
     * if there is a risk the tokens to be duplicated
     */
    public boolean isFailOverSequencer() {
        return failover;
    }





}
