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

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RnaRoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.FastqFile
import de.dkfz.tbi.otp.ngsdata.FastqImportInstance
import de.dkfz.tbi.otp.ngsdata.FileType
import de.dkfz.tbi.otp.ngsdata.LibraryPreparationKit
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

class RnaAlignmentLinkJobSpec extends Specification implements WorkflowSystemDomainFactory, DataTest, RoddyRnaFactory {

    @TempDir
    Path tempWorkDir

    @TempDir
    Path tempBaseDir

    private RnaAlignmentLinkJob job

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowRun,
                WorkflowStep,
                Pipeline,
                LibraryPreparationKit,
                SampleType,
                Sample,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                FileType,
                FastqImportInstance,
                FastqFile,
                RoddyWorkflowConfig,
                RnaRoddyBamFile,
        ]
    }

    void "getLinkMap, should return a list of all created files except the hidden one from work directory to base directory"() {
        given:
        job = Spy(RnaAlignmentLinkJob)
        job.rnaRoddyBamFileService = Mock(RnaRoddyBamFileService)
        job.fileSystemService = new TestFileSystemService()

        String fileName1 = "file1"
        String fileName2 = "file2"
        String fileName3 = ".file3"

        assert(new File(tempWorkDir.toString(), fileName1).createNewFile())
        assert(new File(tempWorkDir.toString(), fileName2).createNewFile())
        assert(new File(tempWorkDir.toString(), fileName3).createNewFile())

        RnaRoddyBamFile bamFile = createBamFile()
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([workDirectory: tempWorkDir.toString()]),
        ])

        List<LinkEntry> expectedLinkEntries = [
                new LinkEntry(target: tempWorkDir.resolve(fileName1), link: tempBaseDir.resolve(fileName1)),
                new LinkEntry(target: tempWorkDir.resolve(fileName2), link: tempBaseDir.resolve(fileName2)),
        ]

        when:
        List<LinkEntry> resultLinkEntries = job.getLinkMap(workflowStep)

        then:
        1 * job.rnaRoddyBamFileService.getBaseDirectory(_) >> tempBaseDir
        1 * job.getRoddyBamFile(_) >> bamFile

        and:
        CollectionUtils.containSame(resultLinkEntries,  expectedLinkEntries)
    }
}
