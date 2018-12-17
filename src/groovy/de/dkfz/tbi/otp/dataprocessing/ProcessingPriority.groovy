package de.dkfz.tbi.otp.dataprocessing

import groovy.transform.TupleConstructor

/**
 * Data with a higher processing priority value should be processed before data with a lower processing priority value.
 */
@TupleConstructor
enum ProcessingPriority {

    MINIMUM(Short.MIN_VALUE),

    NORMAL(0 as short),

    FAST_TRACK(10000 as short),

    MAXIMUM(Short.MAX_VALUE - 1 as short),

    /**
     * A priority value which is strictly greater than the processing value of all data.
     */
    SUPREMUM(Short.MAX_VALUE),

    final short priority

    static final List<ProcessingPriority> displayPriorities = [MINIMUM, NORMAL, FAST_TRACK]

    static getByPriorityNumber(short priority) {
        return values().find { it.priority == priority }
    }
}
