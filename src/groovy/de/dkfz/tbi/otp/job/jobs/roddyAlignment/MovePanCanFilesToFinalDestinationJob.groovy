package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFileService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.MoveFileUtilsService
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

    @Override
    void execute() throws Exception {
        final RoddyBamFile roddyBamFile = getProcessParameterObject()
        boolean withdrawn = roddyBamFile.withdrawn
        if (!withdrawn) {
            RoddyBamFile.withTransaction {
                assert roddyBamFile.isMostRecentBamFile(): "The BamFile ${roddyBamFile} is not the most recent one. This must not happen!"
                roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.INPROGRESS
                assert roddyBamFile.save(flush: true)
            }
            moveResultFiles(roddyBamFile)
        } else {
            this.log.info "The results of ${roddyBamFile} will not be moved since it is marked as withdrawn"
        }

        if (!withdrawn) {
            RoddyBamFile.withTransaction {
                File md5sumFile = roddyBamFile.finalMd5sumFile
                assert WaitingFileUtils.confirmExists(md5sumFile): "The md5sum file of ${roddyBamFile} does not exist"
                roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
                roddyBamFile.fileSize = roddyBamFile.finalBamFile.size()
                roddyBamFile.md5sum = md5sumFile.text.split(" ")[0]
                assert roddyBamFile.save(flush: true)
            }
        }

        deleteTemporaryDirectory(roddyBamFile)

        succeed()
    }


    // This is a helper method for this job. It should be used carefully in other cases since it moves files.
    void moveResultFiles(RoddyBamFile roddyBamFile) {
        assert roddyBamFile : "Input roddyBamFile must not be null"

        Realm realm = configService.getRealmDataManagement(roddyBamFile.project)
        assert realm : "Realm must not be null"

        // make sure that the source and the target roddyExecutionStore are disjoint
        assert !roddyBamFile.finalExecutionStoreDirectory.list() || !roddyBamFile.tmpRoddyExecutionStoreDirectory.list() ||
                (roddyBamFile.tmpRoddyExecutionStoreDirectory.list() as Set).disjoint(roddyBamFile.finalExecutionStoreDirectory.list() as Set)

        ['Bam', 'Bai', 'Md5sum'].each {
            moveFileUtilsService.moveFileIfExists(realm,
                    roddyBamFile."tmpRoddy${it}File",
                    roddyBamFile."final${it}File",
                    true)
        }
        //TODO: OTP-1513 -> adapt implementation of QC files movement after the structure was defined within QC-taskforce
        ['QA', 'ExecutionStore'].each {
            moveFileUtilsService.moveDirContentIfExists(realm,
                    roddyBamFile."tmpRoddy${it}Directory",
                    roddyBamFile."final${it}Directory")
        }
    }


    // This is a helper method for this job. It should be used carefully in other cases since it deletes files.
    void deleteTemporaryDirectory(RoddyBamFile roddyBamFile) {
        assert roddyBamFile : "Input roddyBamFile must not be null"

        Realm realm = configService.getRealmDataManagement(roddyBamFile.project)
        assert realm : "Realm should not be null"

        File tempWorkingDir = roddyBamFile.tmpRoddyDirectory
        executionService.executeCommand(realm, "rm -rf ${tempWorkingDir}")
        assert WaitingFileUtils.confirmDoesNotExist(tempWorkingDir)
    }

}
