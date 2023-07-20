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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.WesFastQcWorkflow
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersion
import de.dkfz.tbi.util.TimeFormats

import java.time.ZonedDateTime

@Transactional(readOnly = true)
class FastQcProcessedFileService {

    ConfigService configService

    @CompileDynamic
    FastqcProcessedFile findSingleByRawSequenceFile(RawSequenceFile rawSequenceFile) {
        return CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllBySequenceFile(rawSequenceFile))
    }

    /**
     * return a working path for the fastqc workflow.
     *
     * It contains:
     * - the prefix 'bash'
     * - the version of the workflow
     * - a timestamp in the format {@link TimeFormats#DATE_TIME_SECONDS_DASHES}
     *
     * Repeated calls create different values, so keep the result if needed multiple times.
     */
    String buildWorkingPath(WorkflowVersion workflowVersion) {
        String prefix
        switch (workflowVersion.workflow.name) {
            case BashFastQcWorkflow.WORKFLOW:
                prefix = 'bash'
                break
            case WesFastQcWorkflow.WORKFLOW:
                prefix = 'wes'
                break
            default:
                throw new WorkflowException("unsupported fastqc workflow: ${workflowVersion.workflow.name}")
        }
        ZonedDateTime zonedDateTime = configService.zonedDateTime
        return "${prefix}-${workflowVersion.workflowVersion}-${TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedZonedDateTime(zonedDateTime)}"
    }
}
