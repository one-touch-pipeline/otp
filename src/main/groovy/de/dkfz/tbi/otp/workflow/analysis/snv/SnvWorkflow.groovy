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
package de.dkfz.tbi.otp.workflow.analysis.snv

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisWorkflow
import de.dkfz.tbi.otp.workflow.analysis.AnalysisLinkJob
import de.dkfz.tbi.otp.workflow.analysis.AnalysisConditionalSkipJob
import de.dkfz.tbi.otp.workflow.jobs.*
import de.dkfz.tbi.otp.workflowExecution.Artefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact

/**
 * represents the SNV Workflow
 */
@Component
@Slf4j
class SnvWorkflow extends AbstractAnalysisWorkflow {

    public static final String WORKFLOW = "Snv Workflow"

    @Override
    List<Class<? extends Job>> getJobList() {
        return [
                AnalysisConditionalSkipJob,
//                SnvFragmentJob,
//                SnvCheckFragmentKeysJob,
                SnvConditionalFailJob,
//                SnvCreateNotificationJob,
                AttachUuidJob,
                SnvPrepareJob,
//                SnvExecuteJob,
//                SnvValidationJob,
//                SnvParseJob,
//                SnvCleanupJob,
                SetCorrectPermissionJob,
                CalculateSizeJob,
                AnalysisLinkJob,
//                SnvFinishJob,
        ]
    }

    @Override
    Artefact createCopyOfArtefact(Artefact artefact) {
        RoddySnvCallingInstance snvCallingInstance = artefact as RoddySnvCallingInstance

        SamplePair samplePair = snvCallingInstance.samplePair

        RoddySnvCallingInstance outputSnvCallingInstance = new RoddySnvCallingInstance([
                samplePair        : samplePair,
                instanceName      : snvCallingInstance.instanceName,
                config            : snvCallingInstance.config,
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
        ]).save(flush: true)

        return outputSnvCallingInstance
    }

    @Override
    void reconnectDependencies(Artefact artefact, WorkflowArtefact newWorkflowArtefact) {
    }

    final String userDocumentation = "notification.template.references.snv"
}
