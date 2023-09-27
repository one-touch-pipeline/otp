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

import asset.pipeline.grails.LinkGenerator
import grails.test.hibernate.HibernateSpec
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.qcTrafficLight.QcThresholdService
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowService

class PanCancerNoBedFileQaOverviewServiceHibernateSpec extends HibernateSpec implements RoddyPancanFactory {

    private PanCancerNoBedFileQaOverviewService service

    @Override
    List<Class> getDomainClasses() {
        return [
                RoddyBamFile,
        ]
    }

    private void setupData() {
        service = new PanCancerNoBedFileQaOverviewService([
                linkGenerator     : Mock(LinkGenerator) {
                    0 * _
                },
                qcThresholdService: Mock(QcThresholdService) {
                    0 * _
                },
        ])
    }

    void "qaClass, when called, should return RoddyMergedBamQa"() {
        given:
        setupData()

        expect:
        service.qaClass() == RoddyMergedBamQa
    }

    void "supportedSeqTypes, when called, should return seqTypes of workflow PanCancerWorkflow using no bedFiles and for WgbsAlignment workflow"() {
        given:
        setupData()

        List<SeqType> seqTypeWithoutBedFile = (1..3).collect {
            createSeqTypePaired([
                    needsBedFile: false
            ])
        }
        List<SeqType> seqTypeWithBedFile = (1..3).collect {
            createSeqTypePaired([
                    needsBedFile: true
            ])
        }
        List<SeqType> seqTypeForWgbsAlignment = (1..3).collect {
            createSeqTypePaired()
        }
        Set<SeqType> seqTypesOfPanCanWorkflow = (seqTypeWithoutBedFile + seqTypeWithBedFile) as Set
        (1..3).each {
            createSeqTypePaired([
                    needsBedFile: true
            ])
            createSeqTypePaired([
                    needsBedFile: false
            ])
            createSeqTypeSingle([
                    needsBedFile: true
            ])
            createSeqTypeSingle([
                    needsBedFile: false
            ])
        }

        List<SeqType> result = seqTypeWithoutBedFile + seqTypeForWgbsAlignment

        service.workflowService = Mock(WorkflowService) {
            1 * getSupportedSeqTypes(PanCancerWorkflow.WORKFLOW) >> seqTypesOfPanCanWorkflow
            1 * getSupportedSeqTypes(WgbsWorkflow.WORKFLOW) >> seqTypeForWgbsAlignment
            0 * _
        }

        expect:
        service.supportedSeqTypes() == result
    }

    void "additionalJoinDomains, when called, should return join of reference genome"() {
        given:
        setupData()

        expect:
        service.additionalJoinDomains() == [
        ]
    }

    void "additionalDomainHierarchies, when called, should return list of RoddyMergedBamQa and and the domains needed for getting x and y chromosomes"() {
        given:
        setupData()

        expect:
        service.additionalDomainHierarchies() == [
                "RoddyMergedBamQa qaX",
                "ReferenceGenomeEntry entryX",
                "RoddyMergedBamQa qaY",
                "ReferenceGenomeEntry entryY",
        ]
    }

    void "restriction, when called, should return list containing allChromosome and the restriction to get values for x and y chromosome"() {
        given:
        setupData()

        expect:
        service.restriction() == [
                "qa.chromosome = :allChromosomes",
                "entryX.alias = 'X'",
                "entryX.referenceGenome = referenceGenome",
                "entryX.name = qaX.chromosome",
                "qaX.qualityAssessmentMergedPass = qaPass",
                "entryY.alias = 'Y'",
                "entryY.referenceGenome = referenceGenome",
                "entryY.name = qaY.chromosome",
                "qaY.qualityAssessmentMergedPass = qaPass",
        ]
    }

    void "additionalParameters, when called, should return map with AllChromosome"() {
        given:
        setupData()

        expect:
        service.additionalParameters() == [
                allChromosomes: RoddyQualityAssessment.ALL,
        ]
    }

    void "columnDefinitionList, when called, should return COLUMN_DEFINITIONS"() {
        given:
        setupData()

        expect:
        service.columnDefinitionList() == PanCancerNoBedFileQaOverviewService.COLUMN_DEFINITIONS
    }

    void "qcKeys, when called, should return empty list"() {
        given:
        setupData()

        expect:
        service.qcKeys() == []
    }

    void "qcKeysMap, when called, should return empty map"() {
        given:
        setupData()

        expect:
        service.qcKeysMap([:]) == [:]
    }

    @Unroll
    void "extractSpecificValues, when called with map containing needed values, then should return map with extracted values"() {
        given:
        setupData()

        Map<String, ?> qaMap = [
                coverageWithoutN: inputCoverage,
                coverageX       : inputCoverageX,
                coverageY       : inputCoverageY,
                bamId           : 0,
                state           : WorkflowRun.State.LEGACY,
        ]
        Map<String, String> expected = [
                createdWithVersion: "NA",
                coverageWithoutN  : outputCoverage,
                coverageX         : outputCoverageX,
                coverageY         : outputCoverageY,
                configFile        : 'N/A',
        ]

        expect:
        service.extractSpecificValues(null, qaMap) == expected

        where:
        inputCoverage | inputCoverageX | inputCoverageY || outputCoverage | outputCoverageX | outputCoverageY
        1.0           | 2.0            | 3.0            || "1.00"         | "2.00"          | "3.00"
        5.0           | 7.0            | 9.0            || "5.00"         | "7.00"          | "9.00"
        12345.0       | 12346.0        | 12347.0        || "12345.00"     | "12346.00"      | "12347.00"
        1.23          | 2.23           | 3.23           || "1.23"         | "2.23"          | "3.23"
        1.2345        | 2.2345         | 3.2345         || "1.23"         | "2.23"          | "3.23"
        1.238         | 2.238          | 3.238          || "1.24"         | "2.24"          | "3.24"
        null          | null           | null           || ""             | ""              | ""
    }
}
