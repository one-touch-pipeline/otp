package de.dkfz.tbi.otp.job.jobs.mergedBamDiscovery

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.ngsdata.MergingService
import de.dkfz.tbi.otp.ngsdata.Individual
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component("mergedBamDiscoveryStartJob")
@Scope("singleton")
class MergedBamDiscoveryWorkkflow extends AbstractStartJobImpl  {

    @Autowired
    MergingService mergingService

    @Scheduled(fixedDelay=1800000l)
    void execute() {
        if (!getExecutionPlan() || !getExecutionPlan().enabled) {
            println "Merged Bam discovery workflow not active"
            return
        }
        println "Discovering merged bam-files"
        println getExecutionPlan()
        List<Long> indIds = Individual.list().collect { it.id }
        for(long indId in indIds) {
            println indId
            mergingService.discoverMergedBams(indId)
        }
    }

    @Override
    String getJobExecutionPlanName() {
        return "mergedBamDiscoveryWorkflow"
    }
}
