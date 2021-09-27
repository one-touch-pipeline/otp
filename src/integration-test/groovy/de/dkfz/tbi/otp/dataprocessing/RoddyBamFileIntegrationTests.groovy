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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import grails.validation.ValidationException
import org.junit.*
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

@Rollback
@Integration
class RoddyBamFileIntegrationTests {

    @Before
    void setup() {
        SessionUtils.metaClass.static.withNewSession = { Closure c -> c() }
    }

    @After
    void cleanup() {
        TestCase.removeMetaClass(SessionUtils)
    }

    @Test
    void testConstraints_allFine() {
        RoddyBamFile bamFile = createRoddyBamFileWithBaseBamFile()
        assert bamFile.save()
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
    void testConstraints_notRoddyPipelineName_shouldFail() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
        bamFile.workPackage.pipeline.name = Pipeline.Name.DEFAULT_OTP
        bamFile.config.pipeline.name = Pipeline.Name.DEFAULT_OTP

        assert !bamFile.validate()
        Errors errors = bamFile.errors
        assert errors.errorCount == 2
        assert errors.fieldErrorCount == 2
        List<FieldError> fieldErrors = errors.fieldErrors

        assert fieldErrors*.field == ['config.pipeline', 'workPackage']
        assert fieldErrors*.rejectedValue == [bamFile.config.pipeline, bamFile.workPackage]
    }

    @Test
    void testConstraints_pipelineInConfigAndWorkPackageInconsistent_shouldFail() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
        bamFile.config.pipeline = DomainFactory.createDefaultOtpPipeline()
        TestCase.assertValidateError(bamFile, 'config', 'validator.invalid', bamFile.config)
    }

    @Test
    void testConstraints_notUniqueIdentifierForWorkPackage_shouldFail() {
        RoddyBamFile bamFile = createRoddyBamFileWithBaseBamFile()
        bamFile.identifier = bamFile.baseBamFile.identifier
        TestCase.assertValidateError(bamFile, 'identifier', 'validator.invalid', bamFile.identifier)
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
    void testIsConsistentAndContainsNoWithdrawnData_seqTrackDoesNotBelongToBamFileWorkPackage_isAlsoValid() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
        SeqTrack seqTrack = bamFile.seqTracks.iterator().next()
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        seqTrack.seqType = DomainFactory.createSeqType()
        bamFile.isConsistentAndContainsNoWithdrawnData()
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_baseBamFileHasDifferentWorkPackageFromBamFile_shouldReturnErrorMessage() {
        RoddyBamFile bamFile = createRoddyBamFileWithBaseBamFile()
        bamFile.baseBamFile.workPackage = DomainFactory.createMergingWorkPackage()
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
        assert ["base bam file is withdrawn for not withdrawn bam file ${bamFile}" as String] == bamFile.isConsistentAndContainsNoWithdrawnData()
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_seqTrackIsMergedSecondTime_shouldReturnErrorMessage() {
        RoddyBamFile bamFile = createRoddyBamFileWithBaseBamFile()
        bamFile.seqTracks.addAll(bamFile.baseBamFile.seqTracks)
        TestCase.shouldFailWithMessageContaining(ValidationException, "the same seqTrack is going to be merged for the second time") {
            bamFile.save()
        }
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_withdrawnBamFileWithWithdrawnSeqTracks_succeeds() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([withdrawn: true])
        List<DataFile> dataFiles = DataFile.findAll()
        dataFiles*.fileWithdrawn = true
        dataFiles*.save()
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
        dataFiles*.save()
        assert ["not withdrawn bam file has withdrawn seq tracks"] == bamFile.isConsistentAndContainsNoWithdrawnData()
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_numberOfMergedLanesNotEqualToNumberOfContainedLanes_shouldReturnErrorMessage() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
        bamFile.numberOfMergedLanes = 5
        TestCase.shouldFailWithMessageContaining(ValidationException, "total number of merged lanes is not equal to number of contained seq tracks: 5 vs 1") {
            bamFile.save()
        }
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
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage()
        assert null == RoddyBamFile.maxIdentifier(workPackage)
    }

    @Test
    void testMaxIdentifier_roddyBamFileExistsForWorkPackage() {
        RoddyBamFile bamFile = createRoddyBamFileWithBaseBamFile()
        assert 1 == RoddyBamFile.maxIdentifier(bamFile.workPackage)
    }

    private RoddyBamFile createRoddyBamFileWithBaseBamFile() {
        return DomainFactory.createRoddyBamFile(DomainFactory.createRoddyBamFile(), [
                md5sum: null,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
                fileSize: -1,
            ]
        )
    }

    @Test
    void testWithdraw_singleFile_ShouldSetToWithdrawn() {
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        assert !roddyBamFile.withdrawn

        LogThreadLocal.withThreadLog(System.out) {
            roddyBamFile.withdraw()
        }
        assert roddyBamFile.withdrawn
    }

    @Test
    void testWithdraw_fileHierarchy_StartWithFile2_ShouldSetFile2And3ToWithdrawn() {
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        RoddyBamFile roddyBamFile3 = DomainFactory.createRoddyBamFile(roddyBamFile2)

        LogThreadLocal.withThreadLog(System.out) {
            roddyBamFile2.withdraw()
        }
        assert !roddyBamFile.withdrawn
        assert roddyBamFile2.withdrawn
        assert roddyBamFile3.withdrawn
    }
}
