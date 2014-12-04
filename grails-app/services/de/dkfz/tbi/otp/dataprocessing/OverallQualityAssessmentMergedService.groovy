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
    List<OverallQualityAssessmentMerged> findAllByProjectAndSeqType(Project project, SeqType seqType) {
        final String HQL = '''
            select
                overallQualityAssessmentMerged
            from OverallQualityAssessmentMerged overallQualityAssessmentMerged
                join overallQualityAssessmentMerged.qualityAssessmentMergedPass qualityAssessmentMergedPass
                join qualityAssessmentMergedPass.processedMergedBamFile processedMergedBamFile
                join processedMergedBamFile.mergingPass mergingPass
                join mergingPass.mergingSet mergingSet
                join mergingSet.mergingWorkPackage mergingWorkPackage
            where
                mergingWorkPackage.sample.individual.project = :project
                and mergingWorkPackage.seqType = :seqType
                and qualityAssessmentMergedPass.identifier = (select max(identifier) from QualityAssessmentMergedPass qualityAssessmentMergedPass2 where qualityAssessmentMergedPass2.processedMergedBamFile = qualityAssessmentMergedPass.processedMergedBamFile)
                and mergingPass.identifier = (select max(identifier) from MergingPass mergingPass2 where mergingPass2.mergingSet = mergingPass.mergingSet)
                and mergingSet.identifier = (select max(identifier) from MergingSet mergingSet2 where mergingSet2.mergingWorkPackage = mergingSet.mergingWorkPackage)
                and processedMergedBamFile.fileOperationStatus = :fileOperationStatus
                and processedMergedBamFile.withdrawn = false
                and processedMergedBamFile.qualityAssessmentStatus = :qualityAssessmentStatus
        '''
        Map parameters = [
            project: project,
            seqType: seqType,
            fileOperationStatus: AbstractBamFile.FileOperationStatus.PROCESSED,
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
        ]

        List<OverallQualityAssessmentMerged> qas = OverallQualityAssessmentMerged.executeQuery(HQL, parameters)
        return qas
    }


    List findSequenceLengthForOverallQualityAssessmentMerged(List<OverallQualityAssessmentMerged> overallQualityAssessmentMergedList) {
        if (!overallQualityAssessmentMergedList) {
            return []
        }
        final String HQL = '''
            select distinct
                overallQualityAssessmentMerged.id, fastqcBasicStatistics.sequenceLength
            from
                OverallQualityAssessmentMerged overallQualityAssessmentMerged,
                MergingSetAssignment mergingSetAssignment,
                FastqcBasicStatistics fastqcBasicStatistics
            where
                overallQualityAssessmentMerged.qualityAssessmentMergedPass.processedMergedBamFile.mergingPass.mergingSet = mergingSetAssignment.mergingSet
                and mergingSetAssignment.bamFile.alignmentPass.seqTrack = fastqcBasicStatistics.fastqcProcessedFile.dataFile.seqTrack
                and overallQualityAssessmentMerged.id in :overallQualityAssessmentMergedIds
        '''
        Map parameters = [
            overallQualityAssessmentMergedIds: overallQualityAssessmentMergedList*.id,
        ]

        List result = OverallQualityAssessmentMerged.executeQuery(HQL, parameters)
        return result
    }

}
