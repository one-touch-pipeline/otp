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

import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFileService
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RnaRoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.alignment.alignment.AbstractRoddyAlignmentValidationJobSpec

import java.nio.file.Path
import java.nio.file.Paths

class RnaAlignmentValidationJobSpec extends AbstractRoddyAlignmentValidationJobSpec {

    @Override
    protected String workflowName() {
        return RnaAlignmentWorkflow.WORKFLOW
    }

    @Override
    protected RnaAlignmentValidationJob createJob() {
        return new RnaAlignmentValidationJob()
    }

    @Override
    protected AbstractBamFile createRoddyBamFile() {
        return createRoddyBamFile(RnaRoddyBamFile)
    }

    @Override
    Class[] getDomainClassesToMock() {
        return super.domainClassesToMock + [
                RnaRoddyBamFile,
        ]
    }

    @Unroll
    void "test getExpectedFiles() and getExpectedDirectories, when called the correct paths (files or directories) should be returned"() {
        given:
        RnaRoddyBamFileService rnaRoddyBamFileService = new RnaRoddyBamFileService()
        rnaRoddyBamFileService.abstractBamFileService = Mock(AbstractBamFileService) {
            getBaseDirectory(_) >> Paths.get("/")
        }

        List<Path> expectedFiles = [
                rnaRoddyBamFileService.getWorkBamFile(abstractBamFile),
                rnaRoddyBamFileService.getWorkBaiFile(abstractBamFile),
                rnaRoddyBamFileService.getWorkMd5sumFile(abstractBamFile),
                rnaRoddyBamFileService.getWorkMergedQAJsonFile(abstractBamFile),
                rnaRoddyBamFileService.getCorrespondingWorkChimericBamFile(abstractBamFile),
                rnaRoddyBamFileService.getWorkArribaFusionPlotPdf(abstractBamFile),
        ]

        List<Path> expectedDirectories = [
                rnaRoddyBamFileService.getWorkDirectory(abstractBamFile),
                rnaRoddyBamFileService.getWorkExecutionStoreDirectory(abstractBamFile),
                rnaRoddyBamFileService.getWorkMergedQADirectory(abstractBamFile),
        ]

        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(_, _) >> abstractBamFile
        }
        job.fileSystemService = new TestFileSystemService()
        job.roddyBamFileService = rnaRoddyBamFileService
        job.rnaRoddyBamFileService = rnaRoddyBamFileService

        when:
        List<Path> files = job.getExpectedFiles(workflowStep)
        List<Path> directories = job.getExpectedDirectories(workflowStep)

        then:
        TestCase.assertContainSame(files, expectedFiles)
        TestCase.assertContainSame(directories, expectedDirectories)

        where:
        needsBedFile << [true, false]
    }
}
