/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.workflow.panCancer

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.FastqImportInstance
import de.dkfz.tbi.otp.ngsdata.FileType
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.tracking.NotificationCreator
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.FileSystems

class PanCancerPrepareJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
                MergingWorkPackage,
                FileType,
                FastqImportInstance,
                RoddyWorkflowConfig,
                RoddyBamFile,
                ReferenceGenomeProjectSeqType,
        ]
    }

    void "test buildWorkDirectoryPath should return workflowRun workDirectory"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        PanCancerPrepareJob job = new PanCancerPrepareJob()

        job.fileSystemService = Mock(FileSystemService) {
            1 * getRemoteFileSystem(_) >> FileSystems.default
        }

        expect:
        job.buildWorkDirectoryPath(workflowStep).toString() == workflowStep.workflowRun.workDirectory
    }

    void "test generateMapForLinking should return empty list"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        PanCancerPrepareJob job = new PanCancerPrepareJob()

        when:
        Collection<LinkEntry> result = job.generateMapForLinking(workflowStep)

        then:
        result == []
    }

    void "test doFurtherPreparation should create a notification"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        List<SeqTrack> seqTracks = [createSeqTrack(), createSeqTrack()]
        RoddyBamFile roddyBamFile = createBamFile()

        PanCancerPrepareJob job = Spy(PanCancerPrepareJob) {
            1 * getSeqTracks(workflowStep) >> seqTracks
            1 * getRoddyBamFile(workflowStep) >> roddyBamFile
        }

        job.notificationCreator = Mock(NotificationCreator)

        when:
        job.doFurtherPreparation(workflowStep)

        then:
        1 * job.notificationCreator.setStartedForSeqTracks(seqTracks, OtrsTicket.ProcessingStep.ALIGNMENT)
    }

    void "test doFurtherPreparation should mark start of workflow in MergingWorkPackage"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        List<SeqTrack> seqTracks = [createSeqTrack(), createSeqTrack()]
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage([seqTracks: seqTracks, needsProcessing: true])
        RoddyBamFile roddyBamFile = createBamFile(workPackage: mergingWorkPackage)

        PanCancerPrepareJob job = Spy(PanCancerPrepareJob) {
            1 * getSeqTracks(workflowStep) >> seqTracks
            1 * getRoddyBamFile(workflowStep) >> roddyBamFile
        }

        job.notificationCreator = Mock(NotificationCreator)

        when:
        job.doFurtherPreparation(workflowStep)

        then:
        !MergingWorkPackage.findAll(seqTracks: seqTracks).first().needsProcessing
    }
}
