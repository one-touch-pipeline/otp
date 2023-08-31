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
package de.dkfz.tbi.otp.workflow.alignment

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.utils.Md5SumService
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerShared
import de.dkfz.tbi.otp.workflow.jobs.AbstractFinishJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Files
import java.nio.file.Path

@Component
@Slf4j
class RoddyAlignmentFinishJob extends AbstractFinishJob implements PanCancerShared {

    @Autowired
    FileService fileService

    @Autowired
    Md5SumService md5SumService

    @Autowired
    AbstractBamFileService abstractBamFileService

    @Autowired
    RoddyBamFileService roddyBamFileService

    @Override
    void updateDomains(WorkflowStep workflowStep) {
        RoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)

        Path bamFilePath = roddyBamFileService.getWorkBamFile(roddyBamFile)
        Path md5sumPath = roddyBamFileService.getWorkMd5sumFile(roddyBamFile)

        String md5sumValue = md5SumService.extractMd5Sum(md5sumPath)

        roddyBamFile.with {
            fileOperationStatus = AbstractBamFile.FileOperationStatus.PROCESSED
            md5sum = md5sumValue
            fileSize = Files.size(bamFilePath)
            dateFromFileSystem = new Date(Files.getLastModifiedTime(bamFilePath).toMillis())
            save(flush: true)
        }

        roddyBamFile.workPackage.bamFileInProjectFolder = roddyBamFile
        roddyBamFile.workPackage.save(flush: true)

        abstractBamFileService.updateSamplePairStatusToNeedProcessing(roddyBamFile)
    }
}
