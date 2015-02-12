package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.utils.DataTableCommand
import grails.converters.JSON
import org.joda.time.LocalDate

class ClusterJobGeneralController {


    public enum GeneralClusterJobsColumns {
        CLUSTER_JOB_ID(0, 'clusterJobId'),
        CLUSTER_JOB_NAME(1, 'clusterJobName'),
        EXIT_STATUS(2, 'exitStatus'),
        QUEUED(3, 'queued'),
        STARTED(4, 'started'),
        ENDED(5, 'ended')

        private final int index

        private final String columnName

        GeneralClusterJobsColumns(int index, String column) {
            this.index = index
            this.columnName = column
        }

        static GeneralClusterJobsColumns fromDataTable(int column) {
            return values().find {it.index == column} ?: CLUSTER_JOB_ID
        }
    }



    public static final String FORMAT_STRING = "yyyy-MM-dd HH:mm:ss"

    ClusterJobService clusterJobService

    def index() {
        def date = clusterJobService.getLatestJobDate()
        return [latestDate: date]
    }

    def findAllClusterJobsByDateBetween(DataTableCommand cmd) {
        Map dataToRender = [:]

        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)
        String sortColumnName = GeneralClusterJobsColumns.fromDataTable(cmd.iSortCol_0).columnName

        def clusterJobs = clusterJobService.findAllClusterJobsByDateBetween(startDate, endDate, cmd.sSearch, cmd.iDisplayStart, cmd.iDisplayLength, sortColumnName, cmd.sSortDir_0)
        def data = clusterJobs.collect {
            return [it.clusterJobId, it.clusterJobName, it.exitStatus?.toString(), it.queued?.toString(FORMAT_STRING), it.started?.toString(FORMAT_STRING), it.ended?.toString(FORMAT_STRING), it.id]
        }
        dataToRender.iTotalRecords = clusterJobService.countAllClusterJobsByDateBetween(startDate, endDate, cmd.sSearch)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    def getAllExitCodes() {
        renderPieDataAsJSON(clusterJobService.&findAllExitCodesByDateBetween)
    }

    def getAllExitStatuses() {
        renderPieDataAsJSON(clusterJobService.&findAllExitStatusesByDateBetween)
    }

    def getAllFailed() {
        renderDataAsJSON(clusterJobService.&findAllFailedByDateBetween, ['failed'])
    }

    def getAllStates() {
        renderDataAsJSON(clusterJobService.&findAllStatesByDateBetween, ['queued', 'started', 'ended'])
    }

    def getAllStatesTimeDistribution() {
        Map dataToRender = [:]

        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)

        dataToRender.data = clusterJobService.findAllStatesTimeDistributionByDateBetween(startDate, endDate)

        render dataToRender as JSON
    }

    def getAllAvgCoreUsage() {
        renderDataAsJSON(clusterJobService.&findAllAvgCoreUsageByDateBetween, ['cores'])
    }

    def getAllMemoryUsage() {
        renderDataAsJSON(clusterJobService.&findAllMemoryUsageByDateBetween, ['memory'])
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

        LocalDate startDate = LocalDate.parse(params.from)
        LocalDate endDate = LocalDate.parse(params.to)

        def results = method(startDate, endDate)
        results.each {
            dataToRender.data << it[1]
            dataToRender.labels << it[0].toString().replace("null", "UNKNOWN") + " (" + it[1].toString() + ")"
        }

        render dataToRender as JSON
    }
}
