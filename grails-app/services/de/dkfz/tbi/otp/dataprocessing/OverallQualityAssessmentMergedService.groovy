package de.dkfz.tbi.otp.dataprocessing

import org.springframework.security.access.prepost.PreAuthorize
import de.dkfz.tbi.otp.ngsdata.*

class OverallQualityAssessmentMergedService {

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, read)")
    List<AbstractQualityAssessment> findAllByProjectAndSeqType(Project project, SeqType seqType) {

        String maxQualityAssessmentMergedPassIdentifier = """
select
    max(identifier)
from
    QualityAssessmentMergedPass qualityAssessmentMergedPass
where
    qualityAssessmentMergedPass.abstractMergedBamFile = abstractMergedBamFile
"""

        String HQL = """
            select
                abstractQualityAssessment
            from
                AbstractQualityAssessment abstractQualityAssessment
                join abstractQualityAssessment.qualityAssessmentMergedPass qualityAssessmentMergedPass
                join qualityAssessmentMergedPass.abstractMergedBamFile abstractMergedBamFile
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
                        abstractQualityAssessment.class in (:roddyMergedBamQaClass)
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
            roddyMergedBamQaClass: [RoddyMergedBamQa.getName(), RnaQualityAssessment.getName()],
            allChromosomes: RoddyQualityAssessment.ALL,
        ]

        List<AbstractQualityAssessment> qas = AbstractQualityAssessment.executeQuery(HQL, parameters, [readOnly: true])
        return qas
    }


    List findSequenceLengthForQualityAssessmentMerged(List<AbstractQualityAssessment> abstractQualityAssessments) {
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
                dataFile.sequenceLength
            from
                AbstractQualityAssessment abstractQualityAssessment,
                SeqTrack seqTrack,
                DataFile dataFile
            where
                abstractQualityAssessment.qualityAssessmentMergedPass.abstractMergedBamFile.workPackage.sample = seqTrack.sample
                and abstractQualityAssessment.qualityAssessmentMergedPass.abstractMergedBamFile.workPackage.seqType = seqTrack.seqType
                and abstractQualityAssessment.qualityAssessmentMergedPass.abstractMergedBamFile.workPackage.seqPlatformGroup = seqTrack.run.seqPlatform.seqPlatformGroup
                and dataFile.seqTrack = seqTrack
                and abstractQualityAssessment.id in :abstractQualityAssessmentIds
        '''
        Map parameters = [
                abstractQualityAssessmentIds: abstractQualityAssessments*.id,
        ]

        List result = AbstractQualityAssessment.executeQuery(HQL, parameters, [readOnly: true])
        return result
    }

    List<ReferenceGenomeEntry> findChromosomeLengthForQualityAssessmentMerged(List<String> chromosomeAliases, List<AbstractQualityAssessment> abstractQualityAssessments) {
        if (!abstractQualityAssessments) {
            return []
        }
        final String HQL = '''
            select distinct
                referenceGenomeEntry
            from
                AbstractQualityAssessment abstractQualityAssessment,
                ReferenceGenomeEntry referenceGenomeEntry
            where
                abstractQualityAssessment.qualityAssessmentMergedPass.abstractMergedBamFile.workPackage.referenceGenome = referenceGenomeEntry.referenceGenome
                and abstractQualityAssessment.id in :abstractQualityAssessmentIds
                and referenceGenomeEntry.alias in :chromosomeAliases
        '''
        Map parameters = [
                abstractQualityAssessmentIds: abstractQualityAssessments*.id,
                chromosomeAliases: chromosomeAliases,
        ]

        List<ReferenceGenomeEntry> result = ReferenceGenomeEntry.executeQuery(HQL, parameters, [readOnly: true])
        return result
    }
}

