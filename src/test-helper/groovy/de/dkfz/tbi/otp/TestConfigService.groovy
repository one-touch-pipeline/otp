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

@SuppressWarnings('JavaIoPackageAccess')
class TestConfigService extends ConfigService {

    static Map cleanProperties

    @SuppressWarnings('UnsafeImplementationAsMap')
    TestConfigService(Map<OtpProperty, String> properties = [:]) {
        super()
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
                ]
            }
        } else {
            otpProperties = otpProperties.findAll {
                it.key in [
                        OtpProperty.TEST_TESTING_GROUP,
                ]
            }
        }
        otpProperties += [
                (OtpProperty.SSH_USER)             : "user",
                (OtpProperty.PATH_PROJECT_ROOT)    : TestCase.getUniqueNonExistentPath().path + '/root_path',
                (OtpProperty.PATH_PROCESSING_ROOT) : TestCase.getUniqueNonExistentPath().path + '/processing_root_path',
                (OtpProperty.PATH_CLUSTER_LOGS_OTP): TestCase.getUniqueNonExistentPath().path + '/logging_root_path',

        ]
        cleanProperties = new HashMap<>(otpProperties)

        otpProperties += properties

        context = [
                getBean: { String beanName ->
                    if (beanName == "configService") {
                        return this
                    } else {
                        assert false
                    }
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
        assert testingGroup != TestCase.primaryGroup(): "'${OtpProperty.TEST_TESTING_GROUP.key}' does not differ from your primary group"
        return testingGroup
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


    private String getAndAssertValue(OtpProperty property) {
        String value = otpProperties.get(property)
        assert value: "'${property}' is not set in otp.properties"
        return value
    }
}
