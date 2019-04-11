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

package de.dkfz.tbi.otp

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.LinkFilesToFinalDestinationService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.MovePanCanFilesToFinalDestinationJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.SessionUtils

import java.time.Duration

class MovePanCanFilesToFinalDestinationJobIntegrationTests extends WorkflowTestCase {

    static final int READ_COUNTS = 1

    @Autowired
    MovePanCanFilesToFinalDestinationJob movePanCanFilesToFinalDestinationJob

    RoddyBamFile roddyBamFile

    @Override
    void setup() {
        SessionUtils.withNewSession {
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
    }

    /*
     * This cannot be tested with an integration test, because integration tests are transactional,
     * so this must be in the workflow test.
     */
    void "test transaction-rollback when linking files fails"() {
        given:
        FileOperationStatus originalFileOperationStatus
        String exceptionMessage = HelperUtils.uniqueString
        Long id
        SessionUtils.withNewSession {
            id = roddyBamFile.id
            movePanCanFilesToFinalDestinationJob.metaClass.getProcessParameterObject = {
                RoddyBamFile roddyBamFile1 = RoddyBamFile.get(id)
                roddyBamFile1.fileOperationStatus = FileOperationStatus.NEEDS_PROCESSING
                roddyBamFile1.md5sum = null
                assert roddyBamFile1.save(flush: true)
                return roddyBamFile1
            }
            movePanCanFilesToFinalDestinationJob.linkFilesToFinalDestinationService.metaClass.cleanupWorkDirectory = { RoddyBamFile roddyBamFile, Realm realm ->
                throw new RuntimeException(exceptionMessage)
            }
            originalFileOperationStatus = roddyBamFile.fileOperationStatus
        }

        when:
        SessionUtils.withNewSession {
            movePanCanFilesToFinalDestinationJob.execute()
        }

        then:
        RuntimeException exception = thrown(RuntimeException)
        exception.message == exceptionMessage

        SessionUtils.withNewSession {
            assert RoddyBamFile.get(id).fileOperationStatus == originalFileOperationStatus
            assert RoddyBamFile.get(id).mergingWorkPackage.bamFileInProjectFolder == null
            return true
        }

        cleanup:
        TestCase.removeMetaClass(LinkFilesToFinalDestinationService, movePanCanFilesToFinalDestinationJob.linkFilesToFinalDestinationService)
        TestCase.removeMetaClass(MovePanCanFilesToFinalDestinationJob, movePanCanFilesToFinalDestinationJob)
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
