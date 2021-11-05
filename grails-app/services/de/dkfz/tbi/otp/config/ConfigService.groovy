/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.config

import grails.gorm.transactions.Transactional
import grails.util.Environment
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.Realm

import java.time.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Transactional
class ConfigService implements ApplicationContextAware {

    protected Map<OtpProperty, String> otpProperties

    static ApplicationContext context
    ProcessingOptionService processingOptionService

    /**
     * Parses the file in the environment variable $OTP_PROPERTIES with fallback to ~/.otp.properties
     * This method must only be used where the ConfigService bean is not available.
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

    @Deprecated
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

    Clock getClock() {
        return Clock.system(timeZoneId)
    }

    ZonedDateTime getZonedDateTime() {
        return ZonedDateTime.now(clock)
    }

    Date getCurrentDate() {
        return Date.from(Instant.from(zonedDateTime))
    }

    boolean useBackdoor() {
        if (Environment.isDevelopmentMode()) {
            return getBooleanValue(OtpProperty.DEVEL_USE_BACKDOOR, false)
        }
        return Environment.getCurrent() == Environment.TEST
    }

    String getBackdoorUser() {
        if (Environment.isDevelopmentMode()) {
            return otpProperties.get(OtpProperty.DEVEL_BACKDOOR_USER)
        } else if (Environment.getCurrent() == Environment.TEST) {
            return 'otp'
        } else {
            return null
        }
    }

    boolean getLdapEnabled() {
        return getBooleanValue(OtpProperty.LDAP_ENABLED)
    }

    String getLdapServer() {
        return otpProperties.get(OtpProperty.LDAP_SERVER)
    }

    String getLdapManagerDistinguishedName() {
        return otpProperties.get(OtpProperty.LDAP_MANAGER_DN)
    }

    String getLdapManagerPassword() {
        return otpProperties.get(OtpProperty.LDAP_MANAGER_PASSWORD)
    }

    String getLdapSearchBase() {
        return otpProperties.get(OtpProperty.LDAP_SEARCH_BASE)
    }

    String getLdapSearchAttribute() {
        return otpProperties.get(OtpProperty.LDAP_SEARCH_ATTRIBUTE)
    }

    String getEnvironmentName() {
        if (otpProperties.get(OtpProperty.CONFIG_ENVIRONMENT_NAME)) {
            return otpProperties.get(OtpProperty.CONFIG_ENVIRONMENT_NAME)
        } else {
            return Environment.getCurrent().name
        }
    }

    PseudoEnvironment getPseudoEnvironment() {
        return PseudoEnvironment.resolveByEnvironmentName(environmentName)
    }

    String getAutoImportSecret() {
        return otpProperties.get(OtpProperty.CONFIG_AUTO_IMPORT_SECRET) ?: ''
    }

    String getDicomInstanceName() {
        return otpProperties.get(OtpProperty.CONFIG_DICOM_INSTANCE_NAME)
    }

    String getWesAuthBaseUrl() {
        return otpProperties.get(OtpProperty.WES_AUTH_BASE_URL)
    }

    String getWesAuthClientId() {
        return otpProperties.get(OtpProperty.WES_AUTH_CLIENT_ID)
    }

    String getWesAuthClientSecret() {
        return otpProperties.get(OtpProperty.WES_AUTH_CLIENT_SECRET)
    }

    private File getAndCheckPathFromProperty(OtpProperty property) {
        return new File(otpProperties.get(property) ?: property.defaultValue)
    }

    private boolean getBooleanValue(OtpProperty otpPropertiesValue) {
        return otpProperties.get(otpPropertiesValue) ? Boolean.parseBoolean(otpProperties.get(otpPropertiesValue)) :
                Boolean.parseBoolean(otpPropertiesValue.defaultValue)
    }

    private boolean getBooleanValue(OtpProperty otpPropertiesValue, boolean defaultValue) {
        return otpProperties.get(otpPropertiesValue) ? Boolean.parseBoolean(otpProperties.get(otpPropertiesValue)) : defaultValue
    }

    @Override
    void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext
    }
}
