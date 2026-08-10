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
package de.dkfz.tbi.otp.workflow

import htsjdk.samtools.*

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.RoddyResultWorkFileServiceFactoryService
import de.dkfz.tbi.otp.dataprocessing.roddy.JobStateLogFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.infrastructure.alignment.PanCancerWorkFileService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Files
import java.nio.file.Path

class RoddyService implements WorkflowShared {

    ConcreteArtefactService concreteArtefactService

    PanCancerWorkFileService panCancerWorkFileService

    RoddyResultWorkFileServiceFactoryService roddyResultWorkFileServiceFactoryService

    private static final String OUTPUT_BAM = "BAM"

    List<String> getReadGroupsInBam(WorkflowStep workflowStep) {
        final RoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)
        final Path path = panCancerWorkFileService.getBamFile(roddyBamFile)
        final SamReaderFactory factory = SamReaderFactory.makeDefault().enable(SamReaderFactory.Option.VALIDATE_CRC_CHECKSUMS)
        // read the header from a plain InputStream: the header is located at the beginning of the BAM, therefore
        // sequential access is sufficient. Using a Path instead would make SamReaderFactory attach the bai index and
        // seek via SeekableByteChannel, which the remote (sftp) file system does not support.
        return Files.newInputStream(path).withCloseable { InputStream stream ->
            factory.open(SamInputResource.of(stream)).withCloseable { SamReader reader ->
                return reader.fileHeader.readGroups*.id.sort()
            }
        }
    }

    List<String> getReadGroupsExpected(WorkflowStep workflowStep) {
        final RoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)
        return roddyBamFile.containedSeqTracks*.readGroupName.sort()
    }

    JobStateLogFile getJobStateLogFile(RoddyResult roddyResult) {
        return JobStateLogFile.getInstance(roddyResultWorkFileServiceFactoryService.getService(roddyResult).getLatestExecutionDirectory(roddyResult))
    }

    RoddyBamFile getRoddyBamFile(WorkflowStep workflowStep) {
        return concreteArtefactService.getOutputArtefact(workflowStep, OUTPUT_BAM)
    }
}
