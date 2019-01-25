package de.dkfz.tbi.otp.infrastructure

import grails.converters.JSON
import org.joda.time.Period
import org.joda.time.format.PeriodFormat

import de.dkfz.tbi.otp.ngsdata.Individual

class ClusterJobDetailController {

    ClusterJobService clusterJobService

    def show() {
        ClusterJob clusterJob = ClusterJob.get(params.id)
        Individual individual = clusterJob ? clusterJobService.findProcessParameterObjectByClusterJob(clusterJob)?.individual : null

        return [
                'job'       : clusterJob,
                'individual': individual,
                'NA'        : ClusterJob.NOT_AVAILABLE
        ]
    }

    def getStatesTimeDistribution() {
        Map dataToRender = [:]

        Map<String, Map<String, Long>> data = clusterJobService.findJobSpecificStatesTimeDistributionByJobId(params.id as Long)

        dataToRender.data = ["queue", "process"].collectEntries {
            return [it, [
                    percentage: data."${it}".percentage,
                    time:       applyPeriodFormat(data."${it}".ms as Long)
                ]
            ]
        }

        render dataToRender as JSON
    }

    private String applyPeriodFormat(Long ms) {
        return PeriodFormat.getDefault().print(new Period(ms))
    }
}
