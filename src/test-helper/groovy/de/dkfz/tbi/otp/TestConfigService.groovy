/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp

import grails.util.Environment
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.workflow.shared.WorkflowTestException
import de.dkfz.tbi.otp.workflowTest.WorkflowTestProperty

import java.nio.file.*
import java.time.*

@SuppressWarnings('JavaIoPackageAccess')
@Slf4j
@Qualifier('TestConfigService')
class TestConfigService extends ConfigService {

    Clock fixedClock

    static Map cleanProperties

    /**
     * Properties set by workflow test initialization scripts
     * @see WorkflowTestProperty
     */
    protected Map<WorkflowTestProperty, String> workflowTestProperties = [:]

    /**
     * Do no use the constructor in integration and workflow tests, but use the autowired config service instead
     */
    @SuppressWarnings('UnsafeImplementationAsMap')
    TestConfigService(Map<OtpProperty, String> properties = [:]) {
        super()

        /*
         * As an extra protection against accidentally using production-settings in test-environments,
         * filter our properties. We wouldn't want to accidentally use the production DB or something.
         */
        if (Environment.current.name == "WORKFLOW_TEST") {
            otpProperties = otpProperties.findAll {
                it.key.usedIn.contains(UsedIn.WORKFLOW_TEST)
            }
        } else {
            otpProperties = otpProperties.findAll {
                it.key in [
                        OtpProperty.TEST_TESTING_GROUP,
                        OtpProperty.TEST_TESTING_PROJECT_UNIX_GROUP,
                        OtpProperty.PATH_JOB_LOGS,
                ]
            }
            otpProperties += [
                    (OtpProperty.WES_URL)                : '-',
                    (OtpProperty.WES_AUTH_TOKEN_URI)     : '-',
                    (OtpProperty.WES_AUTH_CLIENT_ID)     : '-',
                    (OtpProperty.WES_AUTH_CLIENT_SECRET) : '-',
                    (OtpProperty.WES_BASE_DATA_DIRECTORY): '/tmp',
            ]
        }
        otpProperties += [
                (OtpProperty.PATH_PROJECT_ROOT)     : Paths.get(TestCase.uniqueNonExistentPath.path, 'root_path').toString(),
                (OtpProperty.PATH_PROCESSING_ROOT)  : Paths.get(TestCase.uniqueNonExistentPath.path, 'processing_root_path').toString(),
                (OtpProperty.PATH_CLUSTER_LOGS_OTP) : Paths.get(TestCase.uniqueNonExistentPath.path, 'logging_root_path').toString(),
                (OtpProperty.OIDC_ENABLED)          : false,
                (OtpProperty.OIDC_CLIENT)           : '-',
                (OtpProperty.OIDC_REDIRECT_URI)     : '-',
                (OtpProperty.KEYCLOAK_SERVER)       : '-',
                (OtpProperty.KEYCLOAK_CLIENT_ID)    : '-',
                (OtpProperty.KEYCLOAK_CLIENT_SECRET): '-',
                (OtpProperty.LDAP_SERVER)           : 'ldap://test-ldap:123',
                (OtpProperty.LDAP_SEARCH_BASE)      : 'cn=test',
        ]
        cleanProperties = new HashMap<>(otpProperties)

        otpProperties += properties

        context = [
                getBean: { String beanName ->
                    if (beanName == "configService") {
                        return this
                    }
                    throw new IllegalArgumentException("Test tried to get a bean from application context that was not the ConfigService: \"${beanName}\"")
                },
        ] as ApplicationContext

        fileSystemService = new TestFileSystemService()
    }

    /**
     * Do not use the constructor in integration and workflow tests, but use the autowired config service instead
     */
    TestConfigService(Path basePath, Map<OtpProperty, String> properties = [:]) {
        this([
                (OtpProperty.PATH_PROJECT_ROOT)    : basePath.resolve('root').toString(),
                (OtpProperty.PATH_PROCESSING_ROOT) : basePath.resolve('processing').toString(),
                (OtpProperty.PATH_CLUSTER_LOGS_OTP): basePath.resolve('logging').toString(),
        ] + properties)
    }

