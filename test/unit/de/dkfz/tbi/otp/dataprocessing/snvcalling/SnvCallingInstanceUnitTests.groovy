package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates
import de.dkfz.tbi.otp.dataprocessing.MergingPass
import de.dkfz.tbi.otp.dataprocessing.MergingSetAssignment
import de.dkfz.tbi.otp.dataprocessing.ProcessedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.*

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(SnvCallingInstance)
@Build([DataFile, FileType, MergingSetAssignment, MergingPass, ProcessedBamFile, SampleTypePerProject, SnvJobResult, ProcessingOption])
class SnvCallingInstanceUnitTests {

    SnvCallingInstanceTestData testData = new SnvCallingInstanceTestData()
    String samplePairPath

    @Before
    void setUp() {
        testData.createSnvObjects()

        samplePairPath = "${testData.samplePair.sampleType1.name}_${testData.samplePair.sampleType2.name}"

        SamplePair.metaClass.getSamplePairPath = {
            return new OtpPath(testData.samplePair.project, samplePairPath)
        }
    }

    @After
    void after() {
        SamplePair.metaClass = null
    }

    @Test
    void testConstraintsAllFine() {
        SnvCallingInstance instance = createSnvCallingInstance()
        assert instance.save(failOnError: true)
    }

    @Test
    void testGetSnvInstancePath() {
        SnvCallingInstance instance = createSnvCallingInstance()
        OtpPath snvInstancePath = instance.getSnvInstancePath()

        assertEquals(instance.project, snvInstancePath.project)
        File expectedRelativePath = new File(getSnvInstancePathHelper(instance))
        assertEquals(expectedRelativePath, snvInstancePath.relativePath)
    }

    @Test
    void testGetConfigFilePath() {
        SnvCallingInstance instance = createSnvCallingInstance()
        OtpPath configFilePath = instance.getConfigFilePath()

        assertEquals(instance.project, configFilePath.project)
        File expectedRelativePath = new File("${getSnvInstancePathHelper(instance)}/config.txt")
        assertEquals(expectedRelativePath, configFilePath.relativePath)
    }

    @Test
    void testGetPreviousInstance_onlyOneInstance_returnsNull() {
        SnvCallingInstance instance = createSnvCallingInstance().save(failOnError: true)
        assert instance.previousFinishedInstance == null
    }

    @Test
    void testGetPreviousInstance_twoInstances_returnsOlder() {
        SnvCallingInstance instance = createSnvCallingInstance()
        instance.processingState = AnalysisProcessingStates.FINISHED
        instance.save(failOnError: true)
        SnvCallingInstance instance2 = createSnvCallingInstance(
                sampleType1BamFile: instance.sampleType1BamFile,
                sampleType2BamFile: instance.sampleType2BamFile,
                config: instance.config,
                instanceName: "2015-08-25_15h32",
                samplePair: instance.samplePair,
        ).save(failOnError: true)
        assert instance2.previousFinishedInstance == instance
    }

    String getSnvInstancePathHelper(SnvCallingInstance instance) {
        return "${samplePairPath}/${instance.instanceName}"
    }

    SnvCallingInstance createSnvCallingInstance(Map properties = [:]) {
        return testData.createSnvCallingInstance(properties)
    }


    @Test
    void testConfigConstraint_valid() {
        SnvConfig config = DomainFactory.createSnvConfig()
        SnvCallingInstance instance = DomainFactory.createSnvInstanceWithRoddyBamFiles(config: config)

        assert instance.validate()
    }


    @Test
    void testConfigConstraint_invalid() {
        RoddyWorkflowConfig config = DomainFactory.createRoddyWorkflowConfig()
        SnvCallingInstance instance = DomainFactory.createSnvInstanceWithRoddyBamFiles()
        instance.config = config

        TestCase.assertValidateError(instance, "config", "validator.invalid", config)
    }
}
