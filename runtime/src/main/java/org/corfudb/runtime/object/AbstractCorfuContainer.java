package org.corfudb.runtime.object;

import org.corfudb.runtime.CorfuRuntime;

import java.util.UUID;

/** A Corfu container object is a container for other Corfu objects.
 * It has explicit access to its own stream ID, and a runtime, allowing it
 * to manipulate and return other Corfu objects.
 *
 * Created by mwei on 11/12/16.
 */

public abstract class AbstractCorfuContainer<T> implements ICorfuSMRProxyContainer<T> {

    ICorfuSMRProxy<T> proxy;

    /** Get a builder, which allows the construction of
     *  new Corfu objects.
     */
    protected IObjectBuilder<?> getBuilder() {
        return proxy.getObjectBuilder();
    }

    /** Get the stream ID that this container belongs to.
     *
     * @return
     */
    protected UUID getStreamID() {
        return proxy.getStreamID();
    }

    @Override
    public void setProxy$CORFUSMR(ICorfuSMRProxy<T> proxy) {
        this.proxy = proxy;
    }
}
