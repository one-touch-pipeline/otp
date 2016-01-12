package de.dkfz.tbi.otp.job.jobs.fastqc

import de.dkfz.tbi.otp.ngsqc.FastqcUploadService
import de.dkfz.tbi.otp.utils.ProcessHelperService
import de.dkfz.tbi.otp.utils.WaitingFileUtils
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction

class FastqcJob extends AbstractOtpJob {

    @Autowired
    FastqcDataFilesService fastqcDataFilesService

    @Autowired
    PbsService pbsService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    SeqTrackService seqTrackService

    @Autowired
    FastqcUploadService fastqcUploadService

    @Autowired
    ExecutionService executionService

    @Autowired
    ProcessHelperService processHelperService


    @Override
    protected final NextAction maybeSubmit() throws Throwable {
        final SeqTrack seqTrack = getProcessParameterObject()
        final Realm realm = fastqcDataFilesService.fastqcRealm(seqTrack)
        // create fastqc output directory
        File directory = new File(fastqcDataFilesService.fastqcOutputDirectory(seqTrack))
        String cmd = "umask 027; mkdir -p -m 2750 " + directory.path
        executionService.executeCommand(realm, cmd)
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
                seqTrackService.setFastqcFinished(seqTrack)
                return NextAction.SUCCEED
            }
        }
    }


    @Override
    protected final void validate() throws Throwable {
        final SeqTrack seqTrack = getProcessParameterObject()

        File finalDir = new File(fastqcDataFilesService.fastqcOutputDirectory(seqTrack))
        lsdfFilesService.ensureDirIsReadableAndNotEmpty(finalDir)

        DataFile.findAllBySeqTrack(seqTrack).each { DataFile dataFile ->
            lsdfFilesService.ensureFileIsReadableAndNotEmpty(new File("${finalDir}/${fastqcDataFilesService.fastqcFileName(dataFile)}"))
        }

        synchronized (seqTrackService) {
            DataFile.withTransaction { //Ensure that all updates are executed together
                List<DataFile> files = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)
                for (DataFile file in files) {
                    FastqcProcessedFile fastqc = fastqcDataFilesService.getAndUpdateFastqcProcessedFile(file)
                    fastqcUploadService.uploadFileContentsToDataBase(fastqc)
                    fastqcDataFilesService.updateFastqcProcessedFile(fastqc)
                    fastqcDataFilesService.setFastqcProcessedFileUploaded(fastqc)
                }
                seqTrackService.setFastqcFinished(seqTrack)
            }
        }
    }


    private void createAndExecuteFastQcCommand(Realm realm, List<DataFile> dataFiles, File outDir) {
        dataFiles.each { dataFile ->
            String rawSeq = lsdfFilesService.getFileFinalPath(dataFile)
            String fastqcCommand = ProcessingOptionService.findOption('fastqcCommand', null, null)
            String command = "${fastqcCommand} ${rawSeq} --noextract --nogroup -o ${outDir};chmod -R 440 ${outDir}/*.zip"
            pbsService.executeJob(realm, command)
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
            File seqCenterFastQcFile = fastqcDataFilesService.pathToFastQcResultFromSeqCenter(dataFile)
            File seqCenterFastQcFileMd5Sum = fastqcDataFilesService.pathToFastQcResultMd5SumFromSeqCenter(dataFile)
            lsdfFilesService.ensureFileIsReadableAndNotEmpty(seqCenterFastQcFile)
            String md5SumCreationCommand = """
cd ${seqCenterFastQcFile.parent};
md5sum ${seqCenterFastQcFile.name} > ${outDir}/${seqCenterFastQcFileMd5Sum.name};
cp ${seqCenterFastQcFile} ${outDir};
chmod 0644 ${outDir}/*
"""
            executionService.executeCommand(realm, md5SumCreationCommand)
            lsdfFilesService.ensureFileIsReadableAndNotEmpty(new File(outDir, seqCenterFastQcFile.name))

            String validateMd5Sum = "cd ${outDir}; md5sum -c ${seqCenterFastQcFileMd5Sum.name}"
            processHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(validateMd5Sum)

            createFastqcProcessedFileIfNotExisting(dataFile)
            FastqcProcessedFile fastqcProcessedFile = fastqcDataFilesService.getAndUpdateFastqcProcessedFile(dataFile)
            fastqcDataFilesService.setFastqcProcessedFileUploaded(fastqcProcessedFile)
        }
    }


    private boolean fastQcResultsFromSeqCenterAvailable(SeqTrack seqTrack) {
        List<DataFile> files = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)
        return files.every { DataFile dataFile ->
            fastqcDataFilesService.pathToFastQcResultFromSeqCenter(dataFile).exists()
        }
    }
}
