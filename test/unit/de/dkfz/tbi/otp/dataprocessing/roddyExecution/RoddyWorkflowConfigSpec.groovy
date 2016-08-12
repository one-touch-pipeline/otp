package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import spock.lang.*

@Mock([
        Project,
        ProjectCategory,
        Realm,
        RoddyWorkflowConfig,
        SeqType,
        Pipeline,
])
class RoddyWorkflowConfigSpec extends Specification {

    final static String RODDY_NAME = 'roddyName'
    final static String PLUGIN_VERSION = 'pluginVersion:1.1.1'
    final static String CONFIG_VERSION = 'v1_0'


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

    void "test constraint, when project, seqType, pipeline, pluginVersion and configVersion combination is not unique, then validate should return false"() {
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

    void "test getStandardConfigDirectory all fine should return correct path for project"() {
        given:
        Project project = DomainFactory.createProjectWithRealms()
        Pipeline.Name workflowName = Pipeline.Name.PANCAN_ALIGNMENT

        when:
        File file = RoddyWorkflowConfig.getStandardConfigDirectory(project, workflowName)

        then:
        file ==~ ".*/${project.dirName}/${RoddyWorkflowConfig.CONFIG_PATH_ELEMENT}/${workflowName.name()}"
    }

    void "test getStandardConfigDirectory no project given should fail"() {
        given:
        Pipeline.Name workflowName = Pipeline.Name.PANCAN_ALIGNMENT

        when:
        RoddyWorkflowConfig.getStandardConfigDirectory(null, workflowName)

        then:
        AssertionError error = thrown()
        error.message.contains('project')
    }

    void "test getStandardConfigDirectory no pipeline given should fail"() {
        given:
        Project project = DomainFactory.createProjectWithRealms()

        when:
        RoddyWorkflowConfig.getStandardConfigDirectory(project, null)

        then:
        AssertionError error = thrown()
        error.message.contains('pipeline')
    }


    void "test getConfigFileName all fine should return correct file name"() {
        given:
        Pipeline.Name workflowName = Pipeline.Name.PANCAN_ALIGNMENT
        SeqType seqType = DomainFactory.createSeqType(roddyName: RODDY_NAME)

        when:
        String fileName = RoddyWorkflowConfig.getConfigFileName(workflowName, seqType, PLUGIN_VERSION, CONFIG_VERSION)

        then:
        fileName ==~ /${workflowName.name()}_${seqType.roddyName}_${PLUGIN_VERSION}_${CONFIG_VERSION}.xml/
    }

    @Unroll
    void "test getConfigFileName #nullProperty not set should fail"() {
        when:
        RoddyWorkflowConfig.getConfigFileName(pipeline, seqType, pluginVersion, configVersion)

        then:
        AssertionError error = thrown()
        error.message.contains(nullProperty)

        where:
        pipeline                       | seqType                                            | pluginVersion  | configVersion  || nullProperty
        null                           | DomainFactory.createSeqType(roddyName: RODDY_NAME) | PLUGIN_VERSION | CONFIG_VERSION || 'pipeline'
        Pipeline.Name.PANCAN_ALIGNMENT | null                                               | PLUGIN_VERSION | CONFIG_VERSION || 'seqType'
        Pipeline.Name.PANCAN_ALIGNMENT | DomainFactory.createSeqType(roddyName: RODDY_NAME) | null           | CONFIG_VERSION || 'pluginVersion'
        Pipeline.Name.PANCAN_ALIGNMENT | DomainFactory.createSeqType(roddyName: RODDY_NAME) | PLUGIN_VERSION | null           || 'configVersion'
    }

    void "test getConfigFileName seqType has no roddy name should fail"() {
        given:
        Pipeline.Name workflowName = Pipeline.Name.PANCAN_ALIGNMENT
        SeqType seqType = DomainFactory.createSeqType()

        when:
        RoddyWorkflowConfig.getConfigFileName(workflowName, seqType, PLUGIN_VERSION, CONFIG_VERSION)

        then:
        AssertionError error = thrown()
        error.message.contains('seqType.roddyName')
    }

    void "test getConfigFileName config version has invalid name should fail"() {
        given:
        Pipeline.Name workflowName = Pipeline.Name.PANCAN_ALIGNMENT
        SeqType seqType = DomainFactory.createSeqType(roddyName: RODDY_NAME)

        when:
        RoddyWorkflowConfig.getConfigFileName(workflowName, seqType, PLUGIN_VERSION, 'invalid')

        then:
        AssertionError error = thrown()
        error.message.contains('CONFIG_VERSION_PATTERN')
    }

    void "test getStandardConfigFile all fine should return correct file"() {
        given:
        Project project = DomainFactory.createProjectWithRealms()
        Pipeline.Name pipelineName = Pipeline.Name.PANCAN_ALIGNMENT
        SeqType seqType = DomainFactory.createSeqType(roddyName: RODDY_NAME)

        File path = RoddyWorkflowConfig.getStandardConfigDirectory(project, pipelineName)
        String name = RoddyWorkflowConfig.getConfigFileName(pipelineName, seqType, PLUGIN_VERSION, CONFIG_VERSION)
        String expected = "${path.path}/${name}"

        when:
        File file = RoddyWorkflowConfig.getStandardConfigFile(project, pipelineName, seqType, PLUGIN_VERSION, CONFIG_VERSION)

        then:
        file.path == expected
    }

    void "test getNameUsedInConfig all fine should return name"() {
        given:
        Pipeline.Name pipelineName = Pipeline.Name.PANCAN_ALIGNMENT
        SeqType seqType = DomainFactory.createSeqType(roddyName: RODDY_NAME)
        String expected = "${pipelineName.name()}_${seqType.roddyName}_${PLUGIN_VERSION}_${CONFIG_VERSION}"

        when:
        String name = RoddyWorkflowConfig.getNameUsedInConfig(pipelineName, seqType, PLUGIN_VERSION, CONFIG_VERSION)

        then:
        name == expected
    }

    @Unroll
    void "test getNameUsedInConfig #nullProperty not set should fail"() {
        when:
        RoddyWorkflowConfig.getNameUsedInConfig(pipeline, seqType, pluginVersion, configVersion)

        then:
        AssertionError error = thrown()
        error.message.contains(nullProperty)

        where:
        pipeline                       | seqType                                            | pluginVersion  | configVersion  || nullProperty
        null                           | DomainFactory.createSeqType(roddyName: RODDY_NAME) | PLUGIN_VERSION | CONFIG_VERSION || 'pipeline'
        Pipeline.Name.PANCAN_ALIGNMENT | null                                               | PLUGIN_VERSION | CONFIG_VERSION || 'seqType'
        Pipeline.Name.PANCAN_ALIGNMENT | DomainFactory.createSeqType(roddyName: RODDY_NAME) | null           | CONFIG_VERSION || 'pluginVersion'
        Pipeline.Name.PANCAN_ALIGNMENT | DomainFactory.createSeqType(roddyName: RODDY_NAME) | PLUGIN_VERSION | null           || 'configVersion'
    }
}
