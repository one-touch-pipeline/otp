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
package de.dkfz.tbi.otp.workflow.analysis.indel

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisWorkflow
import de.dkfz.tbi.otp.workflow.analysis.RoddyAnalysisFragmentJob
import de.dkfz.tbi.otp.workflow.jobs.*
import de.dkfz.tbi.otp.workflowExecution.Artefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact

/**
 * represents the Indel Workflow
 */
@Component
@Slf4j
class IndelWorkflow extends AbstractAnalysisWorkflow {

    public static final String WORKFLOW = "Indel Workflow"

    @Override
    List<Class<? extends Job>> getJobList() {
        return [
//                CoverageConditionalSkipJob,
                RoddyAnalysisFragmentJob,
//                IndelCheckFragmentKeysJob,
//                IndelConditionalFailJob,
//                IndelCreateNotificationJob,
                AttachUuidJob,
//                IndelPrepareJob,
//                IndelExecuteJob,
//                IndelValidationJob,
//                IndelParseJob,
//                IndelCleanupJob,
                SetCorrectPermissionJob,
                CalculateSizeJob,
//                IndelLinkJob,
//                IndelFinishJob,
        ]
    }

    @Override
    Artefact createCopyOfArtefact(Artefact artefact) {
        IndelCallingInstance indelCallingInstance = artefact as IndelCallingInstance

        SamplePair samplePair = indelCallingInstance.samplePair

        IndelCallingInstance outputIndelCallingInstance = new IndelCallingInstance([
                samplePair        : samplePair,
                instanceName      : indelCallingInstance.instanceName,
                config            : indelCallingInstance.config,
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
        ]).save(flush: true)

        return outputIndelCallingInstance
    }

    @Override
    void reconnectDependencies(Artefact artefact, WorkflowArtefact newWorkflowArtefact) {
    }

    final String userDocumentation = "notification.template.references.indel"
}
