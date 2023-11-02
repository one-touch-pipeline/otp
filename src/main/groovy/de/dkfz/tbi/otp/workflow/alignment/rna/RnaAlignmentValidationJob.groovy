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
package de.dkfz.tbi.otp.workflow.alignment.rna

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RnaRoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.job.processing.RoddyConfigValueService
import de.dkfz.tbi.otp.workflow.alignment.AbstractRoddyAlignmentValidationJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class RnaAlignmentValidationJob extends AbstractRoddyAlignmentValidationJob implements RnaAlignmentShared {

    @Autowired
    RnaRoddyBamFileService rnaRoddyBamFileService

    @Autowired
    RoddyConfigValueService roddyConfigValueService

    @Override
    protected List<Path> getExpectedDirectories(WorkflowStep workflowStep) {
        List<Path> directories = super.getExpectedDirectories(workflowStep)

        RoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)
        directories.add(rnaRoddyBamFileService.getWorkMergedQADirectory(roddyBamFile))

        return directories
    }

    @Override
    protected List<Path> getExpectedFiles(WorkflowStep workflowStep) {
        List<Path> expectedFiles = super.getExpectedFiles(workflowStep)

        RnaRoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)
        expectedFiles.add(rnaRoddyBamFileService.getWorkMergedQAJsonFile(roddyBamFile))
        expectedFiles.add(rnaRoddyBamFileService.getCorrespondingWorkChimericBamFile(roddyBamFile))
        if (roddyConfigValueService.getRunArriba(workflowStep)) {
            expectedFiles.add(rnaRoddyBamFileService.getWorkArribaFusionPlotPdf(roddyBamFile))
        }

        return expectedFiles
    }
}
