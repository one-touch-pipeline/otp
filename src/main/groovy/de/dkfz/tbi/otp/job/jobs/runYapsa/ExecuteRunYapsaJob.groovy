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

    @Override
    protected final NextAction maybeSubmit() throws Throwable {
        final RunYapsaInstance instance = getProcessParameterObject()
        final Realm realm = instance.project.realm

        String jobScript = createScript(instance)

        clusterJobSchedulerService.executeJob(realm, jobScript)

        return NextAction.WAIT_FOR_CLUSTER_JOBS
    }


    private createScript(RunYapsaInstance instance) {
        final RunYapsaConfig config = instance.config
        File outputDirectory = instance.getWorkDirectory()
        ReferenceGenome referenceGenome = instance.getReferenceGenome()

        String moduleLoader = processingOptionService.findOptionAsString(COMMAND_LOAD_MODULE_LOADER)
        String rActivation = processingOptionService.findOptionAsString(COMMAND_ACTIVATION_R)
        String rCommand = processingOptionService.findOptionAsString(COMMAND_R)
        String runYapsaActivationPrefix = processingOptionService.findOptionAsString(COMMAND_ENABLE_MODULE)
        String runYapsaCommand = processingOptionService.findOptionAsString(COMMAND_RUN_YAPSA)

        List<String> runYapsaCall = []
        runYapsaCall << rCommand
        runYapsaCall << configService.getToolsPath().getAbsolutePath() + "/" + runYapsaCommand
        runYapsaCall << "-i ${instance.samplePair.findLatestSnvCallingInstance().getResultRequiredForRunYapsa()}"
        runYapsaCall << "-o ${outputDirectory.absolutePath}"
        if (instance.seqType == SeqTypeService.wholeGenomePairedSeqType) {
            runYapsaCall << "-s WGS"
        } else if (instance.seqType == SeqTypeService.exomePairedSeqType) {
            runYapsaCall << "-s WES"
            BedFile bedFile = BedFile.findByReferenceGenomeAndLibraryPreparationKit(
                    referenceGenome,
                    instance.getLibraryPreparationKit(),
            )
            runYapsaCall << "-t ${bedFileService.filePath(bedFile)}"
        } else {
            throw new UnsupportedOperationException("Sequencing type '${instance.seqType}' not supported by runYapsa")
        }
        runYapsaCall << "-r ${referenceGenomeService.fastaFilePath(referenceGenome)}"
        runYapsaCall << "-v"

        return """\
            ${moduleLoader}
            ${rActivation}
            ${runYapsaActivationPrefix} ${config.programVersion}

            mkdir -p -m 2755 ${outputDirectory.absolutePath}

            ${runYapsaCall.join(" ")}

            """.stripIndent()
    }


    @Override
    protected final void validate() throws Throwable {
        final RunYapsaInstance instance = getProcessParameterObject()
        instance.updateProcessingState(AnalysisProcessingStates.FINISHED)
    }
}
