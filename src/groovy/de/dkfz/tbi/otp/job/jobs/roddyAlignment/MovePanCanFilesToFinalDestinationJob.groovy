package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob

/**
 *
 * This is the last job of the PanCan workflow.
 * Within this job the merged bam file, the corresponding index file, the QA-folder and the roddyExecutionStore folder
 * are linked from the working processing folder in the project folder.
 * After linking, tmp roddy files and not used files in older work directories are deleted.
 */
class MovePanCanFilesToFinalDestinationJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Autowired
    ConfigService configService

    @Autowired
    LinkFilesToFinalDestinationService linkFilesToFinalDestinationService

    @Override
    void execute() throws Exception {
        final RoddyBamFile roddyBamFile = getProcessParameterObject()

        Realm realm = configService.getRealmDataManagement(roddyBamFile.project)
        assert realm : "Realm should not be null"

        linkFilesToFinalDestinationService.prepareRoddyBamFile(roddyBamFile)
        linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)

        succeed()
    }
}
