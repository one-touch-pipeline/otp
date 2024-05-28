/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.workflow

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPanCancerFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflowExecution.ReferenceGenomeSelector
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersionSelector
import de.dkfz.tbi.otp.workflowExecution.decider.AllDecider

@Rollback
@Integration
class TriggerWorkflowServiceIntegrationSpec extends Specification implements DomainFactoryCore, RoddyPanCancerFactory, WorkflowSystemDomainFactory {

    @Autowired
    TriggerWorkflowService service

    void "test getBamFiles, with empty input"() {
        when:
        List<AbstractBamFile> result = service.getBamFiles([])

        then:
        TestCase.assertContainSame(result, [])
    }

    void "test getBamFiles, with seq. track with a BAM file"() {
        when:
        List<AbstractBamFile> result = service.getBamFiles([createSeqTrack().id])

        then:
        TestCase.assertContainSame(result, [])
    }

    void "test getBamFiles, with single BAM file with single seq. track"() {
        given:
        RoddyBamFile bamFile = createBamFile(seqTracks: [createSeqTrack()])

        when:
        List<AbstractBamFile> result = service.getBamFiles(bamFile.containedSeqTracks*.id)

        then:
        TestCase.assertContainSame(result, [bamFile])
    }

    void "test getBamFiles, with single BAM file with multiple seq. tracks, passing one seq. track"() {
        given:
        SeqTrack seqTrack1 = createSeqTrack()
        RoddyBamFile bamFile = createBamFile(seqTracks: [seqTrack1, createSeqTrack(), createSeqTrack()])

        when:
        List<AbstractBamFile> result = service.getBamFiles([seqTrack1.id])

        then:
        TestCase.assertContainSame(result, [bamFile])
    }

    void "test getBamFiles, with single BAM file with multiple seq. tracks, passing all seq. tracks"() {
        given:
        RoddyBamFile bamFile = createBamFile(seqTracks: [createSeqTrack(), createSeqTrack(), createSeqTrack()])

        when:
        List<AbstractBamFile> result = service.getBamFiles(bamFile.containedSeqTracks*.id)

        then:
        TestCase.assertContainSame(result, [bamFile])
    }

    void "test getBamFiles, with multiple BAM files"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        RoddyBamFile bamFile2 = createBamFile(seqTracks: bamFile.containedSeqTracks as List, workPackage: bamFile.workPackage)

        when:
        List<AbstractBamFile> result = service.getBamFiles(bamFile.containedSeqTracks*.id + bamFile2.containedSeqTracks*.id)

