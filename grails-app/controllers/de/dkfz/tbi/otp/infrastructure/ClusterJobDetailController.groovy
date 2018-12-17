package de.dkfz.tbi.otp.infrastructure

import grails.converters.JSON
import org.joda.time.Period
import org.joda.time.format.PeriodFormat

import de.dkfz.tbi.otp.ngsdata.Individual

class ClusterJobDetailController {

    ClusterJobService clusterJobService

    def show () {
        ClusterJob job = ClusterJob.get(params.id)
        Individual individual = clusterJobService.findProcessParameterObjectByClusterJob(job)?.individual

        ['job': job, 'individual': individual]
    }

    def getStatesTimeDistribution () {
        Map dataToRender = [:]

        def data = clusterJobService.findJobSpecificStatesTimeDistributionByJobId(Integer.parseInt(params.id))

        dataToRender.data = [queue: [data.queue.first(), PeriodFormat.getDefault().print(new Period(data.queue.last()))],
                             process: [data.process.first(), PeriodFormat.getDefault().print(new Period(data.process.last()))]]

        render dataToRender as JSON
    }
}
