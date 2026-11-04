/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflow

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.SearchSeqTrackService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.ExternalBamFactoryBam
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

class TriggerWorkflowsControllerSpec extends Specification implements ControllerUnitTest<TriggerWorkflowsController>, DataTest, ExternalBamFactoryBam {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractBamFile,
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
                FastqFile,
                Individual,
                MergingWorkPackage,
                Project,
                RawSequenceFile,
                RoddyBamFile,
                Sample,
                SampleType,
                SeqTrack,
                SeqType,
        ]
    }

    void setup() {
        controller.triggerWorkflowsService = Mock(TriggerWorkflowsService)
        controller.searchSeqTrackService = Mock(SearchSeqTrackService)
    }

    void "generateWarnings, without selected SeqTracks and external BAMs, returns empty SeqTrack, BAM and analysis rows"() {
        given:
        flash.seqTrackIds = []
        flash.extBamFileIds = []

        when:
        controller.generateWarnings()

        then:
        0 * controller.triggerWorkflowsService.getTableData(_)
        response.json.data == []
        response.json.bamData == []
        response.json.analysisData == []
    }

    void "generateWarnings, with external BAMs only, returns their projected BAM rows including sequencing type and joined analysis IDs"() {
        given:
        ExternallyProcessedBamFile externalBamFile = createBamFile()
        flash.seqTrackIds = []
        flash.extBamFileIds = [externalBamFile.id]
        TriggerWorkflowsBamRow row = createBamRow(externalBamFile.id, [3L, 11L])

        when:
        controller.generateWarnings()

        then:
        1 * controller.triggerWorkflowsService.getBamFiles([]) >> []
        1 * controller.triggerWorkflowsService.getTableData([externalBamFile.id]) >> new TriggerWorkflowsTableData([row], [])
        response.json.data == []
        response.json.bamData == [[
                id              : externalBamFile.id,
                project         : 'project',
                individual      : 'individual',
                sampleType      : 'tumor',
                seqType         : 'WGS PAIRED bulk',
                withdrawn       : false,
                libPrepKit      : 'kit',
                species         : 'Human (Homo sapiens) [No strain available]',
                mixedInSpecies  : 'Mouse (Mus musculus) [No strain available]',
                referenceGenome : 'hs37d5',
                seqPlatformGroup: '',
                analysisIds     : '3, 11',
        ]]
    }

    void "generateWarnings, passes internal and external BAM IDs to the table reader and keeps the order of its rows"() {
        given:
        SeqTrack seqTrack = createSeqTrack()
        ExternallyProcessedBamFile externalBamFile = createBamFile()
        RoddyBamFile internalBamFile = new RoddyBamFile()
        internalBamFile.id = externalBamFile.id + 100
        flash.seqTrackIds = [seqTrack.id]
        flash.extBamFileIds = [externalBamFile.id]

        when:
        controller.generateWarnings()

        then:
        1 * controller.triggerWorkflowsService.getBamFiles([seqTrack.id]) >> [internalBamFile]
        1 * controller.triggerWorkflowsService.getTableData([internalBamFile.id, externalBamFile.id]) >> new TriggerWorkflowsTableData([
                createBamRow(internalBamFile.id, []),
                createBamRow(externalBamFile.id, [7L]),
        ], [])
        1 * controller.searchSeqTrackService.projectSeqTrack(seqTrack) >> [id: seqTrack.id]
        response.json.data*.id == [seqTrack.id]
        response.json.bamData*.id == [internalBamFile.id, externalBamFile.id]
        response.json.bamData*.analysisIds == ['', '7']
    }

    void "generateWarnings, with analysis rows from the table reader, returns them in the specified column order"() {
        given:
        ExternallyProcessedBamFile externalBamFile = createBamFile()
        flash.seqTrackIds = []
        flash.extBamFileIds = [externalBamFile.id]
        TriggerWorkflowsAnalysisRow snvRow = new TriggerWorkflowsAnalysisRow(
                analysisType: 'SNV',
                id: 42L,
                project: 'project',
                individual: 'individual',
                sampleType: 'tumor \u2013 control',
                seqType: 'WGS PAIRED bulk',
                libraryPreparationKits: 'kitA, kitB',
                species: 'Human (Homo sapiens) [No strain available], Mouse (Mus musculus) [No strain available]',
                referenceGenomes: 'hs37d5',
                diseaseBamId: externalBamFile.id,
                controlBamId: 99L,
        )
        TriggerWorkflowsAnalysisRow sophiaRow = new TriggerWorkflowsAnalysisRow(
                analysisType: 'Sophia',
                id: 7L,
                project: 'project',
                individual: 'individual',
                sampleType: 'tumor \u2013 control',
                seqType: 'WGS PAIRED bulk',
                libraryPreparationKits: '',
                species: '',
                referenceGenomes: '',
                diseaseBamId: externalBamFile.id,
                controlBamId: 99L,
        )

        when:
        controller.generateWarnings()

        then:
        1 * controller.triggerWorkflowsService.getBamFiles([]) >> []
        1 * controller.triggerWorkflowsService.getTableData([externalBamFile.id]) >>
                new TriggerWorkflowsTableData([createBamRow(externalBamFile.id, [7L, 42L])], [snvRow, sophiaRow])
        response.json.analysisData*.id == [42, 7]
        response.json.analysisData[0] == [
                analysisType          : 'SNV',
                id                    : 42,
                project               : 'project',
                individual            : 'individual',
                sampleType            : 'tumor \u2013 control',
                seqType               : 'WGS PAIRED bulk',
                libraryPreparationKits: 'kitA, kitB',
                species               : 'Human (Homo sapiens) [No strain available], Mouse (Mus musculus) [No strain available]',
                referenceGenomes      : 'hs37d5',
                diseaseBamId          : externalBamFile.id,
                controlBamId          : 99,
        ]
        response.json.analysisData[1].libraryPreparationKits == ''
    }

    private static TriggerWorkflowsBamRow createBamRow(Long id, List<Long> analysisIds) {
        return new TriggerWorkflowsBamRow(
                id: id,
                project: 'project',
                individual: 'individual',
                sampleType: 'tumor',
                seqType: 'WGS PAIRED bulk',
                withdrawn: false,
                libPrepKit: 'kit',
                species: 'Human (Homo sapiens) [No strain available]',
                mixedInSpecies: 'Mouse (Mus musculus) [No strain available]',
                referenceGenome: 'hs37d5',
                seqPlatformGroup: '',
                analysisIds: analysisIds,
        )
    }
}
