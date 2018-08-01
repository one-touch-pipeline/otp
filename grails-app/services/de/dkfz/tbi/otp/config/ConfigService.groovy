package de.dkfz.tbi.otp.config

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.util.*
import org.springframework.beans.*
import org.springframework.context.*

import java.time.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class ConfigService implements ApplicationContextAware {

    protected Map<OtpProperty, String> otpProperties

    static ApplicationContext context

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
        this.otpProperties = properties.collectEntries {key, value ->
            OtpProperty otpProperty = OtpProperty.findByKey(key)
            if (!otpProperty) {
                throw new Exception("Found unknown key'${key}' in the otp properties file")
            }
            if (!otpProperty.validator.validate(value)) {
                throw new Exception("The value '${value}' for the key '${key}' is not valid for the check '${otpProperty.validator}'")
            }
            return [(otpProperty): value]
        }
    }

    static ConfigService getInstance() {
        return context.getBean("configService")
    }

    static Realm getDefaultRealm() {
        return exactlyOneElement(Realm.findAllByName(ProcessingOptionService.findOptionSafe(ProcessingOption.OptionName.REALM_DEFAULT_VALUE, null, null)))
    }


    File getRootPath() {
        return new File(otpProperties.get(OtpProperty.PATH_PROJECT_ROOT) ?: "")
    }

    File getScriptOutputPath() {
        return new File(otpProperties.get(OtpProperty.PATH_SCRIPTS) ?: "")
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

    File getProjectSequencePath(Project project) {
        return new File("${getRootPath().path}/${project.dirName}/sequencing/")
    }


    String getSshUser() {
        return otpProperties.get(OtpProperty.SSH_USER) ?: ""
    }

    SshAuthMethod getSshAuthenticationMethod() {
        SshAuthMethod.getByConfigName(otpProperties.get(OtpProperty.SSH_AUTH_METHOD)) ?: OtpProperty.SSH_AUTH_METHOD.defaultValue
    }

    String getSshPassword() {
        return otpProperties.get(OtpProperty.SSH_PASSWORD) ?: ""
    }

    File getSshKeyFile() {
        return new File(otpProperties.get(OtpProperty.SSH_KEY_FILE) ?: OtpProperty.SSH_KEY_FILE.defaultValue)
    }


    boolean otpSendsMails() {
        return getBooleanValue(OtpProperty.CONFIG_ACTIVATE_EMAIL, false)
    }

    boolean isJobSystemEnabled() {
        return getBooleanValue(OtpProperty.CONFIG_JOB_SYSTEM_START, false)
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
