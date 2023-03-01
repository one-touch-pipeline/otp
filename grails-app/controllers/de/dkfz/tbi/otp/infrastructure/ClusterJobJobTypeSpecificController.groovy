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
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.util.TimeFormats
import de.dkfz.tbi.util.TimeUtils

import java.time.Duration
import java.time.LocalDate

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class ClusterJobJobTypeSpecificController {

    static final int GIGABASES_TO_BASES = 1000000000

    ClusterJobService clusterJobService
    SeqTypeService seqTypeService

    static allowedMethods = [
            index                                   : "GET",
            getJobTypeSpecificAvgCoreUsage          : "GET",
            getJobTypeSpecificAvgMemory             : "GET",
            getJobTypeSpecificStatesTimeDistribution: "GET",
            getJobTypeSpecificCoverageStatistics    : "GET",
            getJobClassesByDate                     : "GET",
            getSeqTypesByJobClass                   : "GET",
            getJobTypeSpecificExitCodes             : "GET",
            getJobTypeSpecificExitStatuses          : "GET",
            getJobTypeSpecificStates                : "GET",
            getJobTypeSpecificWalltimes             : "GET",
    ]

    def index() {
        LocalDate date = clusterJobService.latestJobDate?.toLocalDate() ?: LocalDate.now()
        def jobClasses = clusterJobService.findAllJobClassesByDateBetween(date, date)
        return [
                jobClasses: jobClasses,
                beginDate : TimeFormats.DATE.getFormattedLocalDate(date.minusWeeks(1)),
                latestDate: TimeFormats.DATE.getFormattedLocalDate(date),
        ]
    }

    def getJobTypeSpecificAvgCoreUsage() {
        renderDataAsJSON(clusterJobService.&findJobClassAndSeqTypeSpecificAvgCoreUsageByDateBetween)
    }

    def getJobTypeSpecificAvgMemory() {
        renderDataAsJSON(clusterJobService.&findJobClassAndSeqTypeSpecificAvgMemoryByDateBetween)
    }

    def getJobTypeSpecificStatesTimeDistribution() {
        Map dataToRender = [:]

        def (LocalDate startDate, LocalDate endDate, SeqType seqType) = parseParams()

        Float referenceSize = Float.parseFloat(params.bases)
        Integer coverage = Integer.parseInt(params.coverage)
        Long basesToBeNormalized = Math.round(referenceSize * coverage * GIGABASES_TO_BASES)

        def data = clusterJobService.findSpecificAvgStatesTimeDistribution(params.jobClass, seqType, startDate, endDate, basesToBeNormalized)
        dataToRender.data = [
                'avgQueue'  : TimeUtils.getFormattedDuration(Duration.ofMillis(data.avgQueue)),
                'avgProcess': TimeUtils.getFormattedDuration(Duration.ofMillis(data.avgProcess)),
        ]

        render(dataToRender as JSON)
    }

    def getJobTypeSpecificCoverageStatistics() {
        Map dataToRender = [:]

        def (LocalDate startDate, LocalDate endDate, SeqType seqType) = parseParams()

        Float referenceSize = Float.parseFloat(params.bases)

        def data = clusterJobService.findJobClassAndSeqTypeSpecificCoverages(params.jobClass, seqType, startDate, endDate, referenceSize * GIGABASES_TO_BASES)
        dataToRender.data = [
                'minCov'   : data.minCov ? "${data.minCov.round(2)} (min)" : "",
                'maxCov'   : data.maxCov ? "${data.maxCov.round(2)} (max)" : "",
                'avgCov'   : data.avgCov ? "${data.avgCov?.round(2)} (avg)" : "",
                'medianCov': data.medianCov ? "${data.medianCov?.round(2)} (median)" : "",
        ]

        render(dataToRender as JSON)
    }

    def getJobClassesByDate() {
        Map dataToRender = [:]

        dataToRender.data = clusterJobService.findAllJobClassesByDateBetween(LocalDate.parse(params.from), LocalDate.parse(params.to))

        render(dataToRender as JSON)
    }

    def getSeqTypesByJobClass() {
        Map dataToRender = [:]

        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)
        String jobClass = params.jobClass

        dataToRender.data = clusterJobService.findJobClassSpecificSeqTypesByDateBetween(jobClass, startDate, endDate)

        render(dataToRender as JSON)
    }

    def getJobTypeSpecificExitCodes() {
        renderPieDataAsJSON(clusterJobService.&findJobClassAndSeqTypeSpecificExitCodesByDateBetween)
    }

    def getJobTypeSpecificExitStatuses() {
        renderPieDataAsJSON(clusterJobService.&findJobClassAndSeqTypeSpecificExitStatusesByDateBetween)
    }

    def getJobTypeSpecificStates() {
        Map dataToRender = [data: [], labels: [], keys: []]

        def (startDate, endDate, seqType) = parseParams()

        def result = clusterJobService.findJobClassAndSeqTypeSpecificStatesByDateBetween(params.jobClass, seqType, startDate, endDate)
        def keys = ['queued', 'started', 'ended']
        keys.each { k ->
            dataToRender.data << result.data.get(k)
        }
        dataToRender.labels = result.get("days")
        dataToRender.keys = keys

        render(dataToRender as JSON)
    }

    def getJobTypeSpecificWalltimes() {
        renderDiagramDataAsJSON(clusterJobService.&findJobClassAndSeqTypeSpecificWalltimesByDateBetween)
    }

    private renderDataAsJSON(Closure method) {
        Map dataToRender = [:]

        def (startDate, endDate, seqType) = parseParams()

        dataToRender.data = method(params.jobClass, seqType, startDate, endDate)

        render(dataToRender as JSON)
    }

    private renderPieDataAsJSON(Closure method) {
        Map dataToRender = [data: [], labels: []]

        def (startDate, endDate, seqType) = parseParams()

        List results = method(params.jobClass, seqType, startDate, endDate)
        results.each { result ->
            def (labels, data) = ["${result[0]} (${result[1]})", result[1]]
            dataToRender.labels << labels
            dataToRender.data << data
        }

        render(dataToRender as JSON)
    }

    private renderDiagramDataAsJSON(Closure method) {
        Map dataToRender = [data: [], labels: [], xMax: null]

        def (startDate, endDate, seqType) = parseParams()

        Map results = method(params.jobClass, seqType, startDate, endDate)
        dataToRender.data = results.data
        dataToRender.xMax = results.xMax
        dataToRender.labels = results.labels

        render(dataToRender as JSON)
    }

    private List parseParams() {
        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)
        SeqType seqType = (params.seqType == "null") ? null : seqTypeService.findById(Long.parseLong(params.seqType))

        return [startDate, endDate, seqType]
    }
}
