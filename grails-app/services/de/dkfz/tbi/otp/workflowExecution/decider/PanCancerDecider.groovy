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
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.workflow.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@Component
@Transactional
class PanCancerDecider extends AbstractWorkflowDecider {

    @Autowired
    ConfigFragmentService configFragmentService

    @Autowired
    MailHelperService mailHelperService

    @Autowired
    RoddyBamFileService roddyBamFileService

    @Autowired
    UnalignableSeqTrackEmailCreator unalignableSeqTrackEmailCreator

    @Autowired
    WorkflowArtefactService workflowArtefactService

    @Autowired
    WorkflowRunService workflowRunService

    @Autowired
    WorkflowService workflowService

    @Override
    final protected Workflow getWorkflow() {
        return workflowService.getExactlyOneWorkflow(PanCancerWorkflow.WORKFLOW)
    }

    @Override
    final protected Set<ArtefactType> getSupportedInputArtefactTypes() {
        return [
                ArtefactType.FASTQ,
                ArtefactType.FASTQC,
                ArtefactType.BAM,
        ] as Set
    }

    @Override
    final protected SeqType getSeqType(WorkflowArtefact inputArtefact) {
        Object artefact = inputArtefact.artefact.get()
        switch (artefact) {
            case SeqTrack: return (artefact as SeqTrack).seqType
            case FastqcProcessedFile: return (artefact as FastqcProcessedFile).dataFile.seqType
            case AbstractBamFile: return (artefact as AbstractBamFile).seqType
        }
    }

    @Override
    final protected Collection<WorkflowArtefact> findAdditionalRequiredInputArtefacts(Collection<WorkflowArtefact> inputArtefacts) {
        Set<WorkflowArtefact> result = [] as Set
        inputArtefacts.each { WorkflowArtefact inputArtefact ->
            Individual individual
            SampleType sampleType
            SeqType seqType
            Object artefact = inputArtefact.artefact.get()
            switch (artefact) {
                case SeqTrack:
                    SeqTrack seqTrack = artefact as SeqTrack
                    individual = seqTrack.individual
                    sampleType = seqTrack.sampleType
                    seqType = seqTrack.seqType
                    break
                case FastqcProcessedFile:
                    FastqcProcessedFile fastqcProcessedFile = artefact as FastqcProcessedFile
                    individual = (fastqcProcessedFile.dataFile.individual)
                    sampleType = (fastqcProcessedFile.dataFile.sampleType)
                    seqType = (fastqcProcessedFile.dataFile.seqType)
                    break
                case RoddyBamFile:
                    RoddyBamFile rbf = artefact as RoddyBamFile
                    individual = (rbf.individual)
                    sampleType = (rbf.sampleType)
                    seqType = (rbf.seqType)
                    break
            }

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
            result.addAll(FastqcProcessedFile.createCriteria().list {
                dataFile {
                    seqTrack {
                        sample {
                            eq("individual", individual)
                            eq("sampleType", sampleType)
                        }
                        eq("seqType", seqType)
                    }
                    eq("fileWithdrawn", false)
                }
                workflowArtefact {
                    not {
                        'in'("state", [WorkflowArtefact.State.FAILED, WorkflowArtefact.State.OMITTED])
                    }
                    isNull("withdrawnDate")
                }
            }*.workflowArtefact)
            result.addAll(RoddyBamFile.createCriteria().list {
                workPackage {
                    sample {
                        eq("individual", individual)
                        eq("sampleType", sampleType)
                    }
                    eq("seqType", seqType)
                }
                eq("withdrawn", false)
                workflowArtefact {
                    not {
                        'in'("state", [WorkflowArtefact.State.FAILED, WorkflowArtefact.State.OMITTED])
                    }
                    isNull("withdrawnDate")
                }
            }*.workflowArtefact)
        }

        return result
    }

