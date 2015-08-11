package de.dkfz.tbi.otp.dataprocessing

import org.springframework.security.access.prepost.PreAuthorize
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType

class OverallQualityAssessmentMergedService {

    Double calcCoverageWithoutN(OverallQualityAssessmentMerged overallQualityAssessmentMerged, ReferenceGenome referenceGenome) {
        long qcBasesMapped = overallQualityAssessmentMerged.qcBasesMapped
        long referenceGenomeLengthWithoutN = referenceGenome.lengthWithoutN
        double coverageWithoutN = qcBasesMapped / referenceGenomeLengthWithoutN
        return coverageWithoutN
    }



    @PreAuthorize("hasPermission(#project, read) or hasRole('ROLE_OPERATOR')")
    List<AbstractQualityAssessment> findAllByProjectAndSeqType(Project project, SeqType seqType) {

        String maxQualityAssessmentMergedPassIdentifier = """
select
    max(identifier)
from
    QualityAssessmentMergedPass qualityAssessmentMergedPass
where
    qualityAssessmentMergedPass.processedMergedBamFile = abstractMergedBamFile
"""

        String HQL = """
            select
                abstractQualityAssessment
            from
                AbstractQualityAssessment abstractQualityAssessment
                join abstractQualityAssessment.qualityAssessmentMergedPass qualityAssessmentMergedPass
                join qualityAssessmentMergedPass.processedMergedBamFile abstractMergedBamFile
                join abstractMergedBamFile.workPackage mergingWorkPackage
            where
                mergingWorkPackage.sample.individual.project = :project
                and mergingWorkPackage.seqType = :seqType
                and qualityAssessmentMergedPass.identifier = ( ${maxQualityAssessmentMergedPassIdentifier})
                and mergingWorkPackage.bamFileInProjectFolder = abstractMergedBamFile
                and abstractMergedBamFile.fileOperationStatus = :fileOperationStatus
                and abstractMergedBamFile.qualityAssessmentStatus = :qualityAssessmentStatus
                and (
                    abstractQualityAssessment.class = :overallQualityAssessmentMergedClass
                    or (
                        abstractQualityAssessment.class = :roddyMergedBamQaClass
                        and abstractQualityAssessment.chromosome = :allChromosomes
                    )
                )
        """
        Map parameters = [
            project: project,
            seqType: seqType,
            fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            overallQualityAssessmentMergedClass: OverallQualityAssessmentMerged.getName(),
            roddyMergedBamQaClass: RoddyMergedBamQa.getName(),
            allChromosomes: RoddyQualityAssessment.ALL,
        ]

        List<AbstractQualityAssessment> qas = AbstractQualityAssessment.executeQuery(HQL, parameters, [readOnly: true])
        return qas
    }


    List findSequenceLengthAndReferenceGenomeLengthWithoutNForQualityAssessmentMerged(List<AbstractQualityAssessment> abstractQualityAssessments) {
        if (!abstractQualityAssessments) {
            return []
        }

        /*
            This method assumes:
            It does not matter which seqTrack is used to get the sequencedLength. Within one merged bam file all are the same.
            This is incorrect, see OTP-1670
         */

        final String HQL = '''
            select distinct
                abstractQualityAssessment.id,
                fastqcBasicStatistics.sequenceLength,
                abstractQualityAssessment.qualityAssessmentMergedPass.processedMergedBamFile.workPackage.referenceGenome.lengthWithoutN
            from
                AbstractQualityAssessment abstractQualityAssessment,
                FastqcBasicStatistics fastqcBasicStatistics,
                SeqTrack seqTrack,
                DataFile dataFile
            where
                abstractQualityAssessment.qualityAssessmentMergedPass.processedMergedBamFile.workPackage.sample = seqTrack.sample
                and abstractQualityAssessment.qualityAssessmentMergedPass.processedMergedBamFile.workPackage.seqType = seqTrack.seqType
                and seqTrack = fastqcBasicStatistics.fastqcProcessedFile.dataFile.seqTrack
                and dataFile.seqTrack = seqTrack
                and dataFile.fileWithdrawn = false
                and abstractQualityAssessment.id in :abstractQualityAssessmentIds
        '''
        Map parameters = [
                abstractQualityAssessmentIds: abstractQualityAssessments*.id,
        ]

        List result = AbstractQualityAssessment.executeQuery(HQL, parameters, [readOnly: true])
        return result
    }

}
