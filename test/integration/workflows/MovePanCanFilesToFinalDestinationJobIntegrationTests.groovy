/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package workflows

import org.junit.*
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.LinkFilesToFinalDestinationService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.MovePanCanFilesToFinalDestinationJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.HelperUtils

import java.time.Duration

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
     * This cannot be tested with an integration test, because integration tests are transactional,
     * so this must be in the workflow test.
     */
    @Test
    void testTransactionRollbackWhenLinkingFails() {
        try {
            final String exceptionMessage = HelperUtils.uniqueString
            movePanCanFilesToFinalDestinationJob.metaClass.getProcessParameterObject = {
                roddyBamFile.fileOperationStatus = FileOperationStatus.NEEDS_PROCESSING
                roddyBamFile.md5sum = null
                assert roddyBamFile.save(failOnError: true)
                return roddyBamFile
            }
            movePanCanFilesToFinalDestinationJob.linkFilesToFinalDestinationService.metaClass.cleanupWorkDirectory = { RoddyBamFile roddyBamFile, Realm realm ->
                throw new RuntimeException(exceptionMessage)
            }
            FileOperationStatus originalFileOperationStatus = roddyBamFile.fileOperationStatus
            assert TestCase.shouldFail {
                movePanCanFilesToFinalDestinationJob.execute()
            } == exceptionMessage
            assert RoddyBamFile.findById(roddyBamFile.id).fileOperationStatus == originalFileOperationStatus
            assert roddyBamFile.mergingWorkPackage.bamFileInProjectFolder == null
        } finally {
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
