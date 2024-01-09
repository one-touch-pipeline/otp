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

import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.qcTrafficLight.QcThresholdService
import de.dkfz.tbi.otp.qcTrafficLight.TableCellValue

class CellRangerQaOverviewServiceHibernateSpec extends HibernateSpec implements RoddyPancanFactory {

    private CellRangerQaOverviewService service

    @Override
    List<Class> getDomainClasses() {
        return [
                SingleCellBamFile,
        ]
    }

    private void setupData() {
        service = new CellRangerQaOverviewService([
                linkGenerator     : Mock(LinkGenerator) {
                    0 * _
                },
                qcThresholdService: Mock(QcThresholdService) {
                    0 * _
                },
        ])
    }

    void "qaClass, when called, should return CellRangerQualityAssessment"() {
        given:
        setupData()

        expect:
        service.qaClass() == CellRangerQualityAssessment
    }

    void "supportedSeqTypes, when called, should return seqTypes of workflow CellRanger alignment workflow"() {
        given:
        setupData()
        List<SeqType> seqTypes = DomainFactory.createCellRangerAlignableSeqTypes()

        (1..3).collect {
            createSeqTypePaired()
            createSeqType()
        }

        expect:
        service.supportedSeqTypes() == seqTypes
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

    void "restriction, when called, should return empty list"() {
        given:
        setupData()

        expect:
        service.restriction() == []
    }

    void "additionalParameters, when called, should return empty map"() {
        given:
        setupData()

        expect:
        service.additionalParameters() == [:]
    }

    void "columnDefinitionList, when called, should return COLUMN_DEFINITIONS"() {
        given:
        setupData()

        expect:
        service.columnDefinitionList() == CellRangerQaOverviewService.COLUMN_DEFINITIONS
    }

    void "qcKeys, when called, should return QC_KEY"() {
        given:
        setupData()

        expect:
        service.qcKeys() == CellRangerQaOverviewService.QC_KEY
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
                bamId              : bamId,
                referenceGenomeName: 'referenceGenomeName',
                toolNameName       : 'toolNameName',
                indexToolVersion   : 'indexToolVersion',
                createdWithVersion : 'NA',
        ]

        Map linkMap = [
                action: "viewCellRangerSummary",
                params: ["singleCellBamFile.id": bamId],
        ]

        when:
        Map<String, ?> ret = service.extractSpecificValues(project, qaMap)

        then:
        service.linkGenerator.link(linkMap) >> link

        ret.size() == 2
        ret.createdWithVersion == 'NA'
        ret.containsKey('summary')
        TableCellValue summary = ret.summary
        summary.archived == archived
        summary.value == 'Summary'
        summary.linkTarget == "_blank"
        summary.link == link

        where:
        archived << [
                true,
                false,
        ]
    }
}
