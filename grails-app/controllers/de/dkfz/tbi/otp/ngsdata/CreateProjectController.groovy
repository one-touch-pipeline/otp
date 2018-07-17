package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import org.springframework.validation.*
import org.springframework.web.multipart.*


class CreateProjectController {

    ProjectService projectService
    ProjectGroupService projectGroupService
    ProjectSelectionService projectSelectionService

    def index(CreateProjectControllerSubmitCommand cmd) {
        String message
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
                        phabricatorAlias: cmd.phabricatorAlias,
                        dirName: cmd.directory,
                        dirAnalysis: cmd.analysisDirectory,
                        realm: ConfigService.getDefaultRealm(),
                        alignmentDeciderBeanName: 'noAlignmentDecider',
                        categoryNames: cmd.projectCategories,
                        unixGroup: cmd.unixGroup,
                        projectGroup: cmd.projectGroup,
                        nameInMetadataFiles: cmd.nameInMetadataFiles,
                        copyFiles: cmd.copyFiles,
                        fingerPrinting: cmd.fingerPrinting,
                        mailingListName: cmd.mailingListName,
                        costCenter: cmd.costCenter,
                        description: cmd.description,
                        processingPriority: cmd.priority,
                        tumorEntity: cmd.tumorEntity,
                        projectInfoFile: cmd.projectInfoFile,
                )
                Project project = projectService.createProject(projectParams)
                projectSelectionService.setSelectedProject([project], project.name)
                redirect(controller: "projectConfig")
            }
        }
        return [
            projectGroups: ["No Group"] + projectGroupService.availableProjectGroups()*.name,
            tumorEntities: ["No tumor entity"] + TumorEntity.list().sort()*.name,
            processingPriorities: ProjectService.processingPriorities,
            projectCategories: ProjectCategory.listOrderByName(),
            message: message,
            cmd: cmd,
            hasErrors: hasErrors
        ]
    }
}

class CreateProjectControllerSubmitCommand implements Serializable {
    String name
    String phabricatorAlias
    String directory
    String analysisDirectory
    String nameInMetadataFiles
    String unixGroup
    String mailingListName
    String costCenter
    String projectGroup
    TumorEntity tumorEntity
    List<String> projectCategories = [].withLazyDefault {new String()}
    MultipartFile projectInfoFile
    String description
    String submit
    short priority
    boolean copyFiles
    boolean fingerPrinting = true

    static constraints = {
        name(blank: false, validator: {val, obj ->
            if (Project.findByName(val)) {
                return 'A project with this name exists already'
            }
            if (Project.findByNameInMetadataFiles(val)) {
                return 'A project with \'' + val + '\' as nameInMetadataFiles exists already'
            }
        })
        phabricatorAlias(nullable: true)
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
        costCenter(nullable: true)
        nameInMetadataFiles(nullable: true, validator: {val, obj ->
            if (val && Project.findByNameInMetadataFiles(val)) {
                return '\'' + val + '\' exists already in another project as nameInMetadataFiles entry'
            }
            if (Project.findByName(val)) {
                return '\'' + val + '\' is used in another project as project name'
            }
        })
        tumorEntity(nullable: true)
        projectInfoFile(nullable: true, validator: { val, obj ->
            if (val?.isEmpty()) {
                return "File is empty"
            }

            if (val && !OtpPath.isValidPathComponent(val.originalFilename)) {
                return "Invalid fileName"
            }

            if (val?.getSize() > ProjectService.PROJECT_INFO_MAX_SIZE) {
                "The File exceeds the 20mb file size limit "
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

    void setProcessingPriority(String processingPriority) {
        switch (processingPriority) {
            case "NORMAL":
                this.priority = ProcessingPriority.NORMAL_PRIORITY
                break
            case "FAST_TRACK":
                this.priority = ProcessingPriority.FAST_TRACK_PRIORITY
                break
        }
    }

    void setTumorEntityName(String tumorEntityName) {
        if (tumorEntityName != "No tumor entity") {
            tumorEntity = TumorEntity.findByName(tumorEntityName)
        }
    }

    void setProjectInfoFile(MultipartFile projectInfoFile) {
        if (!projectInfoFile?.originalFilename) {
            this.projectInfoFile = null
        } else  {
            this.projectInfoFile = projectInfoFile
        }
    }
}
