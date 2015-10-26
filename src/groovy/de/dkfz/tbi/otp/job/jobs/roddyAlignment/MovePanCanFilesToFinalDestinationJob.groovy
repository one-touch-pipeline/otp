package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import de.dkfz.tbi.otp.utils.LinkFileUtils
import de.dkfz.tbi.otp.utils.MoveFileUtilsService
import de.dkfz.tbi.otp.utils.WaitingFileUtils
import org.springframework.beans.factory.annotation.Autowired

/**
 *
 * This is the last job of the PanCan workflow.
 * Within this job the merged bam file, the corresponding index file, the QA-folder and the roddyExecutionStore folder
 * are linked from the working processing folder in the project folder.
 * After linking, tmp roddy files and not used files in older work directories are deleted.
 */
class MovePanCanFilesToFinalDestinationJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    ExecutionService executionService

    @Autowired
    ConfigService configService

    //TODO  OTP-1734: delete this service
    @Autowired
    MoveFileUtilsService moveFileUtilsService

    @Autowired
    ExecuteRoddyCommandService executeRoddyCommandService

    @Autowired
    LinkFileUtils linkFileUtils

    @Autowired
    CreateClusterScriptService createClusterScriptService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Override
    void execute() throws Exception {
        final RoddyBamFile roddyBamFile = getProcessParameterObject()

        Realm realm = configService.getRealmDataManagement(roddyBamFile.project)
        assert realm : "Realm should not be null"

        if (!roddyBamFile.withdrawn) {
            RoddyBamFile.withTransaction {
                assert roddyBamFile.isMostRecentBamFile(): "The BamFile ${roddyBamFile} is not the most recent one. This must not happen!"
                assert [FileOperationStatus.NEEDS_PROCESSING, FileOperationStatus.INPROGRESS].contains(roddyBamFile.fileOperationStatus)
                roddyBamFile.fileOperationStatus = FileOperationStatus.INPROGRESS
                assert roddyBamFile.save(flush: true)
                roddyBamFile.validateAndSetBamFileInProjectFolder()
            }
            if (roddyBamFile.isOldStructureUsed()) {
                //TODO  OTP-1734: delete this if block
                deletePreviousMergedBamResultFiles(roddyBamFile, realm)
                moveResultFiles(roddyBamFile, realm)
                executeRoddyCommandService.correctPermissions(roddyBamFile)

                File md5sumFile = roddyBamFile.finalMd5sumFile
                assert WaitingFileUtils.waitUntilExists(md5sumFile): "The md5sum file of ${roddyBamFile} does not exist"
                assert md5sumFile.text: "The md5sum file of ${roddyBamFile} is empty"
                RoddyBamFile.withTransaction {
                    roddyBamFile.fileOperationStatus = FileOperationStatus.PROCESSED
                    roddyBamFile.fileSize = roddyBamFile.finalBamFile.size()
                    roddyBamFile.md5sum = md5sumFile.text.replaceAll("\n", "")
                    roddyBamFile.fileExists = true
                    roddyBamFile.dateFromFileSystem = new Date(roddyBamFile.finalBamFile.lastModified())
                    assert roddyBamFile.save(flush: true)
                }
            } else {
                cleanupWorkDirectory(roddyBamFile, realm)
                executionHelperService.setPermission(realm, roddyBamFile.workDirectory, CreateClusterScriptService.DIRECTORY_PERMISSION)
                String group = executionHelperService.getGroup(roddyBamFile.baseDirectory)
                executionHelperService.setGroup(realm, roddyBamFile.workDirectory, group.trim())
                executeRoddyCommandService.correctGroups(roddyBamFile)
                cleanupOldResults(roddyBamFile, realm)
                linkNewResults(roddyBamFile, realm)

                File md5sumFile = roddyBamFile.workMd5sumFile
                assert md5sumFile.exists(): "The md5sum file of ${roddyBamFile} does not exist"
                assert md5sumFile.text: "The md5sum file of ${roddyBamFile} is empty"
                RoddyBamFile.withTransaction {
                    roddyBamFile.fileOperationStatus = FileOperationStatus.PROCESSED
                    roddyBamFile.fileSize = roddyBamFile.workBamFile.size()
                    roddyBamFile.md5sum = md5sumFile.text.replaceAll("\n", "")
                    roddyBamFile.fileExists = true
                    roddyBamFile.dateFromFileSystem = new Date(roddyBamFile.workBamFile.lastModified())
                    assert roddyBamFile.save(flush: true)
                }
            }

        } else {
            this.log.info "The results of ${roddyBamFile} will not be moved since it is marked as withdrawn"
        }

        if (roddyBamFile.isOldStructureUsed()) {
            //TODO OTP-1734: delete the if block
            deleteTemporaryDirectory(roddyBamFile, realm)
        }

        succeed()
    }

    /*
     * To prevent uncertain states in the file system the old roddy bam file and the old qc for this merged bam file
     * will be deleted before the new merged bam file is moved to the final location
     */
    //TODO: OTP-1734 delete method
    @Deprecated
    void deletePreviousMergedBamResultFiles(RoddyBamFile roddyBamFile, Realm realm) {
        assert roddyBamFile.isOldStructureUsed()
        if (roddyBamFile.tmpRoddyBamFile.exists()) {
            File bamFilePath = roddyBamFile.finalBamFile
            File baiFilePath = roddyBamFile.finalBaiFile
            File md5sumFilePath = roddyBamFile.finalMd5sumFile

            executionService.executeCommand(realm, "rm -f ${bamFilePath} ${baiFilePath} ${md5sumFilePath}")

            assert WaitingFileUtils.waitUntilDoesNotExist(bamFilePath)
            assert WaitingFileUtils.waitUntilDoesNotExist(baiFilePath)
            assert WaitingFileUtils.waitUntilDoesNotExist(md5sumFilePath)
        }

        if (roddyBamFile.tmpRoddyMergedQADirectory.exists()) {
            File mergedQADirectory = roddyBamFile.finalMergedQADirectory

            executionService.executeCommand(realm, "rm -rf ${mergedQADirectory}")

            assert WaitingFileUtils.waitUntilDoesNotExist(mergedQADirectory)
        }
    }


    // This is a helper method for this job. It should be used carefully in other cases since it moves files.
    //TODO: OTP-1734 delete method
    @Deprecated
    void moveResultFiles(RoddyBamFile roddyBamFile, Realm realm) {
        assert roddyBamFile : "Input roddyBamFile must not be null"
        assert realm : "Input realm must not be null"
        assert roddyBamFile.isOldStructureUsed()

        // make sure that the source and the target roddyExecutionStore are disjoint
        assert !roddyBamFile.finalExecutionStoreDirectory.list() || !roddyBamFile.tmpRoddyExecutionStoreDirectory.list() ||
                (roddyBamFile.tmpRoddyExecutionStoreDirectory.list() as Set).disjoint(roddyBamFile.finalExecutionStoreDirectory.list() as Set)

        ['Bam', 'Bai', 'Md5sum'].each {
            moveFileUtilsService.moveFileIfExists(realm,
                    roddyBamFile."tmpRoddy${it}File",
                    roddyBamFile."final${it}File",
                    null)
        }

        ['MergedQA', 'ExecutionStore'].each {
            moveFileUtilsService.moveDirContentIfExists(realm,
                    roddyBamFile."tmpRoddy${it}Directory",
                    roddyBamFile."final${it}Directory")
        }

        Map<SeqTrack, File> tmpRoddySingleLaneQADirectories = roddyBamFile.tmpRoddySingleLaneQADirectories
        Map<SeqTrack, File> finalRoddySingleLaneQADirectories = roddyBamFile.finalSingleLaneQADirectories

        tmpRoddySingleLaneQADirectories.each { seqTrack, singleLaneQcTempDir ->
            File singleLaneQcDirFinal = finalRoddySingleLaneQADirectories.get(seqTrack)
            moveFileUtilsService.moveDirContentIfExists(realm, singleLaneQcTempDir, singleLaneQcDirFinal)
        }
    }


    // This is a helper method for this job. It should be used carefully in other cases since it deletes files.
    //TODO: OTP-1734 delete method
    @Deprecated
    void deleteTemporaryDirectory(RoddyBamFile roddyBamFile, Realm realm) {
        assert roddyBamFile : "Input roddyBamFile must not be null"
        assert realm : "Input realm must not be null"
        assert roddyBamFile.isOldStructureUsed()

        File tempWorkingDir = roddyBamFile.tmpRoddyDirectory
        executionService.executeCommand(realm, "rm -rf ${tempWorkingDir}")
        assert WaitingFileUtils.waitUntilDoesNotExist(tempWorkingDir)
    }



    // This is a helper method for this job. It should be used carefully in other cases since it links files with replacing existing files on link place.
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

        //create the collected links
        linkFileUtils.createAndValidateLinks(linkMapSourceLink, realm)
    }

    // This is a helper method for this job. It should be used carefully in other cases since it delete files.
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
        List<File> foundFiles = roddyBamFile.workDirectory.listFiles() ?: []
        List<File> filesToDelete = foundFiles - expectedFiles

        filesToDelete.findAll {
            it.isDirectory()
        }.each {
            executeRoddyCommandService.deleteContentOfOtherUnixUserDirectory(it)
        }

        lsdfFilesService.deleteFilesRecursive(realm, filesToDelete)
    }

    // This is a helper method for this job. It should be used carefully in other cases since it delete files.
    void cleanupOldResults(RoddyBamFile roddyBamFile, Realm realm) {
        assert roddyBamFile : "Input roddyBamFile must not be null"
        assert realm : "Input realm must not be null"
        assert !roddyBamFile.isOldStructureUsed()

        List<File> filesToDelete = []
        List<File> roddyDirsToDelete = []
        if (roddyBamFile.baseBamFile) {
            if (!roddyBamFile.baseBamFile.isOldStructureUsed()) {
                filesToDelete << roddyBamFile.baseBamFile.workBamFile
                filesToDelete << roddyBamFile.baseBamFile.workBaiFile
                //the md5sum is kept: roddyBamFile.baseBamFile.workMd5sumFile
                filesToDelete << roddyBamFile.baseBamFile.workMergedQADirectory
                roddyDirsToDelete << roddyBamFile.baseBamFile.workMergedQADirectory
            }
        } else {
            List<RoddyBamFile> roddyBamFiles = RoddyBamFile.findAllByWorkPackageAndIdNotEqual(roddyBamFile.mergingWorkPackage, roddyBamFile.id)
            if (roddyBamFiles) {
                List<File> workDirs = roddyBamFiles.findAll { !it.isOldStructureUsed() }*.workDirectory
                filesToDelete << workDirs
                filesToDelete << roddyBamFiles*.finalExecutionDirectories
                filesToDelete << roddyBamFiles*.finalSingleLaneQADirectories*.values()
                roddyDirsToDelete << workDirs
                roddyDirsToDelete << roddyBamFiles.findAll {
                    it.isOldStructureUsed()
                }.collect {
                    [it.finalExecutionDirectories, it.finalSingleLaneQADirectories.values()]
                }
                if (roddyBamFiles.max {it.identifier}.oldStructureUsed) {
                    roddyDirsToDelete << roddyBamFiles[0].finalMergedQADirectory
                }
            }
        }

        if (filesToDelete) {
            roddyDirsToDelete.flatten().findAll { it.exists() }.each {
                executeRoddyCommandService.deleteContentOfOtherUnixUserDirectory(it)
            }

            lsdfFilesService.deleteFilesRecursive(realm, filesToDelete.flatten())
        }
    }
}
