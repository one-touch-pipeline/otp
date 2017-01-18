package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import spock.lang.*

@Mock([
        AbstractMergingWorkPackage,
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
}
