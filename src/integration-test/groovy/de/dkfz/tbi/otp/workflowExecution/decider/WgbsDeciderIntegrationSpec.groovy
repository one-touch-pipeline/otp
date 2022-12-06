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
package de.dkfz.tbi.otp.workflowExecution.decider

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.util.Pair
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

import java.time.LocalDate

@Rollback
@Integration
class WgbsDeciderIntegrationSpec extends Specification implements WorkflowSystemDomainFactory, IsRoddy {

    @Autowired
    WgbsDecider wgbsDecider

    void "test getSeqType for FASTQ file"() {
        given:
        WorkflowArtefact wa = createWorkflowArtefact()
        SeqTrack seqTrack = createSeqTrack(workflowArtefact: wa)

        expect:
        wgbsDecider.getSeqType(wa) == seqTrack.seqType
    }

    void "test findAdditionalRequiredInputArtefacts"() {
        given:
        WorkflowArtefact inputArtefact = createWorkflowArtefact(artefactType: ArtefactType.FASTQ)
        SeqType seqType = createSeqType()
        Individual individual = createIndividual()
        SampleType sampleType = createSampleType()
        Sample sample1 = createSample(individual: individual, sampleType: sampleType)
        createSeqTrack(workflowArtefact: inputArtefact, seqType: seqType, sample: sample1)

        WorkflowArtefact additionalRequiredArtefact1 = createWorkflowArtefact(artefactType: ArtefactType.FASTQ)
        createSeqTrack(workflowArtefact: additionalRequiredArtefact1, seqType: seqType, sample: sample1)

        WorkflowArtefact bamArtefact = createWorkflowArtefact(artefactType: ArtefactType.BAM)
        createBamFile(workflowArtefact: bamArtefact, workPackage: createMergingWorkPackage(sample: sample1, seqType: seqType))

        WorkflowArtefact fastqcArtefact = createWorkflowArtefact(artefactType: ArtefactType.FASTQC)
        DomainFactory.createFastqcProcessedFile(workflowArtefact: fastqcArtefact, dataFile: createDataFile(seqTrack: createSeqTrack(
                sample: sample1,
                seqType: seqType,
        )))

        WorkflowArtefact failedArtefact = createWorkflowArtefact(artefactType: ArtefactType.FASTQ, state: WorkflowArtefact.State.FAILED)
        createSeqTrack(workflowArtefact: failedArtefact, seqType: seqType, sample: sample1)

        WorkflowArtefact withdrawnArtefact = createWorkflowArtefact(artefactType: ArtefactType.FASTQ, withdrawnDate: LocalDate.now(), withdrawnComment: "x")
        createSeqTrack(workflowArtefact: withdrawnArtefact, seqType: seqType, sample: sample1)

        WorkflowArtefact differentSeqType = createWorkflowArtefact(artefactType: ArtefactType.FASTQ)
        createSeqTrack(workflowArtefact: differentSeqType, seqType: DomainFactory.createExomeSeqType(), sample: sample1)

        WorkflowArtefact differentIndividual = createWorkflowArtefact(artefactType: ArtefactType.FASTQ)
        Individual individual2 = createIndividual()
        Sample sample2 = createSample(individual: individual2, sampleType: sampleType)
        createSeqTrack(workflowArtefact: differentIndividual, seqType: seqType, sample: sample2)

        WorkflowArtefact differentSampleType = createWorkflowArtefact(artefactType: ArtefactType.FASTQ)
        SampleType sampleType2 = createSampleType()
        Sample sample3 = createSample(individual: individual, sampleType: sampleType2)
        createSeqTrack(workflowArtefact: differentSampleType, seqType: seqType, sample: sample3)

        expect:
        CollectionUtils.containSame(
                wgbsDecider.findAdditionalRequiredInputArtefacts([inputArtefact]),
                [
                        inputArtefact,
                        additionalRequiredArtefact1,
                ]
        )
    }

    void "test groupArtefactsForWorkflowExecution"() {
        given:
        Project project = createProject()
        SeqType seqType = createSeqType()
        SeqType exomeSeqType = DomainFactory.createExomeSeqType()
        createMergingCriteria(project: project, seqType: seqType)
        createMergingCriteria(project: project, seqType: exomeSeqType)

        Individual individual = createIndividual(project: project)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual: individual, sampleType: sampleType)
        SeqPlatformGroup seqPlatformGroup = createSeqPlatformGroup()
        SeqPlatform seqPlatform = createSeqPlatform(seqPlatformGroups: [seqPlatformGroup])
        LibraryPreparationKit libraryPreparationKit = createLibraryPreparationKit()

        WorkflowArtefact wa1 = createWorkflowArtefact()
        createSeqTrack(workflowArtefact: wa1, sample: sample,
                seqType: seqType,
                run: createRun(seqPlatform: seqPlatform),
                libraryPreparationKit: libraryPreparationKit,
        )
        WorkflowArtefact wa2 = createWorkflowArtefact()
        createSeqTrack(workflowArtefact: wa2, sample: sample,
                seqType: seqType,
                run: createRun(seqPlatform: seqPlatform),
                libraryPreparationKit: libraryPreparationKit,
        )

        WorkflowArtefact differentIndividual = createWorkflowArtefact()
        createSeqTrackWithTwoDataFile([
                workflowArtefact     : differentIndividual,
                sample               : createSample(individual: createIndividual(project: project), sampleType: sampleType),
                seqType              : seqType,
                libraryPreparationKit: libraryPreparationKit,
                run                  : createRun(seqPlatform: seqPlatform),
        ], [
                project: project
        ])

        WorkflowArtefact differentSampleType = createWorkflowArtefact()
        createSeqTrackWithTwoDataFile([
                workflowArtefact     : differentSampleType,
                sample               : createSample(individual: individual, sampleType: createSampleType()),
                seqType              : seqType,
                libraryPreparationKit: libraryPreparationKit,
                run                  : createRun(seqPlatform: seqPlatform),
        ], [
                project: project
        ])

        WorkflowArtefact differentSeqType = createWorkflowArtefact()
        createSeqTrackWithTwoDataFile([
                workflowArtefact     : differentSeqType,
                sample               : sample,
                seqType              : exomeSeqType,
                libraryPreparationKit: libraryPreparationKit,
                run                  : createRun(seqPlatform: seqPlatform),
        ], [
                project: project
        ])

        WorkflowArtefact differentSPG = createWorkflowArtefact()
        SeqPlatform seqPlatform2 = createSeqPlatform()
        createSeqPlatformGroup(seqPlatforms: [seqPlatform2])
        createSeqTrackWithTwoDataFile([
                workflowArtefact     : differentSPG,
                sample               : sample,
                seqType              : seqType,
                libraryPreparationKit: libraryPreparationKit,
                run                  : createRun(seqPlatform: seqPlatform2),
        ], [
                project: project
        ])

        WorkflowArtefact differentLibPrepKit = createWorkflowArtefact()
        createSeqTrackWithTwoDataFile([
                workflowArtefact     : differentLibPrepKit,
                sample               : sample,
                seqType              : seqType,
                libraryPreparationKit: createLibraryPreparationKit(),
                run                  : createRun(seqPlatform: seqPlatform),
        ], [
                project: project
        ])

        when:
        Collection<Collection<WorkflowArtefact>> result = wgbsDecider.groupArtefactsForWorkflowExecution(
                [wa1, wa2, differentIndividual, differentSampleType, differentSeqType, differentSPG, differentLibPrepKit])

