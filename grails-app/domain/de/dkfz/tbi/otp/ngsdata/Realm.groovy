package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.utils.*


class Realm implements Entity, Serializable {

    static final String LATEST_DKFZ_REALM = 'DKFZ_13.1'

    public enum Cluster {
        DKFZ,
        BIOQUANT
    }

    public enum JobScheduler {
        LSF,
        PBS,
    }


    String name                        // name of the realm
    String env                         // environment from grails

    enum OperationType {DATA_MANAGEMENT, DATA_PROCESSING}
    OperationType operationType

    /**
     * Reference to the cluster. It is used to identify the cluster in other code.
     * The following properties of the realm are depend on the used cluster:
     * <ul>
     * <li>{@link #host}</li>
     * <li>{@link #port}</li>
     * <li>{@link #unixUser}</li>
     * <li>{@link #timeout}</li>
     * <li>{@link #defaultJobSubmissionOptions}</li>
     * </ul>
     * Therefore these properties should have the same value for the same cluster name.
     */
    Cluster cluster

    String rootPath                    // mount path of the file system with data
    String processingRootPath          // mount path for the file system with results data
    String programsRootPath            // location of programs
    String loggingRootPath             // mount path of the file system with logging data (needs to be read-write)
    String stagingRootPath             // path where OTP is able to write
    String webHost                     // web address

    JobScheduler jobScheduler
    String host                         // job submission host name
    int port                            // job submission host port
    String unixUser
    int timeout
    String defaultJobSubmissionOptions  // default options for job submission

    static constraints = {
        // TODO OTP-1067: Add validation on the paths
        loggingRootPath blank:false, nullable:false
        stagingRootPath blank:true, nullable:true
        defaultJobSubmissionOptions validator: {
            ClusterJobSubmissionOptionsService.validateJsonString(it)
        }
    }

    @Override
    String toString() {
        return "Realm ${id} ${name} ${operationType} ${env}"
    }
}
