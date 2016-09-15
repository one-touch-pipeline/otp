package de.dkfz.tbi.otp.job.jobs.dataInstallation

import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*

class CopyFilesJob extends AbstractOtpJob implements AutoRestartableJob {


    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ConfigService configService

    @Autowired
    ChecksumFileService checksumFileService

    @Autowired
    PbsService pbsService


    @Override
    protected final AbstractMultiJob.NextAction maybeSubmit() throws Throwable {
        SeqTrack seqTrack = SeqTrack.get(Long.parseLong(getProcessParameterValue()))
        assert seqTrack : "No seqTrack found for id ${Long.parseLong(getProcessParameterValue())}."

        Realm realm = configService.getRealmDataManagement(seqTrack.project)

        checkInitialSequenceFiles(seqTrack)

        seqTrack.dataFiles.each { DataFile dataFile ->
            File sourceFile = new File(lsdfFilesService.getFileInitialPath(dataFile))
            File targetFile = new File(lsdfFilesService.getFileFinalPath(dataFile))
            String md5SumFileName = checksumFileService.md5FileName(dataFile)

            String copyOrLinkCommand = seqTrack.linkedExternally ? "ln -s" : "cp"
            String removeTargetFileIfExists = targetFile.exists() ? "rm ${targetFile}*" : ""
            String fastqFile = seqTrack.linkedExternally ? "" : "${targetFile}"

            String cmd = """
mkdir -p -m 2750 ${targetFile.parent}
cd ${targetFile.parent}
${removeTargetFileIfExists}
${copyOrLinkCommand} ${sourceFile} ${targetFile}
md5sum ${targetFile.name} > ${md5SumFileName}
chmod 440 ${fastqFile} ${md5SumFileName}
"""
            pbsService.executeJob(realm, cmd)
        }
        return AbstractMultiJob.NextAction.WAIT_FOR_CLUSTER_JOBS
    }


    @Override
    protected final void validate() throws Throwable {
        SeqTrack seqTrack = SeqTrack.get(Long.parseLong(getProcessParameterValue()))

        final Collection<String> problems = seqTrack.dataFiles.collect { DataFile dataFile ->
            File targetFile = new File(lsdfFilesService.getFileFinalPath(dataFile))
            try {
                lsdfFilesService.ensureFileIsReadableAndNotEmpty(targetFile)
                assert checksumFileService.compareMd5(dataFile)

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
