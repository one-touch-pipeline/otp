package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.MergingPass
import de.dkfz.tbi.otp.dataprocessing.MergingSet
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.ngsdata.*

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(SnvCallingInstance)
@Mock([MergingPass, MergingSet, MergingWorkPackage,
    Sample, SampleType, SeqType, Individual, Project, ProcessedMergedBamFile, ConfigPerProjectAndSeqType])
class SnvCallingInstanceUnitTests {

    TestData testData = new TestData()

    /*
     * Needed because we test saving to the DB. By default the DB is not reset between the tests.
     * This is why we explicitly set "transactional = true"
     */
    static transactional = true

    ProcessedMergedBamFile bamFileTumor
    ProcessedMergedBamFile bamFileControl

    @Before
    void setUp() {
        Project project = testData.createProject()
        project.save(flush: true)

        Individual individual = testData.createIndividual([project: project])
        individual.save(flush: true)

        SeqType seqType = testData.createSeqType()
        seqType.save(flush: true)

        bamFileTumor = createProcessedMergedBamFile(individual, seqType)
        bamFileControl = createProcessedMergedBamFile(individual, seqType)
    }


    @After
    void tearDown() {
        bamFileTumor = null
        bamFileControl = null
    }


    @Test
    void testConstraintsAllFine() {
        SnvCallingInstance instance = createSnvCallingInstance([tumorBamFile: bamFileTumor,
            controlBamFile: bamFileControl, config: new ConfigPerProjectAndSeqType()])
        assert instance.validate()
    }


    @Test
    void testSeqTypeConstraint() {
        bamFileTumor.mergingPass.mergingSet.mergingWorkPackage.seqType = new SeqType()

        SnvCallingInstance differentSeqTypeInstance = createSnvCallingInstance([tumorBamFile: bamFileTumor,
            controlBamFile: bamFileControl, config: new ConfigPerProjectAndSeqType()])
        assert !differentSeqTypeInstance.validate()
    }


    @Test
    void testIndividualConstraint() {
        bamFileTumor.mergingPass.mergingSet.mergingWorkPackage.sample.individual = new Individual()

        SnvCallingInstance differentIndividualInstance = createSnvCallingInstance([tumorBamFile: bamFileTumor,
            controlBamFile: bamFileControl, config: new ConfigPerProjectAndSeqType()])
        assert !differentIndividualInstance.validate()
    }


    @Test
    void testNoConfigPerProjectAndSeqType() {
        SnvCallingInstance instance = createSnvCallingInstance([tumorBamFile: bamFileTumor,
            controlBamFile: bamFileControl])
        assert !instance.validate()
    }

    SnvCallingInstance createSnvCallingInstance(Map properties = [:]) {
        return new SnvCallingInstance([
            pipelineVersion: "pipeline1.0",
            configFileName: "config1.0",
            processingState: SnvProcessingStates.IN_PROGRESS,
        ] + properties)
    }


    ProcessedMergedBamFile createProcessedMergedBamFile(Individual individual, SeqType seqType) {
        SampleType sampleType = new SampleType(
                name: "tumor")
        sampleType.save(flush: true)

        Sample sample = new Sample (
                individual: individual,
                sampleType: sampleType)
        sample.save(flush: true)

        MergingWorkPackage workPackage = new MergingWorkPackage(
                sample: sample)
        workPackage.save(flush: true)

        MergingSet mergingSet = new MergingSet(
                mergingWorkPackage: workPackage)
        mergingSet.save(flush: true)

        MergingPass mergingPass = new MergingPass(
                identifier: 1,
                mergingSet: mergingSet)
        mergingPass.save(flush: true)

        ProcessedMergedBamFile bamFile = new ProcessedMergedBamFile(
                type: AbstractBamFile.BamType.SORTED,
                mergingPass: mergingPass)
        bamFile.save(flush: true)

        return bamFile
    }
}
