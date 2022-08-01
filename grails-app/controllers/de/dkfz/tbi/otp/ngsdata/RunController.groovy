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

import de.dkfz.tbi.otp.ngsqc.FastqcResultsService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.DataTableCommand
import de.dkfz.tbi.util.TimeFormats

import java.text.SimpleDateFormat

@Secured("hasRole('ROLE_OPERATOR')")
class RunController {

    LsdfFilesService lsdfFilesService
    RunService runService
    FastqcResultsService fastqcResultsService
    SeqCenterService seqCenterService

    static allowedMethods = [
            display        : "GET",
            show           : "GET",
            list           : "GET",
            dataTableSource: "POST",
    ]

    def display() {
        redirect(action: "show", id: params.id)
    }

    def show() {
        params.id = params.id ?: "0"
        Run run = runService.getRun(params.id)
        if (!run || run.project?.archived) {
            return response.sendError(404)
        }
        //This page requires using SAMPLE_NAME, since the DataFile has no connection to a SeqTrack. Its only used for legacy objects
        List<MetaDataKey> keys = []
        keys[0] = CollectionUtils.atMostOneElement(MetaDataKey.findAllByName(MetaDataColumn.SAMPLE_NAME.name()))
        keys[1] = CollectionUtils.atMostOneElement(MetaDataKey.findAllByName(MetaDataColumn.WITHDRAWN.name()))

        List<Map<String, Object>> wrappedMetaDataFiles = runService.retrieveMetaDataFiles(run).collect {
            [
                    metaDataFile: it,
                    fullPath    : MetadataImportService.getMetaDataFileFullPath(it),
            ]
        }

        List<String> finalPaths = DataFile.findAllByRun(run).collect { DataFile dataFile ->
            lsdfFilesService.getRunDirectory(dataFile) as String ?: dataFile.initialDirectory }.unique().sort()

        return [
                run                : run,
                finalPaths         : finalPaths,
                keys               : keys,
                processParameters  : runService.retrieveProcessParameters(run),
                metaDataFileWrapper: wrappedMetaDataFiles,
                seqTracks          : runService.retrieveSequenceTrackInformation(run),
                errorFiles         : runService.dataFilesWithError(run),
                fastqcLinks        : fastqcResultsService.fastqcLinkMap(run),
        ]
    }

    def list() {
        return [
                seqCenters: seqCenterService.allSeqCenters(),
                tableHeader: RunColumn.values()*.message,
                filterTree : [
                        [name: 'seqCenterSelection', msgcode: 'run.search.seqCenter', type: 'LIST',
                         from: seqCenterService.allSeqCenters(), value: 'name', key: 'id'],
                        [name: 'runSearch', msgcode: 'run.search.run', type: 'TEXT'],
                        [name: 'dateCreatedSelection', msgcode: 'run.search.dateCreated', type: 'DATE'],
                        [name: 'dateExecutedSelection', msgcode: 'run.search.dateExecuted', type: 'DATE'],
                ],
        ]
    }

    def dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        RunFiltering filtering = RunFiltering.fromJSON(params.filtering)

        dataToRender.iTotalRecords = runService.countRun(filtering, cmd.sSearch)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        runService.listRuns(cmd.sortOrder, RunColumn.fromDataTable(cmd.iSortCol_0), filtering, cmd.sSearch).each { run ->
            dataToRender.aaData << [
                    id          : run.id,
                    name        : run.name,
                    seqCenters  : run.seqCenter?.toString()?.toLowerCase(),
                    dateCreated : TimeFormats.DATE.getFormattedDate(run.dateCreated),
                    dateExecuted: TimeFormats.DATE.getFormattedDate(run.dateExecuted),
                    archived    : run.project?.archived,
            ]
        }
        render dataToRender as JSON
    }
}

@TupleConstructor
enum RunColumn {
    RUN('run.list.name', "name"),
    SEQCENTER('run.list.seqCenter', "seqCenter"),
    DATECREATED('run.list.dateCreated', "dateCreated"),
    DATEEXECUTED('run.list.dateExecuted', "dateExecuted",),

    final String message
    final String columnName

    static RunColumn fromDataTable(int column) {
        if (column >= values().size() || column < 0) {
            return RUN
        }
        return values()[column]
    }
}

/**
 * Class describing the various filtering to do on WorkPackage.
 */
class RunFiltering {
    List<String> name = []
    List<Long> seqCenter = []
    List<List<Date>> dateCreated = []
    List<List<Date>> dateExecuted = []

    boolean enabled = false

    static RunFiltering fromJSON(String json) {
        RunFiltering filtering = new RunFiltering()
        if (!json) {
            return filtering
        }
        def slurper = new JsonSlurper()
        slurper.parseText(json).each {
            switch (it.type) {
                case "runSearch":
                    if (it.value && it.value.length() >= 3) {
                        filtering.name << it.value
                        filtering.enabled = true
                    }
                    break
                case "seqCenterSelection":
                    if (it.value.isLong()) {
                        filtering.seqCenter << (it.value as Long)
                        filtering.enabled = true
                    }
                    break
                case "dateCreatedSelection":
                    if (it.value) {
                        SimpleDateFormat sdf = new SimpleDateFormat(TimeFormats.DATE.format, Locale.ENGLISH)
                        Date dateFrom = it.value.start ? sdf.parse(it.value.start) : new Date()
                        Date dateTo   = it.value.end   ? sdf.parse(it.value.end)   : new Date()

                        filtering.dateCreated << [dateFrom, dateTo]
                        filtering.enabled = true
                    }
                    break
                case "dateExecutedSelection":
                    if (it.value) {
                        SimpleDateFormat sdf = new SimpleDateFormat(TimeFormats.DATE.format, Locale.ENGLISH)
                        Date dateFrom = it.value.start ? sdf.parse(it.value.start) : new Date()
                        Date dateTo   = it.value.end   ? sdf.parse(it.value.end)   : new Date()

                        filtering.dateExecuted << [dateFrom, dateTo]
                        filtering.enabled = true
                    }
                    break
            }
        }
        return filtering
    }
}
