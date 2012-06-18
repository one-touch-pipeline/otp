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

    Realm getRealmForInitialFTPPath(String path) {
        int idx = path.indexOf("/ftp")
        String prefix = path.substring(0, idx)
        return Realm.findByRootPathLike("${prefix}%")
    }

    String igvPath() {
        return "http://www.broadinstitute.org/igv/projects/current/igv.php?sessionURL="
    }

    /*
     * fragile code. Shall be replaced by accessing servlet container
     * configuration.
     */
    String getMyURL(String requestURL) {
        return requestURL.substring(0, requestURL.indexOf("grails"))
    }

    String getPbsPassword() {
        return grailsApplication.config.otp.pbs.ssh.password
    }
}