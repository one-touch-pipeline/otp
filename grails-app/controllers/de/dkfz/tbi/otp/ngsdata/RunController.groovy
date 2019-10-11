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
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.ngsqc.FastqcResultsService
import de.dkfz.tbi.otp.utils.DataTableCommand

class RunController {

    ProjectService projectService
    LsdfFilesService lsdfFilesService
    RunService runService
    FastqcResultsService fastqcResultsService
    SeqCenterService seqCenterService

    def display = {
        redirect(action: "show", id: params.id)
    }

    def show = {
        params.id = params.id ?: "0"
        Run run = runService.getRun(params.id)
        if (!run) {
            response.sendError(404)
            return
        }
        List<MetaDataKey> keys = []
        keys[0] = MetaDataKey.findByName(MetaDataColumn.SAMPLE_ID.name())
        keys[1] = MetaDataKey.findByName(MetaDataColumn.WITHDRAWN.name())

        return [
                run              : run,
                finalPaths       : lsdfFilesService.getAllPathsForRun(run, true),
                keys             : keys,
                processParameters: runService.retrieveProcessParameters(run),
                metaDataFiles    : runService.retrieveMetaDataFiles(run),
                seqTracks        : runService.retrieveSequenceTrackInformation(run),
                errorFiles       : runService.dataFilesWithError(run),
                fastqcLinks      : fastqcResultsService.fastqcLinkMap(run),
        ]
    }

    def list = {
        Map retValue = [
                seqCenters: seqCenterService.allSeqCenters(),
                tableHeader: RunColumn.values()*.message,
                projects  : projectService.getAllProjects().size(),
        ]
        return retValue
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
                    dateCreated : run.dateCreated?.format("yyyy-MM-dd"),
                    dateExecuted: run.dateExecuted?.format("yyyy-MM-dd"),
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
                        int start_day = it.value.start_day as int
                        int start_month = it.value.start_month as int
                        int start_year = it.value.start_year as int
                        Date dateFrom = new Date(start_year - 1900, start_month - 1, start_day)
                        int end_day = it.value.end_day as int
                        int end_month = it.value.end_month as int
                        int end_year = it.value.end_year as int
                        Date dateTo = new Date(end_year - 1900, end_month - 1, end_day + 1)
                        filtering.dateCreated << [dateFrom, dateTo]
                        filtering.enabled = true
                    }
                    break
                case "dateExecutedSelection":
                    if (it.value) {
                        int start_day = it.value.start_day as int
                        int start_month = it.value.start_month as int
                        int start_year = it.value.start_year as int
                        Date dateFrom = new Date(start_year - 1900, start_month - 1, start_day)
                        int end_day = it.value.end_day as int
                        int end_month = it.value.end_month as int
                        int end_year = it.value.end_year as int
                        Date dateTo = new Date(end_year - 1900, end_month - 1, end_day + 1)
                        filtering.dateExecuted << [dateFrom, dateTo]
                        filtering.enabled = true
                    }
                    break
            }
        }
        return filtering
    }
}
