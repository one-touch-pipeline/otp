/*
 * Copyright 2011-2023 The OTP authors
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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.junit.*
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

@Rollback
@Integration
class RoddyBamFileIntegrationTests implements RoddyPancanFactory {

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
        RoddyBamFile bamFile = createRBF()
        assert bamFile.save(flush: true)
    }

    @Test
    void test_CheckThatSeqtracksConnectionIsSavedToDatabase() {
        createBamFile()

        assert RoddyBamFile.withCriteria {
            seqTracks {
                isNotNull('id')
            }
        }
    }

    @Test
    void testConstraints_noSeqTracks_shouldFail() {
        RoddyBamFile bamFile = createBamFile()
        bamFile.seqTracks = [] as Set
        TestCase.assertAtLeastExpectedValidateError(bamFile, 'seqTracks', 'minSize.notmet', bamFile.seqTracks)
    }

    @Test
    void testConstraints_notRoddyPipelineName_shouldFail() {
        RoddyBamFile bamFile = createBamFile()
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
        RoddyBamFile bamFile = createBamFile()
        bamFile.config.pipeline = DomainFactory.createDefaultOtpPipeline()
        TestCase.assertValidateError(bamFile, 'config', 'validator.invalid', bamFile.config)
    }

    @Test
    void testConstraints_notUniqueIdentifierForWorkPackage_shouldFail() {
        RoddyBamFile bamFile = createRBF()
        RoddyBamFile bamFile2 = createBamFile(workPackage: bamFile.workPackage)
        bamFile.identifier = bamFile2.identifier
        TestCase.assertValidateError(bamFile, 'identifier', 'validator.invalid', bamFile.identifier)
    }

    @Test
    void testConstraints_workPackageIsNull_shouldFail() {
        RoddyBamFile bamFile = createBamFile()
        bamFile.workPackage = null
        TestCase.assertAtLeastExpectedValidateError(bamFile, 'workPackage', 'nullable', bamFile.workPackage)
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_seqTrackDoesNotBelongToBamFileWorkPackage_isAlsoValid() {
        RoddyBamFile bamFile = createBamFile()
        SeqTrack seqTrack = bamFile.seqTracks.iterator().next()
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        seqTrack.seqType = DomainFactory.createSeqType()
        assert bamFile.isConsistentAndContainsNoWithdrawnData().empty
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_withdrawnBamFile_succeeds() {
        RoddyBamFile bamFile = createRBF()
        bamFile.withdrawn = true
        assert bamFile.isConsistentAndContainsNoWithdrawnData().empty
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_withdrawnBamFileWithWithdrawnSeqTracks_succeeds() {
        RoddyBamFile bamFile = createBamFile([withdrawn: true])
        List<RawSequenceFile> rawSequenceFiles = RawSequenceFile.findAll()
        rawSequenceFiles*.fileWithdrawn = true
        rawSequenceFiles*.save(flush: true)
        assert [] == bamFile.isConsistentAndContainsNoWithdrawnData()
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_withdrawnBamFileWithNotWithdrawnSeqTracks_succeeds() {
        RoddyBamFile bamFile = createBamFile([withdrawn: true])
        assert [] == bamFile.isConsistentAndContainsNoWithdrawnData()
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_notWithdrawnBamFileWithWithdrawnSeqTracks_shouldReturnErrorMessage() {
        RoddyBamFile bamFile = createBamFile()
        List<RawSequenceFile> rawSequenceFiles = RawSequenceFile.findAll()
        rawSequenceFiles*.fileWithdrawn = true
        rawSequenceFiles*.save(flush: true)
        assert ["not withdrawn bam file has withdrawn seq tracks"] == bamFile.isConsistentAndContainsNoWithdrawnData()
    }

    @Test
    void testIsConsistentAndContainsNoWithdrawnData_numberOfMergedLanesNotEqualToNumberOfContainedLanes_shouldReturnErrorMessage() {
        RoddyBamFile bamFile = createBamFile()
        bamFile.numberOfMergedLanes = 5
        assert ["total number of merged lanes is not equal to number of contained seq tracks: 5 vs 1"] == bamFile.isConsistentAndContainsNoWithdrawnData()
    }

    @Test
    void testIsMostRecentBamFile() {
        RoddyBamFile bamFile = createRBF()
        assert bamFile.isMostRecentBamFile()
    }

    @Test
    void testMaxIdentifier_noRoddyBamFileExistsForWorkPackage() {
        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage()
        assert RoddyBamFile.maxIdentifier(workPackage) == null
    }

    @Test
    void testMaxIdentifier_roddyBamFileExistsForWorkPackage() {
        RoddyBamFile bamFile = createRBF()
        assert RoddyBamFile.maxIdentifier(bamFile.workPackage) == 0
    }

    private RoddyBamFile createRBF() {
        return createBamFile([
                md5sum: null,
                fileOperationStatus: AbstractBamFile.FileOperationStatus.DECLARED,
                fileSize: -1,
        ])
    }

    @Test
    void testWithdraw_singleFile_ShouldSetToWithdrawn() {
        RoddyBamFile roddyBamFile = createBamFile()
        assert !roddyBamFile.withdrawn

        LogThreadLocal.withThreadLog(System.out) {
            roddyBamFile.withdraw()
        }
        assert roddyBamFile.withdrawn
    }
}