    @Override
    final protected Collection<Collection<WorkflowArtefact>> groupArtefactsForWorkflowExecution(Collection<WorkflowArtefact> inputArtefacts,
                                                                                                Map<String, String> userParams = [:]) {
        boolean ignoreSeqPlatformGroup = "TRUE".equalsIgnoreCase(userParams['ignoreSeqPlatformGroup'].toString())
        return inputArtefacts.groupBy { WorkflowArtefact inputArtefact ->
            Individual individual
            SampleType sampleType
            Project project
            SeqType seqType
            AntibodyTarget antibodyTarget
            SeqPlatformGroup seqPlatformGroup
            LibraryPreparationKit libraryPreparationKit

            Object artefact = inputArtefact.artefact.get()
            switch (artefact) {
                case SeqTrack:
                    SeqTrack seqTrack = artefact as SeqTrack
                    individual = seqTrack.individual
                    sampleType = seqTrack.sampleType
                    project = seqTrack.project
                    seqType = seqTrack.seqType
                    antibodyTarget = seqTrack.antibodyTarget
                    seqPlatformGroup = seqTrack.seqPlatformGroup
                    libraryPreparationKit = seqTrack.libraryPreparationKit
                    break
                case FastqcProcessedFile:
                    FastqcProcessedFile fastqcProcessedFile = artefact as FastqcProcessedFile
                    individual = fastqcProcessedFile.dataFile.individual
                    sampleType = fastqcProcessedFile.dataFile.sampleType
                    project = fastqcProcessedFile.dataFile.project
                    seqType = fastqcProcessedFile.dataFile.seqType
                    antibodyTarget = fastqcProcessedFile.dataFile.seqTrack.antibodyTarget
                    seqPlatformGroup = fastqcProcessedFile.dataFile.seqTrack.seqPlatformGroup
                    libraryPreparationKit = fastqcProcessedFile.dataFile.seqTrack.libraryPreparationKit
                    break
                case RoddyBamFile:
                    RoddyBamFile bamFile = artefact as RoddyBamFile
                    individual = bamFile.individual
                    sampleType = bamFile.sampleType
                    project = bamFile.project
                    seqType = bamFile.seqType
                    antibodyTarget = bamFile.workPackage.antibodyTarget
                    seqPlatformGroup = bamFile.mergingWorkPackage.seqPlatformGroup
                    libraryPreparationKit = bamFile.mergingWorkPackage.libraryPreparationKit
                    break
                default:
                    throw new OtpRuntimeException("Unsupported class: ${artefact.class}")
            }

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
        RoddyBamFile baseBamFile = (artefacts.findAll { it.artefactType == ArtefactType.BAM }*.artefact*.get() as List<RoddyBamFile>)
                .find { bamFile ->
                    bamFile.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.PROCESSED && !bamFile.withdrawn &&
                            bamFile.mergingWorkPackage.bamFileInProjectFolder == bamFile
                }
        List<SeqTrack> seqTracks = artefacts.findAll { it.artefactType == ArtefactType.FASTQ }*.artefact*.get() as List<SeqTrack>
        List<FastqcProcessedFile> fastqcProcessedFiles = artefacts.findAll {
            it.artefactType == ArtefactType.FASTQC
        }*.artefact*.get() as List<FastqcProcessedFile>

        if (seqTracks.empty) {
            return null
        }

        if (!seqTracks.every { SeqTrack seqTrack ->
            seqTrack.dataFiles.every { DataFile dataFile -> fastqcProcessedFiles.find { it.dataFile == dataFile } }
        }) {
            return null
        }
        if (!fastqcProcessedFiles.every { it.dataFile.seqTrack in seqTracks }) {
            return null
        }

        List<SeqTrack> newSeqTracks = seqTracks - (baseBamFile?.containedSeqTracks ?: [])
        if (!newSeqTracks) {
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
            if (baseBamFile) {
                assert workPackage == baseBamFile.mergingWorkPackage
            }
            assert workPackage.referenceGenome == referenceGenome
            if (!workPackage.satisfiesCriteria(seqTrack)) {
                Map<String, String> content = unalignableSeqTrackEmailCreator.getMailContent(workPackage, seqTrack)
                mailHelperService.sendEmailToTicketSystem(content["subject"], content["body"])
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
        String shortName = "${PanCancerWorkflow.WORKFLOW}: ${individual.pid} ${sampleType.displayName} ${seqType.displayNameWithLibraryLayout}"

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

        artefacts.groupBy { it.artefactType }.each { type, groupedArtefacts ->
            if (type == ArtefactType.BAM) {
                groupedArtefacts.each {
                    new WorkflowRunInputArtefact(
                            workflowRun: run,
                            role: PanCancerWorkflow.INPUT_BASE_BAM_FILE,
                            workflowArtefact: it,
                    ).save(flush: true)
                }
            } else {
                String role = (type == ArtefactType.FASTQ) ? PanCancerWorkflow.INPUT_FASTQ : PanCancerWorkflow.INPUT_FASTQC
                groupedArtefacts.each {
                    new WorkflowRunInputArtefact(
                            workflowRun: run,
                            role: "${role}_${it.artefact.get().id}",
                            workflowArtefact: it,
                    ).save(flush: true)
                }
            }
        }

        WorkflowArtefact workflowOutputArtefact = workflowArtefactService.buildWorkflowArtefact(new WorkflowArtefactValues(
                run,
                PanCancerWorkflow.OUTPUT_BAM,
                ArtefactType.BAM,
                artefactDisplayName,
        )).save(flush: true)

        int identifier = RoddyBamFile.nextIdentifier(workPackage)
        RoddyBamFile bamFile = new RoddyBamFile(
                workflowArtefact: workflowOutputArtefact,
                workPackage: workPackage,
                identifier: identifier,
                workDirectoryName: "${roddyBamFileService.WORK_DIR_PREFIX}_${identifier}",
                baseBamFile: baseBamFile,
                seqTracks: newSeqTracks,
        )

        bamFile.numberOfMergedLanes = bamFile.containedSeqTracks.size()
        assert bamFile.save(flush: true)

        run.workDirectory = roddyBamFileService.getWorkDirectory(bamFile)
        run.save(flush: true)

        return workflowOutputArtefact
    }

    @Override
    final protected Map<Pair<Project, SeqType>, List<WorkflowArtefact>> groupInputArtefacts(Collection<WorkflowArtefact> inputArtefacts) {
        return inputArtefacts.groupBy { WorkflowArtefact inputArtefact ->
            Object artefact = inputArtefact.artefact.get()
            switch (artefact) {
                case SeqTrack:
                    SeqTrack seqTrack = artefact as SeqTrack
                    return new Pair<Project, SeqType>(seqTrack.project, seqTrack.seqType)
                case FastqcProcessedFile:
                    FastqcProcessedFile fastqcProcessedFile = artefact as FastqcProcessedFile
                    return new Pair<Project, SeqType>(fastqcProcessedFile.dataFile.project, fastqcProcessedFile.dataFile.seqType)
                case RoddyBamFile:
                    RoddyBamFile rbf = artefact as RoddyBamFile
                    return new Pair<Project, SeqType>(rbf.project, rbf.seqType)
            }
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