/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.otp.dataprocessing

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.*
import org.junit.rules.ErrorCollector

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.ngsdata.*

@TestFor(MergingWorkPackage)
@Build([
    ExternalMergingWorkPackage,
    ReferenceGenome,
    Sample,
    SeqPlatformGroup,
    SeqType,
    SeqPlatform,
    SeqTrack,
    MergingCriteria,
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
        Project project = DomainFactory.createProject(
            name: "project",
            dirName: "dirName",
        )
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

        this.seqType = DomainFactory.createWholeGenomeSeqType(LibraryLayout.SINGLE)
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
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
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
        LibraryPreparationKit libraryPreparationKit = DomainFactory.createLibraryPreparationKit()
        SeqTrack seqTrack = DomainFactory.createSeqTrack(
                libraryPreparationKit: libraryPreparationKit,
                kitInfoReliability: InformationReliability.KNOWN
        )
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
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
    void testGetMergingProperties_WithAntibodyTarget() {
        LibraryPreparationKit libraryPreparationKit = DomainFactory.createLibraryPreparationKit()
        AntibodyTarget antibodyTarget = DomainFactory.createAntibodyTarget()
        SeqTrack seqTrack = DomainFactory.createChipSeqSeqTrack(
                libraryPreparationKit: libraryPreparationKit,
                kitInfoReliability: InformationReliability.KNOWN,
                antibodyTarget: antibodyTarget,
        )
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        def result = MergingWorkPackage.getMergingProperties(seqTrack)
        def expectedResult = [
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                libraryPreparationKit: libraryPreparationKit,
                antibodyTarget: antibodyTarget,
        ]
        assert result == expectedResult
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenCorrect_NoLibraryPreparationKit() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack(libraryPreparationKit: null)
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqTrack.seqPlatformGroup, libraryPreparationKit: seqTrack.libraryPreparationKit)
        assert workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenCorrect_WithLibraryPreparationKit() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack(
                libraryPreparationKit: DomainFactory.createLibraryPreparationKit(),
                kitInfoReliability: InformationReliability.KNOWN,
        )
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqTrack.seqPlatformGroup, libraryPreparationKit: seqTrack.libraryPreparationKit)
        assert workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenCorrect_IgnoreLibraryPreparationKit() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack(
                libraryPreparationKit: DomainFactory.createLibraryPreparationKit(),
                kitInfoReliability: InformationReliability.KNOWN,
        )
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType, useLibPrepKit: false)
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqTrack.seqPlatformGroup, libraryPreparationKit: null)
        assert workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenIncorrectSample() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(sample: Sample.build(), seqType: seqTrack.seqType, seqPlatformGroup: seqTrack.seqPlatformGroup, libraryPreparationKit: seqTrack.libraryPreparationKit)
        assert !workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenIncorrectSeqType() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(sample: seqTrack.sample, seqType: seqType.build(), seqPlatformGroup: seqTrack.seqPlatformGroup, libraryPreparationKit: seqTrack.libraryPreparationKit)
        assert !workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenIncorrectSeqPlatformGroup() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: SeqPlatformGroup.build(), libraryPreparationKit: seqTrack.libraryPreparationKit)
        assert !workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenOnlySeqTrackHasLibraryPrepationKit() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack(
                libraryPreparationKit: DomainFactory.createLibraryPreparationKit(),
                kitInfoReliability: InformationReliability.KNOWN,
        )
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqTrack.seqPlatformGroup, libraryPreparationKit: null)
        assert !workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenOnlyMergingWorkPackageHasLibraryPrepationKit() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqTrack.seqPlatformGroup, libraryPreparationKit: DomainFactory.createLibraryPreparationKit())
        assert !workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenIncorrectLibraryPrepationKit() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack(
                libraryPreparationKit: DomainFactory.createLibraryPreparationKit(),
                kitInfoReliability: InformationReliability.KNOWN,
        )
        DomainFactory.createMergingCriteriaLazy([project: seqTrack.project, seqType: seqTrack.seqType])
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqTrack.seqPlatformGroup, libraryPreparationKit: DomainFactory.createLibraryPreparationKit())
        assert !workPackage.satisfiesCriteria(seqTrack)
    }


    @Test
    void testSatisfiesCriteriaBamFile_whenValid() {
        SeqPlatformGroup seqPlatformGroup = DomainFactory.createSeqPlatformGroup()
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatformWithSeqPlatformGroup(seqPlatformGroups: [seqPlatformGroup])
        SeqTrack seqTrack = DomainFactory.createSeqTrack(run: DomainFactory.createRun(seqPlatform: seqPlatform))
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        MergingWorkPackage workPackage = MergingWorkPackage.build(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqPlatformGroup, pipeline: DomainFactory.createDefaultOtpPipeline())
        AlignmentPass alignmentPass = AlignmentPass.build(seqTrack: seqTrack, workPackage: workPackage)

        ProcessedBamFile processedBamFile = ProcessedBamFile.build(alignmentPass: alignmentPass)
        assert workPackage.satisfiesCriteria(processedBamFile)
    }

    @Test
    void testSatisfiesCriteriaBamFile_whenInvalid() {
        MergingWorkPackage workPackage = MergingWorkPackage.build(statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME)

        ProcessedBamFile processedBamFile = ProcessedBamFile.build()
        assert !workPackage.satisfiesCriteria(processedBamFile)
    }


    @Test
    void test_constraint_onStatSizeFileName_withCorrectNameForPanCan_ShouldBeValid() {
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME,
                pipeline        : DomainFactory.createPanCanPipeline(),
        ], false)
        assert mergingWorkPackage.validate()
    }

    @Test
    void test_constraint_onStatSizeFileName_withCorrectNameNoPanCan_ShouldBeInvalid() {
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME,
                pipeline        : DomainFactory.createDefaultOtpPipeline(),
        ], false)
        TestCase.assertValidateError(mergingWorkPackage, 'statSizeFileName', 'validator.invalid', DomainFactory.DEFAULT_TAB_FILE_NAME)
    }

    @Test
    void test_constraint_onStatSizeFileName_withNullNoPanCan_ShouldBeValid() {
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                statSizeFileName: null,
                pipeline        : DomainFactory.createDefaultOtpPipeline(),
        ], false)
        assert mergingWorkPackage.validate()
    }

    @Test
    void test_constraint_onStatSizeFileName_withNullForPanCan_ShouldBeInvalid() {
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                statSizeFileName: null,
                pipeline        : DomainFactory.createPanCanPipeline(),
        ], false)
        TestCase.assertValidateError(mergingWorkPackage, 'statSizeFileName', 'validator.invalid', null)
    }

    @Test
    void test_constraint_onStatSizeFileName_WhenBlank_ShouldBeInvalid() {
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([:], false)
        mergingWorkPackage.statSizeFileName = '' //setting empty string does not work via map

        TestCase.assertValidateError(mergingWorkPackage, 'statSizeFileName', 'blank', '')
    }

    @Test
    void test_constraint_onStatSizeFileName_WhenValidSpecialChar_ShouldBeValid() {
        Pipeline pipeline = DomainFactory.createPanCanPipeline()

        "-_.".each {
            try {
                String name = "File${it}.tab"
                MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                        statSizeFileName: name,
                        pipeline        : pipeline,
                ], false)
                mergingWorkPackage.validate()
                assert 0 == mergingWorkPackage.errors.errorCount
            } catch (Throwable e) {
                collector.addError(e)
            }
        }
    }

    @Test
    void test_constraint_onStatSizeFileName_WhenInvalidSpecialChar_ShouldBeInvalid() {
        Pipeline pipeline = DomainFactory.createPanCanPipeline()

        "\"',:;%\$§&<>|^§!?=äöüÄÖÜß´`".each {
            try {
                String name = "File${it}.tab"
                MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                        statSizeFileName: name,
                        pipeline        : pipeline,
                ], false)
                TestCase.assertAtLeastExpectedValidateError(mergingWorkPackage, 'statSizeFileName', 'matches.invalid', name)
            } catch (Throwable e) {
                collector.addError(e)
            }
        }
    }


    @Test
    void test_constraint_libraryPreparationKit_WhenNeitherExomeNorWgbsAndNoLibraryPreparationKit_ShouldBeValid() {
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                libraryPreparationKit: null,
                seqType: SeqType.build(),
        ], false)
        assert mergingWorkPackage.validate()
    }

    @Test
    void test_constraint_libraryPreparationKit_WhenNeitherExomeNorWgbsAndWithLibraryPreparationKit_ShouldBeValid() {
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                libraryPreparationKit: DomainFactory.createLibraryPreparationKit(),
                seqType: SeqType.build(),
        ])
        assert mergingWorkPackage.validate()
    }

    @Test
    void test_constraint_libraryPreparationKit_WhenExomeAndWithLibraryPreparationKit_ShouldBeValid() {
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                libraryPreparationKit: DomainFactory.createLibraryPreparationKit(),
                seqType: DomainFactory.createExomeSeqType(),
        ], false)
        assert mergingWorkPackage.validate()
    }

    @Test
    void test_constraint_libraryPreparationKit_WhenExomeAndNoLibraryPreparationKit_ShouldFail() {
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                libraryPreparationKit: null,
                seqType: DomainFactory.createExomeSeqType(),
        ], false)
        TestCase.assertValidateError(mergingWorkPackage, 'libraryPreparationKit', 'validator.invalid', null)
    }

    @Test
    void test_constraint_libraryPreparationKit_WhenWgbsAndWithLibraryPreparationKit_ShouldFail() {
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                libraryPreparationKit: DomainFactory.createLibraryPreparationKit(),
                seqType: DomainFactory.createSeqType(name: SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName),
        ], false)
        TestCase.assertValidateError(mergingWorkPackage, 'libraryPreparationKit', 'validator.invalid', mergingWorkPackage.libraryPreparationKit)
    }

    @Test
    void test_constraint_libraryPreparationKit_WhenWgbsAndNoLibraryPreparationKit_ShouldBeValid() {
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                libraryPreparationKit: null,
                seqType: DomainFactory.createSeqType(name: SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName),
        ], false)
        assert mergingWorkPackage.validate()
    }

    @Test
    void test_constraint_ExternalMergingWorkPackageExistsAlready_NewMergingWorkPackageHasToBaSaved() {
        ExternalMergingWorkPackage externalMergingWorkPackage = DomainFactory.createExternalMergingWorkPackage()
        DomainFactory.createMergingWorkPackage([
                sample: externalMergingWorkPackage.sample,
                seqType: externalMergingWorkPackage.seqType,
        ])
    }

    @Test
    void test_constraint_MergingWorkPackageExistsAlready_NewMergingWorkPackageHasToBaSaved() {
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage(pipeline: DomainFactory.createDefaultOtpPipeline())
        MergingWorkPackage mergingWorkPackage1 = new MergingWorkPackage(
                sample: mergingWorkPackage.sample,
                seqType: mergingWorkPackage.seqType,
                referenceGenome: mergingWorkPackage.referenceGenome,
                seqPlatformGroup: mergingWorkPackage.seqPlatformGroup,
                pipeline: mergingWorkPackage.pipeline,
        )

        TestCase.assertValidateError(mergingWorkPackage1, 'sample', 'The mergingWorkPackage must be unique for one sample and seqType and antibodyTarget', mergingWorkPackage.sample)
    }

}
