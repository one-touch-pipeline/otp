package de.dkfz.tbi.otp.job.jobs.dataInstallation

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.*

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
    RemoteShellHelper remoteShellHelper

    @Autowired
    FileSystemService fileSystemService

    @Autowired
    FileService fileService

    @Override
    protected final NextAction maybeSubmit() throws Throwable {
        SeqTrack seqTrack = SeqTrack.get(Long.parseLong(getProcessParameterValue()))
        assert seqTrack : "No seqTrack found for id ${Long.parseLong(getProcessParameterValue())}."

        Realm realm = seqTrack.project.realm

        checkInitialSequenceFiles(seqTrack)

        NextAction returnValue

        seqTrack.dataFiles.each { DataFile dataFile ->
            File sourceFile = new File(lsdfFilesService.getFileInitialPath(dataFile))
            File targetFile = new File(lsdfFilesService.getFileFinalPath(dataFile))
            String md5SumFileName = checksumFileService.md5FileName(dataFile)

            if (seqTrack.linkedExternally) {
                String cmd = getScript(sourceFile, targetFile,"ln -s")
                remoteShellHelper.executeCommand(realm, cmd)
                returnValue = NextAction.SUCCEED
            } else {
                String cmd = getScript(sourceFile, targetFile,"cp", "md5sum ${targetFile.name} > ${md5SumFileName}", "chmod 440 ${targetFile} ${md5SumFileName}")
                clusterJobSchedulerService.executeJob(realm, cmd)
                returnValue = NextAction.WAIT_FOR_CLUSTER_JOBS
            }
        }
        if (returnValue == NextAction.SUCCEED) {
            validate()
        }
        return returnValue
    }

    private String getScript(File sourceFile, File targetFile, String copyOrLinkCommand, String calculateMd5 = "", String changeMode = "") {
        return """
#for debug kerberos problem
klist || true


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
            String targetFile = lsdfFilesService.getFileFinalPath(dataFile)
            try {
                FileSystem fs = seqTrack.linkedExternally ?
                        fileSystemService.getFilesystemForFastqImport() :
                        fileSystemService.getFilesystemForProcessingForRealm(dataFile.project.realm)
                Path targetPath = fs.getPath(targetFile)

                fileService.ensureFileIsReadableAndNotEmpty(targetPath)
                if (!seqTrack.linkedExternally) {
                    assert checksumFileService.compareMd5(dataFile)
                }

                dataFile.fileSize = Files.size(targetPath)
                dataFile.dateFileSystem = new Date(Files.getLastModifiedTime(targetPath).toMillis())
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
