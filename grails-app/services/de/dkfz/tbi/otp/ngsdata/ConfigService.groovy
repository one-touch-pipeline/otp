package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.ProcessingException
import grails.util.Environment
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

    Realm getRealm(Project project, Realm.OperationType operationType) {
        def c = Realm.createCriteria()
        Realm realm = c.get {
            and {
                eq("name", project.realmName)
                eq("operationType", operationType)
                eq("env", Environment.getCurrent().getName())
            }
        }
        return realm
    }

    Realm getRealmDataProcessing(Project project) {
        return getRealm(project, Realm.OperationType.DATA_PROCESSING)
    }

    Realm getRealmDataManagement(Project project) {
        return getRealm(project, Realm.OperationType.DATA_MANAGEMENT)
    }

    String getProjectRootPath(Project project) {
        Realm realm = getRealm(project, Realm.OperationType.DATA_MANAGEMENT)
        return realm.rootPath
    }

    String getProjectSequencePath(Project proj) {
        String base = getProjectRootPath(proj)
        return "${base}/${proj.dirName}/sequencing/"
    }

    Realm getRealmForInitialFTPPath(String path) {
        int idx = path.indexOf("/ftp")
        String prefix = path.substring(0, idx)
        return Realm.findByRootPathLikeAndOperationType("${prefix}%", Realm.OperationType.DATA_MANAGEMENT)
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