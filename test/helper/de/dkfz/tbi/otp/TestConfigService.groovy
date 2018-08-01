package de.dkfz.tbi.otp

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.util.*
import org.springframework.context.*

class TestConfigService extends ConfigService {

    static Map cleanProperties

    TestConfigService (Map<OtpProperty, String> properties = [:]) {
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
                        OtpProperty.TEST_WORKFLOW_ROOTDIR,

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
                }
        ] as ApplicationContext
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

    File getWorkflowTestRootDir() {
        return new File(getAndAssertValue(OtpProperty.TEST_WORKFLOW_ROOTDIR))
    }


    private String getAndAssertValue(OtpProperty property) {
        String value = otpProperties.get(property)
        assert value : "'${property}' is not set in otp.properties"
        return value
    }
}
