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
package de.dkfz.tbi.otp.workflow.notification

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.StringUtils
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

/**
 * Shared {@link WorkflowNotification} for all alignment workflows.
 *
 * The notification text is derived from the input seq tracks, whereas the GUI URLs and file patterns are derived
 * from the produced output BAM files. Both are fetched via projection queries keyed on the workflow run ids.
 * The {@code workflowName} and {@code pipelineAcknowledgementTemplate} are workflow specific and provided by the
 * concrete subclasses. As for the workflow jobs, the methods rely on the surrounding transaction of the caller.
 */
abstract class AbstractAlignmentNotification extends AbstractWorkflowNotification {

    @Override
    NotificationContent buildContent(Collection<WorkflowRun> workflowRuns) {
        List<SampleNotificationRow> sampleRows = workflowNotificationContentService.fetchInputSampleRows(
                workflowRuns, AlignmentWorkflow.INPUT_FASTQ)
        List<AlignmentBamNotificationRow> bamRows = workflowNotificationContentService.fetchOutputBamRows(
                workflowRuns, AlignmentWorkflow.OUTPUT_BAM)
        Map<Long, Project> projects = workflowNotificationContentService.loadProjectsById(bamRows*.projectId)

        return new NotificationContent(
                notificationTexts: workflowNotificationContentService.buildSampleNotificationText(sampleRows),
                notificationTextsByRunId: workflowNotificationContentService.buildSampleNotificationTextsByRunId(sampleRows),
                guiUrls: buildGuiUrls(bamRows),
                filePatterns: buildFilePatterns(bamRows, projects),
        )
    }

    private Set<String> buildGuiUrls(Collection<AlignmentBamNotificationRow> rows) {
        return rows.collect { AlignmentBamNotificationRow row ->
            "${otpUrl}/alignmentQualityOverview/index?project=${StringUtils.encodeForUrl(row.projectName)}&seqType=${row.seqTypeId}".toString()
        } as Set
    }

    @SuppressWarnings('GStringExpressionWithinString')
    private Set<String> buildFilePatterns(Collection<AlignmentBamNotificationRow> rows, Map<Long, Project> projects) {
        String pid = '${PID}'
        String sampleType = '${SAMPLE_TYPE}'

        return rows.collect { AlignmentBamNotificationRow row ->
            String antiBodyTarget = row.hasAntibodyTarget ? '-${ANTI_BODY_TARGET}' : ''
            projectService.getSequencingDirectory(projects[row.projectId])
                    .resolve(row.seqTypeDirName)
                    .resolve("view-by-pid")
                    .resolve(pid)
                    .resolve("${sampleType}${antiBodyTarget}".toString())
                    .resolve(row.libraryLayoutDirName)
                    .resolve("merged-alignment")
                    .toString()
        }.unique().sort() as Set
    }
}
