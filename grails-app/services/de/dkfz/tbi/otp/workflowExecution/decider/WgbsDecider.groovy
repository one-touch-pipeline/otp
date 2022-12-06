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

import grails.gorm.transactions.Transactional
import grails.util.Pair
import groovy.transform.Canonical
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@Component
@Transactional
class WgbsDecider extends AbstractWorkflowDecider {

    @Autowired
    ConfigFragmentService configFragmentService

    @Autowired
    RoddyBamFileService roddyBamFileService

    @Autowired
    WorkflowArtefactService workflowArtefactService

    @Autowired
    WorkflowRunService workflowRunService

    @Autowired
    WorkflowService workflowService

    @Override
    final protected Workflow getWorkflow() {
        return workflowService.getExactlyOneWorkflow(WgbsWorkflow.WORKFLOW)
    }

    @Override
    final protected Set<ArtefactType> getSupportedInputArtefactTypes() {
        return [
                ArtefactType.FASTQ,
        ] as Set
    }

    @Override
    final protected SeqType getSeqType(WorkflowArtefact inputArtefact) {
        return (inputArtefact.artefact.get() as SeqTrack).seqType
    }

    @Override
    final protected Collection<WorkflowArtefact> findAdditionalRequiredInputArtefacts(Collection<WorkflowArtefact> inputArtefacts) {
        Set<WorkflowArtefact> result = [] as Set
        inputArtefacts.each { WorkflowArtefact inputArtefact ->
            SeqTrack seqTrack = inputArtefact.artefact.get() as SeqTrack
            Individual individual = seqTrack.individual
            SampleType sampleType = seqTrack.sampleType
            SeqType seqType = seqTrack.seqType

            result.addAll(SeqTrack.createCriteria().list {
                sample {
                    eq("individual", individual)
                    eq("sampleType", sampleType)
                }
                eq("seqType", seqType)
                workflowArtefact {
                    not {
                        'in'("state", [WorkflowArtefact.State.FAILED, WorkflowArtefact.State.OMITTED])
                    }
                    isNull("withdrawnDate")
                }
            }.findAll { !it.isWithdrawn() }*.workflowArtefact)
        }

        return result
    }

    @Override
    final protected Collection<Collection<WorkflowArtefact>> groupArtefactsForWorkflowExecution(Collection<WorkflowArtefact> inputArtefacts,
                                                                                                Map<String, String> userParams = [:]) {
        boolean ignoreSeqPlatformGroup = "TRUE".equalsIgnoreCase(userParams['ignoreSeqPlatformGroup'].toString())
        return inputArtefacts.groupBy { WorkflowArtefact inputArtefact ->
            SeqTrack seqTrack = inputArtefact.artefact.get() as SeqTrack
            Individual individual = seqTrack.individual
            SampleType sampleType = seqTrack.sampleType
            Project project = seqTrack.project
            SeqType seqType = seqTrack.seqType
            AntibodyTarget antibodyTarget = seqTrack.antibodyTarget
            SeqPlatformGroup seqPlatformGroup = seqTrack.seqPlatformGroup
            LibraryPreparationKit libraryPreparationKit = seqTrack.libraryPreparationKit

            MergingCriteria mergingCriteria = CollectionUtils.atMostOneElement(MergingCriteria.findAllByProjectAndSeqType(project, seqType))
            if (seqPlatformGroup == null) {
                assert mergingCriteria?.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING
            }

            if (mergingCriteria?.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING || ignoreSeqPlatformGroup) {
                seqPlatformGroup = null
            }
            if (!mergingCriteria.useLibPrepKit) {
                libraryPreparationKit = null
            }

            new GroupBy(
                    individual,
                    sampleType,
                    seqType,
                    antibodyTarget,
                    seqPlatformGroup,
                    libraryPreparationKit,
            )
        }.values()
    }

    @Override
    final protected Collection<WorkflowArtefact> createWorkflowRunsAndOutputArtefacts(Collection<Collection<WorkflowArtefact>> inputArtefacts,
                                                                                      Collection<WorkflowArtefact> initialArtefacts, WorkflowVersion version) {
        return inputArtefacts.collect {
            createWorkflowRunIfPossible(it, version)
        }.findAll()
    }

