package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.beans.factory.annotation.*


class LinkRnaAlignmentFilesToFinalDestinationJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Autowired
    ConfigService configService
    @Autowired
    ExecuteRoddyCommandService executeRoddyCommandService
    @Autowired
    LinkFilesToFinalDestinationService linkFilesToFinalDestinationService


    @Override
    void execute() throws Exception {
        final RnaRoddyBamFile roddyBamFile = getProcessParameterObject()

        Realm realm = configService.getRealmDataManagement(roddyBamFile.project)
        assert realm : "Realm should not be null"

        linkFilesToFinalDestinationService.prepareRoddyBamFile(roddyBamFile)
        linkFilesToFinalDestinationService.cleanupOldRnaResults(roddyBamFile, realm)
        executeRoddyCommandService.correctPermissionsAndGroups(roddyBamFile, realm)
        linkFilesToFinalDestinationService.linkNewRnaResults(roddyBamFile, realm)
        linkFilesToFinalDestinationService.setBamFileValues(roddyBamFile)
    }
}
