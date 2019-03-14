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

import org.springframework.validation.FieldError
import org.springframework.web.multipart.MultipartFile

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*

class CreateProjectController {

    ConfigService configService
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
            } else {
                ProjectService.ProjectParams projectParams = new ProjectService.ProjectParams(
                        name: cmd.name,
                        phabricatorAlias: cmd.phabricatorAlias,
                        dirName: cmd.directory,
                        dirAnalysis: cmd.analysisDirectory,
                        realm: configService.getDefaultRealm(),
                        categoryNames: cmd.projectCategories,
                        unixGroup: cmd.unixGroup,
                        projectGroup: cmd.projectGroup,
                        sampleIdentifierParserBeanName: cmd.sampleIdentifierParserBeanName,
                        qcThresholdHandling: cmd.qcThresholdHandling,
                        nameInMetadataFiles: cmd.nameInMetadataFiles,
                        forceCopyFiles: cmd.forceCopyFiles,
                        fingerPrinting: cmd.fingerPrinting,
                        costCenter: cmd.costCenter,
                        description: cmd.description,
                        processingPriority: cmd.processingPriority,
                        tumorEntity: cmd.tumorEntity,
                        projectInfoFile: cmd.projectInfoFile,
                        species: cmd.species,
                )
                Project project = projectService.createProject(projectParams)
                projectSelectionService.setSelectedProject([project], project.name)
                redirect(controller: "projectConfig")
            }
        }
        return [
            projectGroups: ["No Group"] + projectGroupService.availableProjectGroups()*.name,
            tumorEntities: ["No tumor entity"] + TumorEntity.list().sort()*.name,
            sampleIdentifierParserBeanNames: SampleIdentifierParserBeanName.values(),
            qcThresholdHandlings: QcThresholdHandling.values(),
            defaultQcThresholdHandling: QcThresholdHandling.CHECK_NOTIFY_AND_BLOCK,
            processingPriorities: ProcessingPriority.displayPriorities,
            defaultProcessingPriority: ProcessingPriority.NORMAL,
            species: ['No Species'] + Species.list().sort { it.toString() },
            projectCategories: ProjectCategory.listOrderByName(),
            message: message,
            cmd: cmd,
            hasErrors: hasErrors,
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
    String costCenter
    String projectGroup
    SampleIdentifierParserBeanName sampleIdentifierParserBeanName
    QcThresholdHandling qcThresholdHandling
    TumorEntity tumorEntity
    List<String> projectCategories = [].withLazyDefault { new String() }
    String species
    MultipartFile projectInfoFile
    String description
    String submit
    ProcessingPriority processingPriority
    boolean forceCopyFiles
    boolean fingerPrinting = true

    static constraints = {
        name(blank: false, validator: { val, obj ->
            if (Project.findByName(val)) {
                return 'A project with this name exists already'
            }
            if (Project.findByNameInMetadataFiles(val)) {
                return 'A project with \'' + val + '\' as nameInMetadataFiles exists already'
            }
        })
        phabricatorAlias(nullable: true)
        directory(blank: false, validator: { val, obj ->
            if (Project.findByDirName(val)) {
                return 'This path \'' + val + '\' is used by another project already'
            }
        })
        analysisDirectory(validator: { String val ->
            if (!(!val || OtpPath.isValidAbsolutePath(val))) {
                return "\'${val}\' is not a valid absolute path"
            }
        })
        unixGroup(blank: false, validator: { val, obj ->
            if (val == "") {
                return 'Empty'
            }
            if (!(OtpPath.isValidPathComponent(val))) {
                return 'Unix group contains invalid characters'
            }
        })
        costCenter(nullable: true)
        nameInMetadataFiles(nullable: true, validator: { val, obj ->
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

    void setTumorEntityName(String tumorEntityName) {
        if (tumorEntityName != "No tumor entity") {
            tumorEntity = TumorEntity.findByName(tumorEntityName)
        }
    }

    void setProjectInfoFile(MultipartFile projectInfoFile) {
        if (projectInfoFile?.originalFilename) {
            this.projectInfoFile = projectInfoFile
        } else {
            this.projectInfoFile = null
        }
    }
}
