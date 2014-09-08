package de.dkfz.tbi.otp.ngsdata

import static org.springframework.util.Assert.notNull
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
    def servletContext

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

    /**
     * Get the realm for the DKFZ LSDF.
     *
     * @param operationType the {@link Realm.OperationType}
     * @return the realm for the DKFZ LSDF.
     */
    Realm getRealmForDKFZLSDF(Realm.OperationType operationType) {
        def c = Realm.createCriteria()
        Realm realm = c.get {
            and {
                eq('name', 'DKFZ')
                eq('operationType', operationType)
                eq('env', Environment.getCurrent().getName())
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

    /**
     * Generate a map with "prefixes" that can be used in shell scripts in front of executing or copying
     * commands. These are set depending on the cluster that the script is running on and the location of
     * the data of the project, since some actions must be performed remotely. Currently, the following
     * keys are used:
     *
     * <ul>
     * <li><code>exec</code>: for commands that need to be executed, possible remotely</li>
     * <li><code>cp</code>: for copying, using <code>scp</code> if copying remotely
     * <li><code>dest</code>: a prefix for destination directories, possibly containing the hostname
     *    for remote copying
     * </ul>
     *
     * @param project determines how prefixes are set
     * @return a {@link Map} which contains the prefixes
     */
    Map<String, String> clusterSpecificCommandPrefixes(Project project) {
        notNull(project, 'In input project for the method clusterSpecificPrefixes is null')
        Realm realm = getRealmDataManagement(project)
        notNull(realm, "Did not find data management realm for project ${project}")

        Map <String, String> prefix = [
            'exec': 'sh -c',
            'cp': 'cp',
            'dest': '',
        ]

        if (realm.name == 'BioQuant') {
            String hostname = "${realm.unixUser}@${realm.host}"
            prefix.exec = "ssh -p ${realm.port} ${hostname}"
            prefix.cp = "scp -P ${realm.port}"
            prefix.dest = "${hostname}:"
        }
        return prefix
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
}
