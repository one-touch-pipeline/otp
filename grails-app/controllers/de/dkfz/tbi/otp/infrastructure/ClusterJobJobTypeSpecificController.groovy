package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.ngsdata.SeqType
import grails.converters.JSON

import org.joda.time.LocalDate

class ClusterJobJobTypeSpecificController {

    ClusterJobService clusterJobService

    def index() {
        def date = clusterJobService.getLatestJobDate()
        def today = new LocalDate().toString()
        def jobClasses = clusterJobService.getJobClasses(date, today)
        return [jobClasses: jobClasses, latestDate: date, today: today]
    }

    def getJobTypeSpecificAvgCoreUsage() {
        renderDataAsJSON(clusterJobService.&getJobTypeSpecificAvgCoreUsage)
    }

    def getJobTypeSpecificAvgMemory() {
        renderDataAsJSON(clusterJobService.&getJobTypeSpecificAvgMemory)
    }

    def getJobTypeSpecificStatesTimeDistribution() {
        renderDataAsJSON(clusterJobService.&getJobTypeSpecificAvgStatesTimeDistribution)
    }

    def getJobClassesByDate() {
        Map dataToRender = [:]

        def jobClasses = clusterJobService.getJobClasses(params.from, params.to)
        dataToRender.data = jobClasses

        render dataToRender as JSON
    }

    def getSeqTypesByJobClass() {
        Map dataToRender = [:]

        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)
        String jobClass = params.jobClass

        def seqTypes = clusterJobService.getSeqTypes(jobClass, startDate, endDate)
        dataToRender.data = seqTypes

        render dataToRender as JSON
    }

    def getJobTypeSpecificExitCodes() {
        renderPieDataAsJSON(clusterJobService.&getJobTypeSpecificExitCodes)
    }

    def getJobTypeSpecificExitStatuses() {
        renderPieDataAsJSON(clusterJobService.&getJobTypeSpecificExitStatuses)
    }

    def getJobTypeSpecificStates() {
        Map dataToRender = [data: [], labels: [], keys: []]

        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)
        SeqType seqType = SeqType.get(Long.parseLong(params.seqType))

        def result = clusterJobService.getJobTypeSpecificStates(params.jobClass, seqType, startDate, endDate)
        def keys = ['queued', 'started', 'ended']
        keys.each { k ->
            dataToRender.data << result.data.get(k)
        }
        dataToRender.labels = result.get("days")
        dataToRender.keys = keys

        render dataToRender as JSON
    }

    def getJobTypeSpecificWalltimes() {
        renderDiagramDataAsJSON(clusterJobService.&getJobTypeSpecificWalltimes)
    }

    def getJobTypeSpecificMemories() {
        renderDiagramDataAsJSON(clusterJobService.&getJobTypeSpecificMemories)
    }

    private renderDataAsJSON (Closure method) {
        Map dataToRender = [:]

        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)
        SeqType seqType = SeqType.get(Long.parseLong(params.seqType))

        def result = method(params.jobClass, seqType, startDate, endDate)
        dataToRender.data = result

        render dataToRender as JSON
    }

    private renderPieDataAsJSON (Closure method) {
        Map dataToRender = [data: [], labels: []]

        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)
        SeqType seqType = SeqType.get(Long.parseLong(params.seqType))

        List results = method(params.jobClass, seqType, startDate, endDate)
        results.each { result ->
            def (labels, data) = ["${result[0]} (${result[1]})", result[1]]
            dataToRender.labels << labels
            dataToRender.data << data
        }

        render dataToRender as JSON
    }

    private renderDiagramDataAsJSON (Closure method) {
        Map dataToRender = [data: [], labels: [], xMax: null]

        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)
        SeqType seqType = SeqType.get(Long.parseLong(params.seqType))

        Map results = method(params.jobClass, seqType, startDate, endDate)
        dataToRender.data = results.data
        dataToRender.xMax = results.xMax
        dataToRender.labels = results.labels

        render dataToRender as JSON
    }
}
