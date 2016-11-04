package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.beans.factory.annotation.*

import static de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus.*
import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.*

class LinkFilesToFinalDestinationService {

    @Autowired
    ExecuteRoddyCommandService executeRoddyCommandService

    @Autowired
    LinkFileUtils linkFileUtils

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    CreateClusterScriptService createClusterScriptService

    @Autowired
    ExecutionService executionService

    @Autowired
    AbstractMergedBamFileService abstractMergedBamFileService


    public void prepareRoddyBamFile(RoddyBamFile roddyBamFile) {
        assert roddyBamFile : "roddyBamFile must not be null"
        if (!roddyBamFile.withdrawn) {
            RoddyBamFile.withTransaction {
                assert roddyBamFile.isMostRecentBamFile(): "The BamFile ${roddyBamFile} is not the most recent one. This must not happen!"
                if (!roddyBamFile.seqType.isWgbs()) {
                    assert roddyBamFile.numberOfReadsFromQa >= roddyBamFile.numberOfReadsFromFastQc: "bam file (${roddyBamFile.numberOfReadsFromQa}) has less number of reads than the sum of all fastqc (${roddyBamFile.numberOfReadsFromFastQc})"
                }
                assert [NEEDS_PROCESSING, INPROGRESS].contains(roddyBamFile.fileOperationStatus)
                roddyBamFile.fileOperationStatus = INPROGRESS
                assert roddyBamFile.save(flush: true)
                roddyBamFile.validateAndSetBamFileInProjectFolder()
            }
        }
    }

    public void linkToFinalDestinationAndCleanup(RoddyBamFile roddyBamFile, Realm realm) {
        assert roddyBamFile : "roddyBamFile must not be null"
        assert realm : "realm must not be null"
        if (!roddyBamFile.withdrawn) {
            cleanupWorkDirectory(roddyBamFile, realm)
            executeRoddyCommandService.correctPermissionsAndGroups(roddyBamFile, realm)
            cleanupOldResults(roddyBamFile, realm)
            linkNewResults(roddyBamFile, realm)

            File md5sumFile = roddyBamFile.workMd5sumFile
            assert md5sumFile.exists(): "The md5sum file of ${roddyBamFile} does not exist"
            assert md5sumFile.text: "The md5sum file of ${roddyBamFile} is empty"
            RoddyBamFile.withTransaction {
                roddyBamFile.fileOperationStatus = PROCESSED
                roddyBamFile.fileSize = roddyBamFile.workBamFile.size()
                roddyBamFile.md5sum = md5sumFile.text.replaceAll("\n", "").toLowerCase(Locale.ENGLISH)
                roddyBamFile.fileExists = true
                roddyBamFile.dateFromFileSystem = new Date(roddyBamFile.workBamFile.lastModified())
                assert roddyBamFile.save(flush: true)
                abstractMergedBamFileService.setSamplePairStatusToNeedProcessing(roddyBamFile)
            }
        } else {
            threadLog?.info "The results of ${roddyBamFile} will not be moved since it is marked as withdrawn"
        }
    }

    /**
     * Link files (replaces existing files)
     */
    void linkNewResults(RoddyBamFile roddyBamFile, Realm realm) {
        assert roddyBamFile : "Input roddyBamFile must not be null"
        assert realm : "Input realm must not be null"
        assert !roddyBamFile.isOldStructureUsed()

        Map<File, File> linkMapSourceLink = [:]

        //collect links for files and qa merge directory
        ['Bam', 'Bai', 'Md5sum'].each {
            linkMapSourceLink.put(roddyBamFile."work${it}File", roddyBamFile."final${it}File")
        }
        linkMapSourceLink.put(roddyBamFile.workMergedQADirectory, roddyBamFile.finalMergedQADirectory)

        if (roddyBamFile.seqType.isWgbs()) {
            linkMapSourceLink.put(roddyBamFile.workMergedMethylationDirectory, roddyBamFile.finalMergedMethylationDirectory)
            if (roddyBamFile.getContainedSeqTracks()*.normalizedLibraryName.unique().size() > 1) {
                [roddyBamFile.workLibraryQADirectories.values().asList().sort(), roddyBamFile.finalLibraryQADirectories.values().asList().sort()].transpose().each {
                    linkMapSourceLink.put(it[0], it[1])
                }
                [roddyBamFile.workLibraryMethylationDirectories.values().asList().sort(), roddyBamFile.finalLibraryMethylationDirectories.values().asList().sort()].transpose().each {
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
            executeRoddyCommandService.deleteContentOfOtherUnixUserDirectory(roddyBamFile.baseBamFile.finalMergedQADirectory, realm)
        }

        //create the collected links
        linkFileUtils.createAndValidateLinks(linkMapSourceLink, realm)
    }


    void cleanupWorkDirectory(RoddyBamFile roddyBamFile, Realm realm) {
        assert roddyBamFile : "Input roddyBamFile must not be null"
        assert realm : "Input realm must not be null"
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

        filesToDelete.findAll {
            it.isDirectory()
        }.each {
            executeRoddyCommandService.deleteContentOfOtherUnixUserDirectory(it, realm)
        }

        lsdfFilesService.deleteFilesRecursive(realm, filesToDelete)
    }


    void cleanupOldResults(RoddyBamFile roddyBamFile, Realm realm) {
        assert roddyBamFile : "Input roddyBamFile must not be null"
        assert realm : "Input realm must not be null"
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
                if (roddyBamFiles.max {it.identifier}.oldStructureUsed) {
                    roddyDirsToDelete.add(roddyBamFile.finalMergedQADirectory)
                }
            }
        }

        if (filesToDelete) {
            roddyDirsToDelete.findAll { it.exists() }.each {
                executeRoddyCommandService.deleteContentOfOtherUnixUserDirectory(it, realm)
            }

            lsdfFilesService.deleteFilesRecursive(realm, filesToDelete)
        }
    }
}
