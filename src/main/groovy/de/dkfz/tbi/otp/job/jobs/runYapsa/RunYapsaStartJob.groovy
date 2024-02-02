/*
 * Copyright 2011-2024 The OTP authors
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

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.hibernate.Hibernate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.utils.CollectionUtils

@CompileDynamic
@Component("runYapsaStartJob")
@Scope("singleton")
@Slf4j
class RunYapsaStartJob extends AbstractBamFilePairAnalysisStartJob {
    @Autowired
    RunYapsaService runYapsaService

    @Override
    void prepareCreatingTheProcessAndTriggerTracking(BamFilePairAnalysis bamFilePairAnalysis) {
        assert bamFilePairAnalysis : "bamFilePairAnalysis must not be null"

        notificationCreator.setStartedForSeqTracks(bamFilePairAnalysis.containedSeqTracks, Ticket.ProcessingStep.RUN_YAPSA)
        bamFilePairAnalysis.samplePair.runYapsaProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        bamFilePairAnalysis.samplePair.save(flush: true)
    }

    @Override
    AbstractBamFileAnalysisService getBamFileAnalysisService() {
        return runYapsaService
    }

    @Override
    String getInstanceName(ConfigPerProjectAndSeqType config) {
        assert RunYapsaConfig.isAssignableFrom(Hibernate.getClass(config)):
                "RunYapsa startjob should only ever be started with a YAPSA config, not something else; got ${ config.class }"
        return "runYapsa_${ config.programVersion.replace("/", "-") }_${ formattedDate }"
    }

    @SuppressWarnings("LineLength") // suppressed because breaking the line would break the commands
    @SuppressWarnings("ThrowRuntimeException") // ignored: will be removed with the old workflow system
    @Override
    ConfigPerProjectAndSeqType getConfig(SamplePair samplePair) {
        Pipeline pipeline = bamFileAnalysisService.pipeline
        RunYapsaConfig config = CollectionUtils.<RunYapsaConfig> atMostOneElement(
                RunYapsaConfig.findAllByProjectAndPipelineAndSeqTypeAndObsoleteDate(samplePair.project, pipeline, samplePair.seqType, null)
        )

        if (config == null) {
            throw new RuntimeException("No ${RunYapsaConfig.simpleName} found for ${Pipeline.simpleName} ${pipeline}, ${Individual.simpleName} ${samplePair.individual} (${Project.simpleName} ${samplePair.project}), ${SeqType.simpleName} ${samplePair.seqType}")
        }
        return config
    }
}
