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
package de.dkfz.tbi.otp.workflow.alignment.wgbs

import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.alignment.alignment.AbstractRoddyAlignmentValidationJobSpec

import java.nio.file.Path
import java.nio.file.Paths

class WgbsValidationJobSpec extends AbstractRoddyAlignmentValidationJobSpec {

    @Override
    protected String workflowName() {
        return WgbsWorkflow.WORKFLOW
    }

    @Override
    protected WgbsValidationJob createJob() {
        return new WgbsValidationJob()
    }

    @Override
    protected AbstractBamFile createRoddyBamFile() {
        return createRoddyBamFile(RoddyBamFile)
    }

    @Override
    Class[] getDomainClassesToMock() {
        return super.domainClassesToMock + [
                RoddyBamFile,
        ]
    }

    @Unroll
    void "test getExpectedFiles() and getExpectedDirectories, when called the correct paths (files or directories) should be returned"() {
        given:
        RoddyBamFileService roddyBamFileService = new RoddyBamFileService()
        roddyBamFileService.abstractBamFileService = Mock(AbstractBamFileService) {
            getBaseDirectory(_) >> Paths.get("/")
        }

        if (multipleLibraries) {
            abstractBamFile.seqTracks.first().libraryName = "2"
            abstractBamFile.seqTracks.first().normalizedLibraryName = "2"
            abstractBamFile.seqTracks.add(createSeqTrackWithTwoFastqFile(libraryName: "1"))
            abstractBamFile.numberOfMergedLanes = 2
            abstractBamFile.save(flush: true)
        }

        List<Path> expectedFiles = [
                roddyBamFileService.getWorkBamFile(abstractBamFile),
                roddyBamFileService.getWorkBaiFile(abstractBamFile),
                roddyBamFileService.getWorkMd5sumFile(abstractBamFile),
                roddyBamFileService.getWorkMergedQAJsonFile(abstractBamFile),
        ] + roddyBamFileService.getWorkSingleLaneQAJsonFiles(abstractBamFile).values()

        List<Path> expectedDirectories = [
                roddyBamFileService.getWorkDirectory(abstractBamFile),
                roddyBamFileService.getWorkMergedQADirectory(abstractBamFile),
                roddyBamFileService.getWorkExecutionStoreDirectory(abstractBamFile),
                roddyBamFileService.getWorkMergedMethylationDirectory(abstractBamFile),
        ]

        if (multipleLibraries) {
            expectedFiles.addAll(roddyBamFileService.getWorkLibraryQAJsonFiles(abstractBamFile).values())
            expectedDirectories.addAll(roddyBamFileService.getWorkLibraryMethylationDirectories(abstractBamFile).values().unique(false))
        }

        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(_, _) >> abstractBamFile
        }
        job.fileSystemService = new TestFileSystemService()
        job.roddyBamFileService = roddyBamFileService

        when:
        List<Path> files = job.getExpectedFiles(workflowStep)
        List<Path> directories = job.getExpectedDirectories(workflowStep)

        then:
        TestCase.assertContainSame(files, expectedFiles)
        TestCase.assertContainSame(directories, expectedDirectories)

        where:
        multipleLibraries << [true, false]
    }
}
