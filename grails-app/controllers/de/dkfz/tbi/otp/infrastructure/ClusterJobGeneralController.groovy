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
package de.dkfz.tbi.otp.infrastructure

import grails.converters.JSON
import groovy.transform.TupleConstructor
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.utils.DataTableCommand
import de.dkfz.tbi.util.TimeFormats

import java.time.LocalDate
import java.time.ZonedDateTime

@PreAuthorize("hasRole('ROLE_OPERATOR')")
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

    ClusterJobService clusterJobService

    static allowedMethods = [
            index                          : "GET",
            findAllClusterJobsByDateBetween: "POST",
            getAllExitCodes                : "GET",
            getAllExitStatuses             : "GET",
            getAllFailed                   : "GET",
            getAllStates                   : "GET",
            getAllStatesTimeDistribution   : "GET",
            getAllAvgCoreUsage             : "GET",
            getAllMemoryUsage              : "GET",
    ]

    Map index() {
        LocalDate date = (clusterJobService.latestJobDate ?: ZonedDateTime.now()).toLocalDate()
        return [
                tableHeader: GeneralClusterJobsColumns.values()*.message,
                beginDate  : date.minusWeeks(1),
                latestDate : date,
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
        List<String> data = clusterJobs.collect { ClusterJob job ->
            return [
                    job.clusterJobId,
                    job.clusterJobName,
                    job.exitStatus?.toString(),
                    TimeFormats.DATE_TIME.getFormattedZonedDateTime(job.queued),
                    TimeFormats.DATE_TIME.getFormattedZonedDateTime(job.started),
                    TimeFormats.DATE_TIME.getFormattedZonedDateTime(job.ended),
                    job.id,
            ]
        }
        dataToRender.iTotalRecords = clusterJobService.countAllClusterJobsByDateBetween(startDate, endDate, cmd.sSearch)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        return render(dataToRender as JSON)
    }

    JSON getAllExitCodes() {
        return renderPieDataAsJSON(clusterJobService.&findAllExitCodesByDateBetween)
    }

    JSON getAllExitStatuses() {
        return renderPieDataAsJSON(clusterJobService.&findAllExitStatusesByDateBetween)
    }

    JSON getAllFailed() {
        return renderLineDataAsJSON(clusterJobService.&findAllFailedByDateBetween, ['failed'])
    }

    JSON getAllStates() {
        return renderLineDataAsJSON(clusterJobService.&findAllStatesByDateBetween, ['queued', 'started', 'ended'])
    }

    JSON getAllStatesTimeDistribution() {
        return renderPieDataAsJSON(clusterJobService.&findAllStatesTimeDistributionByDateBetween)
    }

    JSON getAllAvgCoreUsage() {
        return renderLineDataAsJSON(clusterJobService.&findAllAvgCoreUsageByDateBetween, ['cores'])
    }

    JSON getAllMemoryUsage() {
        return renderLineDataAsJSON(clusterJobService.&findAllMemoryUsageByDateBetween, ['memory'])
    }

    private JSON renderLineDataAsJSON(Closure<Map<String, List>> method, List keys) {
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

        return render(dataToRender as JSON)
    }

    private JSON renderPieDataAsJSON(Closure<List> method) {
        Map dataToRender = [data: [], labels: [], keys: []]

        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)

        List results = method(startDate, endDate)
        results.each {
            dataToRender.data << it[1]
            if (it.size() == 3) {
                dataToRender.keys << it[0].toString().replace("null", "UNKNOWN")
                dataToRender.labels << it[2]
            } else {
                dataToRender.keys << it[0].toString().replace("null", "UNKNOWN") + " (" + it[1].toString() + ")"
            }
        }

        return render(dataToRender as JSON)
    }
}
