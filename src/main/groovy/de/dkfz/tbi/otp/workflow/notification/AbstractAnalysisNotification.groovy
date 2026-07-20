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

import groovy.transform.CompileStatic

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.StringUtils
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

import java.nio.file.Path

/**
 * Shared {@link WorkflowNotification} for all analysis workflows.
 *
 * All content sections are derived from a single projection of the analysis output artefacts (the sample pairs).
 * The GUI controller and the result directory of the file patterns are workflow specific and provided by the
 * concrete subclasses. As for the workflow jobs, the methods rely on the surrounding transaction of the caller.
 */
@CompileStatic
abstract class AbstractAnalysisNotification extends AbstractWorkflowNotification {

    /**
     * @return the GUI controller showing the results, e.g. {@code "snv"} for {@code ${OTP_URL}/snv/results}
     */
    protected abstract String getGuiResultController()

    /**
     * @return the result directory inside view-by-pid, e.g. {@code "snv_results"}
     */
    protected abstract String getResultDirectoryName()

    @Override
    NotificationContent buildContent(Collection<WorkflowRun> workflowRuns) {
        List<SamplePairNotificationRow> rows = workflowNotificationContentService.fetchSamplePairRows(
                workflowRuns, AbstractAnalysisWorkflow.ANALYSIS_OUTPUT)
        Map<Long, Project> projects = workflowNotificationContentService.loadProjectsById(rows*.projectId)

        return new NotificationContent(
                notificationTexts: workflowNotificationContentService.buildSamplePairNotificationText(rows),
                guiUrls: buildGuiUrls(rows),
                filePatterns: buildFilePatterns(rows, projects),
        )
    }

    private Set<String> buildGuiUrls(Collection<SamplePairNotificationRow> rows) {
        return rows.collect { SamplePairNotificationRow row ->
            "${otpUrl}/${guiResultController}/results?project=${StringUtils.encodeForUrl(row.projectName)}".toString()
        } as Set
    }

    @SuppressWarnings('GStringExpressionWithinString')
    private Set<String> buildFilePatterns(Collection<SamplePairNotificationRow> rows, Map<Long, Project> projects) {
        String pid = '${PID}'
        String seqTypeDir = '${SEQUENCING_TYPE_DIR}'
        String sampleTypes = '${SAMPLE_TYPE1}_${SAMPLE_TYPE2}'

        return rows*.projectId.unique().collect { Long projectId ->
            Path sequencingDirectory = projectService.getSequencingDirectory(projects[projectId])
            "${sequencingDirectory}/${seqTypeDir}/view-by-pid/${pid}/${resultDirectoryName}/paired/${sampleTypes}".toString()
        } as Set
    }
}
