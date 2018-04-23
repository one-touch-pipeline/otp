package de.dkfz.tbi.otp

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.util.*
import org.springframework.context.*

class TestConfigService extends ConfigService {

    static Map cleanProperties

    TestConfigService (Map properties = [:]) {
        super()
        if (Environment.current.name == "WORKFLOW_TEST") {
            otpProperties = otpProperties.findAll {
                (it.key as String) in [
                        "otp.ssh.authMethod",
                        "otp.ssh.keyFile",
                        "otp.ssh.password",

                        "otp.path.tools",

                        "otp.testing.workflows.account",
                        "otp.testing.workflows.host",
                        "otp.testing.workflows.scheduler",
                        "otp.testing.workflows.rootdir",
                        "otp.testing.group",
                ]
            }
        } else {
            otpProperties = otpProperties.findAll {
                (it.key as String) in [
                    "otp.testing.group",
                ]
            }
        }
        otpProperties += [
                'otp.ssh.user'              : "user",
                'otp.root.path'             : TestCase.getUniqueNonExistentPath().path+'/root_path',
                'otp.processing.root.path'  : TestCase.getUniqueNonExistentPath().path+'/processing_root_path',
                'otp.logging.root.path'     : TestCase.getUniqueNonExistentPath().path+'/logging_root_path',
                'otp.staging.root.path'     : TestCase.getUniqueNonExistentPath().path+'/staging_root_path',
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

    void setOtpProperty(String key, String value) {
        otpProperties.put(key, value)
    }

    void clean() {
        this.otpProperties = new HashMap<>(cleanProperties)
    }


    String getTestingGroup() {
        String testingGroup = getAndAssertValue("otp.testing.group")
        assert testingGroup != TestCase.primaryGroup(): "'otp.testing.group' does not differ from your primary group"
        return testingGroup
    }

    String getWorkflowTestAccountName() {
        return getAndAssertValue("otp.testing.workflows.account")
    }

    Realm.JobScheduler getWorkflowTestScheduler() {
        return Realm.JobScheduler.valueOf(getAndAssertValue("otp.testing.workflows.scheduler"))
    }

    String getWorkflowTestHost() {
        return getAndAssertValue("otp.testing.workflows.host")
    }

    File getWorkflowTestRootDir() {
        return new File(getAndAssertValue("otp.testing.workflows.rootdir"))
    }


    private String getAndAssertValue(String property) {
        String value = otpProperties.get(property)
        assert value : "'${property}' is not set in otp.properties"
        return value
    }
}
