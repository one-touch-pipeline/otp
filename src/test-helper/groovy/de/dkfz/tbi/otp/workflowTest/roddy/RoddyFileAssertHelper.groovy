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
package de.dkfz.tbi.otp.workflowTest.roddy

import grails.converters.JSON
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.infrastructure.alignment.PanCancerLinkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.PanCancerWorkFileService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflowTest.FileAssertHelper

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

@Component
class RoddyFileAssertHelper {

    @Autowired
    FileAssertHelper fileAssertHelper

    @Autowired
    PanCancerWorkFileService panCancerWorkFileService

    @Autowired
    PanCancerLinkFileService panCancerLinkFileService

    void assertFileSystemState(RoddyBamFile bamFile) {
        // content of the final dir: root
        List<Path> rootDirs = [
                panCancerLinkFileService.getQADirectory(bamFile),
                panCancerLinkFileService.getExecutionStoreDirectory(bamFile),
                panCancerWorkFileService.getDirectoryPath(bamFile),
        ]

        List<Path> rootLinks = [
                panCancerLinkFileService.getBamFile(bamFile),
                panCancerLinkFileService.getBaiFile(bamFile),
                panCancerLinkFileService.getMd5sumFile(bamFile),
                getLinkMergedQADirectory(bamFile),
        ]

        rootDirs.addAll(getAdditionalDirectories(bamFile))
        rootLinks.addAll(getAdditionalLinks(bamFile))

        fileAssertHelper.assertDirectoryContentReadable(rootDirs, [], rootLinks)

        assertQaFileSystemState(bamFile)
    }

    private void assertQaFileSystemState(RoddyBamFile bamFile) {
        // content of the final qa dir
        List<Path> qaDirs = getLinkSingleLaneQADirectories(bamFile).values() + [getLinkMergedQADirectory(bamFile)]
        qaDirs.addAll(getAdditionalQaDirectories(bamFile))
        fileAssertHelper.assertDirectoryContentReadable([], [], qaDirs)

        // qa for merged and one for each read group and for each library (if available)
        int numberOfFilesInFinalQaDir = bamFile.numberOfMergedLanes + 1
        if (bamFile.seqType.wgbs && bamFile.hasMultipleLibraries()) {
            numberOfFilesInFinalQaDir += bamFile.seqTracks*.libraryDirectoryName.unique().size()
        }
        Stream<Path> paths = null
        try {
            paths = Files.list(panCancerLinkFileService.getQADirectory(bamFile))
            assert numberOfFilesInFinalQaDir == paths.count()
        } finally {
            paths?.close()
        }
    }

    void assertWorkDirectoryFileSystemState(RoddyBamFile bamFile) {
        List<Path> rootDirs = [
                panCancerWorkFileService.getQADirectory(bamFile),
                panCancerWorkFileService.getExecutionStoreDirectory(bamFile),
                getWorkMergedQADirectory(bamFile),
                panCancerWorkFileService.getConfigDirectory(bamFile),
        ]

        List<Path> rootFiles = []
        rootFiles << panCancerWorkFileService.getBamFile(bamFile)
        rootFiles << panCancerWorkFileService.getBaiFile(bamFile)
        rootFiles << panCancerWorkFileService.getMd5sumFile(bamFile)

        rootDirs.addAll(getAdditionalWorkDirectories(bamFile))
        rootFiles.addAll(getAdditionalWorkFiles(bamFile))

        fileAssertHelper.assertDirectoryContentReadable(rootDirs, rootFiles)
        if (!bamFile.seqType.rna) {
            fileAssertHelper.assertDirectorySameContent(getWorkDirectory(bamFile), rootDirs, rootFiles)
        }

        assertQaWorkDirectoryFileSystemState(bamFile)
    }

    private void assertQaWorkDirectoryFileSystemState(RoddyBamFile bamFile) {
        // the default json is checked in the base class, here only additional json are checked
        List<Path> qaJson = []
        List<Path> qaDirs = [getWorkMergedQADirectory(bamFile)]

        qaDirs.addAll(getWorkSingleLaneQADirectories(bamFile).values())
        qaJson.addAll(getWorkSingleLaneQAJsonFiles(bamFile).values())

        qaDirs.addAll(getAdditionalQaWorkDirectories(bamFile))
        qaJson.addAll(getAdditionalQaWorkFiles(bamFile))

        fileAssertHelper.assertDirectoryContentReadable(qaDirs)
        if (!bamFile.seqType.rna) {
            fileAssertHelper.assertDirectorySameContent(getWorkQADirectory(bamFile), qaDirs)
        }

        qaJson.each {
            fileAssertHelper.assertFileIsReadableAndNotEmpty(it)
            JSON.parse(it.text) // throws ConverterException when the JSON content is not valid
        }
    }

    Path getWorkDirectory(RoddyBamFile bamFile) {
        return panCancerWorkFileService.getDirectoryPath(bamFile)
    }

    Path getLinkMergedQADirectory(RoddyBamFile bamFile) {
        return panCancerLinkFileService.getMergedQADirectory(bamFile)
    }

    Path getWorkMergedQADirectory(RoddyBamFile bamFile) {
        return panCancerWorkFileService.getMergedQADirectory(bamFile)
    }

    Path getWorkQADirectory(RoddyBamFile bamFile) {
        return panCancerWorkFileService.getQADirectory(bamFile)
    }

    Map<SeqTrack, Path> getLinkSingleLaneQADirectories(RoddyBamFile bamFile) {
        return panCancerLinkFileService.getSingleLaneQADirectories(bamFile)
    }

    Map<SeqTrack, Path> getWorkSingleLaneQADirectories(RoddyBamFile bamFile) {
        return panCancerWorkFileService.getSingleLaneQADirectories(bamFile)
    }

    Map<SeqTrack, Path> getWorkSingleLaneQAJsonFiles(RoddyBamFile bamFile) {
        return panCancerWorkFileService.getSingleLaneQAJsonFiles(bamFile)
    }

    @SuppressWarnings("UnusedMethodParameter")
    List<Path> getAdditionalDirectories(RoddyBamFile bamFile) {
        return []
    }

    @SuppressWarnings("UnusedMethodParameter")
    List<Path> getAdditionalLinks(RoddyBamFile bamFile) {
        return []
    }

    @SuppressWarnings("UnusedMethodParameter")
    List<Path> getAdditionalQaDirectories(RoddyBamFile bamFile) {
        return []
    }

    @SuppressWarnings("UnusedMethodParameter")
    List<Path> getAdditionalWorkDirectories(RoddyBamFile bamFile) {
        return []
    }

    @SuppressWarnings("UnusedMethodParameter")
    List<Path> getAdditionalWorkFiles(RoddyBamFile bamFile) {
        return []
    }

    @SuppressWarnings("UnusedMethodParameter")
    List<Path> getAdditionalQaWorkDirectories(RoddyBamFile bamFile) {
        return []
    }

    @SuppressWarnings("UnusedMethodParameter")
    List<Path> getAdditionalQaWorkFiles(RoddyBamFile bamFile) {
        return []
    }
}
