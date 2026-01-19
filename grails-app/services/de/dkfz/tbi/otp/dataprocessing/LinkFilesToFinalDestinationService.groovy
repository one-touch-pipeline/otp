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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.alignment.*
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RoddyConfigService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.LinkEntry

import java.nio.file.Files
import java.nio.file.Path

@Transactional
class LinkFilesToFinalDestinationService {

    FileService fileService
    FileSystemService fileSystemService
    RoddyConfigService roddyConfigService
    PanCancerWorkFileService panCancerWorkFileService
    PanCancerLinkFileService panCancerLinkFileService
    RnaAlignmentWorkFileService rnaAlignmentWorkFileService
    RnaAlignmentLinkFileService rnaAlignmentLinkFileService
    WgbsAlignmentWorkFileService wgbsAlignmentWorkFileService
    WgbsAlignmentLinkFileService wgbsAlignmentLinkFileService

    /**
     * Link files (replaces existing files)
     */
    @Deprecated
    @CompileDynamic
    void linkNewResults(RoddyBamFile roddyBamFile) {
        assert roddyBamFile: "Input roddyBamFile must not be null"
        assert !roddyBamFile.isOldStructureUsed()

        List<LinkEntry> links = []

        // collect links for files and qa merge directory
        links.add(new LinkEntry(link: panCancerLinkFileService.getBamFile(roddyBamFile),
                target: panCancerWorkFileService.getBamFile(roddyBamFile)))
        links.add(new LinkEntry(link: panCancerLinkFileService.getBaiFile(roddyBamFile),
                target: panCancerWorkFileService.getBaiFile(roddyBamFile)))
        links.add(new LinkEntry(link: panCancerLinkFileService.getMd5sumFile(roddyBamFile),
                target: panCancerWorkFileService.getMd5sumFile(roddyBamFile)))
        links.add(new LinkEntry(link: panCancerLinkFileService.getMergedQADirectory(roddyBamFile),
                target: panCancerWorkFileService.getMergedQADirectory(roddyBamFile)))

        if (roddyBamFile.seqType.isWgbs()) {
            links.add(new LinkEntry(link: wgbsAlignmentLinkFileService.getMergedMethylationDirectory(roddyBamFile),
                    target: wgbsAlignmentWorkFileService.getMergedMethylationDirectory(roddyBamFile)))
            if (roddyBamFile.containedSeqTracks*.libraryDirectoryName.unique().size() > 1) {
                [
                        wgbsAlignmentLinkFileService.getLibraryQADirectories(roddyBamFile).values().asList().sort(),
                        wgbsAlignmentWorkFileService.getLibraryQADirectories(roddyBamFile).values().asList().sort(),
                ].transpose().each {
                    links.add(new LinkEntry(link: it[0], target: it[1]))
                }
                [
                        wgbsAlignmentLinkFileService.getLibraryMethylationDirectories(roddyBamFile).values().asList().sort(),
                        wgbsAlignmentWorkFileService.getLibraryMethylationDirectories(roddyBamFile).values().asList().sort(),
                ].transpose().each {
                    links.add(new LinkEntry(link: it[0], target: it[1]))
                }
            }
            links.add(new LinkEntry(link: wgbsAlignmentLinkFileService.getMetadataTableFile(roddyBamFile),
                    target: wgbsAlignmentWorkFileService.getMetadataTableFile(roddyBamFile)))
        }

        // collect links for every execution store
        [panCancerLinkFileService.getExecutionDirectories(roddyBamFile), panCancerWorkFileService.getExecutionDirectories(roddyBamFile)].transpose().each {
            links.add(new LinkEntry(link: it[0], target: it[1]))
        }

        // collect links for the single lane qa
        Map<SeqTrack, Path> workSingleLaneQADirectories = panCancerWorkFileService.getSingleLaneQADirectories(roddyBamFile)
        Map<SeqTrack, Path> finalSingleLaneQADirectories = panCancerLinkFileService.getSingleLaneQADirectories(roddyBamFile)
        workSingleLaneQADirectories.each { seqTrack, singleLaneQaWorkDir ->
            Path singleLaneQcDirFinal = finalSingleLaneQADirectories.get(seqTrack)
            links.add(new LinkEntry(link: singleLaneQcDirFinal, target: singleLaneQaWorkDir))
        }

        // create the collected links
        links.each {
            fileService.createLink(it.link, it.target, roddyBamFile.project.unixGroup)
        }
    }

    @Deprecated
    void linkNewRnaResults(RnaRoddyBamFile roddyBamFile) {
        Path baseDirectory = rnaAlignmentLinkFileService.getDirectoryPath(roddyBamFile)
        List<LinkEntry> links = Files.list(rnaAlignmentWorkFileService.getDirectoryPath(roddyBamFile)).toList().findAll { Path it ->
            !it.fileName.toString().startsWith(".")
        }.collect { Path source ->
            new LinkEntry(link: baseDirectory.resolve(source.fileName), target: source)
        }
        links.each {
            fileService.createLink(it.link, it.target, roddyBamFile.project.unixGroup)
        }
    }

    List<Path> getUnusedResultFiles(RoddyBamFile roddyBamFile) {
        assert roddyBamFile: "Input roddyBamFile must not be null"
        assert !roddyBamFile.isOldStructureUsed()

        List<Path> expectedFiles = [
                panCancerWorkFileService.getBamFile(roddyBamFile),
                panCancerWorkFileService.getBaiFile(roddyBamFile),
                panCancerWorkFileService.getMd5sumFile(roddyBamFile),
                panCancerWorkFileService.getQADirectory(roddyBamFile),
                panCancerWorkFileService.getExecutionStoreDirectory(roddyBamFile),
                panCancerWorkFileService.getConfigDirectory(roddyBamFile),
        ]
        if (roddyBamFile.seqType.isWgbs()) {
            expectedFiles.add(wgbsAlignmentWorkFileService.getMethylationDirectory(roddyBamFile))
            expectedFiles.add(wgbsAlignmentWorkFileService.getMetadataTableFile(roddyBamFile))
        }
        List<Path> foundFiles = Files.list(panCancerWorkFileService.getDirectoryPath(roddyBamFile)).toList() ?: []
        List<Path> unexpectedFiles = foundFiles - expectedFiles
        return unexpectedFiles
    }

    @CompileDynamic
    List<Path> getOldAdditionalResults(RoddyBamFile roddyBamFile) {
        assert roddyBamFile: "Input roddyBamFile must not be null"
        assert !roddyBamFile.isOldStructureUsed()

        List<Path> filesToDelete = []
        List<RoddyBamFile> roddyBamFiles = RoddyBamFile.findAllByWorkPackageAndIdNotEqual(roddyBamFile.mergingWorkPackage, roddyBamFile.id)
        if (roddyBamFiles) {
            List<Path> workDirs = roddyBamFiles.findAll { !it.isOldStructureUsed() }.collect {
                panCancerWorkFileService.getDirectoryPath(it)
            }
            filesToDelete.addAll(workDirs)
            filesToDelete.add(panCancerLinkFileService.getExecutionStoreDirectory(roddyBamFile))
            filesToDelete.add(panCancerLinkFileService.getQADirectory(roddyBamFile))
            if (roddyBamFile.seqType.isWgbs()) {
                filesToDelete.add(wgbsAlignmentLinkFileService.getMethylationDirectory(roddyBamFile))
            }
        }
        return filesToDelete
    }
}
