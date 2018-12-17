package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.junit.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*

@TestFor(ProcessedMergedBamFile)
@Mock([
        AlignmentPass,
        DataFile,
        Individual,
        FileType,
        LibraryPreparationKit,
        MergingCriteria,
        MergingPass,
        MergingSet,
        MergingWorkPackage,
        MergingSetAssignment,
        Pipeline,
        Project,
        ProcessedBamFile,
        ProcessedMergedBamFile,
        Realm,
        ReferenceGenome,
        Run,
        RunSegment,
        Sample,
        SampleType,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SeqTrack,
        SeqType,
        SoftwareTool,
])
class ProcessedMergedBamFileTests {

    MergingPass mergingPass = null
    MergingSet mergingSet = null
    MergingWorkPackage workPackage = null

    @Before
    void setUp() {
        this.workPackage = DomainFactory.createMergingWorkPackage(
                seqType: DomainFactory.createSeqType(),
                pipeline: DomainFactory.createDefaultOtpPipeline(),
        )
        this.workPackage.save(flush: true)

        this.mergingSet = new MergingSet(
                identifier: 1,
                mergingWorkPackage: workPackage)
        this.mergingSet.save(flush: true)

        this.mergingPass = new MergingPass(
                identifier: 1,
                mergingSet: mergingSet)
        this.mergingPass.save(flush: true)
    }

    @After
    void tearDown() {
        mergingPass = null
        mergingSet = null
        workPackage = null
    }

    @Test
    void testSave() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass)
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    @Test
    void testSaveWithNumberOfLanes() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass, [
                numberOfMergedLanes: 3,
        ])
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    @Test
    void testConstraints() {
        // mergingPass must not be null
        ProcessedMergedBamFile bamFile = new ProcessedMergedBamFile(
                type: AbstractBamFile.BamType.SORTED)
        Assert.assertFalse(bamFile.validate())
    }

    @Test
    void testMergingWorkPackageConstraint_NoWorkpackage_ShouldFail() {
        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFileWithoutProcessedBamFile(mergingPass, [
                type       : AbstractBamFile.BamType.MDUP,
                workPackage: null,
        ], false)
        TestCase.assertValidateError(processedMergedBamFile, "workPackage", "nullable", null)
    }

    @Test
    void testIsMostRecentBamFile() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass)
        bamFile.save(flush: true)

        assertTrue(bamFile.isMostRecentBamFile())

        MergingPass secondMergingPass = new MergingPass(
                identifier: 2,
                mergingSet: mergingSet)
        secondMergingPass.save(flush: true)

        ProcessedMergedBamFile secondBamFile = DomainFactory.createProcessedMergedBamFile(secondMergingPass)
        secondBamFile.save(flush: true)

        assertFalse(bamFile.isMostRecentBamFile())
        assertTrue(secondBamFile.isMostRecentBamFile())

        MergingSet secondMergingSet = new MergingSet(
                identifier: 2,
                mergingWorkPackage: workPackage)
        secondMergingSet.save(flush: true)

        MergingPass firstMergingPassOfSecondMergingSet = new MergingPass(
                identifier: 1,
                mergingSet: secondMergingSet)
        firstMergingPassOfSecondMergingSet.save(flush: true)

        ProcessedMergedBamFile firstBamFileOfSecondMergingSet = DomainFactory.createProcessedMergedBamFile(firstMergingPassOfSecondMergingSet)
        firstBamFileOfSecondMergingSet.save(flush: true)

        assertFalse(secondBamFile.isMostRecentBamFile())
        assertTrue(firstBamFileOfSecondMergingSet.isMostRecentBamFile())
    }

    @Test
    void testGetBamFileName() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass)

        assert "${bamFile.sampleType.name}_${bamFile.individual.pid}_${bamFile.seqType.name}_${bamFile.seqType.libraryLayout}_merged.mdup.bam" == bamFile.bamFileName
    }

    @Test
    void testFileNameNoSuffix() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass)
        assert "${bamFile.sampleType.name}_${bamFile.individual.pid}_${bamFile.seqType.name}_${bamFile.seqType.libraryLayout}_merged.mdup" == bamFile.fileNameNoSuffix()
    }
}
