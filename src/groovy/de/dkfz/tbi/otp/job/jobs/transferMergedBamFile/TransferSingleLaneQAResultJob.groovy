package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFileService
import de.dkfz.tbi.otp.job.jobs.WatchdogJob
import de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.Realm

import org.springframework.beans.factory.annotation.Autowired

class TransferSingleLaneQAResultJob extends AbstractEndStateAwareJobImpl{

    @Autowired
    ConfigService configService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService



    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile mergedBamFile = ProcessedMergedBamFile.get(id)
        Project project = processedMergedBamFileService.project(mergedBamFile)

        Realm realm = configService.getRealmDataProcessing(project)
        addOutputParameter(JobParameterKeys.JOB_ID_LIST, WatchdogJob.SKIP_WATCHDOG)
        addOutputParameter(JobParameterKeys.REALM, realm.id.toString())
        succeed()
    }
}
