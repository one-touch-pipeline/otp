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

import grails.gorm.transactions.Transactional
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerQualityAssessment
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

@Transactional
class OverallQualityAssessmentMergedService {

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    List<AbstractQualityAssessment> findAllByProjectAndSeqType(Project project, SeqType seqType, Sample sample = null) {
        String maxQualityAssessmentMergedPassIdentifier = """
select
    max(identifier)
from
    QualityAssessmentMergedPass qualityAssessmentMergedPass
where
    qualityAssessmentMergedPass.abstractMergedBamFile = abstractMergedBamFile
"""

        String selectSample = sample ? "and mergingWorkPackage.sample = :sample" : ""

        final String HQL = """
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
                ${selectSample}
                and qualityAssessmentMergedPass.identifier = ( ${maxQualityAssessmentMergedPassIdentifier})
                and mergingWorkPackage.bamFileInProjectFolder = abstractMergedBamFile
                and abstractMergedBamFile.fileOperationStatus = :fileOperationStatus
                and abstractMergedBamFile.qualityAssessmentStatus = :qualityAssessmentStatus
                and abstractMergedBamFile.withdrawn = false
                and (
                    abstractQualityAssessment.class in (
                        :overallQualityAssessmentMergedClass,
                        :cellRangerQualityAssessmentClass
                    ) or (
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
            overallQualityAssessmentMergedClass: OverallQualityAssessmentMerged.name,
            cellRangerQualityAssessmentClass: CellRangerQualityAssessment.name,
            roddyMergedBamQaClass: [RoddyMergedBamQa.name, RnaQualityAssessment.name],
            allChromosomes: RoddyQualityAssessment.ALL,
        ]
        if (sample) {
            parameters.put("sample", sample)
        }

        List<AbstractQualityAssessment> qas = AbstractQualityAssessment.executeQuery(HQL.toString(), parameters, [readOnly: true])
        return qas
    }

    List<ReferenceGenomeEntry> findChromosomeLengthForQualityAssessmentMerged(
            List<String> chromosomeAliases, List<AbstractQualityAssessment> abstractQualityAssessments) {
        if (!abstractQualityAssessments) {
            return []
        }
        final String HQL = """
            select distinct
                referenceGenomeEntry
            from
                AbstractQualityAssessment abstractQualityAssessment,
                ReferenceGenomeEntry referenceGenomeEntry
            where
                abstractQualityAssessment.qualityAssessmentMergedPass.abstractMergedBamFile.workPackage.referenceGenome = referenceGenomeEntry.referenceGenome
                and abstractQualityAssessment.id in :abstractQualityAssessmentIds
                and referenceGenomeEntry.alias in :chromosomeAliases
        """
        Map parameters = [
                abstractQualityAssessmentIds: abstractQualityAssessments*.id,
                chromosomeAliases: chromosomeAliases,
        ]

        List<ReferenceGenomeEntry> result = ReferenceGenomeEntry.executeQuery(HQL.toString(), parameters, [readOnly: true])
        return result
    }
}

