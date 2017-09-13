package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import org.springframework.validation.*


class MergingCriteriaController {
    ProjectService projectService
    ProjectSelectionService projectSelectionService
    MergingCriteriaService mergingCriteriaService

    def index(ProjectAndSeqTypeCommand cmd) {
        if (!cmd.validate()) {
            render status: 403
            return
        }
        MergingCriteria mergingCriteria = mergingCriteriaService.findMergingCriteria(cmd.project, cmd.seqType)
        [
                mergingCriteria: mergingCriteria,
                project: cmd.project,
                seqType: cmd.seqType,
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
            redirect(action: "index", params: ["project.id": cmd.project.id, "seqType.id": cmd.seqType.id])
        } else {
            redirect(controller: "projectConfig")
        }
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
