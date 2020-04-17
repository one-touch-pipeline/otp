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
package de.dkfz.tbi.otp

import grails.util.Environment
import org.springframework.context.ApplicationContext

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.LocalShellHelper

@SuppressWarnings('JavaIoPackageAccess')
class TestConfigService extends ConfigService {

    static Map cleanProperties

    @SuppressWarnings('UnsafeImplementationAsMap')
    TestConfigService(Map<OtpProperty, String> properties = [:]) {
        super()

        /*
         * As an extra protection against accidentally using production-settings in test-environments,
         * filter our properties. We wouldn't want to accidentally use the production DB or something.
         */
        if (Environment.current.name == "WORKFLOW_TEST") {
            otpProperties = otpProperties.findAll {
                it.key in [
                        OtpProperty.SSH_AUTH_METHOD,
                        OtpProperty.SSH_KEY_FILE,
                        OtpProperty.SSH_PASSWORD,

                        OtpProperty.PATH_TOOLS,
                        OtpProperty.PATH_RODDY,

                        OtpProperty.TEST_WORKFLOW_ACCOUNT,
                        OtpProperty.TEST_WORKFLOW_HOST,
                        OtpProperty.TEST_WORKFLOW_SCHEDULER,
                        OtpProperty.TEST_WORKFLOW_INPUT_DIR,
                        OtpProperty.TEST_WORKFLOW_RESULT_DIR,
                        OtpProperty.TEST_TESTING_GROUP,
                        OtpProperty.TEST_TESTING_PROJECT_UNIX_GROUP,
                        OtpProperty.TEST_WORKFLOW_RODDY_SHARED_FILES_BASE_DIRECTORY,
                ]
            }
        } else {
            otpProperties = otpProperties.findAll {
                it.key in [
                        OtpProperty.TEST_TESTING_GROUP,
                        OtpProperty.TEST_TESTING_PROJECT_UNIX_GROUP,
                ]
            }
        }
        otpProperties += [
                (OtpProperty.SSH_USER)             : "user",
                (OtpProperty.PATH_PROJECT_ROOT)    : TestCase.uniqueNonExistentPath.path + '/root_path',
                (OtpProperty.PATH_PROCESSING_ROOT) : TestCase.uniqueNonExistentPath.path + '/processing_root_path',
                (OtpProperty.PATH_CLUSTER_LOGS_OTP): TestCase.uniqueNonExistentPath.path + '/logging_root_path',

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
    }

    TestConfigService(File baseFolder, Map<OtpProperty, String> properties = [:]) {
        this([
                (OtpProperty.PATH_PROJECT_ROOT)   : new File(baseFolder, 'root').path,
                (OtpProperty.PATH_PROCESSING_ROOT): new File(baseFolder, 'processing').path,
        ] + properties)
    }

    void setOtpProperty(OtpProperty key, String value) {
        otpProperties.put(key, value)
    }

    void clean() {
        this.otpProperties = new HashMap<>(cleanProperties)
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
        assert testingGroup != getPrimaryGroup() : "Standard testing group shouldn't be the user's primary group, please update your .otp.properties!"
    }

    /**
     * OTP runtime assumes that lots of data is generated with the 'default' group, and then chgrp-ed to the project group.
     * This functionality can only be tested if both groups differ.
     */
    private void assertTestGroupsDiffer() {
        String testingGroup = getAndAssertValue(OtpProperty.TEST_TESTING_GROUP)
        String projectGroup = getAndAssertValue(OtpProperty.TEST_TESTING_PROJECT_UNIX_GROUP)

        assert testingGroup != projectGroup:
                "'${OtpProperty.TEST_TESTING_GROUP.key             }' with value '${testingGroup}' does not differ from " +
                "'${OtpProperty.TEST_TESTING_PROJECT_UNIX_GROUP.key}' with value '${projectGroup}.'" +
                "OTP needs the primary/'default' group and the 'project' group to differ, in order to test if data re-owning works."
    }

    String getWorkflowTestAccountName() {
        return getAndAssertValue(OtpProperty.TEST_WORKFLOW_ACCOUNT)
    }

    Realm.JobScheduler getWorkflowTestScheduler() {
        return Realm.JobScheduler.valueOf(getAndAssertValue(OtpProperty.TEST_WORKFLOW_SCHEDULER))
    }

    String getWorkflowTestHost() {
        return getAndAssertValue(OtpProperty.TEST_WORKFLOW_HOST)
    }

    File getWorkflowTestInputRootDir() {
        return new File(getAndAssertValue(OtpProperty.TEST_WORKFLOW_INPUT_DIR))
    }

    File getWorkflowTestResultRootDir() {
        return new File(getAndAssertValue(OtpProperty.TEST_WORKFLOW_RESULT_DIR))
    }

    File getWorkflowTestRoddySharedFilesBaseDir() {
        return new File(getAndAssertValue(OtpProperty.TEST_WORKFLOW_RODDY_SHARED_FILES_BASE_DIRECTORY))
    }

    private String getAndAssertValue(OtpProperty property) {
        String value = otpProperties.get(property)
        assert value: "'${property}' (${property.key}) is not set in otp.properties"
        return value
    }
}
