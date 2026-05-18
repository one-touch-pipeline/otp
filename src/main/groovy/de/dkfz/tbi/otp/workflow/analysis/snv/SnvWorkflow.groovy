/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflow.analysis.snv

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.workflow.analysis.*
import de.dkfz.tbi.otp.workflow.jobs.*
import de.dkfz.tbi.otp.workflowExecution.Artefact
import de.dkfz.tbi.otp.workflowExecution.MultiApiVersionWorkflow

/**
 * represents the SNV Workflow
 */
@Component
@Slf4j
class SnvWorkflow extends AbstractAnalysisWorkflow implements MultiApiVersionWorkflow {

    public static final String WORKFLOW = "Roddy SNV calling"

    public static final Map<Integer, List<String>> JOB_LIST_PER_API_IDENTIFIER = [
            (1): [
                    AnalysisConditionalSkipJob,
                    RoddyAnalysisFragmentJob,
                    SnvCheckFragmentKeysV1Job,
                    SnvConditionalFailJob,
                    // SnvCreateNotificationJob,
                    AttachUuidJob,
                    SnvPrepareJob,
                    SnvExecuteJob,
                    SnvValidationJob,
                    RoddyCleanupJob,
                    SetCorrectPermissionJob,
                    CalculateSizeJob,
                    AnalysisLinkJob,
                    AnalysisFinishJob,
            ],
            (2): [
                    AnalysisConditionalSkipJob,
                    RoddyAnalysisFragmentJob,
                    SnvCheckFragmentKeysV2Job,
                    SnvConditionalFailJob,
                    // SnvCreateNotificationJob,
                    AttachUuidJob,
                    SnvPrepareJob,
                    SnvExecuteJob,
                    SnvValidationJob,
                    RoddyCleanupJob,
                    SetCorrectPermissionJob,
                    CalculateSizeJob,
                    AnalysisLinkJob,
                    AnalysisFinishJob,
            ],
    ].collectEntries { Integer identifier, List<Class<? extends Job>> beanClasses ->
        [(identifier): beanClasses*.simpleName*.uncapitalize().asImmutable()]
    }.asImmutable()

    @Autowired
    SnvWorkFileService snvWorkFileService

    @Override
    List<String> getJobList(Integer identifier) {
        return JOB_LIST_PER_API_IDENTIFIER[identifier]
    }

    @Override
    Artefact createCopyOfArtefact(Artefact artefact) {
        SnvCallingInstance snvCallingInstance = artefact as SnvCallingInstance
        snvCallingInstance.withdrawn = true
        snvCallingInstance.save(flush: true)

        SamplePair samplePair = snvCallingInstance.samplePair

        SnvCallingInstance outputSnvCallingInstance = new SnvCallingInstance([
                samplePair        : samplePair,
                instanceName      : snvWorkFileService.constructInstanceName(artefact.workflowArtefact.producedBy.workflowVersion),
                config            : snvCallingInstance.config,
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
        ]).save(flush: true)

        return outputSnvCallingInstance
    }

    final String userDocumentation = "notification.template.references.snv"
}
