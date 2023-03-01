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
package de.dkfz.tbi.otp.ngsdata.mergingCriteria

import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.MergingCriteriaService
import de.dkfz.tbi.otp.ngsdata.SeqPlatform
import de.dkfz.tbi.otp.ngsdata.SeqPlatformGroup

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class DefaultSeqPlatformGroupController {

    MergingCriteriaService mergingCriteriaService

    static allowedMethods = [
            index                                : "GET",
            removePlatformFromSeqPlatformGroup   : "POST",
            addPlatformToExistingSeqPlatformGroup: "POST",
            emptySeqPlatformGroup                : "POST",
            createNewGroupAndAddPlatform         : "POST",
    ]

    def index() {
        List<SeqPlatformGroup> seqPlatformGroups = mergingCriteriaService.findDefaultSeqPlatformGroupsOperator()
        List<SeqPlatform> allSeqPlatformsWithoutGroup = SeqPlatform.all.sort {
            it.toString()
        } - seqPlatformGroups*.seqPlatforms?.flatten()

        [
                seqPlatformGroups          : seqPlatformGroups,
                allSeqPlatformsWithoutGroup: allSeqPlatformsWithoutGroup,
        ]
    }

    def removePlatformFromSeqPlatformGroup(SeqPlatformGroup seqPlatformGroup, SeqPlatform seqPlatform) {
        mergingCriteriaService.removePlatformFromSeqPlatformGroup(seqPlatformGroup, seqPlatform)
        redirect(action: "index")
    }

    def addPlatformToExistingSeqPlatformGroup(SeqPlatformGroup seqPlatformGroup, SeqPlatform seqPlatform) {
        mergingCriteriaService.addPlatformToExistingSeqPlatformGroup(seqPlatformGroup, seqPlatform)
        redirect(action: "index")
    }

    def emptySeqPlatformGroup(SeqPlatformGroup seqPlatformGroup) {
        mergingCriteriaService.emptySeqPlatformGroup(seqPlatformGroup)
        redirect(action: "index")
    }

    def createNewGroupAndAddPlatform(SeqPlatform seqPlatform) {
        mergingCriteriaService.createNewGroupAndAddPlatform(seqPlatform)
        redirect(action: "index")
    }
}