    void addOtpProperty(OtpProperty key, String value) {
        otpProperties.put(key, value)
    }

    void addOtpProperties(Path baseFolder, Map<OtpProperty, String> properties = [:]) {
        addOtpProperty(OtpProperty.PATH_PROJECT_ROOT, Files.createDirectory(baseFolder.resolve('root')).toString())
        addOtpProperty(OtpProperty.PATH_PROCESSING_ROOT, Files.createDirectory(baseFolder.resolve('processing')).toString())
        addOtpProperty(OtpProperty.PATH_CLUSTER_LOGS_OTP, Files.createDirectory(baseFolder.resolve('logging')).toString())
        otpProperties.putAll(properties)
    }

    void clean() {
        this.otpProperties = new HashMap<>(cleanProperties)
        this.workflowTestProperties = [:]
        this.fixedClock = null
    }

    @Override
    ZoneId getTimeZoneId() {
        return ZoneId.systemDefault()
    }

    @Override
    Clock getClock() {
        return fixedClock ?: super.clock
    }

    @SuppressWarnings("ParameterCount")
    void fixClockTo(int year = 2000, int month = 1, int dayOfMonth = 1, int hour = 0, int minute = 0, int second = 0) {
        ZoneId zoneId = ZoneId.systemDefault()
        fixClockTo(ZonedDateTime.of(year, month, dayOfMonth, hour, minute, second, 0, zoneId))
    }

    void fixClockTo(ZonedDateTime zonedDateTime) {
        ZoneId zoneId = ZoneId.systemDefault()
        fixedClock = Clock.fixed(Instant.from(zonedDateTime), zoneId)
    }

    String getTestingGroup() {
        String testingGroup = getAndAssertValue(OtpProperty.TEST_TESTING_GROUP)
        assertTestingGroupIsNotPrimary(testingGroup)
        assertTestGroupsDiffer()
        return testingGroup
    }

    String getWorkflowProjectUnixGroup() {
        assertTestGroupsDiffer()
        return getAndAssertValue(OtpProperty.TEST_TESTING_PROJECT_UNIX_GROUP)
    }

    static String getPrimaryGroup() {
        return LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("id --group --name").trim()
    }

    /**
     * Standard testing group shouldn't be the user's primary group.
     *
     * When testing file permissions/groups management, the test-data is usually created with the default (primary) group on the /tmp filesystem,
     * and then changed by the software-under-test to the testing-group. This behaviour cannot be verified if the testing-group is the same as default.
     */
    private void assertTestingGroupIsNotPrimary(String testingGroup) {
        assert testingGroup != primaryGroup: "Standard testing group shouldn't be the user's primary group, please update your .otp.properties!"
    }

    /**
     * OTP runtime assumes that lots of data is generated with the 'default' group, and then chgrp-ed to the project group.
     * This functionality can only be tested if both groups differ.
     */
    private void assertTestGroupsDiffer() {
        String testingGroup = getAndAssertValue(OtpProperty.TEST_TESTING_GROUP)
        String projectGroup = getAndAssertValue(OtpProperty.TEST_TESTING_PROJECT_UNIX_GROUP)

        assert testingGroup != projectGroup:
                "'${OtpProperty.TEST_TESTING_GROUP.key}' with value '${testingGroup}' does not differ from " +
                        "'${OtpProperty.TEST_TESTING_PROJECT_UNIX_GROUP.key}' with value '${projectGroup}.'" +
                        "OTP needs the primary/'default' group and the 'project' group to differ, in order to test if data re-owning works."
    }

    // ==================================================
    // Getters for script-based workflow test properties
    // ==================================================

