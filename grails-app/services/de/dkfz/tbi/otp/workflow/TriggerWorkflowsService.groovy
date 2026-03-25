/*
 * Copyright 2011-2026 The OTP authors
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

import grails.gorm.transactions.Transactional
import grails.web.mapping.LinkGenerator
import groovy.transform.CompileDynamic
import groovy.transform.TupleConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.LibraryPreparationKit
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SeqPlatform
import de.dkfz.tbi.otp.ngsdata.SeqPlatformGroup
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTrackService
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.ngsdata.mergingCriteria.DefaultSeqPlatformGroupController
import de.dkfz.tbi.otp.ngsdata.mergingCriteria.ProjectSeqPlatformGroupController
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.utils.LogUsedTimeUtils
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.withdraw.RoddyBamFileWithdrawService
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.decider.AllDecider
import de.dkfz.tbi.otp.workflowExecution.decider.Decider
import de.dkfz.tbi.otp.workflowExecution.decider.DeciderCreateWorkflowAction
import de.dkfz.tbi.otp.workflowExecution.decider.DeciderResult

import static de.dkfz.tbi.otp.dataprocessing.MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING
import static de.dkfz.tbi.otp.dataprocessing.MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT
import static de.dkfz.tbi.otp.dataprocessing.MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC

@Transactional
class TriggerWorkflowsService {

    @Autowired
    LinkGenerator linkGenerator
    AbstractBamFileService abstractBamFileService
    AllDecider allDecider
    SeqTrackService seqTrackService
    SamplePairDeciderService samplePairDeciderService
    TicketService ticketService
    RoddyBamFileWithdrawService roddyBamFileWithdrawService
    MergingCriteriaService mergingCriteriaService
    WorkflowService workflowService
    MessageSourceService messageSourceService

    @CompileDynamic
    List<AbstractBamFile> getBamFiles(List<Long> seqTrackIds) {
        if (!seqTrackIds) {
            return []
        }

        return RoddyBamFile.createCriteria().listDistinct {
            seqTracks {
                'in'('id', seqTrackIds)
            }
            eq('withdrawn', false)
        } as List<RoddyBamFile>
    }

    @CompileDynamic
    List<SeqTrack> getSeqTracks(Collection<Long> bamFileIds) {
        if (!bamFileIds) {
            return []
        }

        return AbstractBamFile.executeQuery('''
            SELECT DISTINCT bf.seqTracks
            FROM AbstractBamFile bf
            WHERE bf.id IN (:bamFileIds)
        ''', [bamFileIds: bamFileIds]) as List<SeqTrack>
    }

    @CompileDynamic
    List<ExternallyProcessedBamFile> getExternalBamFiles(Collection<Long> bamFileIds) {
        if (!bamFileIds) {
            return []
        }

        return ExternallyProcessedBamFile.executeQuery('''
            SELECT DISTINCT bf
            FROM ExternallyProcessedBamFile bf
            WHERE bf.id IN (:bamFileIds)
        ''', [bamFileIds: bamFileIds]) as List<ExternallyProcessedBamFile>
    }

    @CompileDynamic
    List<WorkflowVersionAndReferenceGenomeSelector> getInfo(Collection<SeqTrack> seqTracks) {
        if (!seqTracks) {
            return []
        }

        List<WorkflowVersionSelector> wvs = WorkflowVersionSelector.createCriteria().listDistinct {
            or {
                seqTracks.each { st ->
                    and {
                        eq('project', st.project)
                        eq('seqType', st.seqType)
                    }
                }
            }
            isNull('deprecationDate')
        } as List<WorkflowVersionSelector>
        return wvs.collect {
            new WorkflowVersionAndReferenceGenomeSelector(it,
                    ReferenceGenomeSelector.findAllByProjectAndSeqTypeAndWorkflow(it.project, it.seqType, it.workflowVersion.workflow))
        }
    }

    void triggerWorkflowByProjectAndSampleTypes(Project project, Set<SampleType> sampleTypes) {
        List<WorkflowArtefact> artefacts = abstractBamFileService.findAllByProjectAndSampleType(project, sampleTypes)*.workflowArtefact
        allDecider.decide(artefacts)
    }

    List<DeciderWithActions> getAllDecidersWithActions() {
        return allDecider.allDeciderActionsMap.collect { k, v ->
            return new DeciderWithActions(k.simpleName, v)
        }
    }

    /**
     * HQL query to find SeqTracks that are missing the configuration for the given workflows.
     * It returns the workflow name, project name, seqType name, and the count of SeqTracks for each combination.
     *
     * Note: The supported seqTypes are specified in the WorkflowVersion.
     */
    final static String HQL_WORKFLOWS_MISSING_CONFIG = """
        SELECT w.name AS workflow,
               p.name AS project,
               CONCAT(s.displayName, ' ', s.libraryLayout, ' ', CASE s.singleCell WHEN 'TRUE' THEN 'single cell' ELSE 'bulk' END) AS seqType,
               COUNT(DISTINCT st.id) AS count
        FROM SeqTrack st
               JOIN st.sample.individual.project p
               JOIN st.seqType s,
             WorkflowVersion wv
               JOIN wv.supportedSeqTypes supportedSeqTypes
               JOIN wv.apiVersion.workflow w
        WHERE w.name IN (:workflowNames)
            AND w.deprecatedDate IS NULL
            AND st IN (:seqTracks)
            AND s IN supportedSeqTypes
            AND NOT EXISTS (
                SELECT 1
                FROM WorkflowVersionSelector wvs
                WHERE wvs.project = p
                  AND wvs.seqType = s
                  AND wvs.workflowVersion.apiVersion.workflow = w
                  AND wvs.deprecationDate IS NULL
            )
        GROUP BY workflow, project, seqType
    """

    // The result indexes for the above HQL query
    private static final int IDX_WORKFLOW = 0
    private static final int IDX_PROJECT = 1
    private static final int IDX_SEQTYPE = 2
    private static final int IDX_COUNT = 3

    @Transactional
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    DeciderResult triggerWorkflow(Collection<SeqTrack> seqTrackList, Collection<AbstractBamFile> bamFiles,
                                  boolean ignoreSeqPlatformGroup = false, Map<Class<? extends Decider>, DeciderCreateWorkflowAction> deciderAction) {
        // Modify the notification status
        ticketService.findAllTickets(seqTrackList).each {
            ticketService.resetAlignmentAndAnalysisNotification(it)
        }

        // Start deciders for all workflows
        Collection<WorkflowArtefact> allArtefacts = LogUsedTimeUtils.logUsedTime(log, "search seqTracks") {
            allDecider.findAlignableSeqTracks(seqTrackList)*.workflowArtefact
        }

        if (bamFiles) {
            allArtefacts += LogUsedTimeUtils.logUsedTime(log, "collect bamFile seqTrack artefacts") {
                AbstractBamFile.createCriteria().list {
                    'in'('id', bamFiles*.id)
                    seqTracks {
                        isNotNull('workflowArtefact')
                        projections {
                            property('workflowArtefact')
                        }
                    }
                }
            }

            allArtefacts += LogUsedTimeUtils.logUsedTime(log, "collect bamFile artefacts") {
                bamFiles*.workflowArtefact
            }
        }

        DeciderResult deciderResult = allDecider.decide(allArtefacts, [
                ignoreSeqPlatformGroup: ignoreSeqPlatformGroup.toString()
        ], deciderAction)

        Collection<MergingWorkPackage> mergingWorkPackages = deciderResult.newArtefacts.findAll {
            it.artefactType == ArtefactType.BAM
        }*.artefact*.get()*.workPackage

        if (mergingWorkPackages) {
            LogUsedTimeUtils.logUsedTimeStartEnd(log, "create analyses") {
                samplePairDeciderService.findOrCreateSamplePairs(mergingWorkPackages)
            }
        }

        log.debug(deciderResult.toString())

        return deciderResult
    }

    /**
     * C seqTracks that do not have the alignment workflow configured (deprecated)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    List<Map<String, Object>> createWarningsForMissingSeqPlatformGroup(Collection<SeqTrack> seqTracks) {
        List<SeqPlatformGroup> defaultSeqPlatformGroups = mergingCriteriaService.findDefaultSeqPlatformGroupsOperator()
        List<SeqPlatform> configuredSeqPlatformsByDefault = defaultSeqPlatformGroups
                ? defaultSeqPlatformGroups.collectMany { it.seqPlatforms } : []

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

    private Map<String, String> createSeqPlatformConfigPageLink(Project project, SeqType seqType,
                                                                MergingCriteria.SpecificSeqPlatformGroups specificSeqPlatformGroups) {
        String controller = (
                specificSeqPlatformGroups == USE_PROJECT_SEQ_TYPE_SPECIFIC ? ProjectSeqPlatformGroupController.simpleName :
                DefaultSeqPlatformGroupController.simpleName
        ) - 'Controller'
        Map<String, Long> params = specificSeqPlatformGroups == USE_PROJECT_SEQ_TYPE_SPECIFIC ? [project: project.id, seqType: seqType.id] : null
        String linkName = specificSeqPlatformGroups == USE_PROJECT_SEQ_TYPE_SPECIFIC ?
                messageSourceService.createMessage('triggerWorkflows.warn.info.projectAndSeqTypeSpecificPlatformGroup') :
                messageSourceService.createMessage('triggerWorkflows.warn.info.defaultSeqPlatformGroup')
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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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

    /**
     * check for missing SampleTypePerProject.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    List<Map<String, String>> createWarningsForMissingSampleTypePerProject(Collection<SeqTrack> seqTracks) {
        return SeqTrack.findAll('''
            FROM SeqTrack st
            WHERE st in (:seqTracks)
            AND NOT EXISTS (
                FROM SampleTypePerProject spp
                WHERE spp.project = st.sample.individual.project
                AND spp.sampleType = st.sample.sampleType
            )
        ''', [
                seqTracks: seqTracks,
        ]).collect {
            return [
                    project   : it.project.name,
                    sampleType: it.sampleType.displayName,
            ]
        }.unique().sort {
            [
                    it.project,
                    it.sampleType,
            ]
        }
    }

    /**
     * Count the given seqTracks that do not have the alignment/analysis workflow configured
     *
     * It compares the combined keys of workflow, project, and seqType from the given seqTracks
     * with the keys in the WorkflowVersionSelector table and
     * returns seqTrack counts not found in the WorkflowVersionSelector,
     *
     * The constrains are the workflow names and the seqTrack ids.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    List<Map<String, String>> createWarningsForMissingWorkflowConfig(Collection<SeqTrack> seqTracks) {
        return SeqTrack.executeQuery(HQL_WORKFLOWS_MISSING_CONFIG, [
                seqTracks    : seqTracks,
                workflowNames: allDecider.enabledWorkflowNames,
        ]).collect {
            [
                    workflow: it[IDX_WORKFLOW],
                    project : it[IDX_PROJECT],
                    seqType : it[IDX_SEQTYPE],
                    count   : it[IDX_COUNT].toString(),
            ]
        } as List<Map<String, String>>
    }
}

@TupleConstructor
class WorkflowVersionAndReferenceGenomeSelector {
    WorkflowVersionSelector workflowVersionSelector
    List<ReferenceGenomeSelector> referenceGenomeSelectors
}

@TupleConstructor
class DeciderWithActions {
    String name
    List<DeciderCreateWorkflowAction> createActions
}
