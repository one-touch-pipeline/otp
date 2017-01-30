package workflows

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.joda.time.*
import org.junit.*
import org.springframework.beans.factory.annotation.*

@Ignore
class MovePanCanFilesToFinalDestinationJobIntegrationTests extends WorkflowTestCase {

    static final int READ_COUNTS = 1

    @Autowired
    MovePanCanFilesToFinalDestinationJob movePanCanFilesToFinalDestinationJob

    RoddyBamFile roddyBamFile

    @Before
    void setUp() {
        roddyBamFile = DomainFactory.createRoddyBamFile()
        DomainFactory.createRoddyMergedBamQa(roddyBamFile, [
                pairedRead1: READ_COUNTS,
                pairedRead2: READ_COUNTS,
        ])
        roddyBamFile.seqTracks.each { SeqTrack seqTrack ->
            seqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED
            assert seqTrack.save(flush: true)
            DataFile.findAllBySeqTrack(seqTrack).each { DataFile dataFile ->
                dataFile.nReads = READ_COUNTS
                assert dataFile.save(flush: true)
            }
        }
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
            TestCase.removeMetaClass(LinkFilesToFinalDestinationService, movePanCanFilesToFinalDestinationJob.linkFilesToFinalDestinationService)
            TestCase.removeMetaClass(MovePanCanFilesToFinalDestinationJob, movePanCanFilesToFinalDestinationJob)
        }
    }

    @Override
    List<String> getWorkflowScripts() {
        return ["scripts/workflows/PanCanWorkflow.groovy"]
    }

    @Override
    Duration getTimeout() {
        assert false
    }
}
