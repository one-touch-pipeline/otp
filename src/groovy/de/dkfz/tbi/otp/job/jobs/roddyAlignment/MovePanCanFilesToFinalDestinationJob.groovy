package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFileService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import de.dkfz.tbi.otp.utils.MoveFileUtilsService
import de.dkfz.tbi.otp.utils.ProcessHelperService
import de.dkfz.tbi.otp.utils.WaitingFileUtils
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.ExecutionService

/**
 *
 * This is the last job of the PanCan workflow.
 * Within this job the merged bam file, the corresponding index file, the QA-folder and the roddyExecutionStore folder
 * are moved from the temporary processing folder to the project folder.
 * After moving the results the temporary folder is deleted.
 */
class MovePanCanFilesToFinalDestinationJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ExecutionService executionService

    @Autowired
    ConfigService configService

    @Autowired
    MoveFileUtilsService moveFileUtilsService

    @Autowired
    ExecuteRoddyCommandService executeRoddyCommandService

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
            correctPermissions(roddyBamFile)
            deletePreviousMergedBamResultFiles(roddyBamFile, realm)
            moveResultFiles(roddyBamFile, realm)
            correctPermissions(roddyBamFile)

            File md5sumFile = roddyBamFile.finalMd5sumFile
            assert WaitingFileUtils.waitUntilExists(md5sumFile): "The md5sum file of ${roddyBamFile} does not exist"
            RoddyBamFile.withTransaction {
                roddyBamFile.fileOperationStatus = FileOperationStatus.PROCESSED
                roddyBamFile.fileSize = roddyBamFile.finalBamFile.size()
                roddyBamFile.md5sum = md5sumFile.text.split(" ")[0]
                assert roddyBamFile.save(flush: true)
            }
        } else {
            this.log.info "The results of ${roddyBamFile} will not be moved since it is marked as withdrawn"
        }

        deleteTemporaryDirectory(roddyBamFile, realm)

        succeed()
    }


    void correctPermissions(RoddyBamFile roddyBamFile) {
        assert roddyBamFile : "RoddyBamFile should not be null"
        File baseFile = new File(AbstractMergedBamFileService.destinationDirectory(roddyBamFile))
        String cmd = executeRoddyCommandService.correctPermissionCommand(baseFile)
        ProcessHelperService.executeCommandAndAssertExistCodeAndReturnStdout(cmd)
    }

    /*
     * To prevent uncertain states in the file system the old roddy bam file and the old qc for this merged bam file
     * will be deleted before the new merged bam file is moved to the final location
     */
    void deletePreviousMergedBamResultFiles(RoddyBamFile roddyBamFile, Realm realm) {
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
    void moveResultFiles(RoddyBamFile roddyBamFile, Realm realm) {
        assert roddyBamFile : "Input roddyBamFile must not be null"
        assert realm : "Input realm must not be null"

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
        Map<SeqTrack, File> finalRoddySingleLaneQADirectories = roddyBamFile.finalRoddySingleLaneQADirectories

        tmpRoddySingleLaneQADirectories.each { seqTrack, singleLaneQcTempDir ->
            File singleLaneQcDirFinal = finalRoddySingleLaneQADirectories.get(seqTrack)
            moveFileUtilsService.moveDirContentIfExists(realm, singleLaneQcTempDir, singleLaneQcDirFinal)
        }
    }


    // This is a helper method for this job. It should be used carefully in other cases since it deletes files.
    void deleteTemporaryDirectory(RoddyBamFile roddyBamFile, Realm realm) {
        assert roddyBamFile : "Input roddyBamFile must not be null"
        assert realm : "Input realm must not be null"

        File tempWorkingDir = roddyBamFile.tmpRoddyDirectory
        executionService.executeCommand(realm, "rm -rf ${tempWorkingDir}")
        assert WaitingFileUtils.waitUntilDoesNotExist(tempWorkingDir)
    }

}
