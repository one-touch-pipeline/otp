package de.dkfz.tbi.otp.dataprocessing

/**
 * Data with a higher processing priority value should be processed before data with a lower processing priority value.
 */
class ProcessingPriority {

    static final short MINIMUM_PRIORITY = Short.MIN_VALUE

    static final short NORMAL_PRIORITY = 0

    static final short FAST_TRACK_PRIORITY = 10000

    static final short MAXIMUM_PRIORITY = Short.MAX_VALUE - 1

    /**
     * A priority value which is strictly greater than the processing value of all data.
     */
    static final short SUPREMUM_PRIORITY = Short.MAX_VALUE

    static {
        assert SUPREMUM_PRIORITY > MAXIMUM_PRIORITY
    }
}
