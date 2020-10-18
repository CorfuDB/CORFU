package org.corfudb.infrastructure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.corfudb.runtime.proto.service.CorfuMessage.RequestPayloadMsg;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RequestHandler {
    /**
     * Returns the message payload type
     * @return the type of Corfu message request
     */
    RequestPayloadMsg.PayloadCase type();
}
