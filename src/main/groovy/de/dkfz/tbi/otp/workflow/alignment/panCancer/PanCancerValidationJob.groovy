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
package de.dkfz.tbi.otp.workflow.alignment.panCancer

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.workflow.alignment.AbstractRoddyAlignmentValidationJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class PanCancerValidationJob extends AbstractRoddyAlignmentValidationJob implements PanCancerShared {

    @Override
    protected List<Path> getExpectedDirectories(WorkflowStep workflowStep) {
        List<Path> directories = super.getExpectedDirectories(workflowStep)

        RoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)
        directories.add(roddyBamFileService.getWorkMergedQADirectory(roddyBamFile))

        return directories
    }

    /**
     * Returns the expected files for validation
     *
     * @return the following paths
     * <ul>
     *     <li>super.getExpectedFiles</li>
     *     <li>workMergedQAJsonFile</li>
     *     <li>workMergedQATargetExtractJsonFile (if seq type needs BED file)</li>
     * </ul>
     */
    @Override
    protected List<Path> getExpectedFiles(WorkflowStep workflowStep) {
        List<Path> expectedFiles = super.getExpectedFiles(workflowStep)

        RoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)

        expectedFiles.add(roddyBamFileService.getWorkMergedQAJsonFile(roddyBamFile))
        if (roddyBamFile.seqType.needsBedFile) {
            expectedFiles.add(roddyBamFileService.getWorkMergedQATargetExtractJsonFile(roddyBamFile))
        }
        expectedFiles.addAll(roddyBamFileService.getWorkSingleLaneQAJsonFiles(roddyBamFile).values())

        return expectedFiles
    }
}
