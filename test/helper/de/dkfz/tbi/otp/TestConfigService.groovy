package de.dkfz.tbi.otp

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*
import grails.util.Environment
import org.springframework.context.ApplicationContext

class TestConfigService extends ConfigService {

    static Map cleanProperties

    TestConfigService (Map properties = [:]) {
        super()
        if (Environment.current.name == "WORKFLOW_TEST") {
            otpProperties = otpProperties.findAll {
                (it.key as String) in [
                        "otp.ssh.keyFile",
                        "otp.ssh.password",
                        "otp.ssh.useSshAgent"
                ]
            }
        } else {
            otpProperties = [:]
        }
        otpProperties += [
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
        this.otpProperties = cleanProperties
    }
}
