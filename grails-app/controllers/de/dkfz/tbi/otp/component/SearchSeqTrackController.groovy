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
package de.dkfz.tbi.otp.component

import grails.converters.JSON
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.SearchSeqTrackService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

/**
 * Action handler to search seqTrack by supplying either
 * 1. project and a list of seqType IDs
 * 2. List of PIDs and a list of seqType IDs
 * 3. List of Lane IDs
 * 4. List of ILSe numbers
 * The URL parameters from client should conform the following protocol:
 * project=ProjectAbc&type=project-tab&seqType[]=35&seqType[]=45
 * type=pids-tab&pids[]=ABCD-SXYZ&pids[]=BCDE-UVST&seqType[]=35&seqType[]=45
 * type=lanes-tab&lanes[]=6&lanes[]=7
 * type=ilseNumbers-tab&ilseNumbers[]=12345&ilseNumbers[]=23456
 *
 * @return a list of seqTracks in JSON format
 */
@PreAuthorize("hasRole('ROLE_OPERATOR')")
class SearchSeqTrackController {

    ProjectSelectionService projectSelectionService
    SearchSeqTrackService searchSeqTrackService

    static final String PARAM_KEY_PIDS = 'pids[]'
    static final String PARAM_KEY_SEQTYPES = 'seqTypes[]'
    static final String PARAM_KEY_ILSE_NUMBERS = 'ilseNumbers[]'
    static final String PARAM_KEY_LANES = 'lanes[]'

    static allowedMethods = [
            searchSeqTrackByProjectSeqType: "GET",
            searchSeqTrackByPidSeqType    : "GET",
            searchSeqTrackByLaneId        : "GET",
            searchSeqTrackByIlseNumber    : "GET",
    ]

    JSON searchSeqTrackByProjectSeqType() {
        Project project = projectSelectionService.selectedProject

        Set<Long> seqTypeIds = (params[PARAM_KEY_SEQTYPES].getClass().isArray() ? params[PARAM_KEY_SEQTYPES] :
                [params[PARAM_KEY_SEQTYPES]]).collect { it as long }

        Set<SeqType> seqTypes = SeqType.getAll(seqTypeIds)

        Set<SeqTrack> seqTracks = searchSeqTrackService.getAllSeqTracksByProjectAndSeqTypes(project, seqTypes)

        return redirectHelper(params, seqTracks, null)
    }

    JSON searchSeqTrackByPidSeqType() {
        Set<String> pids = params[PARAM_KEY_PIDS].getClass().isArray() ? params[PARAM_KEY_PIDS] : [params[PARAM_KEY_PIDS]]

        Set<Individual> individuals = Individual.findAll {
            pid in pids
        }

        if (!individuals) {
            response.status = HttpStatus.NOT_FOUND.value()
            return render([error: HttpStatus.NOT_FOUND.reasonPhrase, message: g.message(code: "triggerAlignment.error.noIndividuals") as String] as JSON)
        }

        Set<Long> seqTypeIds = (params[PARAM_KEY_SEQTYPES].getClass().isArray() ? params[PARAM_KEY_SEQTYPES] :
                [params[PARAM_KEY_SEQTYPES]]).collect { it as long }

        Set<SeqType> seqTypes = SeqType.getAll(seqTypeIds)

        Set<SeqTrack> seqTracks = searchSeqTrackService.getAllSeqTracksByIndividualsAndSeqTypes(individuals, seqTypes)

        Set<String> missingItems = (pids as Set) - individuals*.pid
        return redirectHelper(params, seqTracks, missingItems ? "Could not found: ${missingItems.join(' ,')}" : null)
    }

    JSON searchSeqTrackByLaneId() {
        Set<Long> laneIds
        try {
            laneIds = (params[PARAM_KEY_LANES].getClass().isArray() ? params[PARAM_KEY_LANES] :
                    [params[PARAM_KEY_LANES]]).collect { it as long }
        } catch (NumberFormatException ex) {
            response.status = HttpStatus.BAD_REQUEST.value()
            return render([error: HttpStatus.BAD_REQUEST.reasonPhrase, message: ex.message] as JSON)
        }

        if (!laneIds) {
            response.status = HttpStatus.NOT_FOUND.value()
            return render([error: HttpStatus.NOT_FOUND.reasonPhrase, message: g.message(code: "triggerAlignment.error.noLanes") as String] as JSON)
        }

        Set<SeqTrack> seqTracks = SeqTrack.getAll(laneIds)

        Set<String> missingItems = laneIds - seqTracks*.id
        return redirectHelper(params, seqTracks, missingItems ? "Could not found Lane: ${missingItems.join(' ,')}" : null)
    }

    JSON searchSeqTrackByIlseNumber() {
        Set<Long> ilseNumbers
        try {
            ilseNumbers = (params[PARAM_KEY_ILSE_NUMBERS].getClass().isArray() ? params[PARAM_KEY_ILSE_NUMBERS] :
                    [params[PARAM_KEY_ILSE_NUMBERS]]).collect { it as int }
        } catch (NumberFormatException ex) {
            response.status = HttpStatus.BAD_REQUEST.value()
            return render([error: HttpStatus.BAD_REQUEST.reasonPhrase, message: ex.message] as JSON)
        }

        Set<IlseSubmission> ilseSubmissions = IlseSubmission.findAll {
            ilseNumber in ilseNumbers
        }

        if (!ilseSubmissions) {
            response.status = HttpStatus.NOT_FOUND.value()
            return render([error: HttpStatus.NOT_FOUND.reasonPhrase, message: g.message(code: "triggerAlignment.error.noIlseSubmissions") as String] as JSON)
        }

        Set<SeqTrack> seqTracks = searchSeqTrackService.getAllSeqTracksByIlseSubmissions(ilseSubmissions)

        Set<Long> missingItems = ilseNumbers - ilseSubmissions*.ilseNumber
        return redirectHelper(params, seqTracks, missingItems ? "Could not found: ${missingItems.join(' ,')}" : null)
    }

    private def redirectHelper(Map params, Collection<SeqTrack> seqTracks, String message) {
        flash.seqTrackIds = seqTracks*.id
        flash.message = message

        redirect(
                controller: params['redirect[controller]'],
                action: params['redirect[action]'],
        )
    }
}
