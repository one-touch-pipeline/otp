package de.dkfz.tbi.otp.ngsdata

class Realm implements Serializable {

    /**
     * Defines the available clusters.
     * It allowed to reference to a cluster about it names.
     * The following properties of the realm are depend on the used cluster:
     * <ul>
     * <li>{@link #host}</li>
     * <li>{@link #port}</li>
     * <li>{@link #unixUser}</li>
     * <li>{@link #timeout}</li>
     * <li>{@link #pbsOptions}</li>
     * </ul>
     *
     *
     */
    public enum Cluster {
        DKFZ,
        BIOQUANT
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
     * <li>{@link #pbsOptions}</li>
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
    String host                        // PBS head address
    int port
    String unixUser
    int timeout
    String pbsOptions                  // realm dependent options of the PBS system

    static constraints = {
        loggingRootPath blank:false, nullable:false
        stagingRootPath blank:true, nullable:true
    }

    @Override
    String toString() {
        return "Realm ${id} ${name} ${operationType} ${env}"
    }
}
