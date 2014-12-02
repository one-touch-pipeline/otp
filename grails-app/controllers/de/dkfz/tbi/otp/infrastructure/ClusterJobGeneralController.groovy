package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.utils.DataTableCommand
import grails.converters.JSON
import org.joda.time.LocalDate

class ClusterJobGeneralController {

    ClusterJobService clusterJobService

    def index() {
        def date = clusterJobService.getLatestJobDate()
        def today = new LocalDate().toString()
        return [latestDate: date, today: today]
    }

    def getAllClusterJobs(DataTableCommand cmd) {
        def data = []
        def clusterJobs = ClusterJob.getAll()
        for (c in clusterJobs) {
            def job = [c.clusterJobId, c.clusterJobName, c.exitStatus?.toString(), c.queued?.toString("yyyy-MM-dd HH:mm:ss"), c.started?.toString("yyyy-MM-dd HH:mm:ss"), c.ended?.toString("yyyy-MM-dd HH:mm:ss"), c.id]
            data.push(job)
        }
        Map dataToRender = [:]
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    def getAllExitCodes() {
        Map dataToRender = [:]
        def exitCodes = clusterJobService.getAllExitCodes()
        def data = []
        def labels = []
        for (exitCode in exitCodes) {
            labels.push(exitCode[0].toString().replace("null", "UNKNOWN") + " (" + exitCode[1].toString() + ")")
            data.push(exitCode[1])
        }
        dataToRender.data = data
        dataToRender.labels = labels
        render dataToRender as JSON
    }

    def getAllExitStatuses() {
        Map dataToRender = [:]
        def exitStatuses = clusterJobService.getAllExitStatuses()
        def data = []
        def labels = []
        for (exitStatus in exitStatuses) {
            labels.push(exitStatus[0].toString().replace("null", "UNKNOWN") + " (" + exitStatus[1].toString() + ")")
            data.push(exitStatus[1])
        }
        dataToRender.data = data
        dataToRender.labels = labels
        render dataToRender as JSON
    }

    def getAllFailed() {
        Map dataToRender = [:]
        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)
        def data = []
        def labels = []
        def keys = ['failed']
        def result = clusterJobService.getAllFailedByDate(startDate, endDate)
        data = result.get("data")
        labels = result.get("days")
        dataToRender.data = data
        dataToRender.labels = labels
        dataToRender.keys = keys
        render dataToRender as JSON
    }

    def getAllStates() {
        Map dataToRender = [:]
        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)
        def data = []
        def labels = []
        def keys = ['queued', 'started', 'ended']
        def result = clusterJobService.getAllStatesByDate(startDate, endDate)
        data.push(result.get("data").get("queued"))
        data.push(result.get("data").get("started"))
        data.push(result.get("data").get("ended"))
        labels = result.get("days")
        dataToRender.data = data
        dataToRender.labels = labels
        dataToRender.keys = keys
        render dataToRender as JSON
    }

    def getAllStatesTimeDistribution() {
        Map dataToRender = [:]
        def result = clusterJobService.getAllStatesTimeDistribution()
        dataToRender.data = result
        render dataToRender as JSON
    }

    def getAllAvgCoreUsage() {
        Map dataToRender = [:]
        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)
        def data = []
        def labels = []
        def keys = ['cores']
        def result = clusterJobService.getAllAvgCoreUsageByDate(startDate, endDate)
        data = result.get("data")
        labels = result.get("days")
        dataToRender.data = data
        dataToRender.labels = labels
        dataToRender.keys = keys
        render dataToRender as JSON
    }

    def getAllMemoryUsage() {
        Map dataToRender = [:]
        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)
        def data = []
        def labels = []
        def keys = ['memory']
        def result = clusterJobService.getAllMemoryUsageByDate(startDate, endDate)
        data = result.get("data")
        labels = result.get("days")
        dataToRender.data = data
        dataToRender.labels = labels
        dataToRender.keys = keys
        render dataToRender as JSON
    }
}
