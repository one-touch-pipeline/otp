/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.job.jobs.fastqc

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsqc.FastqcUploadService
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.utils.WaitingFileUtils

import java.nio.file.Files
import java.nio.file.Path

@Component
@Scope("prototype")
@Slf4j
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

    @Autowired
    ProcessingOptionService processingOptionService

    @Override
    protected final NextAction maybeSubmit() throws Throwable {
        final SeqTrack seqTrack = getProcessParameterObject()
        final Realm realm = fastqcDataFilesService.fastqcRealm(seqTrack)
        // create fastqc output directory
        File directory = new File(fastqcDataFilesService.fastqcOutputDirectory(seqTrack))
        String cmd = "umask 027; mkdir -p -m 2750 " + directory.path
        remoteShellHelper.executeCommandReturnProcessOutput(realm, cmd).assertExitCodeZeroAndStderrEmpty()
        WaitingFileUtils.waitUntilExists(directory)

        // copy fastqc result file or execute fastqc on cluster
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
        seqTrack.dataFiles.each { DataFile dataFile ->
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
            assert files.findAll {
                !it.indexFile
            }*.nReads.unique().size() == 1
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
            String decompressFileCommand = ""
            String deleteDecompressedFileCommand = ""

            String inputFileName = lsdfFilesService.getFileFinalPath(dataFile)

            FastqcDataFilesService.CompressionFormat usedFormat = FastqcDataFilesService.CompressionFormat.getUsedFormat(inputFileName)
            if (usedFormat) {
                String orgFileName = inputFileName
                inputFileName = fastqcDataFilesService.inputFileNameAdaption(inputFileName)

                decompressFileCommand = "{ ${usedFormat.decompressionCommand} ; } < ${orgFileName} > ${inputFileName}"
                deleteDecompressedFileCommand = "rm -f ${inputFileName}"
            }

            String fastqcCommand = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_FASTQC)
            String fastqcActivation = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_ACTIVATION_FASTQC)
            String moduleLoader = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER)
            String command = """\
                    ${moduleLoader}
                    ${fastqcActivation}
                    ${decompressFileCommand}
                    ${fastqcCommand} ${inputFileName} --noextract --nogroup -o ${outDir}
                    ${deleteDecompressedFileCommand}
                    ${getChmodOnFastqcResultsCommand(outDir)}
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
                    ${getChmodOnFastqcResultsCommand(outDir)}
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

    String getChmodOnFastqcResultsCommand(File resultDirectory) {
        assert resultDirectory: "resultDirectory required to build command"
        return "find ${resultDirectory.absolutePath} -type f -not -perm 440 -print -exec chmod 440 '{}' \\;"
    }

    private boolean fastQcResultsFromSeqCenterAvailable(SeqTrack seqTrack) {
        List<DataFile> files = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)
        return files.every { DataFile dataFile ->
            Files.exists(fastqcDataFilesService.pathToFastQcResultFromSeqCenter(dataFile))
        }
    }
}
