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
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.dataprocessing.MergingCriteriaService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowService

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class ProjectSeqPlatformGroupController {

    static allowedMethods = [
            index                                : "GET",
            update                               : "POST",
            copySeqPlatformGroup                 : "POST",
            copyAllSeqPlatformGroups             : "POST",
            searchForSeqPlatformGroups           : "POST",
            removePlatformFromSeqPlatformGroup   : "POST",
            addPlatformToExistingSeqPlatformGroup: "POST",
            emptySeqPlatformGroup                : "POST",
            createNewGroupAndAddPlatform         : "POST",
            emptyAllSeqPlatformGroups            : 'POST',
    ]

    MergingCriteriaService mergingCriteriaService
    ProjectSelectionService projectSelectionService
    WorkflowService workflowService

    @PreAuthorize("isFullyAuthenticated()")
    def index(ConfigureMergingCriteriaBaseCommand cmd) {
        Project project = projectSelectionService.selectedProject
        MergingCriteria mergingCriteria = mergingCriteriaService.findMergingCriteria(project, cmd.seqType)
        List<SeqPlatformGroup> seqPlatformGroups
        if (cmd.selectedProjectToCopyForm) {
            seqPlatformGroups = mergingCriteriaService.findSeqPlatformGroupsForProjectAndSeqType(
                    cmd.selectedProjectToCopyForm, cmd.selectedSeqTypeToCopyFrom, true)
        } else {
            cmd.selectedSeqTypeToCopyFrom = null
            seqPlatformGroups = mergingCriteriaService.findDefaultSeqPlatformGroups()
        }
        List<SeqPlatformGroup> seqPlatformGroupsPerProjectAndSeqType = mergingCriteriaService.findSeqPlatformGroupsForProjectAndSeqType(
                project, cmd.seqType, true)
        List<SeqPlatform> allUsedSpecificSeqPlatforms = seqPlatformGroupsPerProjectAndSeqType*.seqPlatforms?.flatten() as List<SeqPlatform>
        List<SeqPlatform> allSeqPlatformsWithoutGroup = SeqPlatform.all.sort { it.toString() } - allUsedSpecificSeqPlatforms
        List<SeqType> availableSeqTypes = SeqTypeService.allAlignableSeqTypes
        boolean noLibPrepKit = cmd.seqType in workflowService.getSupportedSeqTypes(WgbsWorkflow.WORKFLOW)

        return [
                mergingCriteria                      : mergingCriteria,
                seqType                              : cmd.seqType,
                noLibPrepKit                         : noLibPrepKit,
                seqPlatformGroups                    : seqPlatformGroups,
                seqPlatformGroupsPerProjectAndSeqType: seqPlatformGroupsPerProjectAndSeqType,
                allSeqPlatformsWithoutGroup          : allSeqPlatformsWithoutGroup,
                allUsedSpecificSeqPlatforms          : allUsedSpecificSeqPlatforms,
                dontAllowCopyingAll                  : seqPlatformGroups*.seqPlatforms?.flatten()?.intersect(allUsedSpecificSeqPlatforms) as Boolean,
                availableSeqTypes                    : availableSeqTypes,
                selectedProjectToCopyForm            : cmd.selectedProjectToCopyForm,
                selectedSeqTypeToCopyFrom            : cmd.selectedSeqTypeToCopyFrom,
        ]
    }

    def update(UpdateMergingCriteriaCommand cmd) {
        if (!cmd.validate()) {
            render(status: 403)
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
        redirectToIndex(["seqType": cmd.seqType] as ConfigureMergingCriteriaBaseCommand)
    }

    def copySeqPlatformGroup(MergingCriteriaAndSeqPlatformGroupCommand cmd) {
        mergingCriteriaService.copySeqPlatformGroup(cmd.seqPlatformGroup, cmd.mergingCriteria)
        redirectToIndex(cmd)
    }

    def copyAllSeqPlatformGroups(MergingCriteriaAndSeqPlatformGroupsCommand cmd) {
        mergingCriteriaService.copySeqPlatformGroups(cmd.seqPlatformGroupList, cmd.mergingCriteria)
        redirectToIndex(cmd)
    }

    def removePlatformFromSeqPlatformGroup(SeqPlatformGroupAndSeqPlatformCommand cmd) {
        mergingCriteriaService.removePlatformFromSeqPlatformGroup(cmd.seqPlatformGroup, cmd.seqPlatform)
        redirectToIndex(cmd)
    }

    def addPlatformToExistingSeqPlatformGroup(SeqPlatformGroupAndSeqPlatformCommand cmd) {
        mergingCriteriaService.addPlatformToExistingSeqPlatformGroup(cmd.seqPlatformGroup, cmd.seqPlatform)
        redirectToIndex(cmd)
    }

    def emptySeqPlatformGroup(SeqPlatformGroupCommand cmd) {
        mergingCriteriaService.emptySeqPlatformGroup(cmd.seqPlatformGroup)
        redirectToIndex(cmd)
    }

    def emptyAllSeqPlatformGroups(SeqPlatformGroupsCommand cmd) {
        mergingCriteriaService.emptyAllSeqPlatformGroups(cmd.seqPlatformGroupList)
        redirectToIndex(cmd)
    }

    def createNewGroupAndAddPlatform(MergingCriteriaAndSeqPlatformCommand cmd) {
        mergingCriteriaService.createNewGroupAndAddPlatform(cmd.seqPlatform, cmd.mergingCriteria)
        redirectToIndex(cmd)
    }

    def searchForSeqPlatformGroups(ConfigureMergingCriteriaBaseCommand cmd) {
        redirectToIndex(cmd)
    }

    private void redirectToIndex(ConfigureMergingCriteriaBaseCommand cmd) {
        redirect(action: "index", params: [
                "seqType.id"                  : cmd.seqType?.id,
                "selectedProjectToCopyForm.id": cmd.selectedProjectToCopyForm?.id,
                "selectedSeqTypeToCopyFrom.id": cmd.selectedSeqTypeToCopyFrom?.id,
        ])
    }
}

class UpdateMergingCriteriaCommand {
    SeqType seqType
    boolean useLibPrepKit
    MergingCriteria.SpecificSeqPlatformGroups useSeqPlatformGroup
}

class ConfigureMergingCriteriaBaseCommand {
    SeqType seqType
    Project selectedProjectToCopyForm
    SeqType selectedSeqTypeToCopyFrom
}

class SeqPlatformGroupCommand extends ConfigureMergingCriteriaBaseCommand {
    SeqPlatformGroup seqPlatformGroup
}

class SeqPlatformGroupAndSeqPlatformCommand extends SeqPlatformGroupCommand {
    SeqPlatform seqPlatform
}

class MergingCriteriaCommand extends ConfigureMergingCriteriaBaseCommand {
    MergingCriteria mergingCriteria
}

class MergingCriteriaAndSeqPlatformGroupCommand extends MergingCriteriaCommand {
    SeqPlatformGroup seqPlatformGroup
}

class MergingCriteriaAndSeqPlatformCommand extends MergingCriteriaCommand {
    SeqPlatform seqPlatform
}

class MergingCriteriaAndSeqPlatformGroupsCommand extends MergingCriteriaCommand {
    List<SeqPlatformGroup> seqPlatformGroupList
}

class SeqPlatformGroupsCommand extends ConfigureMergingCriteriaBaseCommand {
    List<SeqPlatformGroup> seqPlatformGroupList
}
