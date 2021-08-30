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
package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import groovy.json.JsonSlurper
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.administration.UserService
import de.dkfz.tbi.otp.ngsqc.FastqcResultsService
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.DataTableCommand

import java.text.SimpleDateFormat

@Secured('isFullyAuthenticated()')
class SequenceController {
    SeqTrackService seqTrackService
    ProjectService projectService
    FastqcResultsService fastqcResultsService
    UserService userService

    def index() {
        List<SeqType> seqTypes = SeqType.list(sort: "name", order: "asc")
        [
            tableHeader: (SequenceColumn.values() - SequenceColumn.WITHDRAWN)*.message,
            sampleTypes: SampleType.list(sort: "name", order: "asc"),
            seqTypes: new TreeSet(seqTypes.collect { it.displayName }),
            libraryLayouts: new TreeSet(seqTypes.collect { it.libraryLayout }),
            seqCenters: SeqCenter.list(sort: "name", order: "asc"),
            libraryPreparationKits: LibraryPreparationKit.list(sort: "shortDisplayName", order: "asc").shortDisplayName,
            antibodyTargets: AntibodyTarget.list(sort: "name", order: "asc"),
            showRunLinks: userService.hasCurrentUserAdministrativeRoles(),
            filterTree : [
                    [name : 'projectSelection', msgcode: 'sequence.search.project',
                     type : 'LIST', from: projectService.getAllProjects(),
                     value: 'displayName', key: 'id'],
                    [name: 'individualSearch', msgcode: 'sequence.search.individual',
                     type: 'TEXT'],
                    [name : 'sampleTypeSelection', msgcode: 'sequence.search.sample',
                     type : 'LIST', from: SampleType.list(sort: "name", order: "asc"),
                     value: 'name', key: 'id'],
                    [name: 'seqTypeSelection', msgcode: 'sequence.search.seqType',
                     type: 'LIST', from: seqTypes.collect { it.displayName }.unique()],
                    [name: 'ilseIdSearch', msgcode: 'sequence.search.ilse',
                     type: 'TEXT'],
                    [name: 'libraryLayoutSelection', msgcode: 'sequence.search.sequencingReadType',
                     type: 'LIST', from: seqTypes.collect { it.libraryLayout }.unique()],
                    [name: 'singleCell', msgcode: 'sequence.search.singleCell',
                     type: 'LIST', from: [true, false]],
                    [name: 'libraryPreparationKitSelection', msgcode: 'sequence.search.libPrepKit',
                     type: 'LIST', from: LibraryPreparationKit.list(sort: "shortDisplayName", order: "asc")*.shortDisplayName],
                    [name : 'antibodyTargetSelection', msgcode: 'sequence.search.antibodyTarget',
                     type : 'LIST', from: AntibodyTarget.list(sort: "name", order: "asc"),
                     value: 'name', key: 'name'],
                    [name : 'seqCenterSelection', msgcode: 'sequence.search.seqCenter',
                     type : 'LIST', from: SeqCenter.list(sort: "name", order: "asc"),
                     value: 'name', key: 'id'],
                    [name: 'runSearch', msgcode: 'sequence.search.run',
                     type: 'TEXT'],
            ]
        ]
    }

    def dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        SequenceFiltering filtering = SequenceFiltering.fromJSON(params.filtering)

        dataToRender.iTotalRecords = seqTrackService.countSequences(filtering)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        List<Sequence> sequences = seqTrackService.listSequences(cmd.iDisplayStart, cmd.iDisplayLength, cmd.sortOrder,
                SequenceColumn.fromDataTable(cmd.iSortCol_0), filtering)
        List<DataFile> dataFiles = sequences ? fastqcResultsService.fastQCFiles(sequences) : []
        Map<Long, List<DataFile>> seqTrackIdDataFileMap = dataFiles.groupBy {
            it.seqTrack.id
        }

        // need to add an additional field to the sequences
        // if added as dynamic property, it is not included during the JSON conversion
        // because of that, we just copy all properties into a map
        sequences.each { Sequence seq ->
            Map data = [:]
            seq.getProperties().each {
                data.put(it.key, it.value)
            }
            // format date
            data.dateCreated = data.dateCreated.format("yyyy-MM-dd")

            data.withdrawn = SeqTrack.get(seq.seqTrackId).isWithdrawn()

            data.problemDescription = seq.problem?.description

            data.fastQCFiles = seqTrackIdDataFileMap[seq.seqTrackId]?.sort {
                [!it.indexFile, it.mateNumber]
            }.collect {
                [
                        readName: it.getReadName(),
                        fastqId : it.id,
                ]
            }
            dataToRender.aaData << data
        }
        render dataToRender as JSON
    }

    def exportAll(DataTableCommand cmd) {
        SequenceFiltering filtering = SequenceFiltering.fromJSON(params.filtering)
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        String currentDate = dateFormat.format(new Date())

        List<Sequence> sequences = seqTrackService.listSequences(0, -1, cmd.sortOrder, SequenceColumn.fromDataTable(cmd.iSortCol_0), filtering)

        String contentBody = sequences.collect { Sequence row ->
            [
                    row.projectName,
                    row.mockPid,
                    row.sampleTypeName,
                    row.seqTypeDisplayName,
                    row.libraryLayout,
                    row.singleCell,
                    row.seqCenterName,
                    row.libraryPreparationKit,
                    row.antibodyTarget,
                    row.name,
                    row.laneId,
                    row.libraryName,
                    row.singleCellWellLabel,
                    row.ilseId,
                    row.problem?.name() ?: "",
                    row.fileExists,
                    row.dateCreated?.format("yyyy-MM-dd"),
                    SeqTrack.get(row.seqTrackId).isWithdrawn(),
            ].collect { it ?: "" }.join(",")
        }.join("\n")
        String contentHeader = (SequenceColumn.values() - SequenceColumn.FASTQC)
                .collect { g.message(code: it.message) }
                .join(',').replaceAll("<br/?>", " ")
        String content = "${contentHeader}\n${contentBody}\n"
        response.contentType = "application/octet-stream"
        response.setHeader("Content-disposition", "filename=Sequence_Export_" + currentDate + ".csv")
        response.outputStream << content.toString().bytes
    }
}

