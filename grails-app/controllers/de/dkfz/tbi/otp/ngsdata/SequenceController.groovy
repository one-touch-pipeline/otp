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
import groovy.json.JsonSlurper

import de.dkfz.tbi.otp.ngsqc.FastqcResultsService
import de.dkfz.tbi.otp.utils.DataTableCommand

class SequenceController {
    SeqTrackService seqTrackService
    ProjectService projectService
    FastqcResultsService fastqcResultsService

    def index() {
        List<SeqType> seqTypes = SeqType.list(sort: "name", order: "asc")
        [
            projects: projectService.getAllProjects(),
            sampleTypes: SampleType.list(sort: "name", order: "asc"),
            seqTypes: new TreeSet(seqTypes.collect { it.displayName }),
            libraryLayouts: new TreeSet(seqTypes.collect { it.libraryLayout }),
            seqCenters: SeqCenter.list(sort: "name", order: "asc"),
            libraryPreparationKits: LibraryPreparationKit.list(sort: "shortDisplayName", order: "asc").shortDisplayName,
        ]
    }

    def dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        SequenceFiltering filtering = SequenceFiltering.fromJSON(params.filtering)

        dataToRender.iTotalRecords = seqTrackService.countSequences(filtering)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        List<Sequence> sequences = seqTrackService.listSequences(cmd.iDisplayStart, cmd.iDisplayLength, cmd.sortOrder,
                SequenceSortColumn.fromDataTable(cmd.iSortCol_0), filtering)
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

        List<Sequence> sequences = seqTrackService.listSequences(0, -1, cmd.sortOrder, SequenceSortColumn.fromDataTable(cmd.iSortCol_0), filtering)

        def contentBody = sequences.collect { def row ->
            [
                    row.projectName,
                    row.mockPid,
                    row.sampleTypeName,
                    row.seqTypeDisplayName,
                    row.libraryLayout,
                    row.singleCell,
                    row.seqCenterName,
                    row.libraryPreparationKit,
                    row.name,
                    row.laneId,
                    row.libraryName,
                    row.ilseId,
                    row.problem?.name() ?: "",
                    row.fileExists,
                    row.dateCreated?.format("yyyy-MM-dd"),
            ].join(",")
        }.join("\n")
        def contentHeader = [
            'Project',
            'Individual',
            'Sample Type',
            'Sequence Type',
            'Library Layout',
            'Single Cell',
            'Sequence Center',
            'Library Preparation Kit',
            'Run',
            'Lane',
            'Library',
            'ILSe',
            'Known issues',
            'File Exists',
            'Run Date',
        ].join(',')
        def content = "${contentHeader}\n${contentBody}\n"
        response.setContentType("application/octet-stream")
        response.setHeader("Content-disposition", "filename=sequence_export.csv")
        response.outputStream << content.toString().bytes
    }
}

enum SequenceSortColumn {
    PROJECT("projectName"),
    INDIVIDUAL("mockPid"),
    SAMPLE_TYPE("sampleTypeName"),
    SEQ_TYPE("seqTypeName"),
    LIBRARY_LAYOUT("libraryLayout"),
    SINGLE_CELL("singleCell"),
    SEQ_CENTER("seqCenterName"),
    LIBRARY_PREPARATION_KIT("libraryPreparationKit"),
    RUN("name"),
    LANE("laneId"),
    LIBRARY("libraryName"),
    FASTQC("fastqcState"),
    ILSEID("ilseId"),
    KNOWN_ISSUES("problem"),
    FILE_EXISTS("fileExists"),
    DATE("dateCreated")

    private final String columnName

    SequenceSortColumn(String column) {
        this.columnName = column
    }

    static SequenceSortColumn fromDataTable(int column) {
        switch (column) {
            case 0:
                return SequenceSortColumn.PROJECT
            case 1:
                return SequenceSortColumn.INDIVIDUAL
            case 2:
                return SequenceSortColumn.SAMPLE_TYPE
            case 3:
                return SequenceSortColumn.SEQ_TYPE
            case 4:
                return SequenceSortColumn.LIBRARY_LAYOUT
            case 5:
                return SequenceSortColumn.SINGLE_CELL
            case 6:
                return SequenceSortColumn.SEQ_CENTER
            case 7:
                return SequenceSortColumn.LIBRARY_PREPARATION_KIT
            case 8:
                return SequenceSortColumn.RUN
            case 9:
                return SequenceSortColumn.LANE
            case 10:
                return SequenceSortColumn.LIBRARY
            case 11:
                return SequenceSortColumn.FASTQC
            case 12:
                return SequenceSortColumn.ILSEID
            case 13:
                return SequenceSortColumn.KNOWN_ISSUES
            case 14:
                return SequenceSortColumn.FILE_EXISTS
            case 15:
                return SequenceSortColumn.DATE
            default:
                return SequenceSortColumn.PROJECT
        }
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
                    if (it.value && it.value.length() >= 3) {
                        filtering.individual << it.value
                        filtering.enabled = true
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
                    if (it.value) {
                        filtering.ilseId << (it.value as Integer)
                        filtering.enabled = true
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
                    if (it.value && it.value.length() >= 3) {
                        filtering.run << it.value
                        filtering.enabled = true
                    }
                    break
                case "libraryPreparationKitSelection":
                    if (it.value) {
                        filtering.libraryPreparationKit << it.value
                        filtering.enabled = true
                    }
                    break
            }
        }
        return filtering
    }
}
