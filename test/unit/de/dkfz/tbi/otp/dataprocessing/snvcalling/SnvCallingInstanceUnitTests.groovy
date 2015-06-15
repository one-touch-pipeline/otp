package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.MergingPass
import de.dkfz.tbi.otp.dataprocessing.MergingSetAssignment
import de.dkfz.tbi.otp.dataprocessing.ProcessedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
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

        samplePairPath = "${testData.samplePair1.sampleType1.name}_${testData.samplePair1.sampleType2.name}"

        SamplePair.metaClass.getSamplePairPath = {
            return new OtpPath(testData.project, samplePairPath)
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
    void testSeqTypeConstraint() {
        testData.bamFileTumor.mergingPass.mergingSet.mergingWorkPackage.seqType = new SeqType()

        SnvCallingInstance differentSeqTypeInstance = createSnvCallingInstance()
        assert !differentSeqTypeInstance.validate()
    }

    @Test
    void testIndividualConstraint() {
        testData.bamFileTumor.mergingPass.mergingSet.mergingWorkPackage.sample.individual = new Individual()

        SnvCallingInstance differentIndividualInstance = createSnvCallingInstance()
        assert !differentIndividualInstance.validate()
    }

    @Test
    void testNoSnvConfig() {
        SnvCallingInstance instance = createSnvCallingInstance()
        instance.config = null
        assert !instance.validate()
    }

    @Test
    void testInstanceNameIsBlank() {
        SnvCallingInstance instance = createSnvCallingInstance([instanceName: ""])
        assert !instance.validate()
    }

    @Test
    void testInstanceNameNotUnique() {
        SnvCallingInstance instance = createSnvCallingInstance()
        assert instance.save(failOnError: true)

        SnvCallingInstance instanceSameName = createSnvCallingInstance([instanceName: instance.instanceName,
            samplePair: instance.samplePair])
        assert !instanceSameName.validate()
    }

    @Test
    void testInstanceNoSamplePair() {
        SnvCallingInstance instance = createSnvCallingInstance()
        instance.samplePair = null
        assert !instance.validate()
    }

    @Test
    void testIsConsistentWithSamplePair() {
        SnvCallingInstance instance = createSnvCallingInstance()
        SampleType sampleType1 = instance.samplePair.sampleType1
        SampleType sampleType2 = instance.samplePair.sampleType2
        assert SnvCallingInstance.isConsistentWithSamplePair(testData.bamFileControl, instance, sampleType2)
        assert SnvCallingInstance.isConsistentWithSamplePair(testData.bamFileTumor, instance, sampleType1)
    }

    @Test
    void testGetSnvInstancePath() {
        SnvCallingInstance instance = createSnvCallingInstance()
        OtpPath snvInstancePath = instance.getSnvInstancePath()

        assertEquals(instance.project, snvInstancePath.project)
        File expectedRelativePath = new File(getSnvInstancePathHelper(testData.samplePair1, instance))
        assertEquals(expectedRelativePath, snvInstancePath.relativePath)
    }

    @Test
    void testGetConfigFilePath() {
        SnvCallingInstance instance = createSnvCallingInstance()
        OtpPath configFilePath = instance.getConfigFilePath()

        assertEquals(instance.project, configFilePath.project)
        File expectedRelativePath = new File("${getSnvInstancePathHelper(testData.samplePair1, instance)}/config.txt")
        assertEquals(expectedRelativePath, configFilePath.relativePath)
    }

    @Test
    void testGetStepConfigFileLinkedPath() {
        SnvCallingInstance instance = createSnvCallingInstance()
        OtpPath stepConfigFileLinkedPath= instance.getStepConfigFileLinkedPath(SnvCallingStep.CALLING)

        assertEquals(instance.project, stepConfigFileLinkedPath.project)
        File expectedRelativePath = new File("${samplePairPath}/config_${SnvCallingStep.CALLING.configFileNameSuffix}_${instance.instanceName}.txt")
        assertEquals(expectedRelativePath, stepConfigFileLinkedPath.relativePath)
    }

    String getSnvInstancePathHelper(SamplePair samplePair, SnvCallingInstance instance) {
        return "${samplePairPath}/${instance.instanceName}"
    }

    SnvCallingInstance createSnvCallingInstance(Map properties = [:]) {
        return testData.createSnvCallingInstance(properties)
    }
}
