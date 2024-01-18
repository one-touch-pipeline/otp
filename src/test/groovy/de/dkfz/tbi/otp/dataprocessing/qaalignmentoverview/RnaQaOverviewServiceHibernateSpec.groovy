/*
 * Copyright 2011-2024 The OTP authors
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

import de.dkfz.tbi.otp.dataprocessing.RnaQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.RoddyQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPanCancerFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.qcTrafficLight.QcThresholdService
import de.dkfz.tbi.otp.qcTrafficLight.TableCellValue
import de.dkfz.tbi.otp.workflow.alignment.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowService

class RnaQaOverviewServiceHibernateSpec extends HibernateSpec implements RoddyPanCancerFactory {

    private RnaQaOverviewService service

    @Override
    List<Class> getDomainClasses() {
        return [
                RnaRoddyBamFile,
        ]
    }

    private void setupData() {
        service = new RnaQaOverviewService([
                linkGenerator     : Mock(LinkGenerator) {
                    0 * _
                },
                qcThresholdService: Mock(QcThresholdService) {
                    0 * _
                },
        ])
    }

    void "qaClass, when called, should return RnaQualityAssessment"() {
        given:
        setupData()

        expect:
        service.qaClass() == RnaQualityAssessment
    }

    void "supportedSeqTypes, when called, should return seqTypes of workflow Rna alignment workflow"() {
        given:
        setupData()
        List<SeqType> seqTypes = DomainFactory.createRnaAlignableSeqTypes()

        (1..3).collect {
            createSeqTypePaired()
            createSeqType()
        }

        service.workflowService = Mock(WorkflowService) {
            1 * getSupportedSeqTypes(RnaAlignmentWorkflow.WORKFLOW) >> seqTypes
            0 * _
        }

        expect:
        service.supportedSeqTypes() == seqTypes
    }

    void "additionalJoinDomains, when called, should return empty list"() {
        given:
        setupData()

        expect:
        service.additionalJoinDomains() == [
        ]
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
        service.columnDefinitionList() == RnaQaOverviewService.COLUMN_DEFINITIONS
    }

    void "qcKeys, when called, should return QC_KEY"() {
        given:
        setupData()

        expect:
        service.qcKeys() == RnaQaOverviewService.QC_KEY
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
        String link = "link_${nextId}"

        Project project = createProject([
                state: archived ? Project.State.ARCHIVED : Project.State.OPEN,
        ])
        Long bamId = nextId

        Map<String, ?> qaMap = [
                bamId: bamId,
        ]

        Map linkMap = [
                action: "renderPDF",
                params: ["abstractBamFile.id": bamId],
        ]

        when:
        Map<String, ?> ret = service.extractSpecificValues(project, qaMap)

        then:
        service.linkGenerator.link(linkMap) >> link

        ret.size() == 3
        ret.containsKey('arribaPlots')
        TableCellValue arribaPlots = ret.arribaPlots
        arribaPlots.archived == archived
        arribaPlots.value == 'PDF'
        arribaPlots.linkTarget == "_blank"
        arribaPlots.link == link

        where:
        archived << [
                true,
                false,
        ]
    }
}
