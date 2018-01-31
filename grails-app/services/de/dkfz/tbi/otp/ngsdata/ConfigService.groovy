package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import grails.util.*
import org.joda.time.*
import org.springframework.beans.*
import org.springframework.context.*

/**
 * This service knows all the configuration parameters like root paths,
 * hosts names, user ids
 *
 *
 */
class ConfigService implements ApplicationContextAware {

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

    static Realm getDefaultRealm() {
        return CollectionUtils.exactlyOneElement(Realm.findAllByName(ProcessingOptionService.findOptionSafe(ProcessingOption.OptionName.REALM_DEFAULT_VALUE, null, null)))
    }

    static File getRootPathFromSelfFoundContext() {
        ConfigService configServiceBean = context.getBean("configService")
        return configServiceBean.getRootPath()
    }

    File getRootPath() {
        return new File(otpProperties.get("otp.root.path") ?: "")
    }

    static File getProcessingRootPathFromSelfFoundContext() {
        ConfigService configServiceBean = context.getBean("configService")
        return configServiceBean.getProcessingRootPath()
    }

    File getProcessingRootPath() {
        return new File(otpProperties.get("otp.processing.root.path") ?: "")
    }

    static File getLoggingRootPathFromSelfFoundContext() {
        ConfigService configServiceBean = context.getBean("configService")
        return configServiceBean.getLoggingRootPath()
    }

    File getLoggingRootPath() {
        return new File(otpProperties.get("otp.logging.root.path") ?: "")
    }

    static File getStagingRootPathFromSelfFoundContext() {
        ConfigService configServiceBean = context.getBean("configService")
        return configServiceBean.getStagingRootPath()
    }

    File getStagingRootPath() {
        return new File(otpProperties.get("otp.staging.root.path") ?: "")
    }

    File getProjectSequencePath(Project proj) {
        return new File("${getRootPath().path}/${proj.dirName}/sequencing/")
    }

    String getPbsPassword() {
        return otpProperties.get("otp.ssh.password") ?: ""
    }

    File getSshKeyFile() {
        return new File(otpProperties.get("otp.ssh.keyFile") ?: System.getProperty("user.home") + "/.ssh/id_rsa")
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

    String getSshUser() {
        return otpProperties.get("otp.ssh.user") ?: ""
    }

    private boolean getBooleanValue(String otpPropertiesValue, boolean defaultValue) {
        return otpProperties.get(otpPropertiesValue) ? Boolean.parseBoolean(otpProperties.get(otpPropertiesValue)) : defaultValue
    }

    @Override
    void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext
    }
}
