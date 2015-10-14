package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.ngsdata.*
import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.*
import org.junit.rules.ErrorCollector

@TestFor(MergingWorkPackage)
@Build([
    ReferenceGenome,
    Sample,
    SeqPlatformGroup,
    SeqType,
    SeqPlatform,
    SeqTrack,
    MergingWorkPackage,
    ProcessedBamFile,
    AlignmentPass,
])
class MergingWorkPackageUnitTests {

    Sample sample = null
    SeqType seqType = null

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Before
    void setUp() {
        Project project = TestData.createProject(
            name: "project",
            dirName: "dirName",
            realmName: "DKFZ")
        project.save(flush: true)

        Individual individual = new Individual(
            pid: "pid",
            mockPid: "mockPid",
            mockFullName: "mockFullName",
            type: Individual.Type.REAL,
            project: project)
        individual.save(flush: true)

        SampleType sampleType = new SampleType(
            name: "sample-type")
        sampleType.save(flush: true)

        this.sample = new Sample(
            individual: individual,
            sampleType: sampleType)
        this.sample.save(flush: true)

        this.seqType = new SeqType(
            name: "WHOLE_GENOME",
            libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE,
            dirName: "whole_genome_sequencing")
        seqType.save(flush: true)
    }

    @After
    void tearDown() {
        this.sample = null
        this.seqType = null
    }

    @Test
    void testSave() {
        MergingWorkPackage workPackage = new TestData().createMergingWorkPackage(
            sample: sample,
            seqType: seqType)
        Assert.assertTrue(workPackage.validate())
        workPackage.save(flush: true)
    }

    @Test
    void testGetMergingProperties_NoLibraryPreperationKit() {
        SeqTrack seqTrack = SeqTrack.build()

        def result = MergingWorkPackage.getMergingProperties(seqTrack)
        def expectedResult = [
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                libraryPreparationKit: null,
        ]
        assert result == expectedResult
    }

