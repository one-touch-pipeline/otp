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
package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.RoddyConfigService
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersion
import de.dkfz.tbi.otp.utils.TimeFormats

import java.nio.file.Path
import java.time.ZonedDateTime

@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class AbstractAnalysisWorkFileService<T extends BamFilePairAnalysis> implements ArtefactFileService<T> {

    ConfigService configService

    FilestoreService filestoreService
    RoddyConfigService roddyConfigService
    FileService fileService

    Path getDirectoryPath(T instance) {
        return filestoreService.getWorkFolderPath(instance.workflowArtefact.producedBy)
    }

    /**
     * Returns the workflow name part of the directory where the workflow results are stored.
     *
     * For migrated workflows it should have the same value as in the old workflow system.
     *
     * @see #constructInstanceName(WorkflowVersion)
     */
    abstract String getWorkflowDirectoryName()

    /**
     * Construct the name to use for the analysis directory.
     *
     * Its based on following components:
     * - the prefix result
     * - the workflow directory name as returned by {@link #getWorkflowDirectoryName()} (for roddy workflows: the roddy plugin name))
     * - the used plugin version
     * - the current date and time (use format {@link TimeFormats#DATE_TIME_DASHES})
     */
    String constructInstanceName(WorkflowVersion workflowVersion) {
        ZonedDateTime zonedDateTime = configService.zonedDateTime
        String formattedDate = TimeFormats.DATE_TIME_DASHES.getFormattedZonedDateTime(zonedDateTime)
        return "results_${workflowDirectoryName}-${workflowVersion.workflowVersion}_${formattedDate}"
    }
}
