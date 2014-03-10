package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.ngsdata.Realm

/**
 * Encapsulation class for storing the information about one
 * job running on a PBS system.
 */
class PbsJobInfo implements ClusterJobIdentifier, Serializable {
    /**
     * The ID of the job on the PBS system.
     */
    String pbsId
    /**
     * The Realm the job is running on. If {@code null} it is unknown on which
     * PBS system the job is running. All systems need to be queried.
     */
    Realm realm

    @Override
    public String getClusterJobId() {
        return pbsId
    }

    @Override
    public String toString() {
        return "Cluster job ${clusterJobId} on realm ${realm}"
    }
}