    /**
     * Store workflow test properties into TestConfig.workflowTestProperties
     * Note: This method just adds properties and no validation is done.
     * @see #validateWorkflowTestProperties for validation.
     */
    void storeWorkflowTestProperties(Map<WorkflowTestProperty, String> properties) {
        // Store in TestConfig if configService is available (when running in test context)
        log.info("  Storing ${properties.size()} properties in TestConfig.workflowTestProperties")
        workflowTestProperties.putAll(properties)

        // Log each property for debugging
        properties.each { key, value ->
            log.info("  - ${key} = ${value}")
        }
    }

    /**
     * Validate workflow test properties and ensure all required properties exist in WORKFLOW_TEST environment
     * In addition, all property values are checked with their validator for validity.
     * Throws WorkflowTestException if any required property is missing or invalid.
     */
    void validateWorkflowTestProperties() {
        // Check for missing required properties
        Set<WorkflowTestProperty> missingProperties = WorkflowTestProperty.values().findAll { !workflowTestProperties.containsKey(it) }
        if (missingProperties) {
            String message = "Required properties are missing: ${missingProperties*.key.join(', ')}"
            log.error(message)
            throw new WorkflowTestException(message)
        }

        // Validate each property using its validator
        List<String> invalidProperties = workflowTestProperties.collect { WorkflowTestProperty property, String value ->
            return property.validator && !property.validator.validate(String.valueOf(value))
                    ? "The value '${value}' for the key '${property.key}' is not valid for the check '${property.validator}'"
                    : null
        }.findAll { it != null } as List<String>
        if (invalidProperties) {
            String message = "Invalid properties found:\n ${invalidProperties.join('\n')}"
            log.error(message)
            throw new WorkflowTestException(message)
        }
    }

    /**
     * Get workflow test input directory from script-based properties
     */
    Path getWorkflowTestInputRootDir() {
        FileSystem fileSystem = fileSystemService.remoteFileSystem
        return fileSystem.getPath(workflowTestProperties.get(WorkflowTestProperty.TEST_WORKFLOW_INPUT_DIR))
    }

    /**
     * Get workflow test result directory from script-based properties
     */
    Path getWorkflowTestResultRootDir() {
        FileSystem fileSystem = fileSystemService.remoteFileSystem
        return fileSystem.getPath(workflowTestProperties.get(WorkflowTestProperty.TEST_WORKFLOW_RESULT_DIR))
    }

    /**
     * Get Roddy shared files base directory from script-based properties
     */
    Path getWorkflowTestRoddySharedFilesBaseDir() {
        FileSystem fileSystem = fileSystemService.remoteFileSystem
        return fileSystem.getPath(workflowTestProperties.get(WorkflowTestProperty.TEST_WORKFLOW_RODDY_SHARED_FILES_BASE_DIRECTORY))
    }

    /**
     * Get Roddy virtual environments directory from script-based properties
     */
    Path getWorkflowTestRoddyVirtualEnvsBaseDir() {
        FileSystem fileSystem = fileSystemService.remoteFileSystem
        return fileSystem.getPath(workflowTestProperties.get(WorkflowTestProperty.TEST_WORKFLOW_RODDY_VIRTUAL_ENVS_DIRECTORY))
    }

    /**
     * Get workflow test queue from script-based properties
     */
    String getWorkflowTestQueue() {
        return workflowTestProperties.get(WorkflowTestProperty.TEST_WORKFLOW_QUEUE)
    }

    /**
     * Get workflow test config suffix from script-based properties
     */
    String getWorkflowTestConfigSuffix() {
        return workflowTestProperties.get(WorkflowTestProperty.TEST_WORKFLOW_CONFIG_SUFFIX)
    }

    /**
     * Get the workflow test initialization script file.
     * @return the Path object representing the init script
     */
    Path getWorkflowTestInitScript() {
        return Paths.get(getAndAssertValue(OtpProperty.TEST_WORKFLOW_INIT_SCRIPT))
    }

    private String getAndAssertValue(OtpProperty property) {
        String value = otpProperties.get(property)
        assert value: "'${property}' (${property.key}) is not set in otp.properties"
        return value
    }
}
