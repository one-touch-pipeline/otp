/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata

import grails.plugin.springsecurity.annotation.Secured

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.ProcessParameterService

@Secured('isFullyAuthenticated()')
class SeqTrackController {

    SeqTrackService seqTrackService
    ProcessParameterService processParameterService

    def show() {
        params.id = params.id ?: "0"
        SeqTrack seqTrack = seqTrackService.getSeqTrack(params.id)
        if (!seqTrack) {
            response.sendError(404)
            return
        }
        List<JobExecutionPlan> jobExecutionPlans = processParameterService.getAllJobExecutionPlansBySeqTrackAndClass(seqTrack.id as String, SeqTrack.class.name)

        return [
            seqTrack: seqTrack,
            jobExecutionPlans: jobExecutionPlans,
        ]
    }

    def seqTrackSet(SeqTrackSelectionCommand cmd) {
        List<SeqTrack> seqTracks = SeqTrack.createCriteria().list {
            sample {
                eq("individual", cmd.individual)
                eq("sampleType", cmd.sampleType)
            }
            eq("seqType", cmd.seqType)
        }

        return [
                seqTrackSet: new SeqTrackSet(seqTracks),
                lanesPerRun: seqTracks.groupBy { it.run },
                individual : cmd.individual,
                sampleType : cmd.sampleType,
                seqType    : cmd.seqType,
        ]
    }
}

class SeqTrackSelectionCommand {
    Individual individual
    SeqType seqType
    SampleType sampleType
}
