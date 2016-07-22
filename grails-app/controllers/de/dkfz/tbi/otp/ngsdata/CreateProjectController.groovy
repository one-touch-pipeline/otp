package de.dkfz.tbi.otp.ngsdata

import org.springframework.validation.FieldError
import de.dkfz.tbi.otp.dataprocessing.OtpPath

class CreateProjectController {

    ProjectService projectService
    ProjectGroupService projectGroupService
    ProjectCategoryService projectCategoryService

    def index(CreateProjectControllerSubmitCommand cmd) {
        String message;
        boolean hasErrors
        if (cmd.submit == "Submit") {
            hasErrors = cmd.hasErrors()
            if (hasErrors) {
                FieldError errors = cmd.errors.getFieldError()
                message = "'" + errors.getRejectedValue() + "' is not a valid value for '" + errors.getField() + "'. Error code: '" + errors.code + "'"
            }
            else {
                redirect(controller:"projectOverview", action: "specificOverview", params: [project: projectService.createProject(cmd.name, cmd.directory, 'DKFZ_13.1', 'noAlignmentDecider', cmd.unixGroup, cmd.projectGroup, cmd.projectCategory, cmd.nameInMetadataFiles, cmd.copyFiles).name])
            }
        }
        return [
            projectGroups: ["No Group"] + projectGroupService.availableProjectGroups()*.name,
            projectCategories: ProjectCategory.listOrderByName(),
            message: message,
            cmd: cmd,
            hasErrors: hasErrors
        ]
    }
}

class CreateProjectControllerSubmitCommand implements Serializable {
    String name
    String directory
    String nameInMetadataFiles
    String unixGroup
    String projectGroup
    String projectCategory
    String submit
    boolean copyFiles

    static constraints = {
        name(blank: false, validator: {val, obj ->
            if (Project.findByName(val)) {
                return 'A project with this name exists already'
            }
            if (Project.findByNameInMetadataFiles(val)) {
                return 'A project with \'' + val + '\' as nameInMetadataFiles exists already'
            }
        })
        directory(blank: false, validator: {val, obj ->
            if (Project.findByDirName(val)) {
                return 'This path \'' + val + '\' is used by another project already'
            }
        })
        unixGroup(blank: false, validator: {val, obj ->
            if (val == "") {
                return 'Empty'
            }
            if (!(OtpPath.isValidPathComponent(val))) {
                return 'Unix group contains invalid characters'
            }
        })
        nameInMetadataFiles(nullable: true, validator: {val, obj ->
            if (val && Project.findByNameInMetadataFiles(val)) {
                return '\'' + val + '\' exists already in another project as nameInMetadataFiles entry'
            }
            if (Project.findByName(val)) {
                return '\'' + val + '\' is used in another project as project name'
            }
        })
    }

    void setName(String name) {
        this.name = name?.trim()?.replaceAll(" +", " ")
    }

    void setDirectory(String directory) {
        this.directory = directory?.trim()?.replaceAll(" +", " ")
    }

    void setUnixGroup(String unixGroup) {
        this.unixGroup = unixGroup?.trim()?.replaceAll(" +", " ")
    }

    void setNameInMetadataFiles(String nameInMetadataFiles) {
        this.nameInMetadataFiles = nameInMetadataFiles?.trim()?.replaceAll(" +", " ")
        if (this.nameInMetadataFiles == "") {
            this.nameInMetadataFiles = null
        }
    }
}
