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
    JobExecutionPlan getExecutionPlan() {
        return jep
    }

    @Override
    String getJobExecutionPlanName() {
        throw new UnsupportedOperationException()
    }

    @Override
    List<SeqType> getSeqTypes() {
        return [SeqType.getWholeGenomePairedSeqType()]
    }

    @Override
    String getVersion() {
        return 'version'
    }

}
