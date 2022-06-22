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

import grails.gorm.transactions.Transactional
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.withdraw.RoddyBamFileWithdrawService
import de.dkfz.tbi.otp.workflow.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.decider.AllDecider

@Transactional(readOnly = true)
class TriggerAlignmentService {

    SeqTrackService seqTrackService
    SamplePairDeciderService samplePairDeciderService
    OtrsTicketService otrsTicketService
    AllDecider allDecider
    RoddyBamFileWithdrawService roddyBamFileWithdrawService
    MergingCriteriaService mergingCriteriaService

    @Transactional(readOnly = false)
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Collection<MergingWorkPackage> triggerAlignment(Collection<SeqTrack> seqTracks, boolean withdrawBamFiles = false, boolean ignoreSeqPlatformGroup = false) {
        // Mark the bam files as withdrawn
        if (withdrawBamFiles) {
            roddyBamFileWithdrawService.collectObjects(seqTracks as List<SeqTrack>).each { RoddyBamFile bamFile ->
                bamFile.withdraw()
                bamFile.save(flush: true)
            }
        }

        // Modify the notification status
        otrsTicketService.findAllOtrsTickets(seqTracks).each {
            otrsTicketService.resetAlignmentAndAnalysisNotification(it)
        }

        // Start alignment workflows
        Collection<SeqTrack> seqTracksInNewWorkflowSystem = allDecider.findAllSeqTracksInNewWorkflowSystem(seqTracks)
        Collection<MergingWorkPackage> mergingWorkPackages = allDecider.decide(seqTracksInNewWorkflowSystem*.workflowArtefact, false, [
                ignoreSeqPlatformGroup: ignoreSeqPlatformGroup.toString()
        ]).findAll {
            it.artefactType == ArtefactType.BAM
        }*.artefact*.get()*.workPackage + (seqTracks - seqTracksInNewWorkflowSystem).collectMany {
            seqTrackService.decideAndPrepareForAlignment(it)
        }.unique()

        if (mergingWorkPackages) {
            samplePairDeciderService.findOrCreateSamplePairs(mergingWorkPackages)
        }

        return mergingWorkPackages
    }

