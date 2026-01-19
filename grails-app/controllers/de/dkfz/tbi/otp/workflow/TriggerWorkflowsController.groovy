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

import grails.converters.JSON
import org.grails.web.json.JSONObject
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.SearchSeqTrackService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.workflow.alignment.TriggerWorkflowsResult
import de.dkfz.tbi.otp.workflowExecution.decider.*

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class TriggerWorkflowsController {

    private static final String PARAM_KEY_SEQ_TRACKS = 'seqTracks[]'
    private static final String PARAM_KEY_BAM_FILES = 'bamFiles[]'
    private static final String PARAM_KEY_IGNORE_SEQ_GROUP = 'ignoreSeqPlatformGroup'
    private static final String PARAM_KEY_DECIDER_ACTIONS = 'deciderActions[]'

    private static final String MEESAGE_CODE_OPTION_NOTE = 'triggerWorkflows.option.decider.notes'

    TriggerWorkflowsService triggerWorkflowsService
    SearchSeqTrackService searchSeqTrackService
    SeqTypeService seqTypeService
    AllDecider allDecider

    static allowedMethods = [
            index           : "GET",
            generateWarnings: "GET",
            triggerWorkflows: "POST",
    ]

    private final static Map EMPTY_WARNINGS = [
            missingConfigs        : [].asImmutable(),
            seqPlatformGroups     : [].asImmutable(),
            libraryPreparationKits: [].asImmutable(),
            referenceGenomes      : [].asImmutable(),
    ].asImmutable()

    def index() {
        List<DeciderWithActions> deciders = triggerWorkflowsService.allDecidersWithActions

        return [
                seqTypes      : seqTypeService.list().sort {
                    it.displayNameWithLibraryLayout
                },
                warnings      : EMPTY_WARNINGS,
                deciders      : deciders,
                deciderActions: DeciderCreateWorkflowAction.values(),
                deciderNotes  : [
                        General  : g.message(code: "${MEESAGE_CODE_OPTION_NOTE}.general") as String,
                        Fastqc   : g.message(code: "${MEESAGE_CODE_OPTION_NOTE}.fastqc") as String,
                        Alignment: g.message(code: "${MEESAGE_CODE_OPTION_NOTE}.alignment") as String,
                        Analysis : g.message(code: "${MEESAGE_CODE_OPTION_NOTE}.analysis") as String,
                ],
        ]
    }

    /**
     * Generate Warnings in preparation for triggering workflows
     */
    JSON generateWarnings() {
        List<SeqTrack> seqTracks = SeqTrack.getAll(flash.seqTrackIds)
        List<ExternallyProcessedBamFile> extBamFiles = ExternallyProcessedBamFile.getAll(flash.extBamFileIds)
        Set<String> message = flash.message as Set<String>

        if (!seqTracks && !extBamFiles || !seqTracks.size() && !extBamFiles.size()) {
            return render([
                    data    : [],
                    warnings: EMPTY_WARNINGS,
                    message : message,
            ] as JSON)
        }

        List<Map<String, String>> warningsForMissingLibPrepKits = triggerWorkflowsService.createWarningsForMissingLibPrepKits(seqTracks)
        List<Map<String, String>> warningsForWithdrawnSeqTracks = triggerWorkflowsService.createWarningsForWithdrawnSeqTracks(seqTracks)
        List<Map<String, String>> warningsForMissingWorkflowConfig = triggerWorkflowsService.createWarningsForMissingWorkflowConfig(seqTracks)
        List<Map<String, String>> warningsForMissingSeqPlatformGroups = triggerWorkflowsService.createWarningsForMissingSeqPlatformGroup(seqTracks)
        List<Map<String, String>> warningsForMissingReferenceGenomeConfiguration =
                triggerWorkflowsService.createWarningsForMissingReferenceGenomeConfiguration(seqTracks)
        List<Map<String, String>> warningsForSamplesHavingMultipleSeqPlatformGroups =
                triggerWorkflowsService.createWarningsForSamplesHavingMultipleSeqPlatformGroups(seqTracks)
        List<Map<String, String>> warningsForSamplesHavingMultipleLibPrepKits =
                triggerWorkflowsService.createWarningsForSamplesHavingMultipleLibPrepKits(seqTracks)
        List<Map<String, String>> warningsForMissingSampleTypePerProject =
                triggerWorkflowsService.createWarningsForMissingSampleTypePerProject(seqTracks)

        return render([
                data    : seqTracks.collect { SeqTrack seqTrack ->
                    searchSeqTrackService.projectSeqTrack(seqTrack)
                },
                bamData : (triggerWorkflowsService.getBamFiles(seqTracks*.id) + extBamFiles).collect { AbstractBamFile bamFile ->
                    getBamValue(bamFile)
                },
                info    : [
                        workflows: triggerWorkflowsService.getInfo(seqTracks).collect {
                            getWorkflowsValue(it)
                        }
                ],
                warnings: [
                        withdrawnSeqTracks         : warningsForWithdrawnSeqTracks,
                        missingWorkflowConfigs     : warningsForMissingWorkflowConfig,
                        missingLibPrepKits         : warningsForMissingLibPrepKits,
                        missingReferenceGenomes    : warningsForMissingReferenceGenomeConfiguration,
                        missingSeqPlatformGroups   : warningsForMissingSeqPlatformGroups,
                        seqPlatformGroups          : warningsForSamplesHavingMultipleSeqPlatformGroups,
                        libraryPreparationKits     : warningsForSamplesHavingMultipleLibPrepKits,
                        missingSampleTypePerProject: warningsForMissingSampleTypePerProject,
                ],
                message : message,
        ] as JSON)
    }

    /**
     * Trigger the alignment workflow
     */
    JSON triggerWorkflows() {
        Set<Long> seqTracksIds = getIdsFromParams(PARAM_KEY_SEQ_TRACKS)
        Set<Long> bamFilesIds = getIdsFromParams(PARAM_KEY_BAM_FILES)

        Set<SeqTrack> seqTracks = SeqTrack.getAll(seqTracksIds)
        Set<AbstractBamFile> bamFiles = AbstractBamFile.getAll(bamFilesIds)

        boolean ignoreSeqPlatformGroup = Boolean.parseBoolean(params[PARAM_KEY_IGNORE_SEQ_GROUP])

        Map<Class<? extends Decider>, DeciderCreateWorkflowAction> deciderAction = [:]
        if (params[PARAM_KEY_DECIDER_ACTIONS].getClass().isArray()) {
            params[PARAM_KEY_DECIDER_ACTIONS].each { String deciderActionJsonString ->
                JSONObject deciderActionJsonObject = JSON.parse(deciderActionJsonString)
                String deciderName = deciderActionJsonObject.name
                String actionId = deciderActionJsonObject.createAction
                try {
                    Class<? extends Decider> deciderClass = allDecider.getDeciderClassByName(deciderName)
                    if (!deciderClass) {
                        throw new IllegalArgumentException("Unknown decider: ${deciderName}")
                    }
                    DeciderCreateWorkflowAction action = DeciderCreateWorkflowAction.getById(Integer.valueOf(actionId))
                    if (!action) {
                        throw new IllegalArgumentException("Unknown action ID: ${actionId}")
                    }
                    log.debug("Decider and its action: ${deciderName}: ${action}")
                    deciderAction.put(deciderClass, action)
                } catch (ClassNotFoundException | IllegalArgumentException e) {
                    log.warn("Invalid decider action: ${deciderName}: ${actionId}", e)
                    return render(g.message(code: "triggerWorkflows.warn.deciderAction.invalid"))
                }
            }
        }

        TriggerWorkflowsResult triggerAlignmentResult = triggerWorkflowsService.triggerWorkflow(seqTracks, bamFiles, ignoreSeqPlatformGroup, deciderAction)

        return render([
                success        : !triggerAlignmentResult.mergingWorkPackages.empty,
                infos          : triggerAlignmentResult.infos,
                warnings       : triggerAlignmentResult.warnings,
                newWorkPackages: triggerAlignmentResult.mergingWorkPackages*.toString(),
        ] as JSON)
    }

    private Map<String, Object> getBamValue(AbstractBamFile bamFile) {
        return [
                id              : bamFile.id,
                project         : bamFile.project.displayName,
                individual      : bamFile.individual.displayName,
                sampleType      : bamFile.sampleType.displayName,
                withdrawn       : bamFile.withdrawn,
                libPrepKit      : bamFile.workPackage.libraryPreparationKit?.name ?: '',
                species         : bamFile.individual.species.displayName,
                mixedInSpecies  : bamFile.sample.mixedInSpecies*.displayName.join(', '),
                referenceGenome : bamFile.referenceGenome.name,
                seqPlatformGroup: (bamFile.workPackage instanceof MergingWorkPackage) ?
                        (bamFile.workPackage as MergingWorkPackage).seqPlatformGroup?.toString() : '',
        ]
    }

    private Map<String, Object> getWorkflowsValue(WorkflowVersionAndReferenceGenomeSelector selector) {
        return [
                project        : selector.workflowVersionSelector.project.displayName,
                seqType        : selector.workflowVersionSelector.seqType.displayName,
                workflow       : selector.workflowVersionSelector.workflowVersion.workflow.displayName,
                version        : selector.workflowVersionSelector.workflowVersion.workflowVersion,
                referenceGenome: selector.referenceGenomeSelectors.collect {
                    [
                            species        : it.species*.displayName.join(', '),
                            referenceGenome: it.referenceGenome.name,
                    ]
                },
        ]
    }

    private Set<Long> getIdsFromParams(String key) {
        return params[key] ? (params[key].getClass().isArray() ? params[key] :
                [params[key]]).collect { it as long } : []
    }
}
