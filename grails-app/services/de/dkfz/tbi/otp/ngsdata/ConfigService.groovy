package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import grails.util.*
import org.joda.time.*

/**
 * This service knows all the configuration parameters like root paths,
 * hosts names, user ids
 *
 *
 */
class ConfigService {

    private Properties otpProperties

    ConfigService() {
        otpProperties = new Properties()
        String propertiesFile = System.getenv("OTP_PROPERTIES")
        if (propertiesFile && new File(propertiesFile).canRead()) {
            otpProperties.load(new FileInputStream(propertiesFile))
        } else {
            otpProperties.load(new FileInputStream(System.getProperty("user.home") + System.getProperty("file.separator") + ".otp.properties"))
        }
    }

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

    String getPbsPassword() {
        return otpProperties.getProperty("otp.ssh.password") ?: ""
    }

    File getSshKeyFile() {
        return new File(otpProperties.getProperty("otp.ssh.keyFile") ?: System.getProperty("user.home") + "/.ssh/id_rsa")
    }

    boolean useSshAgent() {
        return getBooleanValue("otp.ssh.useSshAgent", true)
    }

    boolean otpSendsMails() {
        return getBooleanValue("otp.mail.allowOtpToSendMails", false)
    }

    boolean isJobSystemEnabled() {
        return getBooleanValue("otp.jobsystem.start", false)
    }

    static DateTimeZone getDateTimeZone() {
        return DateTimeZone.forID(ProcessingOptionService.findOptionAssure(ProcessingOption.OptionName.TIME_ZONE, null, null))
    }

    boolean useBackdoor() {
        if (Environment.isDevelopmentMode()) {
            return getBooleanValue("otp.security.useBackdoor", false)
        } else {
            return false
        }
    }

    String getBackdoorUser() {
        if (Environment.isDevelopmentMode()) {
            return otpProperties.getProperty("otp.security.backdoorUser")
        } else {
            return null
        }
    }

    String getEnvironmentName() {
        if (otpProperties.getProperty("otp.environment.name")) {
            return otpProperties.getProperty("otp.environment.name")
        } else {
            return Environment.getCurrent().name
        }
    }

    File getJobLogDirectory() {
        return getAndCheckPathFromProperty("otp.logging.jobLogDir", "logs/jobs/")
    }

    File getStackTracesDirectory() {
        return getAndCheckPathFromProperty('otp.errorLogging.stacktraces', "logs/stacktraces/")
    }

    private File getAndCheckPathFromProperty(String property, String defaultValue) {
        File file = new File(otpProperties.getProperty(property) ?: defaultValue)
        if (!file.absolute && Environment.getCurrent() == Environment.PRODUCTION) {
            throw new RuntimeException("${property} is \"${file}\", but only an absolute path is allowed.")
        }
        return file
    }

    String getSshUser() {
        return otpProperties.getProperty("otp.ssh.user") ?: ""
    }

    private boolean getBooleanValue(String otpPropertiesValue, boolean defaultValue) {
        return otpProperties.getProperty(otpPropertiesValue) ? Boolean.parseBoolean(otpProperties.getProperty(otpPropertiesValue)) : defaultValue
    }

}