@TupleConstructor
enum SequenceColumn {
    PROJECT("sequence.list.headers.project", "projectName"),
    INDIVIDUAL("sequence.list.headers.individual", "mockPid"),
    SAMPLE_TYPE("sequence.list.headers.sampleType", "sampleTypeName"),
    SEQ_TYPE("sequence.list.headers.seqType", "seqTypeName"),
    LIBRARY_LAYOUT("sequence.list.headers.sequencingReadType", "libraryLayout"),
    SINGLE_CELL("sequence.list.headers.singleCell", "singleCell"),
    SEQ_CENTER("sequence.list.headers.seqCenter", "seqCenterName"),
    LIBRARY_PREPARATION_KIT("sequence.list.headers.libPrepKit", "libraryPreparationKit"),
    ANTIBODY_TARGET("sequence.list.headers.antibodyTarget","antibodyTarget"),
    RUN("sequence.list.headers.run", "name"),
    LANE("sequence.list.headers.lane", "laneId"),
    LIBRARY("sequence.list.headers.library", "libraryName"),
    SINGLE_CELL_WELL_LABEL('sequence.list.headers.singleCellWellLabel', 'singleCellWellLabel'),
    FASTQC("sequence.list.headers.fastqc", "fastqcState"),
    ILSEID("sequence.list.headers.ilseId", "ilseId"),
    KNOWN_ISSUES("sequence.list.headers.warning", "problem"),
    FILE_EXISTS("sequence.list.headers.fileExists", "fileExists"),
    DATE("sequence.list.headers.date", "dateCreated"),
    WITHDRAWN("sequence.list.headers.withdrawn", "withdrawn"),

    final String message
    final String columnName

    static SequenceColumn fromDataTable(int column) {
        if (column >= values().size() || column < 0) {
            return PROJECT
        }
        return values()[column]
    }
}

/**
 * Class describing the various filtering to do on WorkPackage.
 */
class SequenceFiltering {
    List<Long> project = []
    List<String> individual = []
    List<Long> sampleType = []
    List<String> seqType = []
    List<String> libraryLayout = []
    List<Boolean> singleCell = []
    List<Integer> ilseId = []
    List<Integer> seqCenter = []
    List<String> run = []
    List<String> libraryPreparationKit = []
    List<String> antibodyTarget = []

    boolean enabled = false

    static SequenceFiltering fromJSON(String json) {
        SequenceFiltering filtering = new SequenceFiltering()
        if (!json) {
            return filtering
        }
        def slurper = new JsonSlurper()
        slurper.parseText(json).each {
            switch (it.type) {
                case "projectSelection":
                    if (it.value.isLong()) {
                        filtering.project << (it.value as Long)
                        filtering.enabled = true
                    }
                    break
                case "individualSearch":
                    it.value.split(",").each {
                        if (it && it.length() >= 3) {
                            filtering.individual << it.trim()
                            filtering.enabled = true
                        }
                    }
                    break
                case "sampleTypeSelection":
                    if (it.value.isLong()) {
                        filtering.sampleType << (it.value as Long)
                        filtering.enabled = true
                    }
                    break
                case "seqTypeSelection":
                    if (it.value) {
                        filtering.seqType << it.value
                        filtering.enabled = true
                    }
                    break
                case "ilseIdSearch":
                    it.value.split(",").each {
                        if (it) {
                            filtering.ilseId << (it as Integer)
                            filtering.enabled = true
                        }
                    }
                    break
                case "libraryLayoutSelection":
                    if (it.value) {
                        filtering.libraryLayout << it.value
                        filtering.enabled = true
                    }
                    break
                case "singleCell":
                    if (it.value) {
                        filtering.singleCell << it.value.toBoolean()
                        filtering.enabled = true
                    }
                    break
                case "seqCenterSelection":
                    if (it.value.isLong()) {
                        filtering.seqCenter << (it.value as Long)
                        filtering.enabled = true
                    }
                    break
                case "runSearch":
                    it.value.split(",").each {
                        if (it && it.length() >= 3) {
                            filtering.run << it.trim()
                            filtering.enabled = true
                        }
                    }
                    break
                case "libraryPreparationKitSelection":
                    if (it.value) {
                        filtering.libraryPreparationKit << it.value
                        filtering.enabled = true
                    }
                    break
                case "antibodyTargetSelection":
                    if (it.value) {
                        filtering.antibodyTarget << it.value
                        filtering.enabled = true
                    }
                    break
            }
        }
        return filtering
    }
}
