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

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Before
import org.junit.Test
import org.springframework.security.access.AccessDeniedException

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles

@Rollback
@Integration
class OverallQualityAssessmentMergedServiceTest implements UserAndRoles {

    final static String SEQUENCE_LENGTH = "100"

    OverallQualityAssessmentMergedService overallQualityAssessmentMergedService

    OverallQualityAssessmentMerged overallQualityAssessmentMerged


    void setupData() {
        createUserAndRoles()

        overallQualityAssessmentMerged = DomainFactory.createOverallQualityAssessmentMerged()
        AbstractBamFile abstractBamFile = overallQualityAssessmentMerged.processedMergedBamFile
        abstractBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        abstractBamFile.md5sum = "12345678901234567890123456789012"
        abstractBamFile.fileSize = 10000
        abstractBamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.FINISHED
        abstractBamFile.withdrawn = false
        abstractBamFile.workPackage.bamFileInProjectFolder = abstractBamFile
        overallQualityAssessmentMerged.save(flush: true)
    }


    private void prepareFindSequenceLengthForOverallQualityAssessmentMerged() {
        ProcessedBamFile processedBamFile = DomainFactory.assignNewProcessedBamFile(overallQualityAssessmentMerged.mergingSet)
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(processedBamFile.seqTrack)
        dataFiles*.sequenceLength = SEQUENCE_LENGTH
        dataFiles*.save(flush:true, failOnError: true)
    }


    @Test
    void testFindAllByProjectAndSeqType_admin() {
        setupData()
        List expected = [
            overallQualityAssessmentMerged,
        ]

        SpringSecurityUtils.doWithAuth(ADMIN) {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, overallQualityAssessmentMerged.seqType)
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_operator() {
        setupData()
        List expected = [
            overallQualityAssessmentMerged,
        ]

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, overallQualityAssessmentMerged.seqType)
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_userWithAccess() {
        setupData()
        List expected = [
            overallQualityAssessmentMerged,
        ]
        addUserWithReadAccessToProject(User.findByUsername(TESTUSER), overallQualityAssessmentMerged.project)
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, overallQualityAssessmentMerged.seqType)
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_userWithoutAccess() {
        setupData()
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
                overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, overallQualityAssessmentMerged.seqType)
            }
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_wrongProject() {
        setupData()
        List expected = []

        SpringSecurityUtils.doWithAuth(ADMIN) {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(DomainFactory.createProject(), overallQualityAssessmentMerged.seqType)
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_wrongSeqType() {
        setupData()
        List expected = []

        SpringSecurityUtils.doWithAuth(ADMIN) {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, DomainFactory.createSeqType())
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_notLastQaMergedPassIdentifier() {
        setupData()
        List expected = []
        DomainFactory.createQualityAssessmentMergedPass(abstractMergedBamFile: overallQualityAssessmentMerged.processedMergedBamFile, identifier: overallQualityAssessmentMerged.qualityAssessmentMergedPass.identifier + 1)

        SpringSecurityUtils.doWithAuth(ADMIN) {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, DomainFactory.createSeqType())
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_notLastMergingPassIdentifier() {
        setupData()
        List expected = []
        DomainFactory.createMergingSet(mergingSet: overallQualityAssessmentMerged.mergingSet, identifier: overallQualityAssessmentMerged.mergingPass.identifier + 1)

        SpringSecurityUtils.doWithAuth(ADMIN) {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, DomainFactory.createSeqType())
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_notLastMergingSetIdentifier() {
        setupData()
        List expected = []
        DomainFactory.createMergingSet(mergingWorkPackage: overallQualityAssessmentMerged.mergingWorkPackage, identifier: overallQualityAssessmentMerged.mergingSet.identifier + 1)

        SpringSecurityUtils.doWithAuth(ADMIN) {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, DomainFactory.createSeqType())
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_wrongFileOperationStatus() {
        setupData()
        List expected = []
        overallQualityAssessmentMerged.processedMergedBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.INPROGRESS
        overallQualityAssessmentMerged.processedMergedBamFile.md5sum = null

        SpringSecurityUtils.doWithAuth(ADMIN) {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, DomainFactory.createSeqType())
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_BamFileIsWithdrawn() {
        setupData()
        List expected = []
        overallQualityAssessmentMerged.processedMergedBamFile.withdrawn = true

        SpringSecurityUtils.doWithAuth(ADMIN) {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, DomainFactory.createSeqType())
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_wrongQualityAssessmentStatus() {
        setupData()
        List expected = []
        overallQualityAssessmentMerged.processedMergedBamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.IN_PROGRESS

        SpringSecurityUtils.doWithAuth(ADMIN) {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, DomainFactory.createSeqType())
            assert expected == result
        }
    }
}
