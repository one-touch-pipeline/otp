package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

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
         * To simulate the watchdog that the job has already finished on cluster a very low job ID is used.
         * TODO: Remove this entire job after OTP-505 or during OTP-1165
         */
        addOutputParameter(JobParameterKeys.PBS_ID_LIST, "1")
        addOutputParameter(JobParameterKeys.REALM, realm.id.toString())
    }

    private ProcessedBamFile parseInput() {
        long alignmentPassId = Long.parseLong(getProcessParameterValue())
        String type = getParameterValueOrClass("BamType")
        return processedBamFileService.findBamFile(alignmentPassId, type)
    }
}
