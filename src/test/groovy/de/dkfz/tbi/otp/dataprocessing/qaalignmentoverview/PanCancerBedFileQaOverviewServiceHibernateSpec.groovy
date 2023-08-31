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

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.qcTrafficLight.QcThresholdService
import de.dkfz.tbi.otp.qcTrafficLight.TableCellValue
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowService

class PanCancerBedFileQaOverviewServiceHibernateSpec extends HibernateSpec implements RoddyPancanFactory {

    private PanCancerBedFileQaOverviewService service

    @Override
    List<Class> getDomainClasses() {
        return [
                RoddyBamFile,
        ]
    }

    private void setupData() {
        service = new PanCancerBedFileQaOverviewService([
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

    void "supportedSeqTypes, when called, should return seqTypes of workflow PanCancerWorkflow using bedfiles"() {
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
        Set<SeqType> seqTypesOfWorkflow = (seqTypeWithoutBedFile + seqTypeWithBedFile) as Set
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

        service.workflowService = Mock(WorkflowService) {
            1 * getSupportedSeqTypes(PanCancerWorkflow.WORKFLOW) >> seqTypesOfWorkflow
            0 * _
        }

        expect:
        service.supportedSeqTypes() == seqTypeWithBedFile
    }

    void "additionalJoinDomains, when called, should return empty list"() {
        given:
        setupData()

        expect:
        service.additionalJoinDomains() == []
    }

    void "additionalDomainHierarchies, when called, should return empty list"() {
        given:
        setupData()

        expect:
        service.additionalDomainHierarchies() == []
    }

    void "restriction, when called, should return list containing chromosome"() {
        given:
        setupData()

        expect:
        service.restriction() == [
                "qa.chromosome = :allChromosomes",
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
        service.columnDefinitionList() == PanCancerBedFileQaOverviewService.COLUMN_DEFINITIONS
    }

    void "qcKeys, when called, should return QC_KEY"() {
        given:
        setupData()

        expect:
        service.qcKeys() == PanCancerBedFileQaOverviewService.QC_KEY
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
                targetCoverage: input,
                bamId         : 0,
                state         : WorkflowRun.State.LEGACY,
        ]
        Map<String, String> expected = [
                createdWithVersion: 'NA',
                targetCoverage    : output,
                configFile        : 'N/A',
        ]

        expect:
        service.extractSpecificValues(null, qaMap) == expected

        where:
        input   || output
        1.0     || "1.00"
        5.0     || "5.00"
        12345.0 || "12345.00"
        1.23    || "1.23"
        1.2345  || "1.23"
        1.238   || "1.24"
        null    || ""
    }

    @Unroll
    void "extractSpecificValues, when called with map containing needed values, then should return map with link to config file"() {
        given:
        setupData()
        LinkGenerator linkGenerator = Mock(LinkGenerator) {
            link(_) >> 'test-link'
        }
        service.linkGenerator = linkGenerator

        Map<String, ?> qaMap = [
                targetCoverage: 0,
                bamId         : 0,
                state         : state,
        ]
        Map<String, String> expected = [
                createdWithVersion: 'NA',
                targetCoverage    : '0.00',
                configFile        : output,
        ]

        expect:
        service.extractSpecificValues(null, qaMap) == expected

        where:
        state                     | bamId || output
        WorkflowRun.State.LEGACY  | 1     || 'N/A'
        WorkflowRun.State.SUCCESS | 1     || [new TableCellValue(value: 'View', linkTarget: '_blank', link: 'test-link'), new TableCellValue(value: 'Download', link: 'test-link')]
    }

    @Unroll
    void "addDerivedData, when called, when called with map containing needed values, then should return map with extracted values"() {
        given:
        setupData()

        Map<String, ?> qaMap = [
                onTargetMappedBases: onTargetMappedBases,
                allBasesMapped     : allBasesMapped,
        ]

        Map<String, ?> expected = qaMap + [
                onTargetRatio        : onTargetRatio,
                // from baseclass
                percentMappedReads   : null,
                percentDuplicates    : null,
                percentDiffChr       : null,
                percentProperlyPaired: null,
                percentSingletons    : null,
        ]

        when:
        service.addDerivedData([qaMap])

        then:
        TestCase.assertContainSame(qaMap, expected)

        where:
        onTargetMappedBases | allBasesMapped || onTargetRatio
        1                   | 1              || 100.0
        5                   | 1              || 500.0
        5                   | 5              || 100.0
        1                   | 5              || 20.00
        1                   | 10             || 10.0
        1                   | 100            || 1.0
        1                   | 1000           || 0.1
        1                   | 10000          || 0.01
        1                   | 100000         || 0.001
        1234567890          | 1              || 123456789000.0
        1                   | 0              || null
        1                   | null           || null
        null                | 1              || null
        null                | null           || null
    }
}