        then:
        result.size() == 6
        CollectionUtils.containSame(result, [
                [wa1, wa2],
                [differentIndividual],
                [differentSampleType],
                [differentSeqType],
                [differentSPG],
                [differentLibPrepKit],
        ])
    }

    void "test groupArtefactsForWorkflowExecution with SeqPlatformGroups"() {
        given:
        Project project1 = createProject()
        SeqType seqType1 = createSeqType()
        Individual individual = createIndividual(project: project1)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual: individual, sampleType: sampleType)
        LibraryPreparationKit libraryPreparationKit = createLibraryPreparationKit()

        createMergingCriteria(project: project1, seqType: seqType1,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT)
        SeqPlatformGroup seqPlatformGroup11 = createSeqPlatformGroup()
        SeqPlatform seqPlatform11 = createSeqPlatform(seqPlatformGroups: [seqPlatformGroup11])
        SeqPlatformGroup seqPlatformGroup12 = createSeqPlatformGroup()
        SeqPlatform seqPlatform12 = createSeqPlatform(seqPlatformGroups: [seqPlatformGroup12])

        WorkflowArtefact wa11 = createWorkflowArtefact()
        createSeqTrack(workflowArtefact: wa11, sample: sample,
                seqType: seqType1,
                run: createRun(seqPlatform: seqPlatform11),
                libraryPreparationKit: libraryPreparationKit,
        )
        WorkflowArtefact wa12 = createWorkflowArtefact()
        createSeqTrack(workflowArtefact: wa12, sample: sample,
                seqType: seqType1,
                run: createRun(seqPlatform: seqPlatform12),
                libraryPreparationKit: libraryPreparationKit,
        )

        Project project2 = createProject()
        SeqType seqType2 = createSeqType()
        Individual individual2 = createIndividual(project: project2)
        SampleType sampleType2 = createSampleType()
        Sample sample2 = createSample(individual: individual2, sampleType: sampleType2)

        MergingCriteria mergingCriteria2 = createMergingCriteria(project: project2, seqType: seqType2,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC)
        SeqPlatformGroup seqPlatformGroup21 = createSeqPlatformGroup(mergingCriteria: mergingCriteria2)
        SeqPlatform seqPlatform21 = createSeqPlatform(seqPlatformGroups: [seqPlatformGroup21])
        SeqPlatformGroup seqPlatformGroup22 = createSeqPlatformGroup(mergingCriteria: mergingCriteria2)
        SeqPlatform seqPlatform22 = createSeqPlatform(seqPlatformGroups: [seqPlatformGroup22])

        WorkflowArtefact wa21 = createWorkflowArtefact()
        createSeqTrack(workflowArtefact: wa21, sample: sample2,
                seqType: seqType2,
                run: createRun(seqPlatform: seqPlatform21),
                libraryPreparationKit: libraryPreparationKit,
        )
        WorkflowArtefact wa22 = createWorkflowArtefact()
        createSeqTrack(workflowArtefact: wa22, sample: sample2,
                seqType: seqType2,
                run: createRun(seqPlatform: seqPlatform22),
                libraryPreparationKit: libraryPreparationKit,
        )

        Project project3 = createProject()
        SeqType seqType3 = createSeqType()
        Individual individual3 = createIndividual(project: project3)
        SampleType sampleType3 = createSampleType()
        Sample sample3 = createSample(individual: individual3, sampleType: sampleType3)

        createMergingCriteria(project: project3, seqType: seqType3,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING)
        SeqPlatformGroup seqPlatformGroup31 = createSeqPlatformGroup()
        SeqPlatform seqPlatform31 = createSeqPlatform(seqPlatformGroups: [seqPlatformGroup31])
        SeqPlatformGroup seqPlatformGroup32 = createSeqPlatformGroup()
        SeqPlatform seqPlatform32 = createSeqPlatform(seqPlatformGroups: [seqPlatformGroup32])

        WorkflowArtefact wa31 = createWorkflowArtefact()
        createSeqTrack(workflowArtefact: wa31, sample: sample3,
                seqType: seqType3,
                run: createRun(seqPlatform: seqPlatform31),
                libraryPreparationKit: libraryPreparationKit,
        )
        WorkflowArtefact wa32 = createWorkflowArtefact()
        createSeqTrack(workflowArtefact: wa32, sample: sample3,
                seqType: seqType3,
                run: createRun(seqPlatform: seqPlatform32),
                libraryPreparationKit: libraryPreparationKit,
        )

        when:
        Collection<Collection<WorkflowArtefact>> result = wgbsDecider.groupArtefactsForWorkflowExecution(
                [wa11, wa12, wa21, wa22, wa31, wa32])

        then:
        CollectionUtils.containSame(result, [
                [wa11],
                [wa12],
                [wa21],
                [wa22],
                [wa31, wa32],
        ])
    }

    void "test groupArtefactsForWorkflowExecution with ignoreSeqPlatformGroups"() {
        given:
        Project project1 = createProject()
        SeqType seqType1 = createSeqType()
        Individual individual = createIndividual(project: project1)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual: individual, sampleType: sampleType)
        LibraryPreparationKit libraryPreparationKit = createLibraryPreparationKit()

        createMergingCriteria(project: project1, seqType: seqType1,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT)
        SeqPlatformGroup seqPlatformGroup11 = createSeqPlatformGroup()
        SeqPlatform seqPlatform11 = createSeqPlatform(seqPlatformGroups: [seqPlatformGroup11])
        SeqPlatformGroup seqPlatformGroup12 = createSeqPlatformGroup()
        SeqPlatform seqPlatform12 = createSeqPlatform(seqPlatformGroups: [seqPlatformGroup12])

        WorkflowArtefact wa11 = createWorkflowArtefact()
        createSeqTrack(workflowArtefact: wa11, sample: sample,
                seqType: seqType1,
                run: createRun(seqPlatform: seqPlatform11),
                libraryPreparationKit: libraryPreparationKit,
        )
        WorkflowArtefact wa12 = createWorkflowArtefact()
        createSeqTrack(workflowArtefact: wa12, sample: sample,
                seqType: seqType1,
                run: createRun(seqPlatform: seqPlatform12),
                libraryPreparationKit: libraryPreparationKit,
        )

        Project project2 = createProject()
        SeqType seqType2 = createSeqType()
        Individual individual2 = createIndividual(project: project2)
        SampleType sampleType2 = createSampleType()
        Sample sample2 = createSample(individual: individual2, sampleType: sampleType2)

        MergingCriteria mergingCriteria2 = createMergingCriteria(project: project2, seqType: seqType2,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC)
        SeqPlatformGroup seqPlatformGroup21 = createSeqPlatformGroup(mergingCriteria: mergingCriteria2)
        SeqPlatform seqPlatform21 = createSeqPlatform(seqPlatformGroups: [seqPlatformGroup21])
        SeqPlatformGroup seqPlatformGroup22 = createSeqPlatformGroup(mergingCriteria: mergingCriteria2)
        SeqPlatform seqPlatform22 = createSeqPlatform(seqPlatformGroups: [seqPlatformGroup22])

        WorkflowArtefact wa21 = createWorkflowArtefact()
        createSeqTrack(workflowArtefact: wa21, sample: sample2,
                seqType: seqType2,
                run: createRun(seqPlatform: seqPlatform21),
                libraryPreparationKit: libraryPreparationKit,
        )
        WorkflowArtefact wa22 = createWorkflowArtefact()
        createSeqTrack(workflowArtefact: wa22, sample: sample2,
                seqType: seqType2,
                run: createRun(seqPlatform: seqPlatform22),
                libraryPreparationKit: libraryPreparationKit,
        )

        Project project3 = createProject()
        SeqType seqType3 = createSeqType()
        Individual individual3 = createIndividual(project: project3)
        SampleType sampleType3 = createSampleType()
        Sample sample3 = createSample(individual: individual3, sampleType: sampleType3)

        createMergingCriteria(project: project3, seqType: seqType3,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING)
        SeqPlatformGroup seqPlatformGroup31 = createSeqPlatformGroup()
        SeqPlatform seqPlatform31 = createSeqPlatform(seqPlatformGroups: [seqPlatformGroup31])
        SeqPlatformGroup seqPlatformGroup32 = createSeqPlatformGroup()
        SeqPlatform seqPlatform32 = createSeqPlatform(seqPlatformGroups: [seqPlatformGroup32])

        WorkflowArtefact wa31 = createWorkflowArtefact()
        createSeqTrack(workflowArtefact: wa31, sample: sample3,
                seqType: seqType3,
                run: createRun(seqPlatform: seqPlatform31),
                libraryPreparationKit: libraryPreparationKit,
        )
        WorkflowArtefact wa32 = createWorkflowArtefact()
        createSeqTrack(workflowArtefact: wa32, sample: sample3,
                seqType: seqType3,
                run: createRun(seqPlatform: seqPlatform32),
                libraryPreparationKit: libraryPreparationKit,
        )

        when:
        Collection<Collection<WorkflowArtefact>> result = wgbsDecider.groupArtefactsForWorkflowExecution(
                [wa11, wa12, wa21, wa22, wa31, wa32], [ignoreSeqPlatformGroup: 'TRUE'])

        then:
        CollectionUtils.containSame(result, [
                [wa11, wa12],
                [wa21, wa22],
                [wa31, wa32],
        ])
    }

    void "test groupArtefactsForWorkflowExecution with LibraryPreparationKits"() {
        given:
        Project project1 = createProject()
        SeqType seqType1 = createSeqType()
        Individual individual = createIndividual(project: project1)
        SampleType sampleType = createSampleType()
        Sample sample = createSample(individual: individual, sampleType: sampleType)
        SeqPlatformGroup seqPlatformGroup = createSeqPlatformGroup()
        SeqPlatform seqPlatform = createSeqPlatform(seqPlatformGroups: [seqPlatformGroup])

        createMergingCriteria(project: project1, seqType: seqType1,
                useLibPrepKit: true)
        LibraryPreparationKit libraryPreparationKit11 = createLibraryPreparationKit()
        LibraryPreparationKit libraryPreparationKit12 = createLibraryPreparationKit()

        WorkflowArtefact wa11 = createWorkflowArtefact()
        createSeqTrack(workflowArtefact: wa11, sample: sample,
                seqType: seqType1,
                run: createRun(seqPlatform: seqPlatform),
                libraryPreparationKit: libraryPreparationKit11,
        )
        WorkflowArtefact wa12 = createWorkflowArtefact()
        createSeqTrack(workflowArtefact: wa12, sample: sample,
                seqType: seqType1,
                run: createRun(seqPlatform: seqPlatform),
                libraryPreparationKit: libraryPreparationKit12,
        )

        Project project2 = createProject()
        SeqType seqType2 = createSeqType()
        Individual individual2 = createIndividual(project: project2)
        SampleType sampleType2 = createSampleType()
        Sample sample2 = createSample(individual: individual2, sampleType: sampleType2)

        createMergingCriteria(project: project2, seqType: seqType2,
                useLibPrepKit: false)
        LibraryPreparationKit libraryPreparationKit21 = createLibraryPreparationKit()
        LibraryPreparationKit libraryPreparationKit22 = createLibraryPreparationKit()

        WorkflowArtefact wa21 = createWorkflowArtefact()
        createSeqTrack(workflowArtefact: wa21, sample: sample2,
                seqType: seqType2,
                run: createRun(seqPlatform: seqPlatform),
                libraryPreparationKit: libraryPreparationKit21,
        )
        WorkflowArtefact wa22 = createWorkflowArtefact()
        createSeqTrack(workflowArtefact: wa22, sample: sample2,
                seqType: seqType2,
                run: createRun(seqPlatform: seqPlatform),
                libraryPreparationKit: libraryPreparationKit22,
        )

        when:
        Collection<Collection<WorkflowArtefact>> result = wgbsDecider.groupArtefactsForWorkflowExecution(
                [wa11, wa12, wa21, wa22])

        then:
        CollectionUtils.containSame(result, [
                [wa11],
                [wa12],
                [wa21, wa22],
        ])
    }

    void "test createWorkflowRunsAndOutputArtefacts, no FASTQ file is passed, doesn't create run"() {
        given:
        WorkflowArtefact artefact1 = createWorkflowArtefact(artefactType: ArtefactType.BAM)
        createBamFile(workflowArtefact: artefact1)
        WorkflowArtefact artefact2 = createWorkflowArtefact(artefactType: ArtefactType.FASTQC)
        DomainFactory.createFastqcProcessedFile(workflowArtefact: artefact2)

        expect:
        wgbsDecider.createWorkflowRunsAndOutputArtefacts([[artefact1, artefact2]], [], createWorkflowVersion()) == []
    }

    void "test createWorkflowRunsAndOutputArtefacts"() {
        given:
        findOrCreatePipeline()

        SeqPlatformGroup seqPlatformGroup = createSeqPlatformGroup()
        SeqPlatform seqPlatform = createSeqPlatform(seqPlatformGroups: [seqPlatformGroup])
        LibraryPreparationKit libraryPreparationKit = createLibraryPreparationKit()
        Workflow workflow = createWorkflow(name: WgbsWorkflow.WORKFLOW)
        WorkflowVersion version = createWorkflowVersion(workflow: workflow)
        SeqType seqType = createSeqType()
        Project project = createProject()
        SpeciesWithStrain species = createSpeciesWithStrain()
        ReferenceGenome referenceGenome = createReferenceGenome(species: [] as Set, speciesWithStrain: [species] as Set)
        Individual individual = createIndividual(species: species, project: project)

        WorkflowArtefact inputArtefact1 = createWorkflowArtefact(artefactType: ArtefactType.FASTQ, producedBy: createWorkflowRun())
        Sample sample = createSample(individual: individual)
        SeqTrack seqTrack1 = createSeqTrackWithTwoDataFile(workflowArtefact: inputArtefact1, seqType: seqType, sample: sample, run: createRun(seqPlatform: seqPlatform),
                libraryPreparationKit: libraryPreparationKit)

        WorkflowArtefact inputArtefact2 = createWorkflowArtefact(artefactType: ArtefactType.FASTQ)
        SeqTrack seqTrack2 = createSeqTrackWithTwoDataFile(workflowArtefact: inputArtefact2, run: createRun(seqPlatform: seqPlatform),
                libraryPreparationKit: libraryPreparationKit)

        createReferenceGenomeSelector(
                project: project,
                seqType: seqType,
                workflow: workflow,
                referenceGenome: referenceGenome,
        )

        when:
        Collection<WorkflowArtefact> result = wgbsDecider.createWorkflowRunsAndOutputArtefacts(
                [[inputArtefact1, inputArtefact2]], [], version)

        then:
        result.size() == 1
        WorkflowArtefact outputArtefact = result.first()
        outputArtefact.artefactType == ArtefactType.BAM
        RoddyBamFile bamFile = outputArtefact.artefact.get()
        CollectionUtils.containSame(bamFile.seqTracks, [seqTrack1, seqTrack2])
        CollectionUtils.containSame(bamFile.containedSeqTracks, [seqTrack1, seqTrack2])
        WorkflowRun run = outputArtefact.producedBy
        run.workflow == workflow
        CollectionUtils.containSame(run.inputArtefacts.values(), [inputArtefact1, inputArtefact2])
    }

    void "test groupInputArtefacts"() {
        given:
        Project project1 = createProject()
        SeqType seqType1 = createSeqType()
        Project project2 = createProject()
        SeqType seqType2 = DomainFactory.createExomeSeqType()

        WorkflowArtefact artefact2 = createWorkflowArtefact()
        SeqTrack seqTrack = createSeqTrack(seqType: seqType1, workflowArtefact: artefact2)
        seqTrack.individual.project = project1
        seqTrack.individual.save(flush: true)

        WorkflowArtefact differentSeqType = createWorkflowArtefact()
        SeqTrack seqTrack3 = createSeqTrack(seqType: seqType2, workflowArtefact: differentSeqType)
        seqTrack3.individual.project = project1
        seqTrack3.individual.save(flush: true)

        WorkflowArtefact differentProject = createWorkflowArtefact()
        SeqTrack seqTrack2 = createSeqTrack(seqType: seqType1, workflowArtefact: differentProject)
        seqTrack2.individual.project = project2
        seqTrack2.individual.save(flush: true)

        when:
        Map<Pair<Project, SeqType>, List<WorkflowArtefact>> result = wgbsDecider.groupInputArtefacts(
                [artefact2, differentProject, differentSeqType])

        then:
        result.size() == 3
        result[new Pair(project1, seqType1)] == [artefact2]
        result[new Pair(project1, seqType2)] == [differentSeqType]
        result[new Pair(project2, seqType1)] == [differentProject]
    }
}
