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

import grails.gorm.transactions.Transactional
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerQualityAssessment
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

@Transactional
class QualityAssessmentMergedService {

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    List<AbstractQualityAssessment> findAllByProjectAndSeqType(Project project, SeqType seqType, Sample sample = null) {
        String selectSample = sample ? "and mergingWorkPackage.sample = :sample" : ""

        final String hql = """
            select
                abstractQualityAssessment
            from
                AbstractQualityAssessment abstractQualityAssessment
                join abstractQualityAssessment.abstractBamFile abstractBamFile
                join abstractBamFile.workPackage mergingWorkPackage
            where
                mergingWorkPackage.sample.individual.project = :project
                and mergingWorkPackage.seqType = :seqType
                ${selectSample}
                and mergingWorkPackage.bamFileInProjectFolder = abstractBamFile
                and abstractBamFile.fileOperationStatus = :fileOperationStatus
                and abstractBamFile.qualityAssessmentStatus = :qualityAssessmentStatus
                and abstractBamFile.withdrawn = false
                and (
                    abstractQualityAssessment.class in (
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
            fileOperationStatus: AbstractBamFile.FileOperationStatus.PROCESSED,
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            cellRangerQualityAssessmentClass: CellRangerQualityAssessment.name,
            roddyMergedBamQaClass: [RoddyMergedBamQa.name, RnaQualityAssessment.name],
            allChromosomes: RoddyQualityAssessment.ALL,
        ]
        if (sample) {
            parameters.put("sample", sample)
        }

        List<AbstractQualityAssessment> qas = AbstractQualityAssessment.executeQuery(hql.toString(), parameters, [readOnly: true])
        return qas
    }
}
