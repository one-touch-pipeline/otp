package de.dkfz.tbi.otp.config

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.Realm
import grails.util.*
import org.springframework.beans.*
import org.springframework.context.*

import java.time.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class ConfigService implements ApplicationContextAware {

    protected Map<OtpProperty, String> otpProperties

    static ApplicationContext context
    ProcessingOptionService processingOptionService

    /**
     * Parses the file in the environment variable $OTP_PROPERTIES with fallback to ~/.otp.properties
     * This method must only be used in ConfigService, Config.groovy and DataSource.groovy
     */
    static Properties parsePropertiesFile() {
        Properties properties = new Properties()
        String propertiesFile = System.getenv("OTP_PROPERTIES")
        if (propertiesFile && new File(propertiesFile).canRead()) {
            properties.load(new FileInputStream(propertiesFile))
        } else {
            properties.load(new FileInputStream("${System.getProperty("user.home")}${File.separator}.otp.properties"))
        }
        return properties
    }

    ConfigService() {
        Properties properties = parsePropertiesFile()
        this.otpProperties = properties.collectEntries { key, value ->
            OtpProperty otpProperty = OtpProperty.findByKey(key)
            if (otpProperty) {
                return [(otpProperty): value]
            } else {
                return [:]
            }
        }
    }

    static ConfigService getInstance() {
        return context.getBean("configService")
    }

    Realm getDefaultRealm() {
        return exactlyOneElement(Realm.findAllByName(processingOptionService.findOptionAsString(ProcessingOption.OptionName.REALM_DEFAULT_VALUE)))
    }


    File getRootPath() {
        return new File(otpProperties.get(OtpProperty.PATH_PROJECT_ROOT) ?: "")
    }

    File getScriptOutputPath() {
        return new File(otpProperties.get(OtpProperty.PATH_SCRIPTS_OUTPUT) ?: "")
    }

    File getProcessingRootPath() {
        return new File(otpProperties.get(OtpProperty.PATH_PROCESSING_ROOT) ?: "")
    }

    File getToolsPath() {
        return new File(otpProperties.get(OtpProperty.PATH_TOOLS) ?: "")
    }


    File getLoggingRootPath() {
        return new File(otpProperties.get(OtpProperty.PATH_CLUSTER_LOGS_OTP) ?: "")
    }

    File getJobLogDirectory() {
        return getAndCheckPathFromProperty(OtpProperty.PATH_JOB_LOGS)
    }

    File getStackTracesDirectory() {
        return getAndCheckPathFromProperty(OtpProperty.PATH_STACK_TRACES)
    }

    File getRoddyPath() {
        return new File(otpProperties.get(OtpProperty.PATH_RODDY) ?: "")
    }

    // this path is where the metadata file is copied
    File getSeqCenterInboxPath() {
        return new File(otpProperties.get(OtpProperty.PATH_SEQ_CENTER_INBOX) ?: "")
    }

    String getSshUser() {
        return otpProperties.get(OtpProperty.SSH_USER) ?: ""
    }

    SshAuthMethod getSshAuthenticationMethod() {
        return SshAuthMethod.getByConfigName(otpProperties.get(OtpProperty.SSH_AUTH_METHOD)) ?:
                SshAuthMethod.getByConfigName(OtpProperty.SSH_AUTH_METHOD.defaultValue)
    }

    String getSshPassword() {
        return otpProperties.get(OtpProperty.SSH_PASSWORD) ?: ""
    }

    File getSshKeyFile() {
        return new File(otpProperties.get(OtpProperty.SSH_KEY_FILE) ?: OtpProperty.SSH_KEY_FILE.defaultValue)
    }


    boolean otpSendsMails() {
        return getBooleanValue(OtpProperty.CONFIG_EMAIL_ENABLED, false)
    }

    boolean isJobSystemEnabled() {
        return getBooleanValue(OtpProperty.CONFIG_JOB_SYSTEM_START, false)
    }

    ZoneId getTimeZoneId() {
        String zoneName = processingOptionService.findOptionAsString(ProcessingOption.OptionName.TIME_ZONE)
        if (zoneName) {
            return ZoneId.of(zoneName)
        } else {
            return ZoneId.systemDefault()
        }
    }

    boolean useBackdoor() {
        if (Environment.isDevelopmentMode()) {
            return getBooleanValue(OtpProperty.DEVEL_USE_BACKDOOR, false)
        } else {
            return false
        }
    }

    String getBackdoorUser() {
        if (Environment.isDevelopmentMode()) {
            return otpProperties.get(OtpProperty.DEVEL_BACKDOOR_USER)
        } else {
            return null
        }
    }

    String getLdapServer() {
        return otpProperties.get(OtpProperty.LDAP_SERVER)
    }

    String getLdapSearchBase() {
        return otpProperties.get(OtpProperty.LDAP_SEARCH_BASE)
    }

    String getLdapManagerDistinguishedName() {
        return otpProperties.get(OtpProperty.LDAP_MANAGER_DN)
    }

    String getLdapManagerPassword() {
        return otpProperties.get(OtpProperty.LDAP_MANAGER_PASSWORD)
    }

    String getEnvironmentName() {
        if (otpProperties.get(OtpProperty.CONFIG_ENVIRONMENT_NAME)) {
            return otpProperties.get(OtpProperty.CONFIG_ENVIRONMENT_NAME)
        } else {
            return Environment.getCurrent().name
        }
    }

    String getDicomInstanceName() {
        return otpProperties.get(OtpProperty.CONFIG_DICOM_INSTANCE_NAME)
    }

    int getDicomInstanceId() {
        return otpProperties.get(OtpProperty.CONFIG_DICOM_INSTANCE_NAME, 1)
    }

    private File getAndCheckPathFromProperty(OtpProperty property) {
        File file = new File(otpProperties.get(property) ?: property.defaultValue)
        if (!file.absolute && Environment.getCurrent() == Environment.PRODUCTION) {
            throw new RuntimeException("${property} is \"${file}\", but only an absolute path is allowed.")
        }
        return file
    }

    private boolean getBooleanValue(OtpProperty otpPropertiesValue, boolean defaultValue) {
        return otpProperties.get(otpPropertiesValue) ? Boolean.parseBoolean(otpProperties.get(otpPropertiesValue)) : defaultValue
    }

    @Override
    void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext
    }
}
