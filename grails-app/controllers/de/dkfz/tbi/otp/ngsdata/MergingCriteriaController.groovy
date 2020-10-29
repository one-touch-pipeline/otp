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
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.dataprocessing.MergingCriteriaService
import de.dkfz.tbi.otp.project.Project

@Secured("hasRole('ROLE_OPERATOR')")
class MergingCriteriaController {

    MergingCriteriaService mergingCriteriaService
    ProjectSelectionService projectSelectionService

    @Secured('isFullyAuthenticated()')
    def projectAndSeqTypeSpecific(SeqType seqType) {
        Project project = projectSelectionService.selectedProject
        MergingCriteria mergingCriteria = mergingCriteriaService.findMergingCriteria(project, seqType)
        List<SeqPlatformGroup> seqPlatformGroups = mergingCriteriaService.findDefaultSeqPlatformGroups()
        List<SeqPlatformGroup> seqPlatformGroupsPerProjectAndSeqType = mergingCriteriaService.findSeqPlatformGroupsForProjectAndSeqType(
                project, seqType, false)
        List<SeqPlatform> allUsedSpecificSeqPlatforms = seqPlatformGroupsPerProjectAndSeqType*.seqPlatforms.flatten()
        List<SeqPlatform> allSeqPlatformsWithoutGroup = SeqPlatform.all.sort { it.toString() } - allUsedSpecificSeqPlatforms
        [
                mergingCriteria                      : mergingCriteria,
                seqType                              : seqType,
                seqPlatformGroups                    : seqPlatformGroups,
                seqPlatformGroupsPerProjectAndSeqType: seqPlatformGroupsPerProjectAndSeqType,
                allSeqPlatformsWithoutGroup          : allSeqPlatformsWithoutGroup,
                allUsedSpecificSeqPlatforms          : allUsedSpecificSeqPlatforms,
                dontAllowCopyingAll                  : seqPlatformGroups*.seqPlatforms.flatten().intersect(allUsedSpecificSeqPlatforms) as Boolean,
        ]
    }

    def defaultSeqPlatformGroupConfiguration() {
        List<SeqPlatformGroup> seqPlatformGroups = mergingCriteriaService.findDefaultSeqPlatformGroupsOperator()
        List<SeqPlatform> allSeqPlatformsWithoutGroup = SeqPlatform.all.sort {
            it.toString()
        } - seqPlatformGroups*.seqPlatforms.flatten()

        [
                seqPlatformGroups          : seqPlatformGroups,
                allSeqPlatformsWithoutGroup: allSeqPlatformsWithoutGroup,
        ]
    }

    def update(UpdateMergingCriteriaCommand cmd) {
        if (!cmd.validate()) {
            render status: 403
            return
        }

        Errors errors = mergingCriteriaService.createOrUpdateMergingCriteria(
                projectSelectionService.requestedProject, cmd.seqType, cmd.useLibPrepKit, cmd.useSeqPlatformGroup
        )
        if (errors) {
            flash.message = new FlashMessage("An error occurred", errors)
        } else {
            flash.message = new FlashMessage("Data stored successfully")
        }
        redirect(action: "projectAndSeqTypeSpecific", params: ["seqType.id": cmd.seqType.id])
    }

    private void redirectHelper(MergingCriteria mergingCriteria) {
        if (mergingCriteria) {
            redirect(action: "projectAndSeqTypeSpecific", params: ["seqType.id": mergingCriteria.seqType.id])
        } else {
            redirect(action: "defaultSeqPlatformGroupConfiguration")
        }
    }

    def removePlatformFromSeqPlatformGroup(SeqPlatformGroup group, SeqPlatform platform) {
        mergingCriteriaService.removePlatformFromSeqPlatformGroup(group, platform)
        redirectHelper(group.mergingCriteria)
    }

    def addPlatformToExistingSeqPlatformGroup(SeqPlatformGroup group, SeqPlatform platform) {
        mergingCriteriaService.addPlatformToExistingSeqPlatformGroup(group, platform)
        redirectHelper(group.mergingCriteria)
    }

    def createNewSpecificGroupAndAddPlatform(SeqPlatform platform, MergingCriteria mergingCriteria) {
        mergingCriteriaService.createNewGroupAndAddPlatform(platform, mergingCriteria)
        redirectHelper(mergingCriteria)
    }

    def createNewDefaultGroupAndAddPlatform(SeqPlatform platform) {
        mergingCriteriaService.createNewGroupAndAddPlatform(platform)
        redirectHelper(null)
    }

    def emptySeqPlatformGroup(SeqPlatformGroup group) {
        mergingCriteriaService.emptySeqPlatformGroup(group)
        redirectHelper(group.mergingCriteria)
    }

    def copyDefaultToSpecific(SeqPlatformGroup seqPlatformGroup, MergingCriteria mergingCriteria) {
        mergingCriteriaService.copyDefaultToSpecific(seqPlatformGroup, mergingCriteria)
        redirect(action: "projectAndSeqTypeSpecific", params: ["seqType.id": mergingCriteria.seqType.id])
    }

    def copyAllDefaultToSpecific(MergingCriteria mergingCriteria) {
        mergingCriteriaService.copyAllDefaultToSpecific(mergingCriteria)
        redirect(action: "projectAndSeqTypeSpecific", params: ["seqType.id": mergingCriteria.seqType.id])
    }
}

class UpdateMergingCriteriaCommand {
    SeqType seqType
    boolean useLibPrepKit
    MergingCriteria.SpecificSeqPlatformGroups useSeqPlatformGroup
}
