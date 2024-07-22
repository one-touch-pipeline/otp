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
package de.dkfz.tbi.otp

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.TriggerWorkflowService

@Transactional
@PreAuthorize("hasRole('ROLE_OPERATOR')")
class SearchSeqTrackService {

    TriggerWorkflowService triggerWorkflowService

    @CompileDynamic
    Set<SeqTrack> getAllSeqTracksByProjectAndSeqTypes(Project project, Set<SeqType> seqTypes) {
        if (!(project && seqTypes)) {
            throw new IllegalArgumentException("Invalid inputs: project and seqTypes must be specified")
        }

        return SeqTrack.withCriteria {
            sample {
                individual {
                    eq('project', project)
                }
            }
            'in'("seqType", seqTypes)
        }
    }

    @CompileDynamic
    Set<SeqTrack> getAllSeqTracksByIndividualsAndSeqTypes(Set<Individual> individuals, Set<SeqType> seqTypes) {
        if (!(individuals && seqTypes)) {
            throw new IllegalArgumentException("Invalid inputs: Pids and seqTypes must be specified")
        }

        return SeqTrack.withCriteria {
            sample {
                'in'('individual', individuals)
            }
            'in'("seqType", seqTypes)
        }
    }

    @CompileDynamic
    Set<SeqTrack> getAllSeqTracksByIlseSubmissions(Set<IlseSubmission> ilseSubmissions) {
        if (!ilseSubmissions) {
            throw new IllegalArgumentException("Invalid inputs: Ilse submissions must be specified")
        }

        return SeqTrack.withCriteria {
            'in'("ilseSubmission", ilseSubmissions)
        }
    }

    @CompileDynamic
    Set<SeqTrack> getAllSeqTracksByLaneIds(Set<Long> laneIds) {
        if (!laneIds) {
            throw new IllegalArgumentException("Invalid inputs: Lane Ids must be specified")
        }

        return SeqTrack.withCriteria {
            'in'('id', laneIds)
        }
    }

    Map<String, Object> projectSeqTrack(SeqTrack seqTrack) {
        return [
                id              : seqTrack.id,
                project         : seqTrack.project.displayName,
                individual      : seqTrack.individual.displayName,
                sampleType      : seqTrack.sampleType.displayName,
                seqType         : seqTrack.seqType?.toString(),
                lane            : seqTrack.laneId,
                run             : seqTrack.runId,
                ilseId          : seqTrack.ilseId,
                withdrawn       : seqTrack.withdrawn,
                libPrepkit      : seqTrack.libraryPreparationKit ? seqTrack.libraryPreparationKit.name : '',
                seqPlatform     : seqTrack.seqPlatform?.fullName,
                seqPlatformGroup: seqTrack.seqPlatformGroup?.toString(),
                species         : seqTrack.individual.species.displayName,
                mixedInSpecies  : seqTrack.sample.mixedInSpecies*.displayName.join(', '),
                bamIds          : triggerWorkflowService.getBamFiles([seqTrack.id])*.id.join(', '),
        ]
    }
}
