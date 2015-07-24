package de.dkfz.tbi.otp.dataprocessing

import static de.dkfz.tbi.otp.dataprocessing.AbstractBamFileServiceTests.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvJobResult
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import org.junit.Test

class RoddyBamFileTest {

    @Test
    void testConstraints_allFine() {
        RoddyBamFile bamFile = createRoddyBamFileWithBaseBamFile()
        assert bamFile.save(flush: true)
    }

    @Test
    void test_CheckThatSeqtracksConnectionIsSavedToDatabase() {
        DomainFactory.createRoddyBamFile()

        assert RoddyBamFile.withCriteria {
            seqTracks {
                isNotNull('id')
            }
        }
    }

    @Test
    void testConstraints_noSeqTracks_shouldFail() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
        bamFile.seqTracks = [] as Set
        TestCase.assertAtLeastExpectedValidateError(bamFile, 'seqTracks', 'minSize.notmet', bamFile.seqTracks)
    }

    @Test
    void testConstraints_notRoddyWorkflowName_shouldFail() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
        bamFile.workPackage.workflow.name = Workflow.Name.DEFAULT_OTP
        TestCase.assertValidateError(bamFile, 'workPackage', 'validator.invalid', bamFile.workPackage)
    }

    @Test
    void testConstraints_workflowInConfigAndWorkPackageInconsistent_shouldFail() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
        bamFile.config.workflow = Workflow.build(name: Workflow.Name.DEFAULT_OTP)
        TestCase.assertValidateError(bamFile, 'config', 'validator.invalid', bamFile.config)
    }

    @Test
    void testConstraints_notUniqueIdentifierForWorkPackage_shouldFail() {
        RoddyBamFile bamFile = createRoddyBamFileWithBaseBamFile()
        bamFile.identifier = bamFile.baseBamFile.identifier
        TestCase.assertValidateError(bamFile, 'identifier', 'unique', bamFile.identifier)
    }

    @Test
    void testConstraints_configIsNull_shouldFail() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
        bamFile.config = null
        TestCase.assertAtLeastExpectedValidateError(bamFile, 'config', 'nullable', bamFile.config)
    }

    @Test
    void testConstraints_workPackageIsNull_shouldFail() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
        bamFile.workPackage = null
        TestCase.assertAtLeastExpectedValidateError(bamFile, 'workPackage', 'nullable', bamFile.workPackage)
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_seqTrackDoesNotBelongToBamFileWorkPackage_shouldReturnErrorMessage() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
        SeqTrack seqTrack = bamFile.seqTracks.iterator().next()
        seqTrack.seqType = SeqType.build()
        assert ["seqTrack ${seqTrack} does not satisfy merging criteria for ${bamFile.mergingWorkPackage}"] == bamFile.isConsistentAndContainsNoWithdrawnData()
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_baseBamFileHasDifferentWorkPackageFromBamFile_shouldReturnErrorMessage() {
        RoddyBamFile bamFile = createRoddyBamFileWithBaseBamFile()
        bamFile.baseBamFile.workPackage = MergingWorkPackage.build()
        assert ["the base bam file does not satisfy work package criteria"] == bamFile.isConsistentAndContainsNoWithdrawnData()
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_baseBamFileIsNotFinished_shouldReturnErrorMessage() {
        RoddyBamFile bamFile = createRoddyBamFileWithBaseBamFile()
        bamFile.baseBamFile.md5sum = null
        bamFile.baseBamFile.fileSize = -1
        bamFile.baseBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.DECLARED
        assert ["the base bam file is not finished"] == bamFile.isConsistentAndContainsNoWithdrawnData()
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_withdrawnBamFileWithWithdrawnBaseBamFile_succeeds() {
        RoddyBamFile bamFile = createRoddyBamFileWithBaseBamFile()
        bamFile.withdrawn = true
        bamFile.baseBamFile.withdrawn = true
        assert bamFile.isConsistentAndContainsNoWithdrawnData().empty
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_withdrawnBamFileWithNotWithdrawnBaseBamFile_succeeds() {
        RoddyBamFile bamFile = createRoddyBamFileWithBaseBamFile()
        bamFile.withdrawn = true
        assert bamFile.isConsistentAndContainsNoWithdrawnData().empty
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_notWithdrawnBamFileWithWithdrawnBaseBamFile_shouldReturnErrorMessage() {
        RoddyBamFile bamFile = createRoddyBamFileWithBaseBamFile()
        bamFile.baseBamFile.withdrawn = true
        assert ["base bam file is withdrawn for not withdrawn bam file ${bamFile}"] == bamFile.isConsistentAndContainsNoWithdrawnData()
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_seqTrackIsMergedSecondTime_shouldReturnErrorMessage() {
        RoddyBamFile bamFile = createRoddyBamFileWithBaseBamFile()
        bamFile.seqTracks.addAll(bamFile.baseBamFile.seqTracks)
        assert bamFile.isConsistentAndContainsNoWithdrawnData().first()?.startsWith("the same seqTrack is going to be merged for the second time")
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_withdrawnBamFileWithWithdrawnSeqTracks_succeeds() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([withdrawn: true])
        List<DataFile> dataFiles = DataFile.findAll()
        dataFiles*.fileWithdrawn = true
        dataFiles*.save(flush: true)
        assert [] == bamFile.isConsistentAndContainsNoWithdrawnData()
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_withdrawnBamFileWithNotWithdrawnSeqTracks_succeeds() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([withdrawn: true])
        assert [] == bamFile.isConsistentAndContainsNoWithdrawnData()
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_notWithdrawnBamFileWithWithdrawnSeqTracks_shouldReturnErrorMessage() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
        List<DataFile> dataFiles = DataFile.findAll()
        dataFiles*.fileWithdrawn = true
        dataFiles*.save(flush: true)
        assert ["not withdrawn bam file has withdrawn seq tracks"] == bamFile.isConsistentAndContainsNoWithdrawnData()
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_numberOfMergedLanesNotEqualToNumberOfContainedLanes_shouldReturnErrorMessage() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
        bamFile.numberOfMergedLanes = 5
        assert ["total number of merged lanes is not equal to number of contained seq tracks: 5 vs 1"] == bamFile.isConsistentAndContainsNoWithdrawnData()
    }



    @Test
    void testGetContainedSeqTracks() {
        RoddyBamFile bamFile = createRoddyBamFileWithBaseBamFile()
        assert bamFile.containedSeqTracks == [bamFile.seqTracks, bamFile.baseBamFile.seqTracks].flatten() as Set
        assert bamFile.baseBamFile.containedSeqTracks == bamFile.baseBamFile.seqTracks
    }

    @Test
    void testIsMostRecentBamFile() {
        RoddyBamFile bamFile = createRoddyBamFileWithBaseBamFile()
        assert bamFile.isMostRecentBamFile()
        assert !bamFile.baseBamFile.isMostRecentBamFile()
    }

    @Test
    void testMaxIdentifier_noRoddyBamFileExistsForWorkPackage() {
        MergingWorkPackage workPackage = MergingWorkPackage.build()
        assert null == RoddyBamFile.maxIdentifier(workPackage)
    }

    @Test
    void testMaxIdentifier_roddyBamFileExistsForWorkPackage() {
        RoddyBamFile bamFile = createRoddyBamFileWithBaseBamFile()
        assert 1 == RoddyBamFile.maxIdentifier(bamFile.workPackage)
    }

    private RoddyBamFile createRoddyBamFileWithBaseBamFile() {
        return DomainFactory.createRoddyBamFile(
            DomainFactory.createRoddyBamFile(),
            [md5sum: null,
             fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
             fileSize: -1,
            ]
        )
    }


    @Test
    void testMakeWithdrawn_singleFile_ShouldSetToWithdrawn() {
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        assert !roddyBamFile.withdrawn

        LogThreadLocal.withThreadLog(System.out) {
            roddyBamFile.makeWithdrawn()
        }
        assert roddyBamFile.withdrawn
    }

    @Test
    void testMakeWithdrawn_fileHierarchy_StartWithFile2_ShouldSetFile2And3ToWithdrawn() {
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        RoddyBamFile roddyBamFile3 = DomainFactory.createRoddyBamFile(roddyBamFile2)

        LogThreadLocal.withThreadLog(System.out) {
            roddyBamFile2.makeWithdrawn()
        }
        assert !roddyBamFile.withdrawn
        assert roddyBamFile2.withdrawn
        assert roddyBamFile3.withdrawn
    }

    @Test
    void testMakeWithdrawn_singleFileWithSnv_ShouldSnvSetToWithdrawn() {
        SnvJobResult snvJobResult = DomainFactory.createSnvJobResultWithRoddyBamFiles()
        assert !snvJobResult.sampleType1BamFile.withdrawn

        LogThreadLocal.withThreadLog(System.out) {
            snvJobResult.sampleType1BamFile.makeWithdrawn()
        }
        assert snvJobResult.sampleType1BamFile.withdrawn
    }


    @Test
    void testGetOverallQualityAssessment() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
        QualityAssessmentMergedPass qaPass = QualityAssessmentMergedPass.build(
                processedMergedBamFile: bamFile,
        )
        RoddyMergedBamQa.build(
                qualityAssessmentMergedPass: qaPass,
                chromosome: '12',
        )
        RoddyMergedBamQa mergedQa = RoddyMergedBamQa.build(
                ARBITRARY_QA_VALUES + [
                qualityAssessmentMergedPass: qaPass,
                chromosome: RoddyQualityAssessment.ALL,
                insertSizeCV: 123,
                percentageMatesOnDifferentChr: 0.123,
        ])
        assert mergedQa == bamFile.overallQualityAssessment
    }
}