    @Test
    void testGetMergingProperties_WithLibraryPreperationKit() {
        LibraryPreparationKit libraryPreparationKit = LibraryPreparationKit.build()
        SeqTrack seqTrack = SeqTrack.build(
                libraryPreparationKit: libraryPreparationKit,
                kitInfoReliability: InformationReliability.KNOWN
        )

        def result = MergingWorkPackage.getMergingProperties(seqTrack)
        def expectedResult = [
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                libraryPreparationKit: libraryPreparationKit,
        ]
        assert result == expectedResult
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenCorrect_NoLibraryPreparationKit() {
        SeqTrack seqTrack = SeqTrack.build(libraryPreparationKit: null)

        MergingWorkPackage workPackage = MergingWorkPackage.build(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqTrack.seqPlatformGroup, libraryPreparationKit: seqTrack.libraryPreparationKit)
        assert workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenCorrect_WithLibraryPreparationKit() {
        SeqTrack seqTrack = SeqTrack.build(
                libraryPreparationKit: LibraryPreparationKit.build(),
                kitInfoReliability: InformationReliability.KNOWN,
        )

        MergingWorkPackage workPackage = MergingWorkPackage.build(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqTrack.seqPlatformGroup, libraryPreparationKit: seqTrack.libraryPreparationKit)
        assert workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenIncorrectSample() {
        SeqTrack seqTrack = SeqTrack.build()

        MergingWorkPackage workPackage = MergingWorkPackage.build(sample: Sample.build(), seqType: seqTrack.seqType, seqPlatformGroup: seqTrack.seqPlatformGroup, libraryPreparationKit: seqTrack.libraryPreparationKit)
        assert !workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenIncorrectSeqType() {
        SeqTrack seqTrack = SeqTrack.build()

        MergingWorkPackage workPackage = MergingWorkPackage.build(sample: seqTrack.sample, seqType: seqType.build(), seqPlatformGroup: seqTrack.seqPlatformGroup, libraryPreparationKit: seqTrack.libraryPreparationKit)
        assert !workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenIncorrectSeqPlatformGroup() {
        SeqTrack seqTrack = SeqTrack.build()

        MergingWorkPackage workPackage = MergingWorkPackage.build(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: SeqPlatformGroup.build(), libraryPreparationKit: seqTrack.libraryPreparationKit)
        assert !workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenOnlySeqTrackHasLibraryPrepationKit() {
        SeqTrack seqTrack = SeqTrack.build(
                libraryPreparationKit: LibraryPreparationKit.build(),
                kitInfoReliability: InformationReliability.KNOWN,
        )

        MergingWorkPackage workPackage = MergingWorkPackage.build(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqTrack.seqPlatformGroup, libraryPreparationKit: null)
        assert !workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenOnlyMergingWorkPackageHasLibraryPrepationKit() {
        SeqTrack seqTrack = SeqTrack.build()

        MergingWorkPackage workPackage = MergingWorkPackage.build(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqTrack.seqPlatformGroup, libraryPreparationKit: LibraryPreparationKit.build())
        assert !workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenIncorrectLibraryPrepationKit() {
        SeqTrack seqTrack = SeqTrack.build(
                libraryPreparationKit: LibraryPreparationKit.build(),
                kitInfoReliability: InformationReliability.KNOWN,
        )

        MergingWorkPackage workPackage = MergingWorkPackage.build(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqTrack.seqPlatformGroup, libraryPreparationKit: LibraryPreparationKit.build())
        assert !workPackage.satisfiesCriteria(seqTrack)
    }


    @Test
    void testSatisfiesCriteriaBamFile_whenValid() {
        SeqPlatformGroup seqPlatformGroup = SeqPlatformGroup.build(name: "HiSeq 2000/2500")
        SeqPlatform seqPlatform = SeqPlatform.build(seqPlatformGroup: seqPlatformGroup)
        SeqTrack seqTrack = SeqTrack.build(seqPlatform: seqPlatform)

        MergingWorkPackage workPackage = MergingWorkPackage.build(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqPlatformGroup)
        AlignmentPass alignmentPass = AlignmentPass.build(seqTrack: seqTrack, workPackage: workPackage)

        ProcessedBamFile processedBamFile = ProcessedBamFile.build(alignmentPass: alignmentPass)
        assert workPackage.satisfiesCriteria(processedBamFile)
    }

    @Test
    void testSatisfiesCriteriaBamFile_whenInvalid() {
        MergingWorkPackage workPackage = MergingWorkPackage.build()

        ProcessedBamFile processedBamFile = ProcessedBamFile.build()
        assert !workPackage.satisfiesCriteria(processedBamFile)
    }


    @Test
    void test_constraint_onStatSizeFileName_withCorrectNameForPanCan_ShouldBeValid() {
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.buildWithoutSave([
                statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME,
                workflow: DomainFactory.createPanCanWorkflow(),
        ])
        assert mergingWorkPackage.validate()
    }

    @Test
    void test_constraint_onStatSizeFileName_withCorrectNameNoPanCan_ShouldBeInvalid() {
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.buildWithoutSave([
                statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME,
                workflow: DomainFactory.createDefaultOtpWorkflow(),
        ])
        TestCase.assertValidateError(mergingWorkPackage, 'statSizeFileName', 'validator.invalid', DomainFactory.DEFAULT_TAB_FILE_NAME)
    }

    @Test
    void test_constraint_onStatSizeFileName_withNullNoPanCan_ShouldBeValid() {
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.buildWithoutSave([
                statSizeFileName: null,
                workflow: DomainFactory.createDefaultOtpWorkflow(),
        ])
        assert mergingWorkPackage.validate()
    }

    @Test
    void test_constraint_onStatSizeFileName_withNullForPanCan_ShouldBeInvalid() {
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.buildWithoutSave([
                statSizeFileName: null,
                workflow: DomainFactory.createPanCanWorkflow(),
        ])
        TestCase.assertValidateError(mergingWorkPackage, 'statSizeFileName', 'validator.invalid', null)
    }

    @Test
    void test_constraint_onStatSizeFileName_WhenBlank_ShouldBeInvalid() {
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.buildWithoutSave()
        mergingWorkPackage.statSizeFileName = '' //setting empty string does not work via map

        TestCase.assertValidateError(mergingWorkPackage, 'statSizeFileName', 'blank', '')
    }

    @Test
    void test_constraint_onStatSizeFileName_WhenValidSpecialChar_ShouldBeValid() {
        Workflow workflow = DomainFactory.createPanCanWorkflow()

        "-_.".each {
            try {
                String name = "File${it}.tab"
                MergingWorkPackage mergingWorkPackage = MergingWorkPackage.buildWithoutSave([
                        statSizeFileName: name,
                        workflow: workflow,
                ])
                mergingWorkPackage.validate()
                assert 0 == mergingWorkPackage.errors.errorCount
            } catch (Throwable e) {
                collector.addError(e)
            }
        }
    }

    @Test
    void test_constraint_onStatSizeFileName_WhenInvalidSpecialChar_ShouldBeInvalid() {
        Workflow workflow = DomainFactory.createPanCanWorkflow()

        "\"',:;%\$§&<>|^§!?=äöüÄÖÜß´`".each {
            try {
                String name = "File${it}.tab"
                MergingWorkPackage mergingWorkPackage = MergingWorkPackage.buildWithoutSave([
                        statSizeFileName: name,
                        workflow: workflow,
                ])
                TestCase.assertAtLeastExpectedValidateError(mergingWorkPackage, 'statSizeFileName', 'matches.invalid', name)
            } catch (Throwable e) {
                collector.addError(e)
            }
        }
    }


    @Test
    void test_constraint_libraryPreparationKit_WhenNoExomeAndNoLibraryPreparationKit_ShouldBeValid() {
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.buildWithoutSave([
                libraryPreparationKit: null,
                seqType: SeqType.build(),
        ])
        assert mergingWorkPackage.validate()
    }

    @Test
    void test_constraint_libraryPreparationKit_WhenNoExomeAndWithLibraryPreparationKit_ShouldBeValid() {
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.buildWithoutSave([
                libraryPreparationKit: LibraryPreparationKit.build(),
                seqType: SeqType.build(),
        ])
        assert mergingWorkPackage.validate()
    }

    @Test
    void test_constraint_libraryPreparationKit_WhenExomeAndWithLibraryPreparationKit_ShouldBeValid() {
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.buildWithoutSave([
                libraryPreparationKit: LibraryPreparationKit.build(),
                seqType: DomainFactory.createExomeSeqType(),
        ])
        assert mergingWorkPackage.validate()
    }

    @Test
    void test_constraint_libraryPreparationKit_WhenExomeAndNoLibraryPreparationKit_ShouldFail() {
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.buildWithoutSave([
                libraryPreparationKit: null,
                seqType: DomainFactory.createExomeSeqType(),
        ])
        TestCase.assertValidateError(mergingWorkPackage, 'libraryPreparationKit', 'validator.invalid', null)
    }

}
