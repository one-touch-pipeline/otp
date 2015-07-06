package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*
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
            libraryLayout: "SINGLE",
            dirName: "whole_genome_sequencing")
        seqType.save(flush: true)
    }

    void tearDown() {
        this.sample = null
        this.seqType = null
    }

    void testSave() {
        MergingWorkPackage workPackage = new TestData().createMergingWorkPackage(
            sample: sample,
            seqType: seqType)
        Assert.assertTrue(workPackage.validate())
        workPackage.save(flush: true)
    }

    @Test
    void testGetMergingProperties() {
        SeqTrack seqTrack = SeqTrack.build()

        def result = MergingWorkPackage.getMergingProperties(seqTrack)
        def expectedResult = [
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
        ]
        assert result == expectedResult
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenCorrect() {
        SeqTrack seqTrack = SeqTrack.build()

        MergingWorkPackage workPackage = MergingWorkPackage.build(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqTrack.seqPlatformGroup)
        assert workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenIncorrectSample() {
        SeqTrack seqTrack = SeqTrack.build()

        MergingWorkPackage workPackage = MergingWorkPackage.build(sample: Sample.build(), seqType: seqTrack.seqType, seqPlatformGroup: seqTrack.seqPlatformGroup)
        assert !workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenIncorrectSeqType() {
        SeqTrack seqTrack = SeqTrack.build()

        MergingWorkPackage workPackage = MergingWorkPackage.build(sample: seqTrack.sample, seqType: seqType.build(), seqPlatformGroup: seqTrack.seqPlatformGroup)
        assert !workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenIncorrectSeqPlatformGroup() {
        SeqTrack seqTrack = SeqTrack.build()

        MergingWorkPackage workPackage = MergingWorkPackage.build(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: SeqPlatformGroup.build())
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
    void test_constraint_onStatSizeFileName_withCorrectName_ShouldBeValid() {
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.buildWithoutSave([statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME])
        assert mergingWorkPackage.validate()
    }

    @Test
    void test_constraint_onStatSizeFileName_withNullNoPancancer_ShouldBeValid() {
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.buildWithoutSave([
                statSizeFileName: null,
                workflow: Workflow.build(
                        name: Workflow.Name.DEFAULT_OTP,
                        type: Workflow.Type.ALIGNMENT,
                ),
        ])
        assert mergingWorkPackage.validate()
    }

    @Test
    void test_constraint_onStatSizeFileName_withNullForPancancer_ShouldBeInValid() {
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.buildWithoutSave([
                statSizeFileName: null,
                workflow: Workflow.build(
                        name: Workflow.Name.PANCAN_ALIGNMENT,
                        type: Workflow.Type.ALIGNMENT,
                ),
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
        "-_.".each {
            try {
                String name = "File${it}.tab"
                MergingWorkPackage mergingWorkPackage = MergingWorkPackage.buildWithoutSave([statSizeFileName: name])
                mergingWorkPackage.validate()
                assert 0 == mergingWorkPackage.errors.errorCount
            } catch (Throwable e) {
                collector.addError(e)
            }
        }
    }

    @Test
    void test_constraint_onStatSizeFileName_WhenInvalidSpecialChar_ShouldBeInvalid() {
        "\"',:;%\$§&<>|^§!?=äöüÄÖÜß´`".each {
            try {
                String name = "File${it}.tab"
                MergingWorkPackage mergingWorkPackage = MergingWorkPackage.buildWithoutSave([statSizeFileName: name])
                TestCase.assertValidateError(mergingWorkPackage, 'statSizeFileName', 'matches.invalid', name)
            } catch (Throwable e) {
                collector.addError(e)
            }
        }
    }
}
