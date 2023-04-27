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
import groovy.transform.CompileStatic

import de.dkfz.tbi.otp.dataprocessing.AbstractQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.RoddyMergedBamQa
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.FormatHelper
import de.dkfz.tbi.otp.workflow.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowService

@CompileStatic
@Transactional(readOnly = true)
class PanCancerBedFileQaOverviewService extends AbstractRoddyQaOverviewService {

    final static List<? extends ColumnDefinition> COLUMN_DEFINITIONS = [
            new PropertyColumnDefinition('bamFile', 'coverage', 'targetCoverage'),
            new PropertyColumnDefinition('qa', 'onTargetMappedBases', 'onTargetMappedBases'),
            new PropertyColumnDefinition('qa', 'allBasesMapped', 'allBasesMapped'),
    ].asImmutable()

    final static List<String> QC_KEY = [
            "onTargetRatio",
    ].asImmutable()

    WorkflowService workflowService

    @Override
    Class<? extends AbstractQualityAssessment> qaClass() {
        return RoddyMergedBamQa
    }

    @Override
    List<SeqType> supportedSeqTypes() {
        return workflowService.getSupportedSeqTypes(PanCancerWorkflow.WORKFLOW).findAll {
            it.needsBedFile
        } as List
    }

    @Override
    protected List<String> additionalJoinDomains() {
        return []
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
    protected Map<String, ?> extractSpecificValues(Project project, Map<String, ?> qaMap) {
        return [
                createdWithVersion: "${(qaMap.programVersion2 ?: qaMap.workflowVersion) ?: 'NA'}",
                targetCoverage: FormatHelper.formatNumber((Double) qaMap.targetCoverage),
        ]
    }

    @Override
    void addDerivedData(List<Map<String, ?>> qaMapList) {
        super.addDerivedData(qaMapList)
        qaMapList.each { Map<String, ?> qaMap ->
            qaMap.onTargetRatio = calculatePercentage((Number) qaMap.onTargetMappedBases, (Number) qaMap.allBasesMapped)
        }
    }
}
