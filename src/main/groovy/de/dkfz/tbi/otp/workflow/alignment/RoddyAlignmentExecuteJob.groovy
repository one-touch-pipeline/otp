/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.workflow.alignment

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.workflow.jobs.AbstractExecuteRoddyPipelineJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class RoddyAlignmentExecuteJob extends AbstractExecuteRoddyPipelineJob implements AlignmentWorkflowShared {

    @Autowired
    ProcessingOptionService processingOptionService

    @Override
    protected RoddyResult getRoddyResult(WorkflowStep workflowStep) {
        return getRoddyBamFile(workflowStep)
    }

    @Override
    protected String getRoddyWorkflowName() {
        return "AlignmentAndQCWorkflows"
    }

    @Override
    protected String getAnalysisConfiguration(SeqType seqType) {
        return 'qcAnalysis'
    }

    @Override
    protected boolean getFilenameSectionKillSwitch() {
        return false
    }

    @Override
    protected Map<String, String> getConfigurationValues(WorkflowStep workflowStep, String combinedConfig) {
        Map<String, String> conf = [:]

        conf.put("sharedFilesBaseDirectory", processingOptionService.findOptionAsString(ProcessingOption.OptionName.RODDY_SHARED_FILES_BASE_DIRECTORY))

        RoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)
        conf.putAll(roddyConfigValueService.getAlignmentValues(roddyBamFile, combinedConfig))

        return conf
    }

    @Override
    protected List<String> getAdditionalParameters(WorkflowStep workflowStep) {
        return []
    }

    @Override
    protected void createAdditionalConfigFiles(WorkflowStep workflowStep, Path configPath, Realm realm) {
    }
}
