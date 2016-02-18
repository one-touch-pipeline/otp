package de.dkfz.tbi.otp.ngsdata

import org.springframework.validation.FieldError

class CreateProjectController {

    ProjectService projectService
    ProjectGroupService projectGroupService

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
                message = "Created Project " + projectService.createProject(cmd.name, cmd.directory, 'DKFZ_13.1', 'noAlignmentDecider', cmd.group, cmd.nameInMetadataFiles, cmd.copyFiles).toString()
            }
        }
        return [
            groups: ["No Group"] + projectGroupService.availableProjectGroups()*.name,
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
    String group
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

    void setNameInMetadataFiles(String nameInMetadataFiles) {
        this.nameInMetadataFiles = nameInMetadataFiles?.trim()?.replaceAll(" +", " ")
        if (this.nameInMetadataFiles == "") {
            this.nameInMetadataFiles = null
        }
    }
}
