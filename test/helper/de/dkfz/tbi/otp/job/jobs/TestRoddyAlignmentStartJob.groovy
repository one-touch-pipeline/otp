package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.jobs.roddyAlignment.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component('testRoddyAlignmentStartJob')
@Scope('singleton')
class TestRoddyAlignmentStartJob extends RoddyAlignmentStartJob {

    JobExecutionPlan jep

    @Override
    JobExecutionPlan getJobExecutionPlan() {
        return jep
    }

    @Override
    List<SeqType> getSeqTypes() {
        return [SeqType.getWholeGenomePairedSeqType()]
    }
}
