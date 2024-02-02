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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RoddyConfigService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.LinkFileUtils

import java.nio.file.FileSystem
import java.nio.file.Path

@CompileDynamic
@Transactional
class LinkFilesToFinalDestinationService {

    FileService fileService
    FileSystemService fileSystemService
    LinkFileUtils linkFileUtils
    RoddyConfigService roddyConfigService
    RoddyBamFileService roddyBamFileService

    /**
     * Link files (replaces existing files)
     */
    @Deprecated
    void linkNewResults(RoddyBamFile roddyBamFile) {
        assert roddyBamFile: "Input roddyBamFile must not be null"
        assert !roddyBamFile.isOldStructureUsed()

        Map<File, File> linkMapSourceLink = [:]

        // collect links for files and qa merge directory
        ['Bam', 'Bai', 'Md5sum'].each {
            linkMapSourceLink.put(roddyBamFile."work${it}File", roddyBamFile."final${it}File")
        }
        linkMapSourceLink.put(roddyBamFile.workMergedQADirectory, roddyBamFile.finalMergedQADirectory)

        if (roddyBamFile.seqType.isWgbs()) {
            linkMapSourceLink.put(roddyBamFile.workMergedMethylationDirectory, roddyBamFile.finalMergedMethylationDirectory)
            if (roddyBamFile.containedSeqTracks*.libraryDirectoryName.unique().size() > 1) {
                [
                        roddyBamFile.workLibraryQADirectories.values().asList().sort(),
                        roddyBamFile.finalLibraryQADirectories.values().asList().sort(),
                ].transpose().each {
                    linkMapSourceLink.put(it[0], it[1])
                }
                [
                        roddyBamFile.workLibraryMethylationDirectories.values().asList().sort(),
                        roddyBamFile.finalLibraryMethylationDirectories.values().asList().sort(),
                ].transpose().each {
                    linkMapSourceLink.put(it[0], it[1])
                }
            }
            linkMapSourceLink.put(roddyBamFile.workMetadataTableFile, roddyBamFile.finalMetadataTableFile)
        }

        // collect links for every execution store
        [roddyBamFile.workExecutionDirectories, roddyBamFile.finalExecutionDirectories].transpose().each {
            linkMapSourceLink.put(it[0], it[1])
        }

        // collect links for the single lane qa
        Map<SeqTrack, File> workSingleLaneQADirectories = roddyBamFile.workSingleLaneQADirectories
        Map<SeqTrack, File> finalSingleLaneQADirectories = roddyBamFile.finalSingleLaneQADirectories
        workSingleLaneQADirectories.each { seqTrack, singleLaneQaWorkDir ->
            File singleLaneQcDirFinal = finalSingleLaneQADirectories.get(seqTrack)
            linkMapSourceLink.put(singleLaneQaWorkDir, singleLaneQcDirFinal)
        }

        // create the collected links
        linkFileUtils.createAndValidateLinks(linkMapSourceLink, roddyBamFile.project.unixGroup)
    }

    @Deprecated
    void linkNewRnaResults(RnaRoddyBamFile roddyBamFile) {
        File baseDirectory = roddyBamFile.baseDirectory
        Map<File, File> links = roddyBamFile.workDirectory.listFiles().findAll {
            !it.name.startsWith(".")
        }.collectEntries { File source ->
            [(source): new File(baseDirectory, source.name)]
        }
        linkFileUtils.createAndValidateLinks(links, roddyBamFile.project.unixGroup)
    }

    List<Path> getFilesToCleanup(RoddyBamFile roddyBamFile) {
        assert roddyBamFile: "Input roddyBamFile must not be null"
        assert !roddyBamFile.isOldStructureUsed()

        List<File> expectedFiles = [
                roddyBamFile.workBamFile,
                roddyBamFile.workBaiFile,
                roddyBamFile.workMd5sumFile,
                roddyBamFile.workQADirectory,
                roddyBamFile.workExecutionStoreDirectory,
                fileService.toFile(roddyConfigService.getConfigDirectory(roddyBamFile.workDirectory.toPath())),
        ]
        if (roddyBamFile.seqType.isWgbs()) {
            expectedFiles.add(roddyBamFile.workMethylationDirectory)
            expectedFiles.add(roddyBamFile.workMetadataTableFile)
        }
        List<File> foundFiles = roddyBamFile.workDirectory.listFiles() ?: []
        List<File> filesToDelete = foundFiles - expectedFiles
        FileSystem fs = fileSystemService.remoteFileSystem
        return filesToDelete.collect { fileService.toPath(it, fs) }
    }

    List<Path> getOldResultsToCleanup(RoddyBamFile roddyBamFile) {
        assert roddyBamFile: "Input roddyBamFile must not be null"
        assert !roddyBamFile.isOldStructureUsed()

        List<Path> filesToDelete = []
        List<RoddyBamFile> roddyBamFiles = RoddyBamFile.findAllByWorkPackageAndIdNotEqual(roddyBamFile.mergingWorkPackage, roddyBamFile.id)
        if (roddyBamFiles) {
            List<Path> workDirs = roddyBamFiles.findAll { !it.isOldStructureUsed() }.collect {
                roddyBamFileService.getWorkDirectory(it)
            }
            filesToDelete.addAll(workDirs)
            filesToDelete.add(roddyBamFileService.getFinalExecutionStoreDirectory(roddyBamFile))
            filesToDelete.add(roddyBamFileService.getFinalQADirectory(roddyBamFile))
            if (roddyBamFile.seqType.isWgbs()) {
                filesToDelete.add(roddyBamFileService.getFinalMethylationDirectory(roddyBamFile))
            }
        }
        return filesToDelete
    }
}
