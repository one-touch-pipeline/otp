package de.dkfz.tbi.otp.job.jobs.rnaAlignment

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.LinkFilesToFinalDestinationService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService

@Component
@Scope("prototype")
@UseJobLog
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

        Realm realm = roddyBamFile.project.realm
        assert realm : "Realm should not be null"

        linkFilesToFinalDestinationService.prepareRoddyBamFile(roddyBamFile)
        linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanupRna(roddyBamFile, realm)
        succeed()
    }
}
