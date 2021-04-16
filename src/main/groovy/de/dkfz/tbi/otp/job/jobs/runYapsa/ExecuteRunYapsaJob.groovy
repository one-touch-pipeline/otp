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
package de.dkfz.tbi.otp.job.jobs.runYapsa

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

@Component
@Scope("prototype")
@Slf4j
class ExecuteRunYapsaJob extends AbstractOtpJob implements AutoRestartableJob {

    @Autowired BedFileService bedFileService
    @Autowired ClusterJobSchedulerService clusterJobSchedulerService
    @Autowired ReferenceGenomeService referenceGenomeService
    @Autowired ConfigService configService
    @Autowired ProcessingOptionService processingOptionService
    @Autowired FileService fileService
    @Autowired FileSystemService fileSystemService
    @Autowired SnvCallingService snvCallingService

    @Override
    protected final NextAction maybeSubmit() throws Throwable {
        final RunYapsaInstance runYapsaInstance = processParameterObject as RunYapsaInstance
        final Realm realm = runYapsaInstance.project.realm

        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(
                fileService.toPath(runYapsaInstance.workDirectory, fileSystemService.getRemoteFileSystem(realm)),
                realm, "", FileService.DEFAULT_DIRECTORY_PERMISSION_STRING)

        String jobScript = createScript(runYapsaInstance)

        clusterJobSchedulerService.executeJob(realm, jobScript)

        return NextAction.WAIT_FOR_CLUSTER_JOBS
    }

    @SuppressWarnings("LineLength") // suppressed because breaking the line would break the commands
    private String createScript(RunYapsaInstance runYapsaInstance) {
        final RunYapsaConfig CONFIG = runYapsaInstance.config
        File outputDirectory = runYapsaInstance.workDirectory
        ReferenceGenome referenceGenome = runYapsaInstance.referenceGenome

        String moduleLoader = processingOptionService.findOptionAsString(COMMAND_LOAD_MODULE_LOADER)
        String rActivation = processingOptionService.findOptionAsString(COMMAND_ACTIVATION_R)
        String runYapsaActivationPrefix = processingOptionService.findOptionAsString(COMMAND_ENABLE_MODULE)

        List<String> runYapsaCall = []
        runYapsaCall << "runYAPSA.R"
        runYapsaCall << "-i ${snvCallingService.getResultRequiredForRunYapsaAndEnsureIsReadableAndNotEmpty(runYapsaInstance, fileSystemService.filesystemForProcessingForRealm)}"
        runYapsaCall << "-o ${outputDirectory.absolutePath}"
        if (runYapsaInstance.seqType == SeqTypeService.wholeGenomePairedSeqType) {
            runYapsaCall << "-s WGS"
        } else if (runYapsaInstance.seqType == SeqTypeService.exomePairedSeqType) {
            runYapsaCall << "-s WES"
            BedFile bedFile = BedFile.findByReferenceGenomeAndLibraryPreparationKit(
                    referenceGenome,
                    runYapsaInstance.libraryPreparationKit,
            )
            runYapsaCall << "-t ${bedFileService.filePath(bedFile)}"
        } else {
            throw new UnsupportedOperationException("Sequencing type '${runYapsaInstance.seqType}' not supported by runYapsa")
        }
        runYapsaCall << "-r ${referenceGenomeService.fastaFilePath(referenceGenome)}"
        runYapsaCall << "-v"

        return """\
            ${moduleLoader}
            ${rActivation}
            ${runYapsaActivationPrefix} ${CONFIG.programVersion}

            ${runYapsaCall.join(" ")}

            """.stripIndent()
    }

    @Override
    protected final void validate() throws Throwable {
        final RunYapsaInstance runYapsaInstance = processParameterObject as RunYapsaInstance
        final Realm realm = runYapsaInstance.project.realm

        fileService.correctPathPermissionAndGroupRecursive(
                fileService.toPath(runYapsaInstance.workDirectory, fileSystemService.getRemoteFileSystem(realm)),
                realm,
                runYapsaInstance.project.unixGroup,
        )

        runYapsaInstance.processingState = AnalysisProcessingStates.FINISHED
        runYapsaInstance.save(flush: true)
    }
}
