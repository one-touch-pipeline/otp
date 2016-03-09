package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.ProcessStatusService
import de.dkfz.tbi.otp.ngsdata.*

class TransferMergedQAResultJob extends AbstractEndStateAwareJobImpl{

    final String JOB = "__pbsIds"
    final String REALM = "__pbsRealm"

    @Autowired
    ChecksumFileService checksumFileService

    @Autowired
    PbsService pbsService

    @Autowired
    ConfigService configService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ProcessStatusService processStatusService

    @Autowired
    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    @Autowired
    QualityAssessmentMergedPassService qualityAssessmentMergedPassService

    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile bamFile = ProcessedMergedBamFile.get(id)
        Project project = processedMergedBamFileService.project(bamFile)

        String cmd = scriptText(bamFile)
        Realm realm = configService.getRealmDataProcessing(project)
        String jobId = pbsService.executeJob(realm, cmd)
        log.debug "Job ${jobId} submitted to PBS"

        addOutputParameter(JOB, jobId)
        addOutputParameter(REALM, realm.id.toString())
        succeed()
    }

    private String scriptText(ProcessedMergedBamFile file) {
        String tmpQADestinationDirectory = processedMergedBamFileService.qaResultTempDestinationDirectory(file)
        QualityAssessmentMergedPass pass = qualityAssessmentMergedPassService.latestQualityAssessmentMergedPass(file)
        String sourceQAResultDirectory = processedMergedBamFileQaFileService.directoryPath(pass)
        String text = """
mkdir -p -m 2750 ${tmpQADestinationDirectory}
cp -r ${sourceQAResultDirectory}/* ${tmpQADestinationDirectory}
find ${tmpQADestinationDirectory} -type f -exec chmod 0640 '{}' \\;
"""
        return text
    }
}
