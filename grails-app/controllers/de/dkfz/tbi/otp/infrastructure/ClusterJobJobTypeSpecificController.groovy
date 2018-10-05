package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.ngsdata.SeqType
import grails.converters.JSON

import org.joda.time.LocalDate
import org.joda.time.Period
import org.joda.time.format.PeriodFormat

class ClusterJobJobTypeSpecificController {

    static final int GIGABASES_TO_BASES = 1000000000

    ClusterJobService clusterJobService

    def index() {
        LocalDate date = clusterJobService.getLatestJobDate() ?: new LocalDate()
        def jobClasses = clusterJobService.findAllJobClassesByDateBetween(date, date)
        return [jobClasses: jobClasses, latestDate: date?.toString("yyyy-MM-dd")]
    }

    def getJobTypeSpecificAvgCoreUsage() {
        renderDataAsJSON(clusterJobService.&findJobClassAndSeqTypeSpecificAvgCoreUsageByDateBetween)
    }

    def getJobTypeSpecificAvgMemory() {
        renderDataAsJSON(clusterJobService.&findJobClassAndSeqTypeSpecificAvgMemoryByDateBetween)
    }

    def getJobTypeSpecificStatesTimeDistribution() {
        Map dataToRender = [:]

        def (startDate, endDate, seqType) = parseParams()

        Float referenceSize = Float.parseFloat(params.bases)
        Integer coverage = Integer.parseInt(params.coverage)
        Long basesToBeNormalized = Math.round(referenceSize * coverage * GIGABASES_TO_BASES)

        def data = clusterJobService.findJobClassAndSeqTypeSpecificAvgStatesTimeDistributionByDateBetween(params.jobClass, seqType, startDate, endDate, basesToBeNormalized)
        dataToRender.data = ['avgQueue': PeriodFormat.getDefault().print(new Period(data.avgQueue)), 'avgProcess': PeriodFormat.getDefault().print(new Period(data.avgProcess))]

        render dataToRender as JSON
    }

    def getJobTypeSpecificCoverageStatistics() {
        Map dataToRender = [:]

        def (startDate, endDate, seqType) = parseParams()

        Float referenceSize = Float.parseFloat(params.bases)

        def data = clusterJobService.findJobClassAndSeqTypeSpecificCoverages(params.jobClass, seqType, startDate, endDate, referenceSize * GIGABASES_TO_BASES)
        dataToRender.data = [
                'minCov'   : data.minCov ? "${data.minCov.round(2)} (min)" : "",
                'maxCov'   : data.maxCov ? "${data.maxCov.round(2)} (max)" : "",
                'avgCov'   : data.avgCov ? "${data.avgCov?.round(2)} (avg)" : "",
                'medianCov': data.medianCov ? "${data.medianCov?.round(2)} (median)" : "",
        ]

        render dataToRender as JSON
    }

    def getJobClassesByDate() {
        Map dataToRender = [:]

        dataToRender.data = clusterJobService.findAllJobClassesByDateBetween(new LocalDate(params.from), new LocalDate(params.to))

        render dataToRender as JSON
    }

    def getSeqTypesByJobClass() {
        Map dataToRender = [:]

        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)
        String jobClass = params.jobClass

        dataToRender.data = clusterJobService.findJobClassSpecificSeqTypesByDateBetween(jobClass, startDate, endDate)

        render dataToRender as JSON
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

        render dataToRender as JSON
    }

    def getJobTypeSpecificWalltimes() {
        renderDiagramDataAsJSON(clusterJobService.&findJobClassAndSeqTypeSpecificWalltimesByDateBetween)
    }

    private renderDataAsJSON(Closure method) {
        Map dataToRender = [:]

        def (startDate, endDate, seqType) = parseParams()

        dataToRender.data = method(params.jobClass, seqType, startDate, endDate)

        render dataToRender as JSON
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

        render dataToRender as JSON
    }

    private renderDiagramDataAsJSON(Closure method) {
        Map dataToRender = [data: [], labels: [], xMax: null]

        def (startDate, endDate, seqType) = parseParams()

        Map results = method(params.jobClass, seqType, startDate, endDate)
        dataToRender.data = results.data
        dataToRender.xMax = results.xMax
        dataToRender.labels = results.labels

        render dataToRender as JSON
    }

    private List parseParams() {
        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)
        SeqType seqType = (params.seqType != "null") ? SeqType.get(Long.parseLong(params.seqType)) : null

        return [startDate, endDate, seqType]
    }

}
