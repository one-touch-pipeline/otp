/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.workflowTest.alignment.roddy.pancancer

import grails.converters.JSON

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflow.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflowExecution.OtpWorkflow
import de.dkfz.tbi.otp.workflowExecution.decider.AbstractWorkflowDecider
import de.dkfz.tbi.otp.workflowExecution.decider.PanCancerDecider
import de.dkfz.tbi.otp.workflowTest.FileAssertHelper
import de.dkfz.tbi.otp.workflowTest.alignment.roddy.AbstractRoddyAlignmentWorkflowSpec

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

/**
 * base class for all PanCancer workflow tests
 */
abstract class AbstractPanCancerWorkflowSpec extends AbstractRoddyAlignmentWorkflowSpec {

    PanCancerDecider panCancerDecider

    Class<? extends OtpWorkflow> workflowComponentClass = PanCancerWorkflow

    @Override
    protected AbstractWorkflowDecider getDecider() {
        return panCancerDecider
    }

    @Override
    String getWorkflowName() {
        return PanCancerWorkflow.WORKFLOW
    }

    void "test align lanes only, no base bam file exists, one lane, all fine"() {
        given:
        SessionUtils.withTransaction {
            createSeqTrack("readGroup1")
            decide(3, 1)
        }

        when:
        execute(1, 2)

        then:
        verify_AlignLanesOnly_AllFine()
    }

    @Override
    protected void assertWorkflowFileSystemState(RoddyBamFile bamFile) {
        //content of the final dir: root
        List<Path> rootDirs = [
                roddyBamFileService.getFinalQADirectory(bamFile),
                roddyBamFileService.getFinalExecutionStoreDirectory(bamFile),
                roddyBamFileService.getWorkDirectory(bamFile),
        ]

        List<Path> rootLinks = [
                roddyBamFileService.getFinalBamFile(bamFile),
                roddyBamFileService.getFinalBaiFile(bamFile),
                roddyBamFileService.getFinalMd5sumFile(bamFile),
                roddyBamFileService.getFinalMergedQADirectory(bamFile),
        ]
        if (bamFile.seqType.wgbs) {
            rootDirs << roddyBamFileService.getFinalMethylationDirectory(bamFile)
            rootLinks << roddyBamFileService.getFinalMetadataTableFile(bamFile)
            rootLinks << roddyBamFileService.getFinalMergedMethylationDirectory(bamFile)

            if (bamFile.hasMultipleLibraries()) {
                rootLinks.addAll(roddyBamFileService.getFinalLibraryMethylationDirectories(bamFile).values())
                rootLinks.addAll(roddyBamFileService.getFinalLibraryQADirectories(bamFile).values())
            }
        }
        if (bamFile.baseBamFile) {
            rootDirs << roddyBamFileService.getWorkDirectory(bamFile.baseBamFile)
        }
        FileAssertHelper.assertDirectoryContent(roddyBamFileService.getBaseDirectory(bamFile), rootDirs, [], rootLinks)

        assertQaFileSystemState(bamFile)
    }

    private void assertQaFileSystemState(RoddyBamFile bamFile) {
        // content of the final qa dir
        List<Path> qaDirs = roddyBamFileService.getFinalSingleLaneQADirectories(bamFile).values() + [roddyBamFileService.getFinalMergedQADirectory(bamFile)]
        if (bamFile.baseBamFile) {
            qaDirs.addAll(roddyBamFileService.getFinalSingleLaneQADirectories(bamFile.baseBamFile).values())
        }
        if (bamFile.seqType.wgbs && bamFile.hasMultipleLibraries()) {
            qaDirs.addAll(roddyBamFileService.getFinalLibraryQADirectories(bamFile).values())
        }
        FileAssertHelper.assertDirectoryContent(roddyBamFileService.getFinalQADirectory(bamFile), [], [], qaDirs)

        // qa for merged and one for each read group and for each library (if available)
        int numberOfFilesInFinalQaDir = bamFile.numberOfMergedLanes + 1
        if (bamFile.seqType.wgbs && bamFile.hasMultipleLibraries()) {
            numberOfFilesInFinalQaDir += bamFile.seqTracks*.libraryDirectoryName.unique().size()
        }
        Stream<Path> paths = null
        try {
            paths = Files.list(roddyBamFileService.getFinalQADirectory(bamFile))
            assert numberOfFilesInFinalQaDir == paths.count()
        } finally {
            paths?.close()
        }
    }

    @Override
    protected void assertWorkflowWorkDirectoryFileSystemState(RoddyBamFile bamFile, boolean isBaseBamFile) {
        List<Path> rootDirs = [
                roddyBamFileService.getWorkQADirectory(bamFile),
                roddyBamFileService.getWorkExecutionStoreDirectory(bamFile),
                roddyBamFileService.getWorkMergedQADirectory(bamFile),
        ]
        List<Path> rootFiles = []
        if (isBaseBamFile) {
            rootFiles << roddyBamFileService.getWorkMd5sumFile(bamFile)
        } else {
            rootFiles << roddyBamFileService.getWorkBamFile(bamFile)
            rootFiles << roddyBamFileService.getWorkBaiFile(bamFile)
            rootFiles << roddyBamFileService.getWorkMd5sumFile(bamFile)
        }
        if (bamFile.seqType.wgbs) {
            rootDirs << roddyBamFileService.getWorkMethylationDirectory(bamFile)
            rootDirs << roddyBamFileService.getWorkMergedMethylationDirectory(bamFile)

            rootFiles << roddyBamFileService.getWorkMetadataTableFile(bamFile)
            if (bamFile.hasMultipleLibraries()) {
                rootDirs.addAll(roddyBamFileService.getWorkLibraryMethylationDirectories(bamFile).values())
                rootDirs.addAll(roddyBamFileService.getWorkLibraryQADirectories(bamFile).values())
            }
        }
        FileAssertHelper.assertDirectoryContent(roddyBamFileService.getWorkDirectory(bamFile), rootDirs, rootFiles)

        assertQaWorkflowWorkDirectoryFileSystemState(bamFile)
    }

    private void assertQaWorkflowWorkDirectoryFileSystemState(RoddyBamFile bamFile) {
        //the default json is checked in the base class, here only additional json are checked
        List<Path> qaJson = []
        List<Path> qaDirs = [roddyBamFileService.getWorkMergedQADirectory(bamFile)]

        qaDirs.addAll(roddyBamFileService.getWorkSingleLaneQADirectories(bamFile).values())
        qaJson.addAll(roddyBamFileService.getWorkSingleLaneQAJsonFiles(bamFile).values())

        if (bamFile.seqType.wgbs && bamFile.hasMultipleLibraries()) {
            qaDirs.addAll(roddyBamFileService.getWorkLibraryQADirectories(bamFile).values())
            qaJson.addAll(roddyBamFileService.getWorkLibraryQAJsonFiles(bamFile).values())
        }

        FileAssertHelper.assertDirectoryContent(roddyBamFileService.getWorkQADirectory(bamFile), qaDirs)

        qaJson.each {
            FileAssertHelper.assertFileIsReadableAndNotEmpty(it)
            JSON.parse(it.text) // throws ConverterException when the JSON content is not valid
        }
    }
}
