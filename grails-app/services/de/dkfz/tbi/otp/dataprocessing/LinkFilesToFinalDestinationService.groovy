package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.QcTrafficLightStatus
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.*
import de.dkfz.tbi.otp.utils.*

import java.nio.file.Path

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.*

class LinkFilesToFinalDestinationService {

    ExecuteRoddyCommandService executeRoddyCommandService

    LinkFileUtils linkFileUtils

    LsdfFilesService lsdfFilesService

    RemoteShellHelper remoteShellHelper

    AbstractMergedBamFileService abstractMergedBamFileService

    QcTrafficLightNotificationService qcTrafficLightNotificationService

    Md5SumService md5SumService


    void prepareRoddyBamFile(RoddyBamFile roddyBamFile) {
        assert roddyBamFile: "roddyBamFile must not be null"
        if (!roddyBamFile.withdrawn) {
            RoddyBamFile.withTransaction {
                assert roddyBamFile.isMostRecentBamFile(): "The BamFile ${roddyBamFile} is not the most recent one. This must not happen!"
                if (!roddyBamFile.config.adapterTrimmingNeeded) {
                    assert roddyBamFile.numberOfReadsFromQa >= roddyBamFile.numberOfReadsFromFastQc: "bam file (${roddyBamFile.numberOfReadsFromQa}) " +
                            "has less number of reads than the sum of all fastqc (${roddyBamFile.numberOfReadsFromFastQc})"
                }
                assert [FileOperationStatus.NEEDS_PROCESSING, FileOperationStatus.INPROGRESS].contains(roddyBamFile.fileOperationStatus)
                roddyBamFile.fileOperationStatus = FileOperationStatus.INPROGRESS
                assert roddyBamFile.save(flush: true)
                roddyBamFile.validateAndSetBamFileInProjectFolder()
            }
        }
    }

    void linkToFinalDestinationAndCleanup(RoddyBamFile roddyBamFile, Realm realm) {
        assert roddyBamFile: "roddyBamFile must not be null"
        assert realm: "realm must not be null"
        if (!roddyBamFile.withdrawn) {
            cleanupWorkDirectory(roddyBamFile, realm)
            executeRoddyCommandService.correctPermissionsAndGroups(roddyBamFile, realm)
            cleanupOldResults(roddyBamFile, realm)
            handleQcCheck(roddyBamFile) {
                linkNewResults(roddyBamFile, realm)
            }
        } else {
            threadLog?.info "The results of ${roddyBamFile} will not be moved since it is marked as withdrawn"
        }
    }

    void linkToFinalDestinationAndCleanupRna(RnaRoddyBamFile roddyBamFile, Realm realm) {
        executeRoddyCommandService.correctPermissionsAndGroups(roddyBamFile, realm)
        cleanupOldRnaResults(roddyBamFile, realm)
        handleQcCheck(roddyBamFile) {
            linkNewRnaResults(roddyBamFile, realm)
        }
    }

    private String getInvalidQcTrafficLightStatusMessageForStatus(QcTrafficLightStatus status) {
        List<QcTrafficLightStatus> validStatuses = [QcTrafficLightStatus.QC_PASSED, QcTrafficLightStatus.AUTO_ACCEPTED, QcTrafficLightStatus.UNCHECKED, QcTrafficLightStatus.BLOCKED]
        return "Unhandled QcTrafficLightStatus: ${status}. Only the following values are valid: ${validStatuses.join(", ")}"
    }

    private void handleQcCheck(RoddyBamFile roddyBamFile, Closure linkCall) {
        if (!roddyBamFile.qcTrafficLightStatus || roddyBamFile.qcTrafficLightStatus in [QcTrafficLightStatus.QC_PASSED, QcTrafficLightStatus.AUTO_ACCEPTED, QcTrafficLightStatus.UNCHECKED]) {
            linkCall()
        } else if (roddyBamFile.qcTrafficLightStatus == QcTrafficLightStatus.BLOCKED) {
            if (roddyBamFile.project.qcThresholdHandling.notifiesUser) {
                qcTrafficLightNotificationService.informResultsAreBlocked(roddyBamFile)
            }
        } else {
            throw new RuntimeException(getInvalidQcTrafficLightStatusMessageForStatus(roddyBamFile.qcTrafficLightStatus))
        }
        setBamFileValues(roddyBamFile)
    }

    void setBamFileValues(RoddyBamFile roddyBamFile) {
        Path md5sumFile = roddyBamFile.workMd5sumFile.toPath()
        String md5sum = md5SumService.extractMd5Sum(md5sumFile)

        RoddyBamFile.withTransaction {
            roddyBamFile.fileOperationStatus = FileOperationStatus.PROCESSED
            roddyBamFile.fileSize = roddyBamFile.workBamFile.size()
            roddyBamFile.md5sum = md5sum
            roddyBamFile.fileExists = true
            roddyBamFile.dateFromFileSystem = new Date(roddyBamFile.workBamFile.lastModified())
            assert roddyBamFile.save(flush: true)
            abstractMergedBamFileService.setSamplePairStatusToNeedProcessing(roddyBamFile)
        }
    }

