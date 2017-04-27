package org.corfudb.runtime.view;

/** Currently, this class holds static constants used
 * to represent addresses. In the future, it may replace the
 * use of long depending on performance.
 *
 * Created by mwei on 1/6/17.
 */
public class Address {

    /**
     * @param addr
     * @return true for all flag non-address constants
     */
    public static boolean nonAddress(long addr) { return addr < 0; }

    /**
     * @param addr
     * @return true is addr is a legitimate address; false for all flag non-address constants
     */
    public static boolean isAddress(long addr) { return addr >= 0; }

    /**
     * @return a constant which can be used as the base for address iterations
     */
    public static long minAddress() { return 0L; }

    /**
     * @return A constant which can be used in loops going down up to hitting a non-address.
     */
    public static long maxNonAddress() { return -1L; }

    /** The maximum address. */
    public static final long MAX = Long.MAX_VALUE;

    /** A runtime (only) constant,
     * Indicating a stream address is not cached.
     */
    public static final long NEVER_READ = -1L;

    /** Aborted request constant. Used to indicate an attempted read, but
     * was rejected at the request of the client.
     */
    public static final long ABORTED = -2L;

    /** Not found constant. Used to indicate that a search for an entry
     * did not result in a entry.
     */
    public static final long NOT_FOUND = -3L;

    /** Optimistic constant. Used to indicate that an update is
     * optimistic.
     */
    public static final long OPTIMISTIC = -4L;

    /** Constant to indicate that no backpointer is available for
     * the given stream (due to reset).
     */
    public static final long NO_BACKPOINTER = -5L;

    /** A non-existing address constant.
     * Indicating that an address was request, but no match exists.
     */
    public static final long NON_EXIST = -6L;
}
