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
package de.dkfz.tbi.otp.workflow.alignment

import grails.gorm.transactions.Transactional
import grails.web.mapping.LinkGenerator
import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.MergingCriteria.SpecificSeqPlatformGroups
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.mergingCriteria.DefaultSeqPlatformGroupController
import de.dkfz.tbi.otp.ngsdata.mergingCriteria.ProjectSeqPlatformGroupController
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.utils.LogUsedTimeUtils
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.withdraw.RoddyBamFileWithdrawService
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.decider.AllDecider
import de.dkfz.tbi.otp.workflowExecution.decider.DeciderResult

import static de.dkfz.tbi.otp.dataprocessing.MergingCriteria.SpecificSeqPlatformGroups.*

@Transactional(readOnly = true)
class TriggerAlignmentService {

    @Autowired
    LinkGenerator linkGenerator

    SeqTrackService seqTrackService
    SamplePairDeciderService samplePairDeciderService
    TicketService ticketService
    AllDecider allDecider
    RoddyBamFileWithdrawService roddyBamFileWithdrawService
    MergingCriteriaService mergingCriteriaService
    WorkflowService workflowService
    MessageSourceService messageSourceService

    @Transactional(readOnly = false)
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    TriggerAlignmentResult triggerAlignment(Collection<SeqTrack> seqTracks, boolean withdrawBamFiles = false, boolean ignoreSeqPlatformGroup = false) {
        // Mark the bam files as withdrawn
        if (withdrawBamFiles) {
            LogUsedTimeUtils.logUsedTimeStartEnd(log, "withdrawn existing bamFiles") {
                roddyBamFileWithdrawService.collectObjects(seqTracks as List<SeqTrack>).each { RoddyBamFile bamFile ->
                    bamFile.withdraw()
                    bamFile.save(flush: true)
                }
            }
        }

        // Modify the notification status
        ticketService.findAllTickets(seqTracks).each {
            ticketService.resetAlignmentAndAnalysisNotification(it)
        }

        // Start alignment workflows
        Collection<SeqTrack> alignableSeqTracks = LogUsedTimeUtils.logUsedTime(log, "search seqTracks") {
            allDecider.findAlignableSeqTracks(seqTracks)
        }
        DeciderResult deciderResult = allDecider.decide(alignableSeqTracks*.workflowArtefact, [
                ignoreSeqPlatformGroup: ignoreSeqPlatformGroup.toString()
        ])
        Collection<MergingWorkPackage> mergingWorkPackages = deciderResult.newArtefacts.findAll {
            it.artefactType == ArtefactType.BAM
        }*.artefact*.get()*.workPackage

        if (mergingWorkPackages) {
            LogUsedTimeUtils.logUsedTimeStartEnd(log, "create analyses") {
                samplePairDeciderService.findOrCreateSamplePairs(mergingWorkPackages)
            }
        }

        log.debug(deciderResult.toString())

        return new TriggerAlignmentResult(deciderResult, mergingWorkPackages)
    }