    /**
     * Link files (replaces existing files)
     */
    void linkNewResults(RoddyBamFile roddyBamFile, Realm realm) {
        assert roddyBamFile: "Input roddyBamFile must not be null"
        assert realm: "Input realm must not be null"
        assert !roddyBamFile.isOldStructureUsed()

        Map<File, File> linkMapSourceLink = [:]

        //collect links for files and qa merge directory
        ['Bam', 'Bai', 'Md5sum'].each {
            linkMapSourceLink.put(roddyBamFile."work${it}File", roddyBamFile."final${it}File")
        }
        linkMapSourceLink.put(roddyBamFile.workMergedQADirectory, roddyBamFile.finalMergedQADirectory)

        if (roddyBamFile.seqType.isWgbs()) {
            linkMapSourceLink.put(roddyBamFile.workMergedMethylationDirectory, roddyBamFile.finalMergedMethylationDirectory)
            if (roddyBamFile.getContainedSeqTracks()*.getLibraryDirectoryName().unique().size() > 1) {
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

        //collect links for every execution store
        [roddyBamFile.getWorkExecutionDirectories(), roddyBamFile.getFinalExecutionDirectories()].transpose().each {
            linkMapSourceLink.put(it[0], it[1])
        }

        //collect links for the single lane qa
        Map<SeqTrack, File> workSingleLaneQADirectories = roddyBamFile.workSingleLaneQADirectories
        Map<SeqTrack, File> finalSingleLaneQADirectories = roddyBamFile.finalSingleLaneQADirectories
        workSingleLaneQADirectories.each { seqTrack, singleLaneQaWorkDir ->
            File singleLaneQcDirFinal = finalSingleLaneQADirectories.get(seqTrack)
            linkMapSourceLink.put(singleLaneQaWorkDir, singleLaneQcDirFinal)
        }

        if (roddyBamFile.baseBamFile?.isOldStructureUsed()) {
            lsdfFilesService.deleteFilesRecursive(realm, [roddyBamFile.baseBamFile.finalMergedQADirectory])
        }

        //create the collected links
        linkFileUtils.createAndValidateLinks(linkMapSourceLink, realm)
    }

    void linkNewRnaResults(RnaRoddyBamFile roddyBamFile, Realm realm) {
        File baseDirectory = roddyBamFile.getBaseDirectory()
        Map links = roddyBamFile.workDirectory.listFiles().findAll {
            !it.name.startsWith(".")
        }.collectEntries { File source ->
            [(source): new File(baseDirectory, source.name)]
        }
        linkFileUtils.createAndValidateLinks(links, realm)
    }

    void cleanupWorkDirectory(RoddyBamFile roddyBamFile, Realm realm) {
        assert roddyBamFile: "Input roddyBamFile must not be null"
        assert realm: "Input realm must not be null"
        assert !roddyBamFile.isOldStructureUsed()

        List<File> expectedFiles = [
                roddyBamFile.workBamFile,
                roddyBamFile.workBaiFile,
                roddyBamFile.workMd5sumFile,
                roddyBamFile.workQADirectory,
                roddyBamFile.workExecutionStoreDirectory,
        ]
        if (roddyBamFile.seqType.isWgbs()) {
            expectedFiles.add(roddyBamFile.workMethylationDirectory)
            expectedFiles.add(roddyBamFile.workMetadataTableFile)
        }
        List<File> foundFiles = roddyBamFile.workDirectory.listFiles() ?: []
        List<File> filesToDelete = foundFiles - expectedFiles

        lsdfFilesService.deleteFilesRecursive(realm, filesToDelete)
    }


    void cleanupOldResults(RoddyBamFile roddyBamFile, Realm realm) {
        assert roddyBamFile: "Input roddyBamFile must not be null"
        assert realm: "Input realm must not be null"
        assert !roddyBamFile.isOldStructureUsed()

        List<File> filesToDelete = []
        List<File> roddyDirsToDelete = []
        if (roddyBamFile.baseBamFile) {
            if (!roddyBamFile.baseBamFile.isOldStructureUsed()) {
                filesToDelete.add(roddyBamFile.baseBamFile.workBamFile)
                filesToDelete.add(roddyBamFile.baseBamFile.workBaiFile)
                //the md5sum is kept: roddyBamFile.baseBamFile.workMd5sumFile
            }
        } else {
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
        }

        if (filesToDelete) {
            lsdfFilesService.deleteFilesRecursive(realm, filesToDelete)
        }
    }

    void cleanupOldRnaResults(RnaRoddyBamFile roddyBamFile, Realm realm) {
        List<RoddyBamFile> roddyBamFiles = RoddyBamFile.findAllByWorkPackageAndIdNotEqual(roddyBamFile.mergingWorkPackage, roddyBamFile.id)
        List<File> workDirs = roddyBamFiles*.workDirectory
        if (workDirs) {
            lsdfFilesService.deleteFilesRecursive(realm, workDirs)
        }
        String cmd = "find ${roddyBamFile.getBaseDirectory()} -maxdepth 1 -lname '.merging*/*' -delete;"
        remoteShellHelper.executeCommandReturnProcessOutput(realm, cmd)
    }
}
