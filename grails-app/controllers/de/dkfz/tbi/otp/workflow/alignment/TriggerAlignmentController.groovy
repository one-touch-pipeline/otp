/*
 * Copyright 2011-2023 The OTP authors
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

import grails.converters.JSON
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.SearchSeqTrackService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTypeService

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class TriggerAlignmentController {

    static final String PARAM_KEY_SEQ_TRACKS = 'seqTracks[]'
    static final String PARAM_KEY_IGNORE_SEQ_GROUP = 'ignoreSeqPlatformGroup'
    static final String PARAM_KEY_WITHDRAW_BAMFILES = 'withdrawBamFiles'

    TriggerAlignmentService triggerAlignmentService
    SearchSeqTrackService searchSeqTrackService
    SeqTypeService seqTypeService

    static allowedMethods = [
            index           : "GET",
            generateWarnings: "GET",
            triggerAlignment: "POST",
    ]

    final static Map EMPTY_WARNINGS = [
            missingConfigs        : [].asImmutable(),
            seqPlatformGroups     : [].asImmutable(),
            libraryPreparationKits: [].asImmutable(),
            referenceGenomes      : [].asImmutable(),
    ].asImmutable()

    def index() {
        return [
                seqTypes: seqTypeService.findAlignAbleSeqTypes(),
                warnings: EMPTY_WARNINGS,
        ]
    }

    /**
     * Generate Warnings in preparation for triggering workflows
     */
    JSON generateWarnings() {
        List<SeqTrack> seqTracks = SeqTrack.getAll(flash.seqTrackIds)
        Set<String> message = flash.message as Set<String>

        if (!seqTracks || !seqTracks.size()) {
            return render([
                    data    : [],
                    warnings: EMPTY_WARNINGS,
                    message : message,
            ] as JSON)
        }

        List<Map<String, String>> warningsForWithdrawnSeqTracks = triggerAlignmentService.createWarningsForWithdrawnSeqTracks(seqTracks)
        List<Map<String, String>> warningsForMissingAlignmentConfig = triggerAlignmentService.createWarningsForMissingAlignmentConfig(seqTracks)
        List<Map<String, String>> warningsForMissingReferenceGenomeConfiguration =
                triggerAlignmentService.createWarningsForMissingReferenceGenomeConfiguration(seqTracks)
        List<Map<String, String>> warningsForSamplesHavingMultipleSeqPlatformGroups =
                triggerAlignmentService.createWarningsForSamplesHavingMultipleSeqPlatformGroups(seqTracks)
        List<Map<String, String>> warningsForSamplesHavingMultipleLibPrepKits =
                triggerAlignmentService.createWarningsForSamplesHavingMultipleLibPrepKits(seqTracks)

        return render([
                data    : seqTracks.collect { SeqTrack seqTrack ->
                    searchSeqTrackService.projectSeqTrack(seqTrack)
                },
                warnings: [
                        withdrawnSeqTracks     : warningsForWithdrawnSeqTracks,
                        missingAlignmentConfigs: warningsForMissingAlignmentConfig,
                        missingReferenceGenomes: warningsForMissingReferenceGenomeConfiguration,
                        seqPlatformGroups      : warningsForSamplesHavingMultipleSeqPlatformGroups,
                        libraryPreparationKits : warningsForSamplesHavingMultipleLibPrepKits,
                ],
                message : message,
        ] as JSON)
    }

    /**
     * Trigger the alignment workflow
     */
    JSON triggerAlignment() {
        Set<Long> seqTracksIds = (params[PARAM_KEY_SEQ_TRACKS].getClass().isArray() ? params[PARAM_KEY_SEQ_TRACKS] :
                [params[PARAM_KEY_SEQ_TRACKS]]).collect { it as long }

        Set<SeqTrack> seqTracks = SeqTrack.findAll {
            id in seqTracksIds
        }

        boolean ignoreSeqPlatformGroup = Boolean.parseBoolean(params[PARAM_KEY_IGNORE_SEQ_GROUP])
        boolean withdrawBamFiles = Boolean.parseBoolean(params[PARAM_KEY_WITHDRAW_BAMFILES])

        TriggerAlignmentResult triggerAlignmentResult = triggerAlignmentService.triggerAlignment(seqTracks, withdrawBamFiles, ignoreSeqPlatformGroup)

        return render([
                success        : !triggerAlignmentResult.mergingWorkPackages.empty,
                infos          : triggerAlignmentResult.infos,
                warnings       : triggerAlignmentResult.warnings,
                newWorkPackages: triggerAlignmentResult.mergingWorkPackages*.toString(),
        ] as JSON)
    }
}
