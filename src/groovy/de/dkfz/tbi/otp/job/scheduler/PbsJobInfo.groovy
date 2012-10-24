package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.otp.ngsdata.Realm

/**
 * Encapsulation class for storing the information about one
 * job running on a PBS system.
 */
class PbsJobInfo implements Serializable {
    /**
     * The ID of the job on the PBS system.
     */
    String pbsId
    /**
     * The Realm the job is running on. If {@code null} it is unknown on which
     * PBS system the job is running. All systems need to be queried.
     */
    Realm realm
}
