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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightCheckService
import de.dkfz.tbi.otp.utils.*

import java.nio.file.FileSystem
import java.nio.file.Path

@CompileDynamic
@Transactional
class LinkFilesToFinalDestinationService {

    AbstractBamFileService abstractBamFileService
    ExecuteRoddyCommandService executeRoddyCommandService
    FileService fileService
    FileSystemService fileSystemService
    LinkFileUtils linkFileUtils
    LsdfFilesService lsdfFilesService
    Md5SumService md5SumService
    QcTrafficLightCheckService qcTrafficLightCheckService
    RemoteShellHelper remoteShellHelper
    RoddyConfigService roddyConfigService

    void prepareRoddyBamFile(RoddyBamFile roddyBamFile) {
        assert roddyBamFile: "roddyBamFile must not be null"
        if (!roddyBamFile.withdrawn) {
            RoddyBamFile.withTransaction {
                roddyBamFile.refresh()
                assert roddyBamFile.isMostRecentBamFile(): "The BamFile ${roddyBamFile} is not the most recent one. This must not happen!"
                if (!roddyBamFile.config.adapterTrimmingNeeded) {
                    assert roddyBamFile.numberOfReadsFromQa >= roddyBamFile.numberOfReadsFromFastQc: "bam file (${roddyBamFile.numberOfReadsFromQa}) " +
                            "has less number of reads than the sum of all fastqc (${roddyBamFile.numberOfReadsFromFastQc})"
                }
                assert [FileOperationStatus.NEEDS_PROCESSING, FileOperationStatus.INPROGRESS].contains(roddyBamFile.fileOperationStatus)
                roddyBamFile.fileOperationStatus = FileOperationStatus.INPROGRESS
                assert roddyBamFile.save(flush: true)
                validateAndSetBamFileInProjectFolder(roddyBamFile)
            }
        }
    }

    void validateAndSetBamFileInProjectFolder(AbstractBamFile bamFile) {
        RoddyBamFile.withTransaction {
            assert bamFile.fileOperationStatus == FileOperationStatus.INPROGRESS
            assert !bamFile.withdrawn
            assert CollectionUtils.exactlyOneElement(AbstractBamFile.findAllWhere(
                    workPackage        : bamFile.workPackage,
                    withdrawn          : false,
                    fileOperationStatus: FileOperationStatus.INPROGRESS
            )) == bamFile
            bamFile.workPackage.bamFileInProjectFolder = bamFile
            assert bamFile.workPackage.save(flush: true)
        }
    }

    @Deprecated
    void linkToFinalDestinationAndCleanupRna(RnaRoddyBamFile roddyBamFile) {
        executeRoddyCommandService.correctPermissionsAndGroups(roddyBamFile)
        cleanupOldRnaResults(roddyBamFile)
        handleQcCheckAndSetBamFile(roddyBamFile) {
            linkNewRnaResults(roddyBamFile)
        }
    }

    private void handleQcCheckAndSetBamFile(RoddyBamFile roddyBamFile, Closure linkCall) {
        qcTrafficLightCheckService.handleQcCheck(roddyBamFile, linkCall)
        bamFileValues = roddyBamFile
    }

    void setBamFileValues(RoddyBamFile roddyBamFile) {
        Path md5sumFile = roddyBamFile.workMd5sumFile.toPath()
        String md5sum = md5SumService.extractMd5Sum(md5sumFile)

        RoddyBamFile.withTransaction {
            roddyBamFile.fileOperationStatus = FileOperationStatus.PROCESSED
            roddyBamFile.fileSize = roddyBamFile.workBamFile.size()
            roddyBamFile.md5sum = md5sum
            roddyBamFile.dateFromFileSystem = new Date(roddyBamFile.workBamFile.lastModified())

            assert roddyBamFile.save(flush: true)
            abstractBamFileService.updateSamplePairStatusToNeedProcessing(roddyBamFile)
        }
    }

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
                roddyConfigService.getConfigDirectory(roddyBamFile.workDirectory.toPath()).toFile(),
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

        List<File> filesToDelete = []
        List<File> roddyDirsToDelete = []
        List<RoddyBamFile> roddyBamFiles = RoddyBamFile.findAllByWorkPackageAndIdNotEqual(roddyBamFile.mergingWorkPackage, roddyBamFile.id)
        if (roddyBamFiles) {
            List<File> workDirs = roddyBamFiles.findAll { !it.isOldStructureUsed() }*.workDirectory
            filesToDelete.addAll(workDirs)
            filesToDelete.add(roddyBamFile.finalExecutionStoreDirectory)
            filesToDelete.add(roddyBamFile.finalQADirectory)
            if (roddyBamFile.seqType.isWgbs()) {
                filesToDelete.add(roddyBamFile.finalMethylationDirectory)
            }
            roddyDirsToDelete.addAll(workDirs)
            roddyBamFiles.findAll {
                it.isOldStructureUsed()
            }.each {
                roddyDirsToDelete.addAll(it.finalExecutionDirectories)
                roddyDirsToDelete.addAll(it.finalSingleLaneQADirectories.values())
            }
            if (roddyBamFiles.max { it.identifier }.oldStructureUsed) {
                roddyDirsToDelete.add(roddyBamFile.finalMergedQADirectory)
            }
        }
        FileSystem fs = fileSystemService.remoteFileSystem
        return filesToDelete.collect { fileService.toPath(it, fs) }
    }

    @Deprecated
    void cleanupOldRnaResults(RnaRoddyBamFile roddyBamFile) {
        List<RoddyBamFile> roddyBamFiles = RoddyBamFile.findAllByWorkPackageAndIdNotEqual(roddyBamFile.mergingWorkPackage, roddyBamFile.id)
        List<File> workDirs = roddyBamFiles*.workDirectory
        if (workDirs) {
            lsdfFilesService.deleteFilesRecursive(workDirs)
        }
        String cmd = "find ${roddyBamFile.baseDirectory} -maxdepth 1 -lname '.merging*/*' -delete;"
        remoteShellHelper.executeCommandReturnProcessOutput(cmd)
    }
}
