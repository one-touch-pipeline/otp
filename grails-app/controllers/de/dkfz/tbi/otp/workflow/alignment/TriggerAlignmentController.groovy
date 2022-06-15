/*
 * Copyright 2011-2021 The OTP authors
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
import grails.plugin.springsecurity.annotation.Secured

import de.dkfz.tbi.otp.SearchSeqTrackService
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

@Secured("hasRole('ROLE_OPERATOR')")
class TriggerAlignmentController {

    TriggerAlignmentService triggerAlignmentService
    SearchSeqTrackService searchSeqTrackService

    static allowedMethods = [
            index           : "GET",
            triggerAlignment: "POST",
    ]

    def index() {
        return [
                seqTypes: SeqType.findAllByLegacy(false).sort {
                    SeqType a, SeqType b -> a.displayNameWithLibraryLayout <=> b.displayNameWithLibraryLayout
                },
                warnings: generateDummyWarnings(),
        ]
    }

    // TODO: This is only a dummy impl, which will be replaced by otp-1342
    @SuppressWarnings('AvoidFindWithoutAll')
    private Map generateDummyWarnings() {
        Sample dummySample = Sample.first()

        return [
                seqTypes              : [
                        [
                                seqType  : SeqType.first(sort: 'name'),
                                project  : Project.first(sort: 'name'),
                                laneCount: 3
                        ],
                        [
                                seqType  : SeqType.first(),
                                project  : Project.first(),
                                laneCount: 2
                        ],
                ],
                seqPlatformGroups     : [
                        [
                                project          : Project.first(sort: 'name'),
                                mockPid          : dummySample.individual.mockPid,
                                sampleTypeName   : dummySample.sampleType,
                                seqTypeName      : SeqType.first().name,
                                seqReadType      : "PAIRED",
                                singleCell       : "bulk",
                                seqPlatformGroups: [
                                        [
                                                id   : 12345,
                                                count: 2,
                                        ],
                                        [
                                                id   : 342532,
                                                count: 3,
                                        ]
                                ]
                        ]
                ],
                libraryPreparationKits: [
                        [
                                project        : Project.first(sort: 'name'),
                                mockPid        : dummySample.individual.mockPid,
                                sampleTypeName : dummySample.sampleType,
                                seqTypeName    : SeqType.first().name,
                                seqReadType    : "PAIRED",
                                singleCell     : "bulk",
                                libraryPrepKits: [
                                        [
                                                id   : 12345,
                                                count: 1,
                                        ],
                                        [
                                                id   : 342532,
                                                count: 5,
                                        ]
                                ]
                        ]
                ],
        ]
    }

    /**
     * Trigger the alignment workflow
     */
    JSON triggerAlignment() {
        Set<SeqTrack> seqTracks = SeqTrack.findAll {
            id in params['seqTracks[]'].collect { it as long }
        }.unique()

        boolean withdrawBamFiles = params['withdraw'].toBoolean()
        Collection<MergingWorkPackage> workPackages = triggerAlignmentService.triggerAlignment(seqTracks, withdrawBamFiles)

        return render(workPackages*.toString() as JSON)
    }
}
