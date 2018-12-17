package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import org.codehaus.groovy.control.io.NullWriter
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

class RoddyWorkflowConfigServiceIntegrationSpec extends Specification {

    void "test method loadPanCanConfigAndTriggerAlignment, valid"() {
        given:
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        RoddyWorkflowConfigService service = [
                importProjectConfigFile: { Project a, SeqType b, String c, Pipeline d, String, String e, Boolean f, Individual g -> }
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
                importProjectConfigFile: { Project a, SeqType b, String c, Pipeline d, String, String e, Boolean f, Individual g ->
                    throw new Exception("importProjectConfigFile failed")
                }
        ] as RoddyWorkflowConfigService

        when:
        service.loadPanCanConfigAndTriggerAlignment(roddyBamFile.project, roddyBamFile.seqType, HelperUtils.uniqueString, roddyBamFile.pipeline, HelperUtils.uniqueString, HelperUtils.uniqueString, false, roddyBamFile.individual)

        then:
        Exception e = thrown()
        e.message.contains("importProjectConfigFile failed")
        roddyBamFile.mergingWorkPackage.needsProcessing == false
        roddyBamFile.withdrawn == false
    }


    void "test method loadPanCanConfigAndTriggerAlignment no merging work package found"() {
        given:
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        RoddyWorkflowConfigService service = [
                importProjectConfigFile: { Project a, SeqType b, String c, Pipeline d, String, String e, Boolean f, Individual g -> }
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
    static final String TEST_RODDY_CONFIG_FILE_NAME = "${Pipeline.Name.PANCAN_ALIGNMENT.name()}_${TEST_RODDY_SEQ_TYPE_RODDY_NAME}_${LibraryLayout.SINGLE}_${TEST_RODDY_PLUGIN_VERSION_VERSION_PART}_${DomainFactory.TEST_CONFIG_VERSION}.xml"
    static final String TEST_RODDY_CONFIG_FILE_NAME_PLUGIN_VERSION_2 = "${Pipeline.Name.PANCAN_ALIGNMENT.name()}_${TEST_RODDY_SEQ_TYPE_RODDY_NAME}_${LibraryLayout.SINGLE}_${TEST_RODDY_PLUGIN_VERSION_VERSION_PART_2}_${DomainFactory.TEST_CONFIG_VERSION}.xml"
    static final String TEST_RODDY_CONFIG_LABEL_IN_FILE = "${Pipeline.Name.PANCAN_ALIGNMENT.name()}_${TEST_RODDY_SEQ_TYPE_RODDY_NAME}_${LibraryLayout.SINGLE}_${TEST_RODDY_PLUGIN_VERSION}_${DomainFactory.TEST_CONFIG_VERSION}"

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
        return service
    }

    void testImportProjectConfigFile_ProjectIsNull_ShouldFail() {
        given:
        RoddyWorkflowConfigService service = createObjectsAndService()
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        when:
        service.importProjectConfigFile(null, seqType, TEST_RODDY_PLUGIN_VERSION, pipeline, configFile.path, DomainFactory.TEST_CONFIG_VERSION)

        then:
        def e = thrown(AssertionError)
        e.message.contains('The project is not allowed to be null')
    }

    void testImportProjectConfigFile_SeqTypeIsNull_ShouldFail() {
        given:
        RoddyWorkflowConfigService service = createObjectsAndService()
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        Project project = DomainFactory.createProject()

        when:
        service.importProjectConfigFile(project, null, TEST_RODDY_PLUGIN_VERSION, pipeline, configFile.path, DomainFactory.TEST_CONFIG_VERSION)

        then:
        def e = thrown(AssertionError)
        e.message.contains('The seqType is not allowed to be null')
    }

    void testImportProjectConfigFile_PipelineIsNull_ShouldFail() {
        given:
        RoddyWorkflowConfigService service = createObjectsAndService()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)

        when:
        service.importProjectConfigFile(project, seqType, TEST_RODDY_PLUGIN_VERSION, null, configFile.path, DomainFactory.TEST_CONFIG_VERSION)

        then:
        def e = thrown(AssertionError)
        e.message.contains('The pipeline is not allowed to be null')
    }