    /**
     * Count the given seqTracks that do not have the alignment workflow configured (deprecated)
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<Map<String, String>> createWarningsForMissingAlignmentConfig(Collection<SeqTrack> seqTracks) {
        Collection<SeqTrack> seqTracksNew = allDecider.findAllSeqTracksInNewWorkflowSystem(seqTracks)

        //new system
        List<Map<String, String>> entries = seqTracksNew.countBy {
            [
                    it.project,
                    it.seqType,
            ]
        }.findAll {
            !WorkflowVersionSelector.findAllByProjectAndSeqTypeAndDeprecationDateIsNull(it.key[0], it.key[1]).findAll {
                isAlignmentWorkflow(it.workflowVersion.workflow)
            }
        }.collect {
            [
                    project: ((Project) it.key[0]).name,
                    seqType: ((SeqType) it.key[1]).displayNameWithLibraryLayout,
                    count  : it.value as String,
            ]
        }

        //old system
        entries.addAll((seqTracks - seqTracksNew).countBy {
            [
                    it.project,
                    it.seqType,
            ]
        }.findAll {
            !RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndObsoleteDateIsNull(it.key[0], it.key[1]).findAll {
                it.pipeline.type == Pipeline.Type.ALIGNMENT
            }
        }.collect {
            [
                    project: ((Project) it.key[0]).name,
                    seqType: ((SeqType) it.key[1]).displayNameWithLibraryLayout,
                    count  : it.value as String,
            ]
        })

        return entries.sort {
            [
                    it.project,
                    it.seqType,
            ]
        }
    }

    /**
     * check that the SeqTracks of a Sample seqType combination have compatible SeqPlatforms according the MergingCriteria
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<Map<String, Object>> createWarningsForSamplesHavingMultipleSeqPlatformGroups(Collection<SeqTrack> seqTracks) {
        return seqTracks.groupBy {
            [
                    it.project,
                    it.seqType,
            ]
        }.findAll {
            mergingCriteriaService.findMergingCriteria(it.key[0], it.key[1])?.useSeqPlatformGroup !=
                    MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING
        }.collectMany {
            SeqType seqType = it.key[1]
            it.value.groupBy([
                    { SeqTrack seqTrack ->
                        seqTrack.sample
                    },
                    { SeqTrack seqTrack ->
                        seqTrack.seqPlatformGroup
                    },
            ]).findAll { Sample sample, Map<SeqPlatformGroup, List<SeqTrack>> list ->
                list.size() > 1
            }.collect { Sample sample, Map<SeqPlatformGroup, List<SeqTrack>> list ->
                [
                        project              : sample.project.name,
                        individual           : sample.individual.pid,
                        seqType              : seqType.displayNameWithLibraryLayout,
                        sampleType           : sample.sampleType.name,
                        seqPlatformGroupTable: list.collect { SeqPlatformGroup seqPlatformGroup, List<SeqTrack> seqTrackList ->
                            [
                                    seqPlatformGroupId: seqPlatformGroup.id,
                                    count             : seqTrackList.size(),
                                    seqPlatforms      : seqPlatformGroup.seqPlatforms*.fullName.sort(),
                            ]
                        }.sort {
                            it.seqPlatformGroupId
                        },
                ]
            }
        }.sort {
            [
                    it.project,
                    it.individual,
                    it.sampleType,
                    it.seqType,
            ]
        }
    }

    /**
     * check that the SeqTracks of a Sample seqType combination have the same libraryPreparationKit in case it is part of the MergingCriteria
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<Map<String, Object>> createWarningsForSamplesHavingMultipleLibPrepKits(Collection<SeqTrack> seqTracks) {
        return seqTracks.groupBy {
            [
                    it.project,
                    it.seqType,
            ]
        }.findAll {
            MergingCriteria mergingCriteria = mergingCriteriaService.findMergingCriteria(it.key[0], it.key[1])
            return mergingCriteria ? mergingCriteria.useLibPrepKit : !((SeqType) it.key[1]).isWgbs()
        }.collectMany {
            SeqType seqType = it.key[1]
            it.value.groupBy([
                    { SeqTrack seqTrack ->
                        seqTrack.sample
                    },
                    { SeqTrack seqTrack ->
                        seqTrack.libraryPreparationKit
                    },
            ]).findAll { Sample sample, Map<LibraryPreparationKit, List<SeqTrack>> list ->
                list.size() > 1
            }.collect { Sample sample, Map<LibraryPreparationKit, List<SeqTrack>> list ->
                [
                        project                   : sample.individual.project.name,
                        individual                : sample.individual.pid,
                        seqType                   : seqType.displayNameWithLibraryLayout,
                        sampleType                : sample.sampleType.name,
                        libraryPreparationKitTable: list.collect { LibraryPreparationKit libraryPreparationKit, List<SeqTrack> seqTrackList ->
                            [
                                    libraryPreparationKit: libraryPreparationKit?.name ?: '-',
                                    count                : seqTrackList.size(),
                            ]
                        }.sort {
                            it.libraryPreparationKit
                        },
                ]
            }
        }.sort {
            [
                    it.project,
                    it.individual,
                    it.sampleType,
                    it.seqType,
            ]
        }
    }

    /**
     * check that for all project seqType speciesWithStrain combination of the seqTracks an ReferenceGenome is configured.
     * Note: this is only checked for the new workflow system
     */
    @SuppressWarnings("DuplicateNumberLiteral")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<Map<String, String>> createWarningsForMissingReferenceGenomeConfiguration(Collection<SeqTrack> seqTracks) {
        Collection<SeqTrack> seqTracksNew = allDecider.findAllSeqTracksInNewWorkflowSystem(seqTracks)

        return seqTracksNew.countBy {
            [
                    it.project,
                    it.seqType,
                    ([it.individual.species] + it.sample.mixedInSpecies) as Set,
            ]
        }.findAll { entry ->
            !ReferenceGenomeSelector.findAllByProjectAndSeqType(entry.key[0], entry.key[1]).findAll {
                it.species == entry.key[2]
            }
        }.collect {
            [
                    project: ((Project) it.key[0]).name,
                    seqType: ((SeqType) it.key[1]).displayNameWithLibraryLayout,
                    species: (it.key[2] as List)*.toString().sort().join(', '),
                    count  : it.value as String,
            ]
        }.sort {
            [
                    it.project,
                    it.seqType,
                    it.species,
            ]
        }
    }

    /**
     * check for withdrawn seqTracks.
     */
    @SuppressWarnings("DuplicateNumberLiteral")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<Map<String, String>> createWarningsForWithdrawnSeqTracks(Collection<SeqTrack> seqTracks) {
        List<SeqTrack> seqTracksFiltered = DataFile.withCriteria {
            'in'('seqTrack', seqTracks)
            eq('fileWithdrawn', true)
            projections {
                groupProperty('seqTrack')
            }
        }
        return seqTracksFiltered.countBy { SeqTrack seqTrack ->
            [
                    seqTrack.project,
                    seqTrack.individual,
                    seqTrack.seqType,
                    seqTrack.sampleType,
            ]
        }.collect {
            [
                    project   : ((Project) it.key[0]).name,
                    individual: ((Individual) it.key[1]).pid,
                    seqType   : ((SeqType) it.key[2]).displayNameWithLibraryLayout,
                    sampleType: ((SampleType) it.key[3]).name,
                    count     : it.value as String,
            ]
        }.sort {
            [
                    it.project,
                    it.individual,
                    it.seqType,
                    it.sampleType,
            ]
        }
    }

    private boolean isAlignmentWorkflow(Workflow workflow) {
        return workflow.name == PanCancerWorkflow.WORKFLOW
    }
}
