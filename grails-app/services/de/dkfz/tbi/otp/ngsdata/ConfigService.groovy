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
        return proj.realm.rootPath
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

    String getPbsPassword() {
        return grailsApplication.config.otp.pbs.ssh.password
    }
}