    private WorkflowArtefact createWorkflowRunIfPossible(Collection<WorkflowArtefact> artefacts, WorkflowVersion version) {
        List<SeqTrack> seqTracks = artefacts.findAll { it.artefactType == ArtefactType.FASTQ }*.artefact*.get() as List<SeqTrack>

        if (seqTracks.empty) {
            return null
        }

        SeqTrack seqTrack = seqTracks.first()
        Project project = seqTrack.project
        SeqType seqType = seqTrack.seqType
        AntibodyTarget antibodyTarget = seqTrack.antibodyTarget
        Set<SpeciesWithStrain> allSpecies = ([seqTrack.individual.species] + (seqTrack.sample.mixedInSpecies ?: [])) as Set
        Individual individual = seqTrack.individual
        SampleType sampleType = seqTrack.sampleType
        ProcessingPriority priority = project.processingPriority

        ReferenceGenome referenceGenome = CollectionUtils.exactlyOneElement(
                ReferenceGenomeSelector.findAllByProjectAndSeqTypeAndWorkflow(project, seqType, workflow)
                        .findAll { it.species == allSpecies }
        ).referenceGenome

        MergingWorkPackage workPackage = atMostOneElement(
                MergingWorkPackage.findAllWhere(
                        sample: seqTrack.sample,
                        seqType: seqType,
                        antibodyTarget: seqType.hasAntibodyTarget ? antibodyTarget : null,
                )
        )
        if (workPackage) {
            assert workPackage.referenceGenome == referenceGenome
            if (!workPackage.satisfiesCriteria(seqTrack)) {
                return null
            }
        } else {
            workPackage = new MergingWorkPackage(
                    MergingWorkPackage.getMergingProperties(seqTrack) + [
                            referenceGenome: referenceGenome,
                            pipeline       : CollectionUtils.exactlyOneElement(Pipeline.findAllByName(Pipeline.Name.PANCAN_ALIGNMENT)),
                    ])
        }
        workPackage.seqTracks = seqTracks
        workPackage.save(flush: true)

        List<String> runDisplayName = [
                "project: ${project.name}",
                "individual: ${individual.displayName}",
                "sampleType: ${sampleType.displayName}",
                "seqType: ${seqType.displayNameWithLibraryLayout}",
        ]
        List<String> artefactDisplayName = runDisplayName
        artefactDisplayName.remove(0)
        String shortName = "${WgbsWorkflow.WORKFLOW}: ${individual.pid} ${sampleType.displayName} ${seqType.displayNameWithLibraryLayout}"

        List<ExternalWorkflowConfigFragment> configFragments = configFragmentService.getSortedFragments(new SingleSelectSelectorExtendedCriteria(
                workflow, version, project, seqType, referenceGenome, workPackage.libraryPreparationKit))

        WorkflowRun run = workflowRunService.buildWorkflowRun(
                workflow,
                priority,
                "", // set later
                project,
                runDisplayName,
                shortName,
                configFragments,
                version,
        )

        artefacts.each {
            new WorkflowRunInputArtefact(
                    workflowRun: run,
                    role: "${WgbsWorkflow.INPUT_FASTQ}_${it.artefact.get().id}",
                    workflowArtefact: it,
            ).save(flush: true)
        }

        WorkflowArtefact workflowOutputArtefact = workflowArtefactService.buildWorkflowArtefact(new WorkflowArtefactValues(
                run,
                WgbsWorkflow.OUTPUT_BAM,
                ArtefactType.BAM,
                artefactDisplayName,
        )).save(flush: true)

        int identifier = RoddyBamFile.nextIdentifier(workPackage)
        RoddyBamFile bamFile = new RoddyBamFile(
                workflowArtefact: workflowOutputArtefact,
                workPackage: workPackage,
                identifier: identifier,
                workDirectoryName: "${roddyBamFileService.WORK_DIR_PREFIX}_${identifier}",
                seqTracks: seqTracks,
        )

        bamFile.numberOfMergedLanes = bamFile.containedSeqTracks.size()
        assert bamFile.save(flush: true)

        run.workDirectory = roddyBamFileService.getWorkDirectory(bamFile).toString()
        run.save(flush: true)

        return workflowOutputArtefact
    }

    @Override
    final protected Map<Pair<Project, SeqType>, List<WorkflowArtefact>> groupInputArtefacts(Collection<WorkflowArtefact> inputArtefacts) {
        return inputArtefacts.groupBy { WorkflowArtefact inputArtefact ->
            SeqTrack seqTrack = inputArtefact.artefact.get() as SeqTrack
            return new Pair<Project, SeqType>(seqTrack.project, seqTrack.seqType)
        }
    }

    @Canonical
    class GroupBy {
        Individual individual
        SampleType sampleType
        SeqType seqType
        AntibodyTarget antibodyTarget
        SeqPlatformGroup seqPlatformGroup
        LibraryPreparationKit libraryPreparationKit
    }
}