    /**
     * Count the given seqTracks that do not have the alignment workflow configured (deprecated)
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    List<Map<String, String>> createWarningsForMissingAlignmentConfig(Collection<SeqTrack> seqTracks) {
        Collection<SeqTrack> alignableSeqTracks = allDecider.findAlignableSeqTracks(seqTracks)

        List<Map<String, String>> entries = alignableSeqTracks.countBy {
            [
                    it.project,
                    it.seqType,
            ]
        }.findAll {
            !WorkflowVersionSelector.findAllByProjectAndSeqTypeAndDeprecationDateIsNull(it.key[0], it.key[1]).findAll {
                workflowService.isAlignment(it.workflowVersion.workflow)
            }
        }.collect {
            [
                    project: ((Project) it.key[0]).name,
                    seqType: ((SeqType) it.key[1]).displayNameWithLibraryLayout,
                    count  : it.value as String,
            ]
        }

        return entries.sort {
            [
                    it.project,
                    it.seqType,
            ]
        }
    }

    /**
     * C seqTracks that do not have the alignment workflow configured (deprecated)
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    List<Map<String, Object>> createWarningsForMissingSeqPlatformGroup(Collection<SeqTrack> seqTracks) {
        List<SeqPlatformGroup> defaultSeqPlatformGroups = mergingCriteriaService.findDefaultSeqPlatformGroupsOperator()
        List<SeqPlatform> configuredSeqPlatformsByDefault = defaultSeqPlatformGroups ? defaultSeqPlatformGroups.collectMany { it.seqPlatforms } : []

        return (((seqTracks.groupBy {
            [
                    it.project,
                    it.seqType,
            ]
        }.collectEntries {
            [(mergingCriteriaService.findMergingCriteria(it.key[0], it.key[1])): it.value]
        } as Map<MergingCriteria, List<SeqTrack>>).findAll {
            it.key.useSeqPlatformGroup != IGNORE_FOR_MERGING
        }.collectEntries {
            if (it.key.useSeqPlatformGroup != USE_OTP_DEFAULT) {
                return it
            }
            List<SeqTrack> filteredSeqTracks = it.value.findAll { !configuredSeqPlatformsByDefault.contains(it.seqPlatform) }
            return [it.key, filteredSeqTracks]
        } as Map<MergingCriteria, List<SeqTrack>>).collectEntries { mergingCriteria, groupedSeqTracks ->
            if (mergingCriteria.useSeqPlatformGroup != USE_PROJECT_SEQ_TYPE_SPECIFIC) {
                return [mergingCriteria, groupedSeqTracks]
            }
            List<SeqPlatformGroup> containedSeqPlatformGroups = SeqPlatformGroup.findAllByMergingCriteria(mergingCriteria)*.seqPlatforms.flatten()
            List<SeqTrack> filteredSeqTracks = groupedSeqTracks.findAll { !containedSeqPlatformGroups.containsAll(it.seqPlatform) }
            return [mergingCriteria, filteredSeqTracks]
        } as Map<MergingCriteria, List<SeqTrack>>).collectMany {
            MergingCriteria criteria = it.key
            it.value.groupBy {
                it.sample
            }.collect { sample, seqTrackList ->
                Project project = criteria.project
                SeqType seqType = criteria.seqType
                [
                        project     : project.name,
                        individual  : sample.individual.displayName,
                        seqType     : seqType.displayNameWithLibraryLayout,
                        sampleType  : sample.sampleType.name,
                        seqPlatforms: seqTrackList*.seqPlatform*.fullName.unique().sort().join(', '),
                        link        : createSeqPlatformConfigPageLink(project, seqType, criteria.useSeqPlatformGroup),
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

    private Map<String, String> createSeqPlatformConfigPageLink(Project project, SeqType seqType, SpecificSeqPlatformGroups specificSeqPlatformGroups) {
        String controller = (specificSeqPlatformGroups == USE_PROJECT_SEQ_TYPE_SPECIFIC ? ProjectSeqPlatformGroupController.simpleName :
                DefaultSeqPlatformGroupController.simpleName) - 'Controller'
        Map<String, Long> params = specificSeqPlatformGroups == USE_PROJECT_SEQ_TYPE_SPECIFIC ? [project: project.id, seqType: seqType.id] : null
        String linkName = specificSeqPlatformGroups == USE_PROJECT_SEQ_TYPE_SPECIFIC ?
                messageSourceService.createMessage('triggerAlignment.warn.info.projectAndSeqTypeSpecificPlatformGroup') :
                messageSourceService.createMessage('triggerAlignment.warn.info.defaultSeqPlatformGroup')
        return [
                name: linkName,
                path: linkGenerator.link([
                        controller: controller,
                        action    : 'index',
                        absolute  : 'true',
                        params    : params,
                ]),
        ]
    }

    /**
     * check that the SeqTracks of a Sample seqType combination have compatible SeqPlatforms according the MergingCriteria
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    List<Map<String, Object>> createWarningsForSamplesHavingMultipleSeqPlatformGroups(Collection<SeqTrack> seqTracks) {
        return seqTracks.groupBy {
            [
                    it.project,
                    it.seqType,
            ]
        }.findAll {
            mergingCriteriaService.findMergingCriteria(it.key[0], it.key[1])?.useSeqPlatformGroup !=
                    IGNORE_FOR_MERGING
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
    @CompileDynamic
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

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<Map<String, Object>> createWarningsForMissingLibPrepKits(Collection<SeqTrack> seqTracks) {
        Set<SeqType> seqTypes = SeqTypeService.seqTypesRequiredLibPrepKit as Set
        return seqTracks.findAll {
            !it.libraryPreparationKit && seqTypes.contains(it.seqType)
        }.collect {
            [
                    project   : it.individual.project.name,
                    individual: it.individual.pid,
                    seqType   : it.seqType.displayNameWithLibraryLayout,
                    sampleType: it.sampleType.name,
                    lane      : it.laneId,
                    run       : it.run.name,
            ]
        }.sort {
            [
                    it.project,
                    it.individual,
                    it.sampleType,
                    it.seqType,
                    it.lane,
                    it.run,
            ]
        }
    }

    /**
     * check that for all project seqType speciesWithStrain combination of the seqTracks an ReferenceGenome is configured.
     */
    @SuppressWarnings("DuplicateNumberLiteral")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    List<Map<String, String>> createWarningsForMissingReferenceGenomeConfiguration(Collection<SeqTrack> seqTracks) {
        Collection<SeqTrack> alignableSeqTracks = allDecider.findAlignableSeqTracks(seqTracks)

        return alignableSeqTracks.countBy {
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
    @CompileDynamic
    List<Map<String, String>> createWarningsForWithdrawnSeqTracks(Collection<SeqTrack> seqTracks) {
        List<SeqTrack> seqTracksFiltered = RawSequenceFile.withCriteria {
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
}
