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
package de.dkfz.tbi.otp.alignment.roddy

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.SessionUtils

import java.time.Duration

/**
 * tests for AbstractRoddyJob using PanCanAlignmentWorkflow
 */
class AbstractRoddyJobIntegrationTests extends AbstractRoddyAlignmentWorkflowTests {

    void "execute roddy, roddy call succeeds, no cluster jobs sent, roddy job failed and successfully restarted"() {
        given:
        RoddyWorkflowConfig config
        String programVersion
        SessionUtils.withNewSession {
            createFirstRoddyBamFile()
            createSeqTrack("readGroup2")

            config = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.findAll())
            programVersion = config.programVersion

            // setting not existing plugin must make roddy exit without sending any cluster jobs
            config.programVersion = HelperUtils.getUniqueString()
            config.save()
        }

        when:
        execute()

        then:
        RuntimeException exception = thrown(RuntimeException)
        exception.message =~ /Plugin '.*' is not available, available are:/

        when:
        SessionUtils.withNewSession {
            config.refresh()
            config.programVersion = programVersion
            config.save()
            restartWorkflowFromFailedStep()
        }

        then:
        checkAllAfterSuccessfulExecution_alignBaseBamAndNewLanes()
    }

    void "execute roddy, roddy call succeeds, one clusterJob fails, RoddyJob failed and is successfully restarted"() {
        given:
        String DUMMY_STAT_SIZE_FILE_NAME = "dummy.tab"
        RoddyBamFile firstBamFile
        MergingWorkPackage workPackage
        SessionUtils.withNewSession {
            firstBamFile = createFirstRoddyBamFile()
            createSeqTrack("readGroup2")

            // create invalid stat file and register it in the database
            // this will make one of the cluster jobs fail
            workPackage = firstBamFile.workPackage
            File statDir = referenceGenomeService.pathToChromosomeSizeFilesPerReference(workPackage.referenceGenome)
            remoteShellHelper.executeCommand(realm, "chmod g+w ${statDir}")
            File statFile = new File(statDir, DUMMY_STAT_SIZE_FILE_NAME)
            workPackage.refresh()
            workPackage.statSizeFileName = statFile.name
            workPackage.save()
        }

        when:
        execute()

        then:
        RuntimeException exception = thrown(RuntimeException)
        exception.message.contains("Status code: 15")

        when:
        SessionUtils.withNewSession {
            workPackage.refresh()
            workPackage.statSizeFileName = getChromosomeStatFileName()
            workPackage.save()

            restartWorkflowFromFailedStep()
        }

        then:
        checkAllAfterRoddyClusterJobsRestartAndSuccessfulExecution_alignBaseBamAndNewLanes()
    }


    @Override
    void checkForFailedClusterJobs() {
        assert ClusterJob.all.every { it.jobLog != null }
    }


    @Override
    SeqType findSeqType() {
        return CollectionUtils.exactlyOneElement(SeqType.findAllWhere(
                name: SeqTypeNames.WHOLE_GENOME.seqTypeName,
                libraryLayout: SequencingReadType.PAIRED,
        ))
    }

    @Override
    Duration getTimeout() {
        return Duration.ofMinutes(60)
    }
}
