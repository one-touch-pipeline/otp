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
package de.dkfz.tbi.otp.workflow.alignment

import com.fasterxml.jackson.core.JsonParseException
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AlignmentInfoService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.workflow.jobs.AbstractCreateNotificationJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

@Component
@Slf4j
class RoddyAlignmentCreateNotificationJob extends AbstractCreateNotificationJob implements AlignmentWorkflowShared {

    @Autowired
    MessageSourceService messageSourceService

    @Autowired
    AlignmentInfoService alignmentInfoService

    @CompileDynamic
    @Override
    String createNotificationText(WorkflowStep workflowStep) {
        if (!workflowStep.workflowRun.combinedConfig) {
            throw new IllegalArgumentException("CombinedConfig in worflowRun ${workflowStep.workflowRun} is null")
        }

        Map<String, String> config
        try {
            config = alignmentInfoService.extractCValuesMapFromJsonConfigString(workflowStep.workflowRun.combinedConfig)
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Failed to parse combinedConfig in workflowRun ${workflowStep.workflowRun}. " +
                    "Please ensure it is a valid JSON string.", e)
        }

        RoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)

        StringBuilder builder = new StringBuilder()
        builder << messageSourceService.createMessage("notification.template.alignment.processing", [
                seqType           : roddyBamFile.seqType.displayNameWithLibraryLayout,
                individuals       : "",
                referenceGenome   : roddyBamFile.referenceGenome,
                alignmentProgram  : getProgramVersion(config),
                alignmentParameter: getProgramParameter(config),
        ])
        builder << messageSourceService.createMessage("notification.template.alignment.processing.roddy", [
                mergingProgram    : getMergingProgramVersion(config),
                mergingParameter  : getMergingProgramParameter(config),
                samtoolsProgram   : getSAMToolsVersion(config),
                programVersion    : workflowStep.workflowRun.workflowVersion.workflowVersion,
        ])

        return builder.toString()
    }

    protected String getSAMToolsVersion(Map<String, String> config) {
        return config.get("SAMTOOLS_VERSION") ? "Version ${config.get("SAMTOOLS_VERSION")}" : ""
    }

    protected String getProgramVersion(Map<String, String> config) {
        return config.get("BWA_VERSION") ? "BWA Version ${config.get("BWA_VERSION")}" : ""
    }

    protected String getProgramParameter(Map<String, String> config) {
        return config.get("BWA_MEM_OPTIONS")
    }

    protected String getMergingProgramVersion(Map<String, String> config) {
        return config.get('SAMBAMBA_MARKDUP_VERSION') ? "Sambamba Version ${config.get('SAMBAMBA_MARKDUP_VERSION')}" : ""
    }

    protected String getMergingProgramParameter(Map<String, String> config) {
        return config.get('SAMBAMBA_MARKDUP_OPTS')
    }
}
