package de.dkfz.tbi.otp.job.jobs.dataInstallation

import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class CopyFilesJob extends AbstractOtpJob implements AutoRestartableJob {


    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ConfigService configService

    @Autowired
    ChecksumFileService checksumFileService

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    @Autowired
    ExecutionService executionService


    @Override
    protected final AbstractMultiJob.NextAction maybeSubmit() throws Throwable {
        SeqTrack seqTrack = SeqTrack.get(Long.parseLong(getProcessParameterValue()))
        assert seqTrack : "No seqTrack found for id ${Long.parseLong(getProcessParameterValue())}."

        Realm realm = configService.getRealmDataManagement(seqTrack.project)

        checkInitialSequenceFiles(seqTrack)

        AbstractMultiJob.NextAction returnValue

        seqTrack.dataFiles.each { DataFile dataFile ->
            File sourceFile = new File(lsdfFilesService.getFileInitialPath(dataFile))
            File targetFile = new File(lsdfFilesService.getFileFinalPath(dataFile))
            String md5SumFileName = checksumFileService.md5FileName(dataFile)

            if (seqTrack.linkedExternally) {
                String cmd = getScript(sourceFile, targetFile,"ln -s")
                executionService.executeCommand(realm, cmd)
                returnValue = AbstractMultiJob.NextAction.SUCCEED
            } else {
                String cmd = getScript(sourceFile, targetFile,"cp", "md5sum ${targetFile.name} > ${md5SumFileName}", "chmod 440 ${targetFile} ${md5SumFileName}")
                clusterJobSchedulerService.executeJob(realm, cmd)
                returnValue = AbstractMultiJob.NextAction.WAIT_FOR_CLUSTER_JOBS
            }
        }
        if (returnValue == AbstractMultiJob.NextAction.SUCCEED) {
            validate()
        }
        return returnValue
    }

    private String getScript(File sourceFile, File targetFile, String copyOrLinkCommand, String calculateMd5 = "", String changeMode = "") {
        return """
#for debug kerberos problem
klist


mkdir -p -m 2750 ${targetFile.parent}
cd ${targetFile.parent}
if [ -e "${targetFile.path}" ]; then
    echo "File ${targetFile.path} already exists."
    rm ${targetFile.path}*
fi
${copyOrLinkCommand} ${sourceFile} ${targetFile}
${calculateMd5}
${changeMode}
"""
    }

    @Override
    protected final void validate() throws Throwable {
        SeqTrack seqTrack = SeqTrack.get(Long.parseLong(getProcessParameterValue()))

        final Collection<String> problems = seqTrack.dataFiles.collect { DataFile dataFile ->
            File targetFile = new File(lsdfFilesService.getFileFinalPath(dataFile))
            try {
                lsdfFilesService.ensureFileIsReadableAndNotEmpty(targetFile)
                if (!seqTrack.linkedExternally) {
                    assert checksumFileService.compareMd5(dataFile)
                }

                dataFile.fileSize = targetFile.size()
                dataFile.dateFileSystem = new Date(targetFile.lastModified())
                dataFile.fileExists = true
                assert dataFile.save(flush: true)

                return null
            } catch (Throwable t) {
                return "Copying or linking of targetFile ${targetFile} from dataFile ${dataFile} failed, ${t.message}"
            }
        }.findAll()
        if (problems) {
            throw new ProcessingException(problems.join(","))
        }
    }


    protected void checkInitialSequenceFiles(SeqTrack seqTrack) {
        List<DataFile> dataFiles = seqTrack.dataFiles
        if (!dataFiles) {
            throw new ProcessingException("No files in processing for seqTrack ${seqTrack}")
        }
        final Collection<String> missingPaths = []
        for (DataFile file in dataFiles) {
            String path = lsdfFilesService.getFileInitialPath(file)
            if (!lsdfFilesService.isFileReadableAndNotEmpty(new File(path))) {
                missingPaths.add(path)
            }
        }
        if (!missingPaths.empty) {
            throw new ProcessingException("The following ${missingPaths.size()} files are missing:\n${missingPaths.join("\n")}")
        }
    }
}
