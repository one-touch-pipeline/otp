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
package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.codehaus.groovy.control.io.NullWriter
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.OtpException
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

@Rollback
@Integration
class RoddyWorkflowConfigServiceIntegrationSpec extends Specification {

    void "test method loadPanCanConfigAndTriggerAlignment, valid"() {
        given:
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        RoddyWorkflowConfigService service = [
                importProjectConfigFile: { Project a, SeqType b, String c, Pipeline d, String e, String f, String g, boolean h, Individual i -> },
                getMd5sum: { String s -> HelperUtils.randomMd5sum },
        ] as RoddyWorkflowConfigService

        when:
        LogThreadLocal.withThreadLog(NullWriter.DEFAULT, {
            service.loadPanCanConfigAndTriggerAlignment(roddyBamFile.project, roddyBamFile.seqType, HelperUtils.uniqueString, roddyBamFile.pipeline, HelperUtils.uniqueString, HelperUtils.uniqueString, false, roddyBamFile.individual)
        })

        then:
        roddyBamFile.mergingWorkPackage.needsProcessing == true
        roddyBamFile.withdrawn == true
    }


    void "test method loadPanCanConfigAndTriggerAlignment no individual should throw exception"() {
        given:
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()

        when:
        new RoddyWorkflowConfigService().loadPanCanConfigAndTriggerAlignment(roddyBamFile.project, roddyBamFile.seqType, HelperUtils.uniqueString, roddyBamFile.pipeline, HelperUtils.uniqueString, HelperUtils.uniqueString, false, null)

        then:
        AssertionError e = thrown()
        e.message.contains("The individual is not allowed to be null")
    }


    void "test method loadPanCanConfigAndTriggerAlignment importProjectConfigFile throws exception"() {
        given:
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        RoddyWorkflowConfigService service = [
                importProjectConfigFile: { Project a, SeqType b, String c, Pipeline d, String e, String f, String g, boolean h, Individual i ->
                    throw new OtpException("importProjectConfigFile failed")
                },
                getMd5sum: { String s -> HelperUtils.randomMd5sum },
        ] as RoddyWorkflowConfigService

        when:
        service.loadPanCanConfigAndTriggerAlignment(roddyBamFile.project, roddyBamFile.seqType, HelperUtils.uniqueString, roddyBamFile.pipeline, HelperUtils.uniqueString, HelperUtils.uniqueString, false, roddyBamFile.individual)

        then:
        OtpException e = thrown()
        e.message.contains("importProjectConfigFile failed")
        roddyBamFile.mergingWorkPackage.needsProcessing == false
        roddyBamFile.withdrawn == false
    }


    void "test method loadPanCanConfigAndTriggerAlignment no merging work package found"() {
        given:
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        RoddyWorkflowConfigService service = [
                importProjectConfigFile: { Project a, SeqType b, String c, Pipeline d, String e, String f, String g, boolean h, Individual i -> },
                getMd5sum: { String s -> HelperUtils.randomMd5sum },
        ] as RoddyWorkflowConfigService

        when:
        service.loadPanCanConfigAndTriggerAlignment(roddyBamFile.project, DomainFactory.createSeqType(), HelperUtils.uniqueString, roddyBamFile.pipeline, HelperUtils.uniqueString, HelperUtils.uniqueString, false, roddyBamFile.individual)

        then:
        AssertionError e = thrown()
        e.message.contains("no MWP found")
        roddyBamFile.mergingWorkPackage.needsProcessing == false
        roddyBamFile.withdrawn == false
    }


