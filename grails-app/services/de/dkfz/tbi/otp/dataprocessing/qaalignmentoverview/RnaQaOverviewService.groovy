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
package de.dkfz.tbi.otp.dataprocessing.qaalignmentoverview

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.AbstractQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.RnaQualityAssessment
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.qcTrafficLight.TableCellValue
import de.dkfz.tbi.otp.workflow.alignment.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowService

@Transactional(readOnly = true)
class RnaQaOverviewService extends AbstractRoddyQaOverviewService {

    final static List<? extends ColumnDefinition> COLUMN_DEFINITIONS = [
            new PropertyColumnDefinition('qa', "totalReadCounter", "totalReadCounter"),
            new PropertyColumnDefinition('qa', "threePNorm", "threePNorm"),
            new PropertyColumnDefinition('qa', "fivePNorm", "fivePNorm"),
            new PropertyColumnDefinition('qa', "chimericPairs", "chimericPairs"),
            new PropertyColumnDefinition('qa', "duplicatesRate", "duplicatesRate"),
            new PropertyColumnDefinition('qa', "end1Sense", "end1Sense"),
            new PropertyColumnDefinition('qa', "end2Sense", "end2Sense"),
            new PropertyColumnDefinition('qa', "estimatedLibrarySize", "estimatedLibrarySize"),
            new PropertyColumnDefinition('qa', "exonicRate", "exonicRate"),
            new PropertyColumnDefinition('qa', "expressionProfilingEfficiency", "expressionProfilingEfficiency"),
            new PropertyColumnDefinition('qa', "genesDetected", "genesDetected"),
            new PropertyColumnDefinition('qa', "intergenicRate", "intergenicRate"),
            new PropertyColumnDefinition('qa', "intragenicRate", "intragenicRate"),
            new PropertyColumnDefinition('qa', "intronicRate", "intronicRate"),
            new PropertyColumnDefinition('qa', "mapped", "mapped"),
            new PropertyColumnDefinition('qa', "mappedUnique", "mappedUnique"),
            new PropertyColumnDefinition('qa', "mappedUniqueRateOfTotal", "mappedUniqueRateOfTotal"),
            new PropertyColumnDefinition('qa', "mappingRate", "mappingRate"),
            new PropertyColumnDefinition('qa', "meanCV", "meanCV"),
            new PropertyColumnDefinition('qa', "uniqueRateofMapped", "uniqueRateofMapped"),
            new PropertyColumnDefinition('qa', "rRNARate", "rRNARate"),
    ].asImmutable()

    final static List<String> QC_KEY = [
            "totalReadCounter",
            "threePNorm",
            "fivePNorm",
            "chimericPairs",
            "duplicatesRate",
            "end1Sense",
            "end2Sense",
            "estimatedLibrarySize",
            "exonicRate",
            "expressionProfilingEfficiency",
            "genesDetected",
            "intergenicRate",
            "intragenicRate",
            "intronicRate",
            "mapped",
            "mappedUnique",
            "mappedUniqueRateOfTotal",
            "mappingRate",
            "meanCV",
            "uniqueRateofMapped",
            "rRNARate",
    ].asImmutable()

    WorkflowService workflowService

    @Override
    Class<? extends AbstractQualityAssessment> qaClass() {
        return RnaQualityAssessment
    }

    @Override
    List<SeqType> supportedSeqTypes() {
        return workflowService.getSupportedSeqTypes(RnaAlignmentWorkflow.WORKFLOW) as List
    }

    @Override
    protected List<String> additionalJoinDomains() {
        return [
        ]
    }

    @Override
    protected List<String> additionalDomainHierarchies() {
        return []
    }

    @Override
    protected List<ColumnDefinition> columnDefinitionList() {
        return COLUMN_DEFINITIONS
    }

    @Override
    protected List<String> qcKeys() {
        return QC_KEY
    }

    @Override
    protected Map<String, Double> qcKeysMap(Map<String, ?> qaMap) {
        return [:]
    }

    @Override
    @CompileDynamic
    protected Map<String, ?> extractSpecificValues(Project project, Map<String, ?> qaMap) {
        Map<String, ?> result = [
                createdWithVersion: "${(qaMap.programVersion2 ?: qaMap.workflowVersion) ?: 'NA'}",
                arribaPlots       : new TableCellValue(
                        archived: project.archived,
                        value: "PDF",
                        linkTarget: "_blank",
                        link: linkGenerator.link([
                                action: "renderPDF",
                                params: ["abstractBamFile.id": qaMap.bamId],
                        ])),
        ]
        result.putAll(getConfigXML(qaMap.bamId as long, qaMap.state as WorkflowRun.State))
        return result
    }
}
