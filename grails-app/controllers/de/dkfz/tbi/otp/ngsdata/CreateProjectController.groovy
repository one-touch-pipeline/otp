package de.dkfz.tbi.otp.ngsdata

import org.springframework.validation.FieldError
import de.dkfz.tbi.otp.dataprocessing.OtpPath

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
                ProjectService.ProjectParams projectParams = new ProjectService.ProjectParams(
                        name: cmd.name,
                        dirName: cmd.directory,
                        dirAnalysis: cmd.analysisDirectory,
                        realmName: Realm.LATEST_DKFZ_REALM,
                        alignmentDeciderBeanName: 'noAlignmentDecider',
                        categoryNames: cmd.projectCategories,
                        unixGroup: cmd.unixGroup,
                        projectGroup: cmd.projectGroup,
                        nameInMetadataFiles: cmd.nameInMetadataFiles,
                        copyFiles: cmd.copyFiles,
                        mailingListName: cmd.mailingListName,
                        description: cmd.description,
                )
                redirect(controller: "projectOverview", action: "specificOverview", params: [project: projectService.createProject(projectParams).name])
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
    String analysisDirectory
    String nameInMetadataFiles
    String unixGroup
    String mailingListName
    String projectGroup
    List<String> projectCategories = [].withLazyDefault {new String()}
    String description
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
        analysisDirectory(validator: { String val ->
            if(!(!val || OtpPath.isValidAbsolutePath(val))) {
                return "\'${val}\' is not a valid absolute path"
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
        mailingListName(nullable: true, validator: {val, obj ->
            if (val) {
                return val.startsWith("tr_")
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
