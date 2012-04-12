package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.ProcessingException

/**
 * This service knows all the configuration parameters like root paths,
 * hosts names, user ids
 * 
 *
 */
class ConfigService {

    /**
     * Dependency injection grails application
     */
    def grailsApplication

    String getProjectRootPath(Project proj) {
        String host = proj.host
        switch (host) {
            case "BioQuant":
                return "$ROOT_PATH/project/"
            case "DKFZ":
                return "$ROOT_PATH/project/"
            default:
                throw new Exception()
        }
    }

    String getProjectSequencePath(Project proj) {
        String base = getProjectRootPath(proj)
        return "${base}/${proj.dirName}/sequencing/"
    }

    String igvPath() {
        return "http://www.broadinstitute.org/igv/projects/current/igv.php?sessionURL="
    }

    String dataWebServer() {
        return "https://otp.local/"
    }

    String otpWebServer() {
        return "https://otp.local/otpdevel/"
    }

    String getPbsHost(String realm) {
        switch (realm) {
            case "BioQuant":
                return grailsApplication.config.otp.pbs.bioquant
            case "DKFZ":
                return grailsApplication.config.otp.pbs.dkfz
            default:
                throw new ProcessingException("No valid realm specified.")
        }
    }

    String getPbsPort() {
        return grailsApplication.config.otp.pbs.ssh.port
    }

    String getPbsTimeout() {
        return grailsApplication.config.otp.pbs.ssh.timeout
    }

    String getPbsUser() {
        return grailsApplication.config.otp.pbs.ssh.username
    }

    String getPbsPassword() {
        return grailsApplication.config.otp.pbs.ssh.password
    }
}
