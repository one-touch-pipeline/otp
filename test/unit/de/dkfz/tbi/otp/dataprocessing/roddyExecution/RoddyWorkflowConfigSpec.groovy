package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import grails.test.mixin.web.*
import org.junit.*
import spock.lang.Specification
import spock.lang.Unroll

@Mock([
        Project,
        RoddyWorkflowConfig,
        SeqType,
        Pipeline,
])
class RoddyWorkflowConfigSpec extends Specification {


    @Unroll
    void "test constraint, when value for constraint #constraint on #property is invalid, then validate should return false"() {
        given:
        RoddyWorkflowConfig config = DomainFactory.createRoddyWorkflowConfig()

        when:
        config[property] = value

        then:
        TestCase.assertValidateError(config, property, constraint, value)

        where:
        property         | constraint          | value
        'configFilePath' | 'validator.invalid' | 'invalidPath'
        'pipeline'       | 'nullable'          | null
        'pluginVersion'  | 'nullable'          | null
        'pluginVersion'  | 'blank'             | ''
        'configVersion'  | 'blank'             | ''
        'configVersion'  | 'matches.invalid'   | 'invalidValue'
    }

    void "test constraint, when seqType is null for new object, then validate should return false"() {
        given:
        RoddyWorkflowConfig config = DomainFactory.createRoddyWorkflowConfig([:], false)

        when:
        config.seqType = null

        then:
        TestCase.assertValidateError(config, 'seqType', 'validator.invalid', null)
    }

    void "test constraint, when seqType is null for existing object, then validate should return true"() {
        given:
        RoddyWorkflowConfig config = DomainFactory.createRoddyWorkflowConfig()

        when:
        config.seqType = null

        then:
        assert config.validate()
    }

    void "test constraint, when configFilePath is not unique, then validate should return false"() {
        given:
        RoddyWorkflowConfig config1 = DomainFactory.createRoddyWorkflowConfig()
        RoddyWorkflowConfig config2 = DomainFactory.createRoddyWorkflowConfig()

        when:
        config2.configFilePath = config1.configFilePath

        then:
        TestCase.assertValidateError(config2, 'configFilePath', 'unique', config2.configFilePath)
    }

    void "test constraint, when project, seqType, workflow, pluginVersion and configVersion combination is not unique, then validate should return false"() {
        given:
        RoddyWorkflowConfig config1 = DomainFactory.createRoddyWorkflowConfig()
        RoddyWorkflowConfig config2 = DomainFactory.createRoddyWorkflowConfig()

        when:
        config2.project = config1.project
        config2.seqType = config1.seqType
        config2.pipeline = config1.pipeline
        config2.pluginVersion = config1.pluginVersion
        config2.configVersion = config1.configVersion

        then:
        TestCase.assertValidateError(config2, 'configVersion', 'unique', config2.configVersion)
    }

}
