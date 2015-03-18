package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.dataprocessing.MergingSet.State
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Run.StorageRealm
import de.dkfz.tbi.otp.ngsdata.SoftwareTool.Type
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

class ProcessedMergedBamFileIntegrationTests {

    TestData testData = new TestData()
    Sample sample
    SeqType seqType
    MergingSet mergingSet
    Project project
    Individual individual
    SampleType sampleType
    SoftwareTool softwareTool
    SeqPlatform seqPlatform
    Run run

    final static String directory = "/tmp/otp-unit-test/pmbfs/processing/project-dir/results_per_pid/patient/merging//sample-type/seq-type/library/DEFAULT/0/pass0"

    @Before
    void setUp() {
        project = TestData.createProject(
                        name: "project",
                        dirName: "project-dir",
                        realmName: 'DKFZ',
                        )
        assertNotNull(project.save([flush: true]))

        individual = new Individual(
                        pid: "patient",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true]))

        sampleType = new SampleType(
                        name: "sample-type"
                        )
        assertNotNull(sampleType.save([flush: true]))

        sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true]))

        seqType = new SeqType(
                        name: "seq-type",
                        libraryLayout: "library",
                        dirName: "seq-type-dir"
                        )
        assertNotNull(seqType.save([flush: true]))
        seqType.refresh()

        softwareTool = new SoftwareTool(
                        programName: "name",
                        programVersion: "version",
                        qualityCode: "quality",
                        type: Type.ALIGNMENT
                        )
        assertNotNull(softwareTool.save([flush: true]))

        seqPlatform = SeqPlatform.build()

        SeqCenter seqCenter = new SeqCenter(
                        name: "name",
                        dirName: "dirName"
                        )
        assertNotNull(seqCenter.save([flush: true]))

        run = new Run(
                        name: "name",
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform,
                        storageRealm: StorageRealm.DKFZ
                        )
        assertNotNull(run.save([flush: true]))
    }

    @After
    void tearDown() {
        sample = null
        seqType = null
        mergingSet = null
        project = null
        individual = null
        sampleType = null
        softwareTool = null
        seqPlatform = null
        run = null
    }


    @Test
    void testConstraintsExternal() {
        SeqTrack seqTrack = createSeqTrack("lane no. 1")
        ProcessedBamFile processedBamFile = createProcessedBamFile(12345, seqTrack)
        MergingPass mergingPass = createMergingPass(seqTrack)
        createMergingSetAssignment(processedBamFile)
        FastqSet fastqSet = new FastqSet(
                seqTracks: [seqTrack]
        )
        assertNotNull(fastqSet.save([flush: true]))

        ProcessedMergedBamFile bamFile5 = new ProcessedMergedBamFile(
                type: BamType.SORTED,
                mergingPass: mergingPass,
        )
        assertTrue(bamFile5.validate())

        ProcessedMergedBamFile bamFile6 = new ProcessedMergedBamFile(
                type: BamType.SORTED,
                mergingPass: mergingPass,
                fastqSet: fastqSet,
        )
        assertTrue(bamFile6.validate())

        ProcessedMergedBamFile bamFile7 = new ProcessedMergedBamFile(
                type: BamType.SORTED,
                fastqSet: fastqSet,
        )
        assertFalse(bamFile7.validate())

        ProcessedMergedBamFile bamFile8 = new ProcessedMergedBamFile(
                type: BamType.SORTED,
        )
        assertFalse(bamFile8.validate())
    }

    @Test
    void testToString() {
        SeqTrack seqTrack = createSeqTrack("lane no. 1")
        ProcessedBamFile processedBamFile = createProcessedBamFile(12345, seqTrack)
        MergingPass mergingPass = createMergingPass(seqTrack)
        createMergingSetAssignment(processedBamFile)
        ProcessedMergedBamFile bamFile = new ProcessedMergedBamFile(
                type: BamType.SORTED,
                mergingPass: mergingPass,
        )
        assertNotNull(bamFile.save([flush: true]))

        String expected = /id: \d+ pass: 0 \(latest\) set: 0 \(latest\) <br>sample: mockPid sample-type seqType: seq-type library <br>project: project/
        String actual = bamFile.toString()
        assertTrue("Expected string matching '" + expected + "'. Got: " + actual, actual.matches(expected))
    }

    @Test
    void test_getOverallQualityAssessment_WhenOnePassExists_ShouldReturnThis() {

        final Long ARBITRARY_IDENTIFIER = 42

        def processedMergedBamFile = createProcessedMergedBamFile()
        def oqa = createOverallQualityAssessment(processedMergedBamFile, ARBITRARY_IDENTIFIER)

        assert processedMergedBamFile.overallQualityAssessment == oqa
    }

    @Test
    void test_getOverallQualityAssessment_WhenMultiplePassesExists_ShouldReturnLatest() {

        final Long IDENTIFIER_FORMER = 100
        final Long IDENTIFIER_LATER = 200

        assert IDENTIFIER_FORMER < IDENTIFIER_LATER

        def processedMergedBamFile = createProcessedMergedBamFile()
        def oqaFormer = createOverallQualityAssessment(processedMergedBamFile, IDENTIFIER_FORMER)
        def oqaLater = createOverallQualityAssessment(processedMergedBamFile, IDENTIFIER_LATER)

        assert processedMergedBamFile.overallQualityAssessment == oqaLater
        assert processedMergedBamFile.overallQualityAssessment != oqaFormer
    }

    private MergingPass createMergingPass(SeqTrack seqTrack) {
        MergingWorkPackage mergingWorkPackage = testData.findOrSaveMergingWorkPackage(seqTrack)

        mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage,
                        status: State.NEEDS_PROCESSING,
                        )
        assertNotNull(mergingSet.save([flush: true]))

        MergingPass mergingPass = new MergingPass(
                        identifier: 0,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true]))
        return mergingPass
    }

    private static FastqSet createFastqSet(List<SeqTrack> seqTracks) {
        FastqSet fastqSet = new FastqSet(
                seqTracks: seqTracks
        )
        assertNotNull(fastqSet.save([flush: true]))
        return fastqSet
    }

    private ProcessedBamFile createProcessedBamFile(int identifier, SeqTrack seqTrack) {
        AlignmentPass alignmentPass = testData.createAlignmentPass(
                        identifier: identifier,
                        seqTrack: seqTrack,
                        description: "test"
                        )
        assertNotNull(alignmentPass.save([flush: true]))

        ProcessedBamFile processedBamFile = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: BamType.SORTED,
                        status: AbstractBamFile.State.NEEDS_PROCESSING,
                        qualityAssessmentStatus: QaProcessingStatus.FINISHED
                        )
        assertNotNull(processedBamFile.save([flush: true]))

        return processedBamFile
    }

    MergingSetAssignment createMergingSetAssignment(ProcessedBamFile processedBamFile) {
        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile
                        )
        assertNotNull(mergingSetAssignment.save([flush: true]))
        return mergingSetAssignment
    }

    SeqTrack createSeqTrack(String laneId = "laneId") {
        SeqTrack seqTrack = new SeqTrack(
                        laneId: laneId,
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack.save([flush: true]))
        return seqTrack
    }

    private ProcessedMergedBamFile createProcessedMergedBamFile() {

        MergingPass mergingPass = createMergingPass(createSeqTrack())

        ProcessedMergedBamFile processedMergedBamFile = new ProcessedMergedBamFile([
                type                   : BamType.SORTED,
                mergingPass            : mergingPass,
                withdrawn              : false,
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                status                 : AbstractBamFile.State.PROCESSED,
        ])
        assert processedMergedBamFile.save([flush: true])

        return processedMergedBamFile
    }

    private static OverallQualityAssessmentMerged createOverallQualityAssessment(ProcessedMergedBamFile processedMergedBamFile, Long identifier) {

        assert processedMergedBamFile: 'processedMergedBamFile must not be null'

        QualityAssessmentMergedPass qualityAssessmentMergedPass = new QualityAssessmentMergedPass([
                processedMergedBamFile: processedMergedBamFile,
                identifier: QualityAssessmentMergedPass.nextIdentifier(processedMergedBamFile),
        ])
        assert qualityAssessmentMergedPass.save([flush: true])

        OverallQualityAssessmentMerged overallQualityAssessmentMerged = new OverallQualityAssessmentMerged([
                id                   : identifier,
                qualityAssessmentMergedPass: qualityAssessmentMergedPass,
        ])
        assert overallQualityAssessmentMerged.save([flush: true])

        return overallQualityAssessmentMerged
    }
}