        then:
        TestCase.assertContainSame(result, [bamFile, bamFile2])
    }

    void "test getBamFiles, with multiple BAM files, one is withdrawn"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        RoddyBamFile bamFile2 = createBamFile(seqTracks: bamFile.containedSeqTracks as List, withdrawn: true, workPackage: bamFile.workPackage)

        when:
        List<AbstractBamFile> result = service.getBamFiles(bamFile.containedSeqTracks*.id + bamFile2.containedSeqTracks*.id)

        then:
        TestCase.assertContainSame(result, [bamFile])
    }

    void "test getSeqTracks, with empty input"() {
        when:
        List<SeqTrack> result = service.getSeqTracks([])

        then:
        TestCase.assertContainSame(result, [])
    }

    void "test getSeqTracks, with single BAM file"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        createSeqTrack()

        when:
        List<SeqTrack> result = service.getSeqTracks([bamFile.id])

        then:
        TestCase.assertContainSame(result, bamFile.containedSeqTracks)
    }

    void "test getSeqTracks, with multiple BAM files"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        RoddyBamFile bamFile2 = createBamFile()

        when:
        List<SeqTrack> result = service.getSeqTracks([bamFile.id, bamFile2.id])

        then:
        TestCase.assertContainSame(result, bamFile.containedSeqTracks + bamFile2.containedSeqTracks)
    }

    void "test getSeqTracks, with multiple BAM files, should return each seq tracks only once"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        RoddyBamFile bamFile2 = createBamFile(seqTracks: bamFile.containedSeqTracks as List, withdrawn: true, workPackage: bamFile.workPackage)

        when:
        List<SeqTrack> result = service.getSeqTracks([bamFile.id, bamFile2.id])

        then:
        result.size() == bamFile.containedSeqTracks.size()
        TestCase.assertContainSame(result, bamFile.containedSeqTracks)
    }

    void "test getInfo, with empty input"() {
        expect:
        service.getInfo([]) == []
    }

    void "test getInfo, with several seq tracks"() {
        given:
        createBamFile()
        SeqTrack seqTrack1 = createSeqTrack()
        WorkflowVersionSelector workflowVersionSelector1 = createWorkflowVersionSelector(project: seqTrack1.project, seqType: seqTrack1.seqType)
        ReferenceGenomeSelector referenceGenomeSelector1a = createReferenceGenomeSelector(project: seqTrack1.project, seqType: seqTrack1.seqType, workflow: workflowVersionSelector1.workflowVersion.workflow)
        ReferenceGenomeSelector referenceGenomeSelector1b = createReferenceGenomeSelector(project: seqTrack1.project, seqType: seqTrack1.seqType, workflow: workflowVersionSelector1.workflowVersion.workflow)
        SeqTrack seqTrack2 = createSeqTrack()
        WorkflowVersionSelector workflowVersionSelector2 = createWorkflowVersionSelector(project: seqTrack2.project, seqType: seqTrack2.seqType)
        ReferenceGenomeSelector referenceGenomeSelector2a = createReferenceGenomeSelector(project: seqTrack2.project, seqType: seqTrack2.seqType, workflow: workflowVersionSelector2.workflowVersion.workflow)
        ReferenceGenomeSelector referenceGenomeSelector2b = createReferenceGenomeSelector(project: seqTrack2.project, seqType: seqTrack2.seqType, workflow: workflowVersionSelector2.workflowVersion.workflow)
        createWorkflowVersionSelector(project: seqTrack1.project, seqType: seqTrack2.seqType)

        when:
        List<WorkflowVersionAndReferenceGenomeSelector> result = service.getInfo([seqTrack1, seqTrack2])

        then:
        TestCase.assertContainSame(result*.workflowVersionSelector, [workflowVersionSelector1, workflowVersionSelector2])
        TestCase.assertContainSame(result.find { it.workflowVersionSelector == workflowVersionSelector1 }.referenceGenomeSelectors,
                [referenceGenomeSelector1a, referenceGenomeSelector1b])
        TestCase.assertContainSame(result.find { it.workflowVersionSelector == workflowVersionSelector2 }.referenceGenomeSelectors,
                [referenceGenomeSelector2a, referenceGenomeSelector2b])
    }

    void "triggerWorkflowByProjectAndSampleTypes should trigger allDecider for workflow artefacts"() {
        given:
        service.allDecider = Mock(AllDecider)

        Project project = createProject()
        SampleType sampleType1 = createSampleType()
        SampleType sampleType2 = createSampleType()
        SampleType sampleType3 = createSampleType()
        WorkflowArtefact workflowArtefact1 = createWorkflowArtefact()
        WorkflowArtefact workflowArtefact2 = createWorkflowArtefact()
        WorkflowArtefact workflowArtefact3 = createWorkflowArtefact()
        createBamFile([
                workflowArtefact: workflowArtefact1,
                workPackage     : createMergingWorkPackage([sample: createSample([sampleType: sampleType1, individual: createIndividual([project: project])])]),
        ])
        createBamFile([
                workflowArtefact: workflowArtefact2,
                workPackage     : createMergingWorkPackage([sample: createSample([sampleType: sampleType2, individual: createIndividual([project: project])])]),
        ])
        createBamFile([
                workflowArtefact: workflowArtefact3,
                workPackage     : createMergingWorkPackage([sample: createSample([sampleType: sampleType3, individual: createIndividual([project: project])])]),
        ])
        createBamFile([workPackage: createMergingWorkPackage([sample: createSample([individual: createIndividual([project: project])])])])
        createBamFile([workPackage: createMergingWorkPackage([sample: createSample([sampleType: sampleType3])])])
        createBamFile()
        createWorkflow(name: PanCancerWorkflow.WORKFLOW)
        createWorkflow(name: RnaAlignmentWorkflow.WORKFLOW)

        when:
        service.triggerWorkflowByProjectAndSampleTypes(project, [sampleType1, sampleType2, sampleType3] as Set)

        then:
        1 * service.allDecider.decide([workflowArtefact1, workflowArtefact2, workflowArtefact3])
    }
}
