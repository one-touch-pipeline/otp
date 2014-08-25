package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.MergingPass
import de.dkfz.tbi.otp.dataprocessing.MergingSet
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.*

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(SnvCallingInstance)
@Build([SnvJobResult])
class SnvCallingInstanceUnitTests {

    SnvCallingInstanceTestData testData = new SnvCallingInstanceTestData()
    String sampleCombinationPath

    @Before
    void setUp() {
        testData.createSnvObjects()

        MergingPass.metaClass.isLatestPass= {true}
        MergingSet.metaClass.isLatestSet= {true}

        sampleCombinationPath = "${testData.sampleTypeCombination.sampleType1.name}_${testData.sampleTypeCombination.sampleType2.name}"

        SampleTypeCombinationPerIndividual.metaClass.getSampleTypeCombinationPath = {
            return new OtpPath(testData.project, sampleCombinationPath)
        }
    }


    @Test
    void testConstraintsAllFine() {
        SnvCallingInstance instance = createSnvCallingInstance()
        assert instance.validate()
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
        assert instance.validate()
        assert instance.save()

        SnvCallingInstance instanceSameName = createSnvCallingInstance([instanceName: instance.instanceName,
            sampleTypeCombination: instance.sampleTypeCombination])
        assert !instanceSameName.validate()
    }

    @Test
    void testInstanceNoSampleTypeCombination() {
        SnvCallingInstance instance = createSnvCallingInstance()
        instance.sampleTypeCombination = null
        assert !instance.validate()
    }

    @Test
    void testIsConsistentWithSampleTypeCombination() {
        SnvCallingInstance instance = createSnvCallingInstance()
        SampleType sampleType1 = instance.sampleTypeCombination.sampleType1
        SampleType sampleType2 = instance.sampleTypeCombination.sampleType2
        assert SnvCallingInstance.isConsistentWithSampleTypeCombination(testData.bamFileControl, instance, sampleType2)
        assert SnvCallingInstance.isConsistentWithSampleTypeCombination(testData.bamFileTumor, instance, sampleType1)
    }

    @Test
    void testGetSnvInstancePath() {
        SnvCallingInstance instance = createSnvCallingInstance()
        OtpPath snvInstancePath = instance.getSnvInstancePath()

        assertEquals(instance.project, snvInstancePath.project)
        File expectedRelativePath = new File(getSnvInstancePathHelper(testData.sampleTypeCombination, instance))
        assertEquals(expectedRelativePath, snvInstancePath.relativePath)
    }

    @Test
    void testGetConfigFilePath() {
        SnvCallingInstance instance = createSnvCallingInstance()
        OtpPath configFilePath = instance.getConfigFilePath()

        assertEquals(instance.project, configFilePath.project)
        File expectedRelativePath = new File("${getSnvInstancePathHelper(testData.sampleTypeCombination, instance)}/config.txt")
        assertEquals(expectedRelativePath, configFilePath.relativePath)
    }

    @Test
    void testGetSnvCallingFileFinalPath() {
        SnvCallingInstance instance = createSnvCallingInstance()
        OtpPath snvCallingFileFinalPath = instance.getSnvCallingFileFinalPath()

        assertEquals(instance.project, snvCallingFileFinalPath.project)
        File expectedRelativePath = new File("${getSnvInstancePathHelper(testData.sampleTypeCombination, instance)}/snvs_654321_raw.vcf.gz")
        assertEquals(expectedRelativePath, snvCallingFileFinalPath.relativePath)
    }

    @Test
    void testGetStepConfigFileLinkedPath() {
        SnvCallingInstance instance = createSnvCallingInstance()
        OtpPath stepConfigFileLinkedPath= instance.getStepConfigFileLinkedPath(SnvCallingStep.CALLING)

        assertEquals(instance.project, stepConfigFileLinkedPath.project)
        File expectedRelativePath = new File("${sampleCombinationPath}/config_${SnvCallingStep.CALLING.configFileNameSuffix}_${instance.instanceName}.txt")
        assertEquals(expectedRelativePath, stepConfigFileLinkedPath.relativePath)
    }

    String getSnvInstancePathHelper(SampleTypeCombinationPerIndividual combination, SnvCallingInstance instance) {
        return "${sampleCombinationPath}/${instance.instanceName}"
    }

    /**
     * This test ensures that "latest" is defined in respect of the {@link SnvCallingInstance}, not in respect of the
     * {@link SnvJobResult}.
     */
    @Test
    void testFindLatestResultForSameBamFiles_correctOrder() {
        final SnvCallingInstance tumor1InstanceA = testData.createAndSaveSnvCallingInstance()
        // Using a different (does not matter if "earlier" or "later") instance name, because instance names have to be unique for the same sample type combination.
        final SnvCallingInstance tumor1InstanceB = testData.createAndSaveSnvCallingInstance(instanceName: '2014-09-24_15h04')
        final SnvJobResult tumor1CallingResultB = testData.createAndSaveSnvJobResult(tumor1InstanceB, SnvCallingStep.CALLING)
        final SnvJobResult tumor1CallingResultA = testData.createAndSaveSnvJobResult(tumor1InstanceA, SnvCallingStep.CALLING)

        assert tumor1InstanceA.id < tumor1InstanceB.id
        assert tumor1CallingResultA.id > tumor1CallingResultB.id

        // tumor1CallingResultB should be returned ...
        // - although it has been created after the other result
        // - because it belongs to the instance that has been created later
        // This case will probably not occur in production, because currently the SnvStartJob does not create a new
        // SnvCallingInstance as long as another instance for the same BAM files is in progress.
        assert tumor1InstanceA.findLatestResultForSameBamFiles(SnvCallingStep.CALLING) == tumor1CallingResultB
        assert tumor1InstanceB.findLatestResultForSameBamFiles(SnvCallingStep.CALLING) == tumor1CallingResultB
    }

    SnvCallingInstance createSnvCallingInstance(Map properties = [:]) {
        return testData.createSnvCallingInstance(properties)
    }
}