    static final String TEST_RODDY_SEQ_TYPE_RODDY_NAME = 'roddyName'
    static final String TEST_RODDY_PLUGIN_VERSION_PLUGIN_PART = 'plugin'
    static final String TEST_RODDY_PLUGIN_VERSION_VERSION_PART = '1.2.3'
    static final String TEST_RODDY_PLUGIN_VERSION_VERSION_PART_2 = '1.2.4'
    static final String TEST_RODDY_PLUGIN_VERSION = "${TEST_RODDY_PLUGIN_VERSION_PLUGIN_PART}:${TEST_RODDY_PLUGIN_VERSION_VERSION_PART}"
    static final String TEST_RODDY_PLUGIN_VERSION_2 = "${TEST_RODDY_PLUGIN_VERSION_PLUGIN_PART}:${TEST_RODDY_PLUGIN_VERSION_VERSION_PART_2}"
    static final String TEST_RODDY_CONFIG_FILE_NAME = "${Pipeline.Name.PANCAN_ALIGNMENT.name()}_${TEST_RODDY_SEQ_TYPE_RODDY_NAME}_${SequencingReadType.SINGLE}_" +
            "${TEST_RODDY_PLUGIN_VERSION_VERSION_PART}_${DomainFactory.TEST_CONFIG_VERSION}.xml"
    static final String TEST_RODDY_CONFIG_FILE_NAME_PLUGIN_VERSION_2 = "${Pipeline.Name.PANCAN_ALIGNMENT.name()}_${TEST_RODDY_SEQ_TYPE_RODDY_NAME}_" +
            "${SequencingReadType.SINGLE}_${TEST_RODDY_PLUGIN_VERSION_VERSION_PART_2}_${DomainFactory.TEST_CONFIG_VERSION}.xml"
    static final String TEST_RODDY_CONFIG_LABEL_IN_FILE = "${Pipeline.Name.PANCAN_ALIGNMENT.name()}_${TEST_RODDY_SEQ_TYPE_RODDY_NAME}_${SequencingReadType.SINGLE}_" +
            "${TEST_RODDY_PLUGIN_VERSION}_${DomainFactory.TEST_CONFIG_VERSION}"

    File configDir
    File configFile
    File secondConfigFile

    private RoddyWorkflowConfigService createObjectsAndService() {
        configDir = TestCase.createEmptyTestDirectory()
        configFile = new File(configDir, TEST_RODDY_CONFIG_FILE_NAME)
        CreateFileHelper.createRoddyWorkflowConfig(configFile, TEST_RODDY_CONFIG_LABEL_IN_FILE)
        secondConfigFile = new File(configDir, TEST_RODDY_CONFIG_FILE_NAME_PLUGIN_VERSION_2)
        CreateFileHelper.createRoddyWorkflowConfig(secondConfigFile, TEST_RODDY_CONFIG_LABEL_IN_FILE)
        RoddyWorkflowConfigService service = new RoddyWorkflowConfigService()
        service.fileSystemService = new TestFileSystemService()
        service.workflowConfigService = new WorkflowConfigService()
        return service
    }

    void "test importProjectConfigFile fails when project is null"() {
        given:
        RoddyWorkflowConfigService service = createObjectsAndService()
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        service.importProjectConfigFile(null, seqType, TEST_RODDY_PLUGIN_VERSION, pipeline, configFile.path, DomainFactory.TEST_CONFIG_VERSION, HelperUtils.randomMd5sum)

        then:
        AssertionError e = thrown()
        e.message.contains('The project is not allowed to be null')
    }

    void "test importProjectConfigFile fails when seqType is null"() {
        given:
        RoddyWorkflowConfigService service = createObjectsAndService()
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        Project project = DomainFactory.createProject()

        when:
        service.importProjectConfigFile(project, null, TEST_RODDY_PLUGIN_VERSION, pipeline, configFile.path, DomainFactory.TEST_CONFIG_VERSION, HelperUtils.randomMd5sum)

        then:
        AssertionError e = thrown()
        e.message.contains('The seqType is not allowed to be null')
    }

    void "test importProjectConfigFile fails when pipeline is null"() {
        given:
        RoddyWorkflowConfigService service = createObjectsAndService()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)

        when:
        service.importProjectConfigFile(project, seqType, TEST_RODDY_PLUGIN_VERSION, null, configFile.path, DomainFactory.TEST_CONFIG_VERSION, HelperUtils.randomMd5sum)

