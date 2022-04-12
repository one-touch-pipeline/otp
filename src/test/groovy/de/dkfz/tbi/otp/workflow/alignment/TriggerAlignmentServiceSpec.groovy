/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.workflow.alignment

import grails.test.hibernate.HibernateSpec
import grails.testing.services.ServiceUnitTest

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.withdraw.RoddyBamFileWithdrawService
import de.dkfz.tbi.otp.workflow.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.decider.AllDecider

class TriggerAlignmentServiceSpec extends HibernateSpec implements ServiceUnitTest<TriggerAlignmentService>, IsRoddy, WorkflowSystemDomainFactory {

    @Override
    List<Class> getDomainClasses() {
        return [
                AbstractMergedBamFile,
                RoddyBamFile,
                DataFile,
                Project,
                SampleType,
                SeqType,
                Sample,
                Individual,
                SeqTrack,
                ExternalMergingWorkPackage,
                MergingWorkPackage,
                WorkflowArtefact,
                WorkflowRun,
                ReferenceGenomeSelector,
                BamFilePairAnalysis,
        ]
    }

    void "run triggerAlignment, which should trigger 1 new workflow and 1 old workflow"() {
        given:
        final SeqType st1 = createSeqType()
        final SeqType st2 = createSeqType()
        final SeqType st3 = createSeqType()

        Project project = createProject()
        Individual individual = createIndividual(project: project)

        Workflow wf = createWorkflow([
                supportedSeqTypes: [st1, st2]
        ])
        WorkflowRun run = createWorkflowRun([
                workflow: wf,
                project : project,
        ])

        WorkflowArtefact workflowArtefact1 = createWorkflowArtefact([
                producedBy  : run,
        ])
        WorkflowArtefact workflowArtefact2 = createWorkflowArtefact([
                producedBy: run
        ])

        SeqTrack seqTrack1 = createSeqTrackWithTwoDataFile([
                sample          : createSample(individual: individual),
                seqType         : st1,
                workflowArtefact: workflowArtefact1,
        ])
        SeqTrack seqTrack2 = createSeqTrackWithTwoDataFile([
                sample          : createSample(individual: individual),
                seqType         : st2,
                workflowArtefact: workflowArtefact2,
        ])

        SeqTrack seqTrack3 = createSeqTrackWithTwoDataFile([
                seqType: st3,
        ])

        WorkflowArtefact outputArtefact = createWorkflowArtefact([
                artefactType: ArtefactType.BAM,
                outputRole  : PanCancerWorkflow.OUTPUT_BAM,
                producedBy  : run,
        ])

        MergingWorkPackage mergingWorkPackage1 = createMergingWorkPackage()
        MergingWorkPackage mergingWorkPackage2 = createMergingWorkPackage()
        RoddyBamFile bamFile1 = createRoddyBamFile([
                workflowArtefact: outputArtefact,
                workPackage     : mergingWorkPackage1,
                seqTracks       : [seqTrack1, seqTrack2],
        ], RoddyBamFile)

        RoddyBamFile bamFile2 = createRoddyBamFile([
                workPackage: mergingWorkPackage2,
                seqTracks  : [seqTrack3],
        ], RoddyBamFile)

        // Mock service for new workflow system
        service.allDecider = Mock(AllDecider) {
            1 * decide(_) >> [outputArtefact]
            1 * findAllSeqTracksInNewWorkflowSystem(_) >> [seqTrack1, seqTrack2]
            0 * _
        }

        // Mock service for old workflow system and check if workflow is triggered
        service.seqTrackService = Mock(SeqTrackService) {
            1 * decideAndPrepareForAlignment(_) >> [mergingWorkPackage2]
            0 * _
        }

        // Check resetting OTRS tickets works
        service.otrsTicketService = Mock(OtrsTicketService) {
            1 * findAllOtrsTickets(_) >> [createOtrsTicket(), createOtrsTicket()]
            2 * resetAlignmentAndAnalysisNotification(_)
        }

        // Make sure sample pairs are created
        service.samplePairDeciderService = Mock(SamplePairDeciderService) {
            1 * findOrCreateSamplePairs([mergingWorkPackage1, mergingWorkPackage2])
        }

        service.roddyBamFileWithdrawService = Mock(RoddyBamFileWithdrawService) {
            _ * collectObjects(_) >> {
                return [bamFile1, bamFile2]
            }
        }

        when:
        Collection<MergingWorkPackage> mergingWorkPackages = service.triggerAlignment([seqTrack1, seqTrack2, seqTrack3] as Set, true)

        then:
        mergingWorkPackages.size() == 2
        TestCase.assertContainSame(mergingWorkPackages, [mergingWorkPackage1, mergingWorkPackage2])

        bamFile1.withdrawn
        bamFile2.withdrawn
    }
}
