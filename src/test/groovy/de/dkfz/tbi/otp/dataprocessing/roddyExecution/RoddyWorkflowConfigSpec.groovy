/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.WorkflowConfigService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

class RoddyWorkflowConfigSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Pipeline,
                Project,
                RoddyWorkflowConfig,
                SeqType,
        ]
    }

    final static String RODDY_NAME = 'roddyName'
    final static String PROGRAM_VERSION = 'programVersion:1.1.1'
    final static String CONFIG_VERSION = 'v1_0'

    void setup() {
        new TestConfigService()
    }

    @Unroll
    void "test constraint, when value for constraint #constraint on #property is invalid, then validate should return false"() {
        given:
        RoddyWorkflowConfig config = DomainFactory.createRoddyWorkflowConfig()
        config[property] = value

        expect:
        TestCase.assertValidateError(config, property, constraint, value)

        where:
        property         | constraint                | value
        'configFilePath' | 'validator.absolute.path' | 'invalidPath'
        'pipeline'       | 'nullable'                | null
        'programVersion' | 'nullable'                | null
        'programVersion' | 'blank'                   | ''
        'configVersion'  | 'blank'                   | ''
        'configVersion'  | 'matches.invalid'         | 'invalidValue'
    }

    void "test constraint, when seqType is null for new object, then validate should return false"() {
        given:
        RoddyWorkflowConfig config = DomainFactory.createRoddyWorkflowConfig([:], false)
        config.seqType = null

        expect:
        TestCase.assertValidateError(config, 'seqType', 'validator.invalid', null)
    }

    void "test constraint, when seqType is null for obsolete object, then validate should return true"() {
        given:
        RoddyWorkflowConfig config = DomainFactory.createRoddyWorkflowConfig()
        new WorkflowConfigService().makeObsolete(config)
        config.seqType = null

        expect:
        assert config.validate()
    }

    void "test constraint, when configFilePath is not unique, then validate should return false"() {
        given:
        RoddyWorkflowConfig config1 = DomainFactory.createRoddyWorkflowConfig()
        RoddyWorkflowConfig config2 = DomainFactory.createRoddyWorkflowConfig()
        config2.configFilePath = config1.configFilePath

        expect:
        TestCase.assertValidateError(config2, 'configFilePath', 'unique', config2.configFilePath)
    }

    void "test constraint, when project, seqType, pipeline, programVersion and configVersion combination is not unique, then validate should return false"() {
        given:
        RoddyWorkflowConfig config1 = DomainFactory.createRoddyWorkflowConfig(obsoleteDate: new Date())
        RoddyWorkflowConfig config2 = DomainFactory.createRoddyWorkflowConfig()
        config2.project = config1.project
        config2.seqType = config1.seqType
        config2.pipeline = config1.pipeline
        config2.programVersion = config1.programVersion
        config2.configVersion = config1.configVersion

        expect:
        TestCase.assertValidateError(config2, 'configVersion', 'validator.invalid', config2.configVersion)
    }

    void "test constraint, adapterTrimming not set with WGBS or RNA, fails"() {
        given:
        RoddyWorkflowConfig config = DomainFactory.createRoddyWorkflowConfig()
        config.adapterTrimmingNeeded = false
        config.seqType = DomainFactory.createSeqType(name: seqTypeName)

        expect:
        TestCase.assertValidateError(config, 'adapterTrimmingNeeded', 'required', config.adapterTrimmingNeeded)

        where:
        seqTypeName << [SeqTypeNames.RNA, SeqTypeNames.WHOLE_GENOME_BISULFITE, SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION]
    }

    void "test constraint, adapterTrimming set for Indel or SNV, fails"() {
        given:
        RoddyWorkflowConfig config = DomainFactory.createRoddyWorkflowConfig()
        config.adapterTrimmingNeeded = true
        config.pipeline = DomainFactory.createPipeline(name, type)

        expect:
        TestCase.assertValidateError(config, 'adapterTrimmingNeeded', 'not.allowed', config.adapterTrimmingNeeded)

        where:
        type                | name
        Pipeline.Type.SNV   | Pipeline.Name.RODDY_SNV
        Pipeline.Type.INDEL | Pipeline.Name.RODDY_INDEL
    }

    void "test constraint, adapterTrimming set correctly, succeeds"() {
        given:
        RoddyWorkflowConfig config = DomainFactory.createRoddyWorkflowConfig(
                [seqType: DomainFactory.createSeqType(name: seqTypeName)],
                false,
        )
        config.adapterTrimmingNeeded = adapterTrimming

        expect:
        config.validate()

        where:
        seqTypeName                                      | adapterTrimming
        SeqTypeNames.RNA                                 | true
        SeqTypeNames.WHOLE_GENOME_BISULFITE              | true
        SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION | true
        SeqTypeNames.WHOLE_GENOME                        | true
        SeqTypeNames.WHOLE_GENOME                        | false
        SeqTypeNames.EXOME                               | true
        SeqTypeNames.EXOME                               | false
    }

    void "test getStandardConfigDirectory all fine should return correct path for project"() {
        given:
        Project project = DomainFactory.createProject()
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
        Project project = DomainFactory.createProject()

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
        String fileName = RoddyWorkflowConfig.getConfigFileName(workflowName, seqType, PROGRAM_VERSION, CONFIG_VERSION)

        then:
        fileName ==~ /${workflowName.name()}_${seqType.roddyName}_${seqType.libraryLayout}_${PROGRAM_VERSION}_${CONFIG_VERSION}.xml/
    }

    @Unroll
    void "test getConfigFileName #nullProperty not set should fail"() {
        when:
        RoddyWorkflowConfig.getConfigFileName(pipeline, seqType, programVersion, configVersion)

        then:
        AssertionError error = thrown()
        error.message.contains(nullProperty)

        where:
        pipeline                       | seqType                                            | programVersion  | configVersion  || nullProperty
        null                           | DomainFactory.createSeqType(roddyName: RODDY_NAME) | PROGRAM_VERSION | CONFIG_VERSION || 'pipeline'
        Pipeline.Name.PANCAN_ALIGNMENT | null                                               | PROGRAM_VERSION | CONFIG_VERSION || 'seqType'
        Pipeline.Name.PANCAN_ALIGNMENT | DomainFactory.createSeqType(roddyName: RODDY_NAME) | null            | CONFIG_VERSION || 'pluginNameAndVersion'
        Pipeline.Name.PANCAN_ALIGNMENT | DomainFactory.createSeqType(roddyName: RODDY_NAME) | PROGRAM_VERSION | null           || 'configVersion'
    }

    void "test getConfigFileName seqType has no roddy name should fail"() {
        given:
        Pipeline.Name workflowName = Pipeline.Name.PANCAN_ALIGNMENT
        SeqType seqType = DomainFactory.createSeqType()

        when:
        RoddyWorkflowConfig.getConfigFileName(workflowName, seqType, PROGRAM_VERSION, CONFIG_VERSION)

        then:
        AssertionError error = thrown()
        error.message.contains('seqType.roddyName')
    }

    void "test getConfigFileName config version has invalid name should fail"() {
        given:
        Pipeline.Name workflowName = Pipeline.Name.PANCAN_ALIGNMENT
        SeqType seqType = DomainFactory.createSeqType(roddyName: RODDY_NAME)

        when:
        RoddyWorkflowConfig.getConfigFileName(workflowName, seqType, PROGRAM_VERSION, 'invalid')

        then:
        AssertionError error = thrown()
        error.message.contains('CONFIG_VERSION_PATTERN')
    }

    void "test getStandardConfigFile all fine should return correct file"() {
        given:
        Project project = DomainFactory.createProject()
        Pipeline.Name pipelineName = Pipeline.Name.PANCAN_ALIGNMENT
        SeqType seqType = DomainFactory.createSeqType(roddyName: RODDY_NAME)

        File path = RoddyWorkflowConfig.getStandardConfigDirectory(project, pipelineName)
        String name = RoddyWorkflowConfig.getConfigFileName(pipelineName, seqType, PROGRAM_VERSION, CONFIG_VERSION)
        String expected = "${path.path}/${name}"

        when:
        File file = RoddyWorkflowConfig.getStandardConfigFile(project, pipelineName, seqType, PROGRAM_VERSION, CONFIG_VERSION)

        then:
        file.path == expected
    }

    void "test getNameUsedInConfig all fine should return name"() {
        given:
        Pipeline.Name pipelineName = Pipeline.Name.PANCAN_ALIGNMENT
        SeqType seqType = DomainFactory.createSeqType(roddyName: RODDY_NAME)
        String expected = "${pipelineName.name()}_${seqType.roddyName}_${seqType.libraryLayout}_${PROGRAM_VERSION}_${CONFIG_VERSION}"

        when:
        String name = RoddyWorkflowConfig.getNameUsedInConfig(pipelineName, seqType, PROGRAM_VERSION, CONFIG_VERSION)

        then:
        name == expected
    }

    @Unroll
    void "test getNameUsedInConfig #nullProperty not set should fail"() {
        when:
        RoddyWorkflowConfig.getNameUsedInConfig(pipeline, seqType, programVersion, configVersion)

        then:
        AssertionError error = thrown()
        error.message.contains(nullProperty)

        where:
        pipeline                       | seqType                                            | programVersion  | configVersion  || nullProperty
        null                           | DomainFactory.createSeqType(roddyName: RODDY_NAME) | PROGRAM_VERSION | CONFIG_VERSION || 'pipeline'
        Pipeline.Name.PANCAN_ALIGNMENT | null                                               | PROGRAM_VERSION | CONFIG_VERSION || 'seqType'
        Pipeline.Name.PANCAN_ALIGNMENT | DomainFactory.createSeqType(roddyName: RODDY_NAME) | null            | CONFIG_VERSION || 'pluginNameAndVersion'
        Pipeline.Name.PANCAN_ALIGNMENT | DomainFactory.createSeqType(roddyName: RODDY_NAME) | PROGRAM_VERSION | null           || 'configVersion'
    }

    void "test pipeline constraint when pipeline not valid"() {
        given:
        Pipeline pipeline = DomainFactory.createOtpSnvPipelineLazy()
        RoddyWorkflowConfig config = DomainFactory.createRoddyWorkflowConfig([pipeline: pipeline], false)

        expect:
        TestCase.assertValidateError(config, 'pipeline', 'validator.invalid', pipeline)
    }
}
