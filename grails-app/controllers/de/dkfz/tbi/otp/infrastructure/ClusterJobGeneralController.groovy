package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.utils.DataTableCommand
import grails.converters.JSON
import org.joda.time.LocalDate

class ClusterJobGeneralController {

    public static final String FORMAT_STRING = "yyyy-MM-dd HH:mm:ss"

    ClusterJobService clusterJobService

    def index() {
        def date = clusterJobService.getLatestJobDate()
        def today = new LocalDate().toString()
        return [latestDate: date, today: today]
    }

    def getAllClusterJobs(DataTableCommand cmd) {
        Map dataToRender = [:]
        def clusterJobs = ClusterJob.getAll()
        def data = clusterJobs.collect {
            return [it.clusterJobId, it.clusterJobName, it.exitStatus?.toString(), it.queued?.toString(FORMAT_STRING), it.started?.toString(FORMAT_STRING), it.ended?.toString(FORMAT_STRING), it.id]
        }
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    def getAllExitCodes() {
        renderPieDataAsJSON(clusterJobService.&getAllExitCodes)
    }

    def getAllExitStatuses() {
        renderPieDataAsJSON(clusterJobService.&getAllExitStatuses)
    }

    def getAllFailed() {
        renderDataAsJSON(clusterJobService.&getAllFailedByDate, ['failed'])
    }

    def getAllStates() {
        renderDataAsJSON(clusterJobService.&getAllStatesByDate, ['queued', 'started', 'ended'])
    }

    def getAllStatesTimeDistribution() {
        Map dataToRender = [:]
        def result = clusterJobService.getAllStatesTimeDistribution()
        dataToRender.data = result
        render dataToRender as JSON
    }

    def getAllAvgCoreUsage() {
        renderDataAsJSON(clusterJobService.&getAllAvgCoreUsageByDate, ['cores'])
    }

    def getAllMemoryUsage() {
        renderDataAsJSON(clusterJobService.&getAllMemoryUsageByDate, ['memory'])
    }

    private renderDataAsJSON (Closure method, keys) {
        Map dataToRender = [data: [], labels: [], keys: []]
        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)
        def results = method(startDate, endDate)
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

    private renderPieDataAsJSON (Closure method) {
        Map dataToRender = [data: [], labels: []]
        def results = method()
        results.each {
            dataToRender.data << it[1]
            dataToRender.labels << it[0].toString().replace("null", "UNKNOWN") + " (" + it[1].toString() + ")"
        }
        render dataToRender as JSON
    }
}
