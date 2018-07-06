package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import grails.util.*
import groovy.transform.*
import org.joda.time.*
import org.springframework.beans.*
import org.springframework.context.*

import java.time.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

/**
 * This service knows all the configuration parameters like root paths,
 * hosts names, user ids
 *
 *
 */
class ConfigService implements ApplicationContextAware {

    @TupleConstructor
    static enum SshAuthMethod {
        SSH_AGENT("sshagent"),
        KEY_FILE("keyfile"),
        PASSWORD("password"),
        final String configName

        static SshAuthMethod getByConfigName(String configName) {
            return values().find { it.configName == configName }
        }
    }

    protected Map<String, String> otpProperties

    static ApplicationContext context

    ConfigService() {
        Properties properties = new Properties()
        String propertiesFile = System.getenv("OTP_PROPERTIES")
        if (propertiesFile && new File(propertiesFile).canRead()) {
            properties.load(new FileInputStream(propertiesFile))
        } else {
            properties.load(new FileInputStream(System.getProperty("user.home") + System.getProperty("file.separator") + ".otp.properties"))
        }
        this.otpProperties = (Map) properties
    }

    static ConfigService getInstance() {
        return context.getBean("configService")
    }

    static Realm getDefaultRealm() {
        return exactlyOneElement(Realm.findAllByName(ProcessingOptionService.findOptionSafe(ProcessingOption.OptionName.REALM_DEFAULT_VALUE, null, null)))
    }

    File getRootPath() {
        return new File(otpProperties.get("otp.root.path") ?: "")
    }

    File getProcessingRootPath() {
        return new File(otpProperties.get("otp.processing.root.path") ?: "")
    }

    File getLoggingRootPath() {
        return new File(otpProperties.get("otp.logging.root.path") ?: "")
    }

    File getToolsPath() {
        return new File(otpProperties.get("otp.path.tools") ?: "")
    }

    File getProjectSequencePath(Project proj) {
        return new File("${getRootPath().path}/${proj.dirName}/sequencing/")
    }

    String getSshUser() {
        return otpProperties.get("otp.ssh.user") ?: ""
    }

    SshAuthMethod getSshAuthenticationMethod() {
        SshAuthMethod.getByConfigName(otpProperties.get("otp.ssh.authMethod")) ?: SshAuthMethod.SSH_AGENT
    }

    String getSshPassword() {
        return otpProperties.get("otp.ssh.password") ?: ""
    }

    File getSshKeyFile() {
        return new File(otpProperties.get("otp.ssh.keyFile") ?: System.getProperty("user.home") + "/.ssh/id_rsa")
    }

    boolean otpSendsMails() {
        return getBooleanValue("otp.mail.allowOtpToSendMails", false)
    }

    boolean isJobSystemEnabled() {
        return getBooleanValue("otp.jobsystem.start", false)
    }

    @Deprecated
    static DateTimeZone getDateTimeZone() {
        String zoneName = ProcessingOptionService.findOptionSafe(ProcessingOption.OptionName.TIME_ZONE, null, null)
        if (zoneName) {
            return DateTimeZone.forID(zoneName)
        } else {
            return DateTimeZone.default
        }
    }

    static ZoneId getTimeZoneId() {
        String zoneName = ProcessingOptionService.findOptionSafe(ProcessingOption.OptionName.TIME_ZONE, null, null)
        if (zoneName) {
            return ZoneId.of(zoneName)
        } else {
            return ZoneId.systemDefault()
        }
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
            return otpProperties.get("otp.security.backdoorUser")
        } else {
            return null
        }
    }

    String getEnvironmentName() {
        if (otpProperties.get("otp.environment.name")) {
            return otpProperties.get("otp.environment.name")
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
        File file = new File(otpProperties.get(property) ?: defaultValue)
        if (!file.absolute && Environment.getCurrent() == Environment.PRODUCTION) {
            throw new RuntimeException("${property} is \"${file}\", but only an absolute path is allowed.")
        }
        return file
    }

    private boolean getBooleanValue(String otpPropertiesValue, boolean defaultValue) {
        return otpProperties.get(otpPropertiesValue) ? Boolean.parseBoolean(otpProperties.get(otpPropertiesValue)) : defaultValue
    }

    @Override
    void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext
    }
}
