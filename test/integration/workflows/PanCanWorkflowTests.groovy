package workflows

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.MovePanCanFilesToFinalDestinationJob
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.HelperUtils

import org.joda.time.Duration
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

@Ignore
class PanCanWorkflowTests extends WorkflowTestCase {

    @Autowired
    MovePanCanFilesToFinalDestinationJob movePanCanFilesToFinalDestinationJob

    RoddyBamFile roddyBamFile

    @Before
    void setUp() {
        roddyBamFile = DomainFactory.createRoddyBamFile()
    }

    /*
     * This cannot be tested with an integration test, because integration tests are transactional, so this must be in the workflow test.
     */
    @Test
    void testBamFileInProjectFolderIsSetPersistentlyEvenIfMovingFails() {
        try {
            final String exceptionMessage = HelperUtils.uniqueString
            movePanCanFilesToFinalDestinationJob.metaClass.getProcessParameterObject = {
                roddyBamFile.fileOperationStatus = FileOperationStatus.NEEDS_PROCESSING
                roddyBamFile.md5sum = null
                assert roddyBamFile.save(failOnError: true)
                return roddyBamFile
            }
            movePanCanFilesToFinalDestinationJob.configService.metaClass.getRealmDataManagement = { Project project ->
                return Realm.build()
            }
            movePanCanFilesToFinalDestinationJob.linkFilesToFinalDestinationService.metaClass.cleanupWorkDirectory = { RoddyBamFile roddyBamFile, Realm realm ->
                throw new RuntimeException(exceptionMessage)
            }

            assert TestCase.shouldFail {
                movePanCanFilesToFinalDestinationJob.execute()
            } == exceptionMessage
            assert roddyBamFile.fileOperationStatus == FileOperationStatus.INPROGRESS
            assert roddyBamFile.mergingWorkPackage.bamFileInProjectFolder == roddyBamFile
        } finally {
            TestCase.removeMetaClass(ConfigService, movePanCanFilesToFinalDestinationJob.configService)
            TestCase.removeMetaClass(MovePanCanFilesToFinalDestinationJob, movePanCanFilesToFinalDestinationJob)
        }
    }

    @Override
    List<String> getWorkflowScripts() {
        return ["scripts/workflows/PanCanWorkflow.groovy"]
    }

    @Override
    Duration getTimeout() {
        Duration.standardDays(7)
    }
}
