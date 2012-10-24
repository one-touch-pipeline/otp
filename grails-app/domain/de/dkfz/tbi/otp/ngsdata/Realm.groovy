package de.dkfz.tbi.otp.ngsdata

class Realm implements Serializable {

    String name                        // name of the realm
    String env                         // environment from grails

    enum OperationType {DATA_MANAGEMENT, DATA_PROCESSING}
    OperationType operationType

    String rootPath                    // mount path of the file system with data
    String processingRootPath          // mount path for the file system with results data
    String programsRootPath            // location of programs
    String webHost                     // web address
    String host                        // PBS head address
    int port
    String unixUser
    int timeout
    String pbsOptions                  // realm dependent options of the PBS system

    static constraints = {
    }
}
