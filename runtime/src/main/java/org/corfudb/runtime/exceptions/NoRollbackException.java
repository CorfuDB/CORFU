package org.corfudb.runtime.exceptions;

import java.util.Optional;

import lombok.Getter;
import org.corfudb.protocols.logprotocol.SMREntry;

/**
 * Created by mwei on 11/21/16.
 */
public class NoRollbackException extends RuntimeException {

    @Getter
    private final long undoableEntryVersion;

    public NoRollbackException(long rollbackVersion) {
        super("Can't roll back due to non-undoable exception "
                + " but need "
                + rollbackVersion + " so can't undo");
        undoableEntryVersion = rollbackVersion;
    }

    public NoRollbackException(long address, long rollbackVersion) {
        super("Could only roll back to " + address + " but need "
                + rollbackVersion + " so can't undo");
        undoableEntryVersion = address;
    }

    public NoRollbackException(Optional<SMREntry> entry, long address, long rollbackVersion) {
        super("Can't roll back due to " +
                (entry.isPresent() ?
                entry.get().getSMRMethod() : "Unknown Entry")
                + "@"
                + address
                + " but need "
                + rollbackVersion + " so can't undo");
        undoableEntryVersion = address;
    }
}
