package de.dkfz.tbi.otp.dataprocessing

import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.ngsdata.*

class OverallQualityAssessmentMergedService {

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
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
                and abstractMergedBamFile.withdrawn = false
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

    List<ReferenceGenomeEntry> findChromosomeLengthForQualityAssessmentMerged(
            List<String> chromosomeAliases, List<AbstractQualityAssessment> abstractQualityAssessments) {
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

