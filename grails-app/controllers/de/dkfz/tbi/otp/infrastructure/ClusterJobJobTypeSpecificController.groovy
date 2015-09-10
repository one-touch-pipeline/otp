package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.ngsdata.SeqType
import grails.converters.JSON

import org.joda.time.LocalDate
import org.joda.time.Period
import org.joda.time.format.PeriodFormat

class ClusterJobJobTypeSpecificController {

    ClusterJobService clusterJobService

    def index() {
        LocalDate date = clusterJobService.getLatestJobDate()
        def jobClasses = clusterJobService.findAllJobClassesByDateBetween(date, date)
        return [jobClasses: jobClasses, latestDate: date.toString("yyyy-MM-dd")]
    }

    def getJobTypeSpecificAvgCoreUsage() {
        renderDataAsJSON(clusterJobService.&findJobClassAndSeqTypeSpecificAvgCoreUsageByDateBetween)
    }

    def getJobTypeSpecificAvgMemory() {
        renderDataAsJSON(clusterJobService.&findJobClassAndSeqTypeSpecificAvgMemoryByDateBetween)
    }

    def getJobTypeSpecificStatesTimeDistribution() {
        Map dataToRender = [:]

        def (startDate, endDate, seqType, multiplexing, xten) = parseParams()

        def data = clusterJobService.findJobClassAndSeqTypeSpecificAvgStatesTimeDistributionByDateBetween(params.jobClass, seqType, startDate, endDate, multiplexing, xten)
        dataToRender.data = ['avgQueue': PeriodFormat.getDefault().print(new Period(data.avgQueue)), 'avgProcess': PeriodFormat.getDefault().print(new Period(data.avgProcess))]

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

        def (startDate, endDate, seqType, multiplexing, xten) = parseParams()

        def result = clusterJobService.findJobClassAndSeqTypeSpecificStatesByDateBetween(params.jobClass, seqType, startDate, endDate, multiplexing, xten)
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

    def getJobTypeSpecificMemories() {
        renderDiagramDataAsJSON(clusterJobService.&findJobClassAndSeqTypeSpecificMemoriesByDateBetween)
    }

    private renderDataAsJSON (Closure method) {
        Map dataToRender = [:]

        def (startDate, endDate, seqType, multiplexing, xten) = parseParams()

        dataToRender.data = method(params.jobClass, seqType, startDate, endDate, multiplexing, xten)

        render dataToRender as JSON
    }

    private renderPieDataAsJSON (Closure method) {
        Map dataToRender = [data: [], labels: []]

        def (startDate, endDate, seqType, multiplexing, xten) = parseParams()

        List results = method(params.jobClass, seqType, startDate, endDate, multiplexing, xten)
        results.each { result ->
            def (labels, data) = ["${result[0]} (${result[1]})", result[1]]
            dataToRender.labels << labels
            dataToRender.data << data
        }

        render dataToRender as JSON
    }

    private renderDiagramDataAsJSON (Closure method) {
        Map dataToRender = [data: [], labels: [], xMax: null]

        def (startDate, endDate, seqType, multiplexing, xten) = parseParams()

        Map results = method(params.jobClass, seqType, startDate, endDate, multiplexing, xten)
        dataToRender.data = results.data
        dataToRender.xMax = results.xMax
        dataToRender.labels = results.labels

        render dataToRender as JSON
    }

    private List parseParams() {
        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)
        SeqType seqType = SeqType.get(Long.parseLong(params.seqType))
        boolean multiplexing = Boolean.valueOf(params.multiplexing)
        boolean xten = Boolean.valueOf(params.xten)

        return [startDate, endDate, seqType, multiplexing, xten]
    }
}
