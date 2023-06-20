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
package de.dkfz.tbi.otp.workflow.fastqc

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.workflow.jobs.AbstractExecuteWesPipelineJob
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.wes.WesWorkflowType

import java.nio.file.Path

/**
 * Job to trigger Fast QC workflows on Weskit system
 */
@Component
@Slf4j
class FastqcExecuteWesPipelineJob extends AbstractExecuteWesPipelineJob implements FastqcShared {

    @Autowired
    FastqcDataFilesService fastqcDataFilesService

    @Autowired
    FastqcReportService fastqcReportService

    @Override
    WesWorkflowType getWorkflowType() {
        return WesWorkflowType.NEXTFLOW
    }

    @Override
    String getWorkflowUrl(WorkflowRun workflowRun) {
        return "nf-seq-qc-${workflowRun.workflowVersion.workflowVersion}/main.nf"
    }

    @Override
    Map<Path, Map<String, String>> getRunSpecificParameters(WorkflowStep workflowStep, Path basePath) {
        Map<Path, Map<String, String>> parameters = [:]

        List<FastqcProcessedFile> fastqcProcessedFiles = getFastqcProcessedFiles(workflowStep)
        fastqcProcessedFiles.eachWithIndex { FastqcProcessedFile fastqcProcessedFile, int idx ->
            Path inputPath = lsdfFilesService.getFileViewByPidPathAsPath(fastqcProcessedFile.dataFile)
            Path outputPath = basePath.resolve(idx.toString())

            parameters.put(outputPath, [
                    input    : inputPath.toString(),
                    outputDir: outputPath.toString(),
            ])
        }

        return parameters
    }

    @Override
    boolean shouldWeskitJobSend(WorkflowStep workflowStep) {
        List<FastqcProcessedFile> fastqcProcessedFiles = getFastqcProcessedFiles(workflowStep)
        if (fastqcReportService.canFastqcReportsBeCopied(fastqcProcessedFiles)) {
            logService.addSimpleLogEntry(workflowStep, "Copying fastqc reports for Weskit")
            fastqcReportService.copyExistingFastqcReports(workflowStep.realm, fastqcProcessedFiles,
                    filestoreService.getWorkFolderPath(workflowStep.workflowRun))
            return false
        }
        return true
    }
}
