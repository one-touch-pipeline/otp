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
package de.dkfz.tbi.otp.infrastructure

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import groovy.transform.TupleConstructor
import org.joda.time.LocalDate

import de.dkfz.tbi.otp.utils.DataTableCommand

@Secured("hasRole('ROLE_OPERATOR')")
class ClusterJobGeneralController {

    @TupleConstructor
    enum GeneralClusterJobsColumns {
        CLUSTER_JOB_ID('jobstats.general.table.id', 'clusterJobId'),
        CLUSTER_JOB_NAME('jobstats.general.table.name', 'clusterJobName'),
        EXIT_STATUS('jobstats.general.table.status', 'exitStatus'),
        QUEUED('jobstats.general.table.queued', 'queued'),
        STARTED('jobstats.general.table.started', 'started'),
        ENDED('jobstats.general.table.ended', 'ended'),

        final String message
        final String columnName

        static GeneralClusterJobsColumns fromDataTable(int column) {
            if (column >= values().size() || column < 0) {
                return CLUSTER_JOB_ID
            }
            return values()[column]
        }
    }

    static final String FORMAT_STRING = "yyyy-MM-dd HH:mm:ss"

    ClusterJobService clusterJobService

    Map index() {
        LocalDate date = clusterJobService.latestJobDate ?: new LocalDate()
        return [
                tableHeader: GeneralClusterJobsColumns.values()*.message,
                latestDate: date,
        ]
    }

    JSON findAllClusterJobsByDateBetween(DataTableCommand cmd) {
        Map dataToRender = [:]

        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)
        String sortColumnName = GeneralClusterJobsColumns.fromDataTable(cmd.iSortCol_0).columnName

        List<ClusterJob> clusterJobs = clusterJobService.findAllClusterJobsByDateBetween(
                startDate,
                endDate,
                cmd.sSearch,
                cmd.iDisplayStart,
                cmd.iDisplayLength,
                sortColumnName,
                cmd.sSortDir_0
        )
        List<String> data = clusterJobs.collect {
            return [
                    it.clusterJobId,
                    it.clusterJobName,
                    it.exitStatus?.toString(),
                    it.queued?.toString(FORMAT_STRING),
                    it.started?.toString(FORMAT_STRING),
                    it.ended?.toString(FORMAT_STRING),
                    it.id,
            ]
        }
        dataToRender.iTotalRecords = clusterJobService.countAllClusterJobsByDateBetween(startDate, endDate, cmd.sSearch)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    JSON getAllExitCodes() {
        renderPieDataAsJSON(clusterJobService.&findAllExitCodesByDateBetween)
    }

    JSON getAllExitStatuses() {
        renderPieDataAsJSON(clusterJobService.&findAllExitStatusesByDateBetween)
    }

    JSON getAllFailed() {
        renderDataAsJSON(clusterJobService.&findAllFailedByDateBetween, ['failed'])
    }

    JSON getAllStates() {
        renderDataAsJSON(clusterJobService.&findAllStatesByDateBetween, ['queued', 'started', 'ended'])
    }

    JSON getAllStatesTimeDistribution() {
        Map dataToRender = [:]

        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)

        dataToRender.data = clusterJobService.findAllStatesTimeDistributionByDateBetween(startDate, endDate)

        render dataToRender as JSON
    }

    JSON getAllAvgCoreUsage() {
        renderDataAsJSON(clusterJobService.&findAllAvgCoreUsageByDateBetween, ['cores'])
    }

    JSON getAllMemoryUsage() {
        renderDataAsJSON(clusterJobService.&findAllMemoryUsageByDateBetween, ['memory'])
    }

    private JSON renderDataAsJSON(Closure<Map<String, List>> method, List keys) {
        Map dataToRender = [data: [], labels: [], keys: []]

        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)

        Map results = method(startDate, endDate)
        if (keys.size() > 1) {
            keys.each { k ->
                dataToRender.data << results.data.get(k)
            }
        } else {
            dataToRender.data = results.data
        }
        dataToRender.labels = results.days
        dataToRender.keys = keys

        render dataToRender as JSON
    }

    private JSON renderPieDataAsJSON(Closure<List> method) {
        Map dataToRender = [data: [], labels: []]

        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)

        List results = method(startDate, endDate)
        results.each {
            dataToRender.data << it[1]
            dataToRender.labels << it[0].toString().replace("null", "UNKNOWN") + " (" + it[1].toString() + ")"
        }

        render dataToRender as JSON
    }
}
