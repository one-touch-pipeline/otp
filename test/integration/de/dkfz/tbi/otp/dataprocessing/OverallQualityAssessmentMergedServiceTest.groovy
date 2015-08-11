package de.dkfz.tbi.otp.dataprocessing

import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.acls.domain.BasePermission

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsqc.FastqcBasicStatistics
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest

class OverallQualityAssessmentMergedServiceTest extends AbstractIntegrationTest {

    final static String SEQUENCE_LENGTH = '100'

    final static long LENGTH_WITHOUT_N = 222

    OverallQualityAssessmentMergedService overallQualityAssessmentMergedService

    OverallQualityAssessmentMerged overallQualityAssessmentMerged



    void setUp() {
        createUserAndRoles()

        overallQualityAssessmentMerged = OverallQualityAssessmentMerged.build()
        AbstractBamFile abstractBamFile = overallQualityAssessmentMerged.processedMergedBamFile
        abstractBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        abstractBamFile.md5sum = "12345678901234567890123456789012"
        abstractBamFile.fileSize = 10000
        abstractBamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.FINISHED
        abstractBamFile.withdrawn = false
        overallQualityAssessmentMerged.referenceGenome.lengthWithoutN = LENGTH_WITHOUT_N
        abstractBamFile.workPackage.bamFileInProjectFolder = abstractBamFile

    }



    private void prepareFindSequenceLengthAndReferenceGenomeLengthWithoutNForOverallQualityAssessmentMerged() {
        ProcessedBamFile processedBamFile = DomainFactory.assignNewProcessedBamFile(overallQualityAssessmentMerged.mergingSet)
        [1..3].each {
            FastqcBasicStatistics fastqcBasicStatistics = FastqcBasicStatistics.build(sequenceLength: SEQUENCE_LENGTH)
            fastqcBasicStatistics.fastqcProcessedFile.dataFile.seqTrack = processedBamFile.seqTrack
        }
    }



    void testFindAllByProjectAndSeqType_admin() {
        List expected = [
            overallQualityAssessmentMerged
        ]

        SpringSecurityUtils.doWithAuth("admin") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, overallQualityAssessmentMerged.seqType)
            assert expected == result
        }
    }

    void testFindAllByProjectAndSeqType_operator() {
        List expected = [
            overallQualityAssessmentMerged
        ]

        SpringSecurityUtils.doWithAuth("operator") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, overallQualityAssessmentMerged.seqType)
            assert expected == result
        }
    }

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

    void testFindAllByProjectAndSeqType_userWithoutAccess() {

        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, overallQualityAssessmentMerged.seqType)
            }
        }
    }

    void testFindAllByProjectAndSeqType_wrongProject() {
        List expected = []

        SpringSecurityUtils.doWithAuth("admin") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(Project.build(), overallQualityAssessmentMerged.seqType)
            assert expected == result
        }
    }

    void testFindAllByProjectAndSeqType_wrongSeqType() {
        List expected = []

        SpringSecurityUtils.doWithAuth("admin") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, SeqType.build())
            assert expected == result
        }
    }

    void testFindAllByProjectAndSeqType_notLastQaMergedPassIdentifier() {
        List expected = []
        QualityAssessmentMergedPass.build(processedMergedBamFile: overallQualityAssessmentMerged.processedMergedBamFile, identifier: overallQualityAssessmentMerged.qualityAssessmentMergedPass.identifier + 1)

        SpringSecurityUtils.doWithAuth("admin") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, SeqType.build())
            assert expected == result
        }
    }

    void testFindAllByProjectAndSeqType_notLastMergingPassIdentifier() {
        List expected = []
        MergingPass.build(mergingSet: overallQualityAssessmentMerged.mergingSet, identifier: overallQualityAssessmentMerged.mergingPass.identifier + 1)

        SpringSecurityUtils.doWithAuth("admin") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, SeqType.build())
            assert expected == result
        }
    }

    void testFindAllByProjectAndSeqType_notLastMergingSetIdentifier() {
        List expected = []
        MergingSet.build(mergingWorkPackage: overallQualityAssessmentMerged.mergingWorkPackage, identifier: overallQualityAssessmentMerged.mergingSet.identifier + 1)

        SpringSecurityUtils.doWithAuth("admin") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, SeqType.build())
            assert expected == result
        }
    }

    void testFindAllByProjectAndSeqType_wrongFileOperationStatus() {
        List expected = []
        overallQualityAssessmentMerged.processedMergedBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.INPROGRESS
        overallQualityAssessmentMerged.processedMergedBamFile.md5sum = null

        SpringSecurityUtils.doWithAuth("admin") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, SeqType.build())
            assert expected == result
        }
    }

    void testFindAllByProjectAndSeqType_BamFileIsWithdrawn() {
        List expected = []
        overallQualityAssessmentMerged.processedMergedBamFile.withdrawn = true

        SpringSecurityUtils.doWithAuth("admin") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, SeqType.build())
            assert expected == result
        }
    }

    void testFindAllByProjectAndSeqType_wrongQualityAssessmentStatus() {
        List expected = []
        overallQualityAssessmentMerged.processedMergedBamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.IN_PROGRESS

        SpringSecurityUtils.doWithAuth("admin") {
            List<OverallQualityAssessmentMerged> result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(overallQualityAssessmentMerged.project, SeqType.build())
            assert expected == result
        }
    }



    void testFindSequenceLengthAndReferenceGenomeLengthWithoutNForQualityAssessmentMerged() {
        prepareFindSequenceLengthAndReferenceGenomeLengthWithoutNForOverallQualityAssessmentMerged()
        List expected = [
            [
                overallQualityAssessmentMerged.id,
                SEQUENCE_LENGTH,
                LENGTH_WITHOUT_N,
            ]
        ]

        List result = overallQualityAssessmentMergedService.findSequenceLengthAndReferenceGenomeLengthWithoutNForQualityAssessmentMerged([
            overallQualityAssessmentMerged
        ])
        assert expected == result
    }

    void testFindSequenceLengthAndReferenceGenomeLengthWithoutNForQualityAssessmentMerged_listIsEmpty() {
        //these objects are created to ensure, the database is not empty
        prepareFindSequenceLengthAndReferenceGenomeLengthWithoutNForOverallQualityAssessmentMerged()
        List expected = []

        List result = overallQualityAssessmentMergedService.findSequenceLengthAndReferenceGenomeLengthWithoutNForQualityAssessmentMerged([])
        assert expected == result
    }

    void testFindSequenceLengthAndReferenceGenomeLengthWithoutNForQualityAssessmentMerged_listIsNull() {
        //these objects are created to ensure, the database is not empty
        prepareFindSequenceLengthAndReferenceGenomeLengthWithoutNForOverallQualityAssessmentMerged()
        List expected = []

        List result = overallQualityAssessmentMergedService.findSequenceLengthAndReferenceGenomeLengthWithoutNForQualityAssessmentMerged(null)
        assert expected == result
    }

    void testFindSequenceLengthAndReferenceGenomeLengthWithoutNForQualityAssessmentMerged_noFastqcAvailable() {
        List expected = []

        List result = overallQualityAssessmentMergedService.findSequenceLengthAndReferenceGenomeLengthWithoutNForQualityAssessmentMerged([
            overallQualityAssessmentMerged
        ])
        assert expected == result
    }
}
