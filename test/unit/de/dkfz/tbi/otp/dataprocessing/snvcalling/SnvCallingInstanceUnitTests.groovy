package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.buildtestdata.mixin.*
import grails.test.mixin.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(SnvCallingInstance)
@Build([Realm, DataFile, FileType, MergingCriteria, MergingSetAssignment, MergingPass, ProcessedBamFile, SampleTypePerProject, SnvJobResult, ProcessingOption])
class SnvCallingInstanceUnitTests {

    SnvCallingInstanceTestData testData = new SnvCallingInstanceTestData()
    String samplePairPath

    @Before
    void setUp() {
        testData.createSnvObjects()

        samplePairPath = "${testData.samplePair.sampleType1.name}_${testData.samplePair.sampleType2.name}"

        SamplePair.metaClass.getSnvSamplePairPath = {
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
        OtpPath snvInstancePath = instance.getInstancePath()

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
