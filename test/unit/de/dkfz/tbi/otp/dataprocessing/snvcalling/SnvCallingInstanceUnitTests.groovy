package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.MergingPass
import de.dkfz.tbi.otp.dataprocessing.MergingSet
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.ngsdata.*

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(SnvCallingInstance)
@Build([SnvJobResult])
class SnvCallingInstanceUnitTests {

    TestData testData = new TestData()

    ProcessedMergedBamFile bamFileTumor
    ProcessedMergedBamFile bamFileControl
    SampleTypeCombinationPerIndividual sampleTypeCombination
    String sampleCombinationPath

    @Before
    void setUp() {
        Project project = testData.createProject()
        assert project.save(flush: true, failOnError: true)

        Individual individual = testData.createIndividual([project: project])
        assert individual.save(flush: true, failOnError: true)

        SeqType seqType = testData.createSeqType()
        assert seqType.save(flush: true, failOnError: true)

        bamFileTumor = createProcessedMergedBamFile(individual, seqType)
        bamFileControl = createProcessedMergedBamFile(individual, seqType)

        sampleTypeCombination = new SampleTypeCombinationPerIndividual(
                individual: individual,
                sampleType1: bamFileTumor.sampleType,
                sampleType2: bamFileControl.sampleType,
                seqType: seqType
                )
        assert sampleTypeCombination.save()

        MergingPass.metaClass.isLatestPass= {true}
        MergingSet.metaClass.isLatestSet= {true}

        sampleCombinationPath = "${sampleTypeCombination.sampleType1.name}_${sampleTypeCombination.sampleType2.name}"

        SampleTypeCombinationPerIndividual.metaClass.getSampleTypeCombinationPath = {
            return new OtpPath(project, sampleCombinationPath)
        }
    }

    @After
    void tearDown() {
        bamFileTumor = null
        bamFileControl = null
        sampleTypeCombination = null
    }


    @Test
    void testConstraintsAllFine() {
        SnvCallingInstance instance = createSnvCallingInstance()
        assert instance.validate()
    }

    @Test
    void testSeqTypeConstraint() {
        bamFileTumor.mergingPass.mergingSet.mergingWorkPackage.seqType = new SeqType()

        SnvCallingInstance differentSeqTypeInstance = createSnvCallingInstance()
        assert !differentSeqTypeInstance.validate()
    }

    @Test
    void testIndividualConstraint() {
        bamFileTumor.mergingPass.mergingSet.mergingWorkPackage.sample.individual = new Individual()

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
    void testGetSnvInstancePath() {
        SnvCallingInstance instance = createSnvCallingInstance()
        OtpPath snvInstancePath = instance.getSnvInstancePath()

        assertEquals(instance.project, snvInstancePath.project)
        File expectedRelativePath = new File(getSnvInstancePathHelper(sampleTypeCombination, instance))
        assertEquals(expectedRelativePath, snvInstancePath.relativePath)
    }

    @Test
    void testGetConfigFilePath() {
        SnvCallingInstance instance = createSnvCallingInstance()
        OtpPath configFilePath = instance.getConfigFilePath()

        assertEquals(instance.project, configFilePath.project)
        File expectedRelativePath = new File("${getSnvInstancePathHelper(sampleTypeCombination, instance)}/config.txt")
        assertEquals(expectedRelativePath, configFilePath.relativePath)
    }

    String getSnvInstancePathHelper(SampleTypeCombinationPerIndividual combination, SnvCallingInstance instance) {
        return "${sampleCombinationPath}/${instance.instanceName}"
    }

    SnvCallingInstance createSnvCallingInstance(Map properties = [:]) {
        return new SnvCallingInstance([
            processingState: SnvProcessingStates.IN_PROGRESS,
            tumorBamFile: bamFileTumor,
            controlBamFile: bamFileControl,
            config: new SnvConfig(),
            instanceName: "2014-08-25_15h32",
            sampleTypeCombination: sampleTypeCombination
        ] + properties)
    }


    ProcessedMergedBamFile createProcessedMergedBamFile(Individual individual, SeqType seqType) {
        SampleType sampleType = new SampleType(
                name: "sampleType${TestCase.uniqueString}")
        assert sampleType.save(flush: true, failOnError: true)

        Sample sample = new Sample (
                individual: individual,
                sampleType: sampleType)
        assert sample.save(flush: true, failOnError: true)

        MergingWorkPackage workPackage = new MergingWorkPackage(
                sample: sample,
                seqType: seqType
                )
        assert workPackage.save(flush: true, failOnError: true)

        MergingSet mergingSet = new MergingSet(
                mergingWorkPackage: workPackage)
        assert mergingSet.save(flush: true, failOnError: true)

        MergingPass mergingPass = new MergingPass(
                identifier: 1,
                mergingSet: mergingSet)
        assert mergingPass.save(flush: true, failOnError: true)

        ProcessedMergedBamFile bamFile = new ProcessedMergedBamFile(
                type: AbstractBamFile.BamType.SORTED,
                mergingPass: mergingPass)
        assert bamFile.save(flush: true, failOnError: true)

        return bamFile
    }
}
