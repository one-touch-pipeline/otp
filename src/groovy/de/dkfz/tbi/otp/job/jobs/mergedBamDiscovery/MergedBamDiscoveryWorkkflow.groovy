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

    @Scheduled(fixedDelay=60000l)
    void execute() {
        if (!getExecutionPlan() || !getExecutionPlan().enabled) {
            println "Merged Bam discovery workflow not active"
            return
        }
        println "Discovering merged bam-files"
        println getExecutionPlan()
        List<Individual> inds = Individual.list()
        for(Individual ind in inds) {
            println ind
            mergingService.discoverMergedBams(ind)
        }
    }
}