/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.workflow.panCancer

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.workflow.jobs.AbstractRoddyClusterValidationJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class PanCancerValidationJob extends AbstractRoddyClusterValidationJob implements PanCancerShared {

    @Autowired
    RoddyBamFileService roddyBamFileService

    /**
     * Returns the expected files for validation
     *
     * @return the following paths
     * <ul>
     *     <li>workBamFile
     *     <li>workBaiFile
     *     <li>workMd5sumFile
     *     <li>workMergedQAJsonFile
     *     <li>workMergedQATargetExtractJsonFile (if seq type needs BED file)
     * </ul>
     */
    @Override
    protected List<Path> getExpectedFiles(WorkflowStep workflowStep) {
        RoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)

        List<Path> expectedFiles = [
                roddyBamFileService.getWorkBamFile(roddyBamFile),
                roddyBamFileService.getWorkBaiFile(roddyBamFile),
                roddyBamFileService.getWorkMd5sumFile(roddyBamFile),
                roddyBamFileService.getWorkMergedQAJsonFile(roddyBamFile),
        ]

        if (roddyBamFile.seqType.needsBedFile) {
            expectedFiles.add(roddyBamFileService.getWorkMergedQATargetExtractJsonFile(roddyBamFile))
        }

        return expectedFiles
    }

    /**
     * Returns the expected directories for validation
     *
     * @return the following paths
     * <ul>
     *     <li>workDirectory
     *     <li>workExecutionStoreDirectory
     * </ul>
     */
    @Override
    protected List<Path> getExpectedDirectories(WorkflowStep workflowStep) {
        RoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)

        return [
                roddyBamFileService.getWorkDirectory(roddyBamFile),
                roddyBamFileService.getWorkExecutionStoreDirectory(roddyBamFile),
        ]
    }

    /**
     * Returns errors if expected read groups are different from the read groups created
     * Empty list means no further validation error
     *
     * @return currently only one error in the list if any exists
     */
    @Override
    protected List<String> doFurtherValidationAndReturnProblems(WorkflowStep workflowStep) {
        RoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)
        final List<String> readGroupsInBam    = roddyService.getReadGroupsInBam(workflowStep)
        final List<String> expectedReadGroups = roddyService.getReadGroupsExpected(workflowStep)

        return (readGroupsInBam == expectedReadGroups) ? Collections.<String> emptyList() : [
                """Read groups in BAM file are not as expected.
                |Read groups in ${roddyBamFileService.getWorkBamFile(roddyBamFile)}:
                |${readGroupsInBam.join('\n')}
                |Expected read groups:
                |${expectedReadGroups.join('\n')}
                |""".stripMargin(),
        ]
    }

    @Override
    protected void saveResult(WorkflowStep workflowStep) {
    }
}
