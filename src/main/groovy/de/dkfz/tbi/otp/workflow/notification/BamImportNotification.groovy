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

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.bamImport.BamImportWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

/**
 * {@link WorkflowNotification} for the Externally merged BAM files installation (BamImport) workflow.
 */
@Component
class BamImportNotification extends AbstractWorkflowNotification {

    @Override
    String workflowName() {
        return BamImportWorkflow.WORKFLOW
    }

    @Override
    String pipelineAcknowledgementTemplate() {
        return null
    }

    @Override
    NotificationContent buildContent(Collection<WorkflowRun> workflowRuns) {
        List<BamImportNotificationRow> rows = workflowNotificationContentService.fetchBamImportNotificationRows(
                workflowRuns, BamImportWorkflow.OUTPUT_BAM)
        Map<Long, Project> projects = workflowNotificationContentService.loadProjectsById(rows*.projectId)

        return new NotificationContent(
                notificationTexts: workflowNotificationContentService.buildBamImportNotificationText(rows),
                notificationTextsByRunId: workflowNotificationContentService.buildBamImportNotificationTextsByRunId(rows),
                guiUrls: [] as Set,
                filePatterns: buildFilePatterns(rows, projects),
        )
    }

    @SuppressWarnings('GStringExpressionWithinString')
    private Set<String> buildFilePatterns(Collection<BamImportNotificationRow> rows, Map<Long, Project> projects) {
        String pid = '${PID}'
        String sampleType = '${SAMPLE_TYPE}'

        return rows.collect { BamImportNotificationRow row ->
            String antiBodyTarget = row.hasAntibodyTarget ? '-${ANTI_BODY_TARGET}' : ''
            projectService.getSequencingDirectory(projects[row.projectId])
                    .resolve(row.seqTypeDirName)
                    .resolve("view-by-pid")
                    .resolve(pid)
                    .resolve("${sampleType}${antiBodyTarget}".toString())
                    .resolve(row.libraryLayoutDirName)
                    .resolve("merged-alignment")
                    .resolve("nonOTP")
                    .toString()
        }.unique().sort() as Set
    }
}
