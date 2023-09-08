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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.FormatHelper
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowService

@Transactional(readOnly = true)
class PanCancerNoBedFileQaOverviewService extends AbstractRoddyQaOverviewService {

    static final List<String> CHROMOSOMES_XY = [
            Chromosomes.CHR_X,
            Chromosomes.CHR_Y,
    ]*.alias.asImmutable()

    final static List<? extends ColumnDefinition> COLUMN_DEFINITIONS = [
            new PropertyColumnDefinition('bamFile', 'coverage', 'coverageWithoutN'),
            new CalculatedColumnDefinition('1.0 * qaX.qcBasesMapped / entryX.lengthWithoutN', 'coverageX'),
            new CalculatedColumnDefinition('1.0 * qaY.qcBasesMapped / entryY.lengthWithoutN', 'coverageY'),
    ].asImmutable()

    WorkflowService workflowService

    @Override
    Class<? extends AbstractQualityAssessment> qaClass() {
        return RoddyMergedBamQa
    }

    @Override
    List<SeqType> supportedSeqTypes() {
        return (
                workflowService.getSupportedSeqTypes(PanCancerWorkflow.WORKFLOW).findAll {
                    !it.needsBedFile
                } + workflowService.getSupportedSeqTypes(WgbsWorkflow.WGBS_WORKFLOW)
        ) as List
    }

    @Override
    protected List<String> additionalJoinDomains() {
        return [
        ]
    }

    @Override
    protected List<String> additionalDomainHierarchies() {
        return CHROMOSOMES_XY.collectMany { String chromosome ->
            [
                    "RoddyMergedBamQa qa${chromosome}",
                    "ReferenceGenomeEntry entry${chromosome}",
            ]
        } as List<String>
    }

    @Override
    protected List<String> restriction() {
        return super.restriction() + (CHROMOSOMES_XY.collectMany { String chromosome ->
            [
                    "entry${chromosome}.alias = '${chromosome}'",
                    "entry${chromosome}.referenceGenome = referenceGenome",
                    "entry${chromosome}.name = qa${chromosome}.chromosome",
                    "qa${chromosome}.qualityAssessmentMergedPass = qaPass",
            ]
        } as List<String>)
    }

    @Override
    protected List<ColumnDefinition> columnDefinitionList() {
        return COLUMN_DEFINITIONS
    }

    @Override
    protected List<String> qcKeys() {
        return []
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
                coverageWithoutN  : FormatHelper.formatNumber((Number) qaMap.coverageWithoutN), // Coverage w/o N
                coverageX         : FormatHelper.formatNumber((Number) qaMap.coverageX), // ChrX Coverage w/o N
                coverageY         : FormatHelper.formatNumber((Number) qaMap.coverageY), // ChrY Coverage w/o N
        ]
        result.putAll(getRoddyConfig(qaMap.bamId as long, qaMap.state as WorkflowRun.State))
        return result
    }
}
