package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

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
class TransferSingleLaneQAResultJob extends AbstractEndStateAwareJobImpl{

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService



    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile mergedBamFile = ProcessedMergedBamFile.get(id)
        Project project = processedMergedBamFileService.project(mergedBamFile)

        Realm realm = project.realm
        addOutputParameter(JobParameterKeys.JOB_ID_LIST, WatchdogJob.SKIP_WATCHDOG)
        addOutputParameter(JobParameterKeys.REALM, realm.id.toString())
        succeed()
    }
}
