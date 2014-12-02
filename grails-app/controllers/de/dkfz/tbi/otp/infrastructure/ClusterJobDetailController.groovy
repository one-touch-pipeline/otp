package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.ngsdata.Individual
import grails.converters.JSON

class ClusterJobDetailController {

    ClusterJobService clusterJobService

    def show () {
        ClusterJob job = ClusterJob.get(params.id)
        Individual individual = clusterJobService.getIndividualByClusterJob(job)
        ['job': job, 'individual': individual]
    }

    def getStatesTimeDistribution () {
        def jobId = params.id
        Map dataToRender = [:]
        def data = clusterJobService.getJobSpecificStatesTimeDistribution(Integer.parseInt(jobId))
        dataToRender.data = data
        render dataToRender as JSON
    }
}
