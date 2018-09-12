package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import org.springframework.validation.*


class MergingCriteriaController {
    ProjectService projectService
    ProjectSelectionService projectSelectionService
    MergingCriteriaService mergingCriteriaService

    def projectAndSeqTypeSpecific(ProjectAndSeqTypeCommand cmd) {
        if (!cmd.validate()) {
            render status: 403
            return
        }
        MergingCriteria mergingCriteria = mergingCriteriaService.findMergingCriteria(cmd.project, cmd.seqType)
        List<SeqPlatformGroup> seqPlatformGroups = mergingCriteriaService.findDefaultSeqPlatformGroups()
        List<SeqPlatformGroup> seqPlatformGroupsPerProjectAndSeqType = mergingCriteriaService.findSeqPlatformGroupsForProjectAndSeqType(cmd.project, cmd.seqType).sort{it.id}.reverse()
        List<SeqPlatform> allUsedSpecificSeqPlatforms = seqPlatformGroupsPerProjectAndSeqType*.seqPlatforms.flatten()
        List<SeqPlatform> allSeqPlatformsWithoutGroup = SeqPlatform.all.sort{it.toString()} - allUsedSpecificSeqPlatforms
        [
                mergingCriteria                      : mergingCriteria,
                project                              : cmd.project,
                seqType                              : cmd.seqType,
                seqPlatformGroups                    : seqPlatformGroups,
                seqPlatformGroupsPerProjectAndSeqType: seqPlatformGroupsPerProjectAndSeqType,
                allSeqPlatformsWithoutGroup          : allSeqPlatformsWithoutGroup,
                allUsedSpecificSeqPlatforms          : allUsedSpecificSeqPlatforms,
                dontAllowCopyingAll                  : seqPlatformGroups*.seqPlatforms.flatten().intersect(allUsedSpecificSeqPlatforms) as Boolean,
        ]
    }

    def defaultSeqPlatformGroupConfiguration() {
        List<SeqPlatformGroup> seqPlatformGroups = mergingCriteriaService.findDefaultSeqPlatformGroupsOperator()
        List<SeqPlatform> allSeqPlatformsWithoutGroup = SeqPlatform.all.sort{it.toString()} - seqPlatformGroups*.seqPlatforms.flatten()

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

        Errors errors = mergingCriteriaService.createOrUpdateMergingCriteria(cmd.project, cmd.seqType, cmd.libPrepKit, cmd.seqPlatformGroup)
        if (errors) {
            flash.message = "An error occurred"
            flash.errors = errors
        } else {
            flash.message = "Data stored successfully"
        }
        redirect(action: "projectAndSeqTypeSpecific", params: ["project.id": cmd.project.id, "seqType.id": cmd.seqType.id])
    }

    private void redirectHelper(MergingCriteria mergingCriteria) {
        if (mergingCriteria) {
            redirect(action: "projectAndSeqTypeSpecific", params: ["project.id": mergingCriteria.project.id, "seqType.id": mergingCriteria.seqType.id])
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

    def deleteSeqPlatformGroup(SeqPlatformGroup group) {
        mergingCriteriaService.deleteSeqPlatformGroup(group)
        redirectHelper(group.mergingCriteria)
    }

    def copyDefaultToSpecific(SeqPlatformGroup seqPlatformGroup, MergingCriteria mergingCriteria) {
        mergingCriteriaService.copyDefaultToSpecific(seqPlatformGroup, mergingCriteria)
        redirect(action: "projectAndSeqTypeSpecific", params: ["project.id": mergingCriteria.project.id, "seqType.id": mergingCriteria.seqType.id])
    }

    def copyAllDefaultToSpecific(MergingCriteria mergingCriteria) {
        mergingCriteriaService.copyAllDefaultToSpecific(mergingCriteria)
        redirect(action: "projectAndSeqTypeSpecific", params: ["project.id": mergingCriteria.project.id, "seqType.id": mergingCriteria.seqType.id])
    }
}

class ProjectAndSeqTypeCommand {
    Project project
    SeqType seqType
}
 class UpdateMergingCriteriaCommand {
    Project project
    SeqType seqType
    boolean libPrepKit
    MergingCriteria.SpecificSeqPlatformGroups seqPlatformGroup
}
