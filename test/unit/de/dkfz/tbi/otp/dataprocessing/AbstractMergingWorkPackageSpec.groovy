package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import spock.lang.*

@Mock([
        AbstractMergingWorkPackage,
        AntibodyTarget,
        Individual,
        LibraryPreparationKit,MergingSet,
        MergingWorkPackage,
        Pipeline,
        Project,
        ProjectCategory,
        ReferenceGenome,
        Sample,
        SampleType,
        SeqPlatformGroup,
        SeqType,
])
class AbstractMergingWorkPackageSpec extends Specification {

    TestAbstractMergingWorkPackage testAMWP
    TestAbstractMergingWorkPackage testAMWP2
    TestAbstractMergedBamFile testAMBF

    class TestAbstractMergingWorkPackage extends AbstractMergingWorkPackage {

        @Override
        AbstractMergedBamFile getBamFileThatIsReadyForFurtherAnalysis() {
            return null
        }

    }

    class TestAbstractMergedBamFile extends AbstractMergedBamFile {

        @Override
        boolean isMostRecentBamFile() {
            return false
        }

        @Override
        String getBamFileName() {
            return null
        }

        @Override
        String getBaiFileName() {
            return null
        }

        @Override
        AlignmentConfig getAlignmentConfig() {
            return null
        }

        @Override
        void withdraw() {

        }

        @Override
        protected File getPathForFurtherProcessingNoCheck() {
            return null
        }

        @Override
        AbstractMergingWorkPackage getMergingWorkPackage() {
            return TestAbstractMergingWorkPackage.get(workPackage.id)
        }

        @Override
        Set<SeqTrack> getContainedSeqTracks() {
            return null
        }

        @Override
        AbstractQualityAssessment getOverallQualityAssessment() {
            return null
        }

        @Override
        File getFinalInsertSizeFile() {
            return null
        }

        @Override
        Integer getMaximalReadLength() {
            return null
        }
    }

    def setup() {
        testAMWP = new TestAbstractMergingWorkPackage()
        testAMWP2 = new TestAbstractMergingWorkPackage()
        testAMBF = new TestAbstractMergedBamFile()
    }

    void "test constraint bamFileInProjectFolder, when workpackage invalid then validate fails"() {
        given: "Given an AbstractMergingWorkPackage, you assign a different one to an AbstractMergedBamFile"
        testAMBF.workPackage = testAMWP2
        testAMWP.bamFileInProjectFolder = testAMBF
        testAMBF.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED

        expect:
        TestCase.assertAtLeastExpectedValidateError(testAMWP, "bamFileInProjectFolder", "validator.invalid", testAMBF)
    }

    void "test constraint bamFileInProjectFolder, when fileOperationStatus invalid then validate fails"() {
        given:
        testAMBF.workPackage = testAMWP
        testAMWP.bamFileInProjectFolder = testAMBF
        testAMBF.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING

        expect:
        TestCase.assertAtLeastExpectedValidateError(testAMWP, "bamFileInProjectFolder", "validator.invalid", testAMBF)
    }

    void "test constraint bamFileInProjectFolder, when valid"() {
        given:
        testAMBF.workPackage = testAMWP
        testAMWP.bamFileInProjectFolder = testAMBF
        testAMBF.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED

        when:
        testAMWP.validate()

        then:
        noExceptionThrown()
    }



    void "constraint for antibodyTarget, when seqType is not chip seq and antibodyTarget is not set, then validate should not create errors"() {
        when:
        AbstractMergingWorkPackage workPackage = createTestAbstractMergingWorkPackage([
                seqType       : DomainFactory.createSeqType(),
                antibodyTarget: null,
        ])
        workPackage.validate()

        then:
        workPackage.errors.errorCount == 0
    }

    void "constraint for antibodyTarget, when seqType is chip seq and antibodyTarget is set, then validate should not create errors"() {
        when:
        AbstractMergingWorkPackage workPackage = createTestAbstractMergingWorkPackage([
                seqType       : DomainFactory.createChipSeqType(),
                antibodyTarget: DomainFactory.createAntibodyTarget(),
        ])
        workPackage.validate()

        then:
        workPackage.errors.errorCount == 0
    }

    void "constraint for antibodyTarget, when seqType is chip seq and antibodyTarget is not set, then validate should create errors"() {
        when:
        AbstractMergingWorkPackage workPackage = createTestAbstractMergingWorkPackage([
                seqType       : DomainFactory.createChipSeqType(),
                antibodyTarget: null,
        ])

        then:
        TestCase.assertValidateError(workPackage, 'antibodyTarget', 'For ChipSeq the antibody target have to be set', workPackage.antibodyTarget)
    }

    void "constraint for antibodyTarget, when seqType is not chip seq and antibodyTarget is set, then validate should create errors"() {
        when:
        AbstractMergingWorkPackage workPackage = createTestAbstractMergingWorkPackage([
                seqType       : DomainFactory.createSeqType(),
                antibodyTarget: DomainFactory.createAntibodyTarget(),
        ])

        then:
        TestCase.assertValidateError(workPackage, 'antibodyTarget', 'For non ChipSeq the antibody target may not be set', workPackage.antibodyTarget)
    }

    TestAbstractMergingWorkPackage createTestAbstractMergingWorkPackage(Map properties) {
        new TestAbstractMergingWorkPackage([
                sample:                new Sample(),
                referenceGenome:       new ReferenceGenome(),
                pipeline:              DomainFactory.createPanCanPipeline(),
        ] + properties)
    }
}
