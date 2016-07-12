package de.dkfz.tbi.otp.ngsdata

import javax.servlet.ServletContext
import org.codehaus.groovy.grails.commons.GrailsApplication
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
    GrailsApplication grailsApplication
    ServletContext servletContext

    static Realm getRealm(Project project, Realm.OperationType operationType) {
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

    static String getProjectRootPath(Project project) {
        Realm realm = getRealm(project, Realm.OperationType.DATA_MANAGEMENT)
        return realm.rootPath
    }

    String getProjectSequencePath(Project proj) {
        String base = getProjectRootPath(proj)
        return "${base}/${proj.dirName}/sequencing/"
    }

    String igvPath() {
        return "http://www.broadinstitute.org/igv/projects/current/igv.php?sessionURL="
    }

    String igvSessionFileServer() {
        String dir = igvSessionFiles()
        return "${grailsApplication.config.grails.serverURL}/static/$dir/"
    }

    String igvSessionFileDirectory() {
        return servletContext.getRealPath(igvSessionFiles())
    }

    private String igvSessionFiles() {
        return "igvSessionFiles"
    }

    String getPbsPassword() {
        return grailsApplication.config.otp.pbs.ssh.password
    }

    File getSshKeyFile() {
        if (grailsApplication.config.otp.pbs.ssh.keyFile) {
            return new File(grailsApplication.config.otp.pbs.ssh.keyFile)
        }
        return null
    }

    boolean useSshAgent() {
        return grailsApplication.config.otp.pbs.ssh.useSshAgent
    }
}