    void testImportProjectConfigFile_PluginVersionToUseIsNull_ShouldFail() {
        given:
        RoddyWorkflowConfigService service = createObjectsAndService()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()

        when:
        service.importProjectConfigFile(project, seqType, null, pipeline, configFile.path, DomainFactory.TEST_CONFIG_VERSION)

        then:
        def e = thrown(AssertionError)
        e.message.contains('The pluginVersionToUse is not allowed to be null')
    }

    void testImportProjectConfigFile_ConfigFilePathIsNull_ShouldFail() {
        given:
        RoddyWorkflowConfigService service = createObjectsAndService()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()

        when:
        service.importProjectConfigFile(project, seqType, TEST_RODDY_PLUGIN_VERSION, pipeline, null, DomainFactory.TEST_CONFIG_VERSION)

        then:
        def e = thrown(AssertionError)
        e.message.contains('The configFilePath is not allowed to be null')
    }

    void testImportProjectConfigFile_ConfigVersionIsBlank_ShouldFail() {
        given:
        RoddyWorkflowConfigService service = createObjectsAndService()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()

        when:
        service.importProjectConfigFile(project, seqType, TEST_RODDY_PLUGIN_VERSION, pipeline, configFile.path, '')

        then:
        def e = thrown(AssertionError)
        e.message.contains('The configVersion is not allowed to be null')
    }

    void testImportProjectConfigFile_NoPreviousRoddyWorkflowConfigExists() {
        given:
        RoddyWorkflowConfigService service = createObjectsAndService()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        assert RoddyWorkflowConfig.list().size() == 0

        when:
        service.importProjectConfigFile(project, seqType, TEST_RODDY_PLUGIN_VERSION, pipeline, configFile.path, DomainFactory.TEST_CONFIG_VERSION)

        then:
        RoddyWorkflowConfig roddyWorkflowConfig = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.list())
        roddyWorkflowConfig.project == project
        roddyWorkflowConfig.seqType == seqType
        roddyWorkflowConfig.pipeline == pipeline
        roddyWorkflowConfig.configFilePath == configFile.path
        roddyWorkflowConfig.pluginVersion == TEST_RODDY_PLUGIN_VERSION
        roddyWorkflowConfig.previousConfig == null
        roddyWorkflowConfig.configVersion == DomainFactory.TEST_CONFIG_VERSION
        roddyWorkflowConfig.individual == null
    }

    void testImportProjectConfigFile_WithIndividual_NoPreviousRoddyWorkflowConfigExists() {
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
        service.importProjectConfigFile(project, seqType, TEST_RODDY_PLUGIN_VERSION, pipeline, configFile.path, DomainFactory.TEST_CONFIG_VERSION, false, individual)

        then:
        RoddyWorkflowConfig roddyWorkflowConfig = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.list())
        roddyWorkflowConfig.individual == individual
    }


    void testImportProjectConfigFile_PreviousRoddyWorkflowConfigExists() {
        given:
        RoddyWorkflowConfigService service = createObjectsAndService()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(project: project, seqType: seqType, pipeline: pipeline, pluginVersion: TEST_RODDY_PLUGIN_VERSION_2, configVersion: DomainFactory.TEST_CONFIG_VERSION)
        assert RoddyWorkflowConfig.list().size() == 1

        when:
        service.importProjectConfigFile(project, seqType, TEST_RODDY_PLUGIN_VERSION, pipeline, configFile.path, DomainFactory.TEST_CONFIG_VERSION)

        then:
        assert RoddyWorkflowConfig.list().size() == 2
        RoddyWorkflowConfig roddyWorkflowConfig2 = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.findAllByPluginVersion(TEST_RODDY_PLUGIN_VERSION))
        roddyWorkflowConfig2.previousConfig == roddyWorkflowConfig1
        roddyWorkflowConfig1.obsoleteDate
    }
}
