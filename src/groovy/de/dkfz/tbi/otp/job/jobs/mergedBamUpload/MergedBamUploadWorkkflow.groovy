package de.dkfz.tbi.otp.job.jobs.mergedBamUpload

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.ngsdata.MergingService
import de.dkfz.tbi.otp.ngsdata.Individual
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired


@Component("mergedBamUploadStartJob")
@Scope("singleton")
class MergedBamUploadWorkkflow extends AbstractStartJobImpl  {

    @Autowired
    MergingService mergingService

    @Scheduled(fixedRate=600000l)
    void execute() {
        if (!getExecutionPlan() || !getExecutionPlan().enabled) {
            return
        }
        List<Individual> inds = Individual.list()
        for(Individual ind in inds) {
            println ind
            mergingService.discoverMergedBams(ind)
        }
    }
}