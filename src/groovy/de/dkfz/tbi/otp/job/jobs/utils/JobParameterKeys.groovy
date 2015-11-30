package de.dkfz.tbi.otp.job.jobs.utils

import de.dkfz.tbi.otp.job.processing.AbstractMultiJob

/**
 * A class that contains some valid keys for job input and output parameters.
 *
 */
class JobParameterKeys {

    // The values of all constants should be unique. Non-unique values might lead to confusion and bad things happening.

    /**
     * @deprecated Create/use a subclass of {@link AbstractMultiJob}, then cluster job IDs no longer have to be passed
     * between jobs.
     */
    @Deprecated
    static final String PBS_ID_LIST = '__pbsIds'
    /**
     * @deprecated Create/use a subclass of {@link AbstractMultiJob}, then cluster job IDs no longer have to be passed
     * between jobs.
     */
    @Deprecated
    static final String REALM = '__pbsRealm'
    static final String SCRIPT = 'SCRIPT'

}