        then:
        AssertionError e = thrown()
        e.message.contains('The pipeline is not allowed to be null')
    }

    void "test importProjectConfigFile fails when programVersionToUse is null"() {
        given:
        RoddyWorkflowConfigService service = createObjectsAndService()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()

        when:
        service.importProjectConfigFile(project, seqType, null, pipeline, configFile.path, DomainFactory.TEST_CONFIG_VERSION, HelperUtils.randomMd5sum)

        then:
        AssertionError e = thrown()
        e.message.contains('The programVersionToUse is not allowed to be null')
    }

    void "test importProjectConfigFile fails when configFilePath is null"() {
        given:
        RoddyWorkflowConfigService service = createObjectsAndService()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()

        when:
        service.importProjectConfigFile(project, seqType, TEST_RODDY_PLUGIN_VERSION, pipeline, null, DomainFactory.TEST_CONFIG_VERSION, HelperUtils.randomMd5sum)

        then:
        AssertionError e = thrown()
        e.message.contains('The configFilePath is not allowed to be null')
    }

    void "test importProjectConfigFile fails when configVersion is blank"() {
        given:
        RoddyWorkflowConfigService service = createObjectsAndService()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()

        when:
        service.importProjectConfigFile(project, seqType, TEST_RODDY_PLUGIN_VERSION, pipeline, configFile.path, '', HelperUtils.randomMd5sum)

        then:
        AssertionError e = thrown()
        e.message.contains('The configVersion is not allowed to be null')
    }

    void "test importProjectConfigFile when no previous RoddyWorkflowConfig exists"() {
        given:
        RoddyWorkflowConfigService service = createObjectsAndService()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        assert RoddyWorkflowConfig.list().size() == 0

        when:
        service.importProjectConfigFile(project, seqType, TEST_RODDY_PLUGIN_VERSION, pipeline, configFile.path, DomainFactory.TEST_CONFIG_VERSION, HelperUtils.randomMd5sum)

        then:
        RoddyWorkflowConfig roddyWorkflowConfig = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.list())
        roddyWorkflowConfig.project == project
        roddyWorkflowConfig.seqType == seqType
        roddyWorkflowConfig.pipeline == pipeline
        roddyWorkflowConfig.configFilePath == configFile.path
        roddyWorkflowConfig.programVersion == TEST_RODDY_PLUGIN_VERSION
        roddyWorkflowConfig.previousConfig == null
        roddyWorkflowConfig.configVersion == DomainFactory.TEST_CONFIG_VERSION
        roddyWorkflowConfig.individual == null
    }

    void "test importProjectConfigFile with individual when no previous RoddyWorkflowConfig exists"() {
        given:
        RoddyWorkflowConfigService service = createObjectsAndService()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        Individual individual = DomainFactory.createIndividual(project: project)

        assert RoddyWorkflowConfig.list().size() == 0
        configFile = new File(configDir, "${individual.pid}/${TEST_RODDY_CONFIG_FILE_NAME}")
        CreateFileHelper.createRoddyWorkflowConfig(configFile, TEST_RODDY_CONFIG_LABEL_IN_FILE)

        when:
        service.importProjectConfigFile(project, seqType, TEST_RODDY_PLUGIN_VERSION, pipeline, configFile.path, DomainFactory.TEST_CONFIG_VERSION,
                HelperUtils.randomMd5sum, false, individual)

        then:
        RoddyWorkflowConfig roddyWorkflowConfig = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.list())
        roddyWorkflowConfig.individual == individual
    }


    void "test importProjectConfigFile when previous RoddyWorkflowConfig exists"() {
        given:
        RoddyWorkflowConfigService service = createObjectsAndService()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(
                project: project,
                seqType: seqType,
                pipeline: pipeline,
                programVersion: TEST_RODDY_PLUGIN_VERSION_2,
                configVersion: DomainFactory.TEST_CONFIG_VERSION
        )
        assert RoddyWorkflowConfig.list().size() == 1

        when:
        service.importProjectConfigFile(project, seqType, TEST_RODDY_PLUGIN_VERSION, pipeline, configFile.path, DomainFactory.TEST_CONFIG_VERSION, HelperUtils.randomMd5sum)

        then:
        RoddyWorkflowConfig.list().size() == 2
        RoddyWorkflowConfig roddyWorkflowConfig2 = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.findAllByProgramVersion(TEST_RODDY_PLUGIN_VERSION))
        roddyWorkflowConfig2.previousConfig == roddyWorkflowConfig1
        roddyWorkflowConfig1.obsoleteDate
    }
}
