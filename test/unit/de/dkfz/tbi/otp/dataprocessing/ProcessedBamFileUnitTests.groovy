package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Individual.Type
import de.dkfz.tbi.otp.utils.logging.*
import grails.buildtestdata.mixin.*
import grails.test.mixin.*
import org.junit.*

import static org.junit.Assert.*

@TestFor(ProcessedBamFile)
@Build([
        AlignmentPass,
        BamFilePairAnalysis,
        BedFile,
        LibraryPreparationKit,
        ExomeSeqTrack,
        MergingSetAssignment,
        MergingPass,
        ReferenceGenomeProjectSeqType,
        ProjectSeqType,
])
class ProcessedBamFileUnitTests {

    AlignmentPass alignmentPass
    Project project
    Sample sample
    Run run
    SoftwareTool softwareTool
    SeqPlatform seqPlatform
    TestData testData

    @Before
    void setUp() {
        testData = new TestData()

        project = DomainFactory.createProject(
            name: "name",
            dirName: "dirName",
            realmName: "realmName"
            )
        assertNotNull(project.save([flush: true, failOnError: true]))

        seqPlatform = DomainFactory.createSeqPlatform()

        SeqCenter seqCenter = new SeqCenter(
            name: "name",
            dirName: "dirName"
            )
        assertNotNull(seqCenter.save([flush: true, failOnError: true]))

        run = new Run(
            name: "name",
            seqCenter: seqCenter,
            seqPlatform: seqPlatform,
            )
        assertNotNull(run.save([flush: true, failOnError: true]))

        Individual individual = new Individual(
            pid: "pid",
            mockPid: "mockPid",
            mockFullName: "mockFullName",
            type: Type.REAL,
            project: project
            )
        assertNotNull(individual.save([flush: true, failOnError: true]))

        SampleType sampleType = new SampleType(
            name: "name"
            )
        assertNotNull(sampleType.save([flush: true, failOnError: true]))

        sample = new Sample(
            individual: individual,
            sampleType: sampleType
        )
        assertNotNull(sample.save([flush: true, failOnError: true]))

        SeqType seqType = DomainFactory.createSeqType()

        softwareTool = new SoftwareTool(
            programName: "name",
            programVersion: "version",
            type: de.dkfz.tbi.otp.ngsdata.SoftwareTool.Type.ALIGNMENT
            )
        assertNotNull(softwareTool.save([flush: true, failOnError: true]))

        SeqTrack seqTrack = new SeqTrack(
            laneId: "laneId",
            run: run,
            sample: sample,
            seqType: seqType,
            seqPlatform: seqPlatform,
            pipelineVersion: softwareTool
            )
        assertNotNull(seqTrack.save([flush: true, failOnError: true]))

        alignmentPass = DomainFactory.createAlignmentPass(
            identifier: 1,
            seqTrack: seqTrack,
            description: "test"
            )
        assertNotNull(alignmentPass.save([flush: true, failOnError: true]))
    }

    @After
    void tearDown() {
        alignmentPass = null
        project = null
        sample = null
        run = null
        softwareTool = null
        seqPlatform = null
        testData = null
    }

    @Test
    void testSave() {
        ProcessedBamFile bamFile = new ProcessedBamFile(
            type: BamType.SORTED,
            alignmentPass: alignmentPass,
            withdrawn: false,
            status: State.DECLARED
        )
        assertTrue(bamFile.validate())
        assertNotNull(bamFile.save([flush: true, failOnError: true]))

        bamFile.withdrawn = true
        assertTrue(bamFile.validate())
        assertNotNull(bamFile.save([flush: true, failOnError: true]))

        bamFile.status = State.NEEDS_PROCESSING
        assertFalse(bamFile.validate())

        bamFile.withdrawn = false
        assertTrue(bamFile.validate())
        assertNotNull(bamFile.save([flush: true, failOnError: true]))

        bamFile.type = BamType.RMDUP
        assertFalse(bamFile.validate())
    }

    @Test
    void testGetBedFile_ExomeBamFile() {
        SeqType exomeSeqType = DomainFactory.createExomeSeqType()

        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenomeLazy()

        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = new ReferenceGenomeProjectSeqType([
            project : project,
            seqType: exomeSeqType,
            referenceGenome: referenceGenome,
        ])
        assert referenceGenomeProjectSeqType.save([flush: true])

        LibraryPreparationKit libraryPreparationKit = testData.createLibraryPreparationKit("kit")
        assertNotNull(libraryPreparationKit.save([flush: true]))

        BedFile bedFile = testData.createBedFile(referenceGenome, libraryPreparationKit)
        assert bedFile.save([flush: true])

        ExomeSeqTrack exomeSeqTrack = new ExomeSeqTrack(
                laneId: "laneId",
                run: run,
                sample: sample,
                seqType: exomeSeqType,
                seqPlatform: seqPlatform,
                pipelineVersion: softwareTool,
                libraryPreparationKit: libraryPreparationKit,
                kitInfoReliability: InformationReliability.KNOWN
                )
        assertNotNull(exomeSeqTrack.save([flush: true, failOnError: true]))

        AlignmentPass exomeAlignmentPass = DomainFactory.createAlignmentPass(
                identifier: 1,
                seqTrack: exomeSeqTrack,
                description: "test"
                )
        assertNotNull(exomeAlignmentPass.save([flush: true, failOnError: true]))

        ProcessedBamFile exomeBamFile = new ProcessedBamFile(
                type: BamType.SORTED,
                alignmentPass: exomeAlignmentPass,
                withdrawn: false,
                status: State.DECLARED
                )
        assertNotNull(exomeBamFile.save([flush: true, failOnError: true]))

        assert exomeBamFile.bedFile == bedFile
    }

    @Test
    void testGetBedFile_WholeGenomeBamFile() {
        ProcessedBamFile bamFile = new ProcessedBamFile(
            type: BamType.SORTED,
            alignmentPass: alignmentPass,
            withdrawn: false,
            status: State.DECLARED
        )
        assertNotNull(bamFile.save([flush: true, failOnError: true]))
        assert shouldFail(AssertionError, { bamFile.bedFile }).contains("A BedFile is only available when the sequencing type is exome")
    }

    @Test
    void testWithdraw_SetOneBamFileWithdrawn() {
        ProcessedBamFile bamFile = DomainFactory.createProcessedBamFile()

        LogThreadLocal.withThreadLog(System.out) {
            bamFile.withdraw()
        }
        assert bamFile.withdrawn
    }

    @Test
    void testWithdraw_SetBameFileAndMergedBamFileWithdrawn() {
        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile()
        ProcessedBamFile bamFile = DomainFactory.assignNewProcessedBamFile(processedMergedBamFile)

        LogThreadLocal.withThreadLog(System.out) {
            bamFile.withdraw()
        }
        assert bamFile.withdrawn
        assert processedMergedBamFile.withdrawn
    }
}
