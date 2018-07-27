package de.dkfz.tbi.otp.job.jobs.fastqc

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsqc.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction

import java.nio.file.Files
import java.nio.file.Path

@Component
@Scope("prototype")
@UseJobLog
class FastqcJob extends AbstractOtpJob implements AutoRestartableJob {

    @Autowired
    FastqcDataFilesService fastqcDataFilesService

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    SeqTrackService seqTrackService

    @Autowired
    FastqcUploadService fastqcUploadService

    @Autowired
    RemoteShellHelper remoteShellHelper

    @Autowired
    FileService fileService


    @Override
    protected final NextAction maybeSubmit() throws Throwable {
        final SeqTrack seqTrack = getProcessParameterObject()
        final Realm realm = fastqcDataFilesService.fastqcRealm(seqTrack)
        // create fastqc output directory
        File directory = new File(fastqcDataFilesService.fastqcOutputDirectory(seqTrack))
        String cmd = "umask 027; mkdir -p -m 2750 " + directory.path
        remoteShellHelper.executeCommandReturnProcessOutput(realm, cmd).assertExitCodeZeroAndStderrEmpty()
        WaitingFileUtils.waitUntilExists(directory)

        // copy fastqc or execute fastqc on cluster
        List<DataFile> dataFiles = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)
        deleteExistingFastqcResults(realm, dataFiles, directory)

        FastqcProcessedFile.withTransaction {
            if (!fastQcResultsFromSeqCenterAvailable(seqTrack)) {
                dataFiles.each { DataFile dataFile ->
                    assert dataFile.fileExists && dataFile.fileSize > 0L
                }
                createAndExecuteFastQcCommand(realm, dataFiles, directory)
                return NextAction.WAIT_FOR_CLUSTER_JOBS
            } else {
                createAndExecuteCopyCommand(realm, dataFiles, directory)
                validateAndReadFastQcResult()
                return NextAction.SUCCEED
            }
        }
    }


    @Override
    protected final void validate() throws Throwable {
        validateAndReadFastQcResult()
    }

    private validateAndReadFastQcResult() {
        final SeqTrack seqTrack = getProcessParameterObject()

        File finalDir = new File(fastqcDataFilesService.fastqcOutputDirectory(seqTrack))
        DataFile.findAllBySeqTrack(seqTrack).each { DataFile dataFile ->
            lsdfFilesService.ensureFileIsReadableAndNotEmpty(new File("${finalDir}/${fastqcDataFilesService.fastqcFileName(dataFile)}"))
        }

        SeqTrack.withTransaction {
            List<DataFile> files = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)
            for (DataFile file in files) {
                FastqcProcessedFile fastqc = fastqcDataFilesService.getAndUpdateFastqcProcessedFile(file)
                fastqcUploadService.uploadFastQCFileContentsToDataBase(fastqc)
                fastqcDataFilesService.updateFastqcProcessedFile(fastqc)
                fastqcDataFilesService.setFastqcProcessedFileUploaded(fastqc)
            }
            assert files*.nReads.unique().size() == 1
            seqTrackService.setFastqcFinished(seqTrack)
            seqTrackService.fillBaseCount(seqTrack)
            setnBasesInClusterJobForFastqc(processingStep)
        }
    }


    private setnBasesInClusterJobForFastqc(ProcessingStep processingStep) {
        ClusterJob.findAllByProcessingStep(processingStep).each {
            it.nBases = ClusterJobService.getBasesSum(it)
            it.save(flush: true)
        }
    }

    private void createAndExecuteFastQcCommand(Realm realm, List<DataFile> dataFiles, File outDir) {
        dataFiles.each { dataFile ->
            String rawSeq = lsdfFilesService.getFileFinalPath(dataFile)
            String fastqcCommand = ProcessingOptionService.findOption(ProcessingOption.OptionName.COMMAND_FASTQC, null, null)
            String fastqcActivation = ProcessingOptionService.findOption(ProcessingOption.OptionName.COMMAND_ACTIVATION_FASTQC, null, null)
            String moduleLoader = ProcessingOptionService.findOption(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER, null, null)
            String command = """\
                    ${moduleLoader}
                    ${fastqcActivation}
                    ${fastqcCommand} ${rawSeq} --noextract --nogroup -o ${outDir}
                    chmod -R 440 ${outDir}/*.zip
                    """.stripIndent()
            clusterJobSchedulerService.executeJob(realm, command)
            createFastqcProcessedFileIfNotExisting(dataFile)
        }
    }

    private void deleteExistingFastqcResults(Realm realm, List<DataFile> dataFiles, File outDir) {
        dataFiles.each { DataFile dataFile ->
            File fastqcResult = new File(outDir, fastqcDataFilesService.fastqcFileName(dataFile))
            if (fastqcResult.exists()) {
                lsdfFilesService.deleteFile(realm, fastqcResult)
            }
        }
    }


    private createFastqcProcessedFileIfNotExisting(DataFile dataFile) {
        if (!FastqcProcessedFile.findByDataFile(dataFile)) {
            fastqcDataFilesService.createFastqcProcessedFile(dataFile)
        }
    }


    private void createAndExecuteCopyCommand(Realm realm, List<DataFile> dataFiles, File outDir) {
        dataFiles.each { dataFile ->
            Path seqCenterFastQcFile = fastqcDataFilesService.pathToFastQcResultFromSeqCenter(dataFile)
            Path seqCenterFastQcFileMd5Sum = fastqcDataFilesService.pathToFastQcResultMd5SumFromSeqCenter(dataFile)
            fileService.ensureFileIsReadableAndNotEmpty(seqCenterFastQcFile)
            String copyAndMd5sumCommand = """\
                    set -e
                    cd ${seqCenterFastQcFile.parent};
                    md5sum ${seqCenterFastQcFile.fileName} > ${outDir}/${seqCenterFastQcFileMd5Sum.fileName};
                    cp ${seqCenterFastQcFile} ${outDir};
                    chmod 0644 ${outDir}/*
                    """.stripIndent()
            remoteShellHelper.executeCommandReturnProcessOutput(realm, copyAndMd5sumCommand).assertExitCodeZeroAndStderrEmpty()
            lsdfFilesService.ensureFileIsReadableAndNotEmpty(new File(outDir, seqCenterFastQcFile.fileName.toString()))

            String validateMd5Sum = "cd ${outDir}; md5sum -c ${seqCenterFastQcFileMd5Sum.fileName}"
            LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout(validateMd5Sum)

            createFastqcProcessedFileIfNotExisting(dataFile)
            FastqcProcessedFile fastqcProcessedFile = fastqcDataFilesService.getAndUpdateFastqcProcessedFile(dataFile)
            fastqcDataFilesService.setFastqcProcessedFileUploaded(fastqcProcessedFile)
        }
    }


    private boolean fastQcResultsFromSeqCenterAvailable(SeqTrack seqTrack) {
        List<DataFile> files = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)
        return files.every { DataFile dataFile ->
            Files.exists(fastqcDataFilesService.pathToFastQcResultFromSeqCenter(dataFile))
        }
    }
}
