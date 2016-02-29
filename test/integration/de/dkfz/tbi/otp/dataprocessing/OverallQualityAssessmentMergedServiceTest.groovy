package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest
import grails.plugin.springsecurity.SpringSecurityUtils
import org.junit.Before
import org.junit.Test
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.acls.domain.BasePermission

class OverallQualityAssessmentMergedServiceTest extends AbstractIntegrationTest {

    final static String SEQUENCE_LENGTH = "100"

    OverallQualityAssessmentMergedService overallQualityAssessmentMergedService

    OverallQualityAssessmentMerged overallQualityAssessmentMerged



    @Before
    void setUp() {
        createUserAndRoles()

        overallQualityAssessmentMerged = OverallQualityAssessmentMerged.build()
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
        List expected = [
            overallQualityAssessmentMerged
        ]

        SpringSecurityUtils.doWithAuth("admin") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, overallQualityAssessmentMerged.seqType)
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_operator() {
        List expected = [
            overallQualityAssessmentMerged
        ]

        SpringSecurityUtils.doWithAuth("operator") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, overallQualityAssessmentMerged.seqType)
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_userWithAccess() {
        List expected = [
            overallQualityAssessmentMerged
        ]
        SpringSecurityUtils.doWithAuth("admin") {
            aclUtilService.addPermission(overallQualityAssessmentMerged.project, "testuser", BasePermission.READ)
        }

        SpringSecurityUtils.doWithAuth("testuser") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, overallQualityAssessmentMerged.seqType)
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_userWithoutAccess() {

        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, overallQualityAssessmentMerged.seqType)
            }
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_wrongProject() {
        List expected = []

        SpringSecurityUtils.doWithAuth("admin") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(Project.build(), overallQualityAssessmentMerged.seqType)
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_wrongSeqType() {
        List expected = []

        SpringSecurityUtils.doWithAuth("admin") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, SeqType.build())
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_notLastQaMergedPassIdentifier() {
        List expected = []
        QualityAssessmentMergedPass.build(abstractMergedBamFile: overallQualityAssessmentMerged.processedMergedBamFile, identifier: overallQualityAssessmentMerged.qualityAssessmentMergedPass.identifier + 1)

        SpringSecurityUtils.doWithAuth("admin") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, SeqType.build())
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_notLastMergingPassIdentifier() {
        List expected = []
        MergingPass.build(mergingSet: overallQualityAssessmentMerged.mergingSet, identifier: overallQualityAssessmentMerged.mergingPass.identifier + 1)

        SpringSecurityUtils.doWithAuth("admin") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, SeqType.build())
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_notLastMergingSetIdentifier() {
        List expected = []
        MergingSet.build(mergingWorkPackage: overallQualityAssessmentMerged.mergingWorkPackage, identifier: overallQualityAssessmentMerged.mergingSet.identifier + 1)

        SpringSecurityUtils.doWithAuth("admin") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, SeqType.build())
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_wrongFileOperationStatus() {
        List expected = []
        overallQualityAssessmentMerged.processedMergedBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.INPROGRESS
        overallQualityAssessmentMerged.processedMergedBamFile.md5sum = null

        SpringSecurityUtils.doWithAuth("admin") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, SeqType.build())
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_BamFileIsWithdrawn() {
        List expected = []
        overallQualityAssessmentMerged.processedMergedBamFile.withdrawn = true

        SpringSecurityUtils.doWithAuth("admin") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, SeqType.build())
            assert expected == result
        }
    }

    @Test
    void testFindAllByProjectAndSeqType_wrongQualityAssessmentStatus() {
        List expected = []
        overallQualityAssessmentMerged.processedMergedBamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.IN_PROGRESS

        SpringSecurityUtils.doWithAuth("admin") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, SeqType.build())
            assert expected == result
        }
    }



    @Test
    void testFindSequenceLengthForQualityAssessmentMerged() {
        prepareFindSequenceLengthForOverallQualityAssessmentMerged()
        List expected = [
            [
                overallQualityAssessmentMerged.id,
                SEQUENCE_LENGTH,
            ]
        ]

        List result = overallQualityAssessmentMergedService.findSequenceLengthForQualityAssessmentMerged([
            overallQualityAssessmentMerged
        ])

        assert expected == result
    }

    @Test
    void testFindSequenceLengthForQualityAssessmentMerged_listIsEmpty() {
        //these objects are created to ensure, the database is not empty
        prepareFindSequenceLengthForOverallQualityAssessmentMerged()
        List expected = []

        List result = overallQualityAssessmentMergedService.findSequenceLengthForQualityAssessmentMerged([])
        assert expected == result
    }

    @Test
    void testFindSequenceLengthForQualityAssessmentMerged_listIsNull() {
        //these objects are created to ensure, the database is not empty
        prepareFindSequenceLengthForOverallQualityAssessmentMerged()
        List expected = []

        List result = overallQualityAssessmentMergedService.findSequenceLengthForQualityAssessmentMerged(null)
        assert expected == result
    }

    @Test
    void testFindSequenceLengthForQualityAssessmentMerged_noFastqcAvailable() {
        List expected = []

        List result = overallQualityAssessmentMergedService.findSequenceLengthForQualityAssessmentMerged([
            overallQualityAssessmentMerged
        ])
        assert expected == result
    }
}
