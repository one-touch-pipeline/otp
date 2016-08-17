package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.jobs.utils.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class BamFileIndexingJob extends AbstractJobImpl {

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Autowired
    AlignmentPassService alignmentPassService

    @Override
    public void execute() throws Exception {
        ProcessedBamFile bamFile = parseInput()
        Realm realm = alignmentPassService.realmForDataProcessing(bamFile.alignmentPass)

        /*
         * The original version of this job executed a job on the cluster which was checked by the watchdog afterwards.
         * TODO: Remove this entire job after OTP-505 or during OTP-1165
         */
        addOutputParameter(JobParameterKeys.JOB_ID_LIST, WatchdogJob.SKIP_WATCHDOG)
        addOutputParameter(JobParameterKeys.REALM, realm.id.toString())
    }

    private ProcessedBamFile parseInput() {
        long alignmentPassId = Long.parseLong(getProcessParameterValue())
        String type = getParameterValueOrClass("BamType")
        return processedBamFileService.findBamFile(alignmentPassId, type)
    }
}
