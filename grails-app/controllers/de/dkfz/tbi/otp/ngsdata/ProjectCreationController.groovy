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

import org.springframework.web.multipart.MultipartFile

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.searchability.Keyword

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ProjectCreationController {
    static allowedMethods = [
            index: "GET",
            save : "POST",
    ]

    ProjectService projectService
    ProjectGroupService projectGroupService
    ProjectSelectionService projectSelectionService

    def index(ProjectRequest fromRequest) {
        List<ProjectRequest> projectRequests = ProjectRequest.findAllByStatus(ProjectRequest.Status.APPROVED_BY_PI_WAITING_FOR_OPERATOR)

        return [
                projectRequests                : projectRequests,
                projectRequest                 : fromRequest,
                projectGroups                  : ["No Group"] + projectGroupService.availableProjectGroups()*.name,
                tumorEntities                  : ["No tumor entity"] + TumorEntity.list().sort()*.name,
                sampleIdentifierParserBeanNames: SampleIdentifierParserBeanName.values(),
                qcThresholdHandlings           : QcThresholdHandling.values(),
                defaultQcThresholdHandling     : QcThresholdHandling.CHECK_NOTIFY_AND_BLOCK,
                processingPriorities           : ProcessingPriority.displayPriorities,
                defaultProcessingPriority      : ProcessingPriority.NORMAL,
                defaultDate                    : "3000-01-01",
                projectTypes                   : Project.ProjectType.values(),
                defaultProjectType             : Project.ProjectType.SEQUENCING,
                allSpeciesWithStrains          : SpeciesWithStrain.list().sort { it.toString() },
                keywords                       : Keyword.listOrderByName() ?: [],
                projects                       : Project.listOrderByName(),
                cmd                            : flash.cmd as ProjectCreationCommand,
        ]
    }

    def save(ProjectCreationCommand cmd) {
        if (cmd.hasErrors()) {
            flash.cmd = cmd
            flash.message = new FlashMessage(g.message(code: "projectCreation.store.failure") as String, cmd.errors)
            redirect(action: "index", params: ["fromRequest.id": cmd.projectRequest?.id])
        } else if (cmd.projectRequest && Project.findAllByUnixGroup(cmd.unixGroup)) {
            flash.cmd = cmd
            flash.message = new FlashMessage(g.message(code: "projectCreation.store.failure") as String, [g.message(code: "projectCreation.store.error") as String])
            redirect(action: "index", params: ["fromRequest.id": cmd.projectRequest?.id])
        } else {
            Project project = projectService.createProject(cmd)
            projectSelectionService.setSelectedProject([project], project.name)
            flash.message = new FlashMessage(g.message(code: "projectCreation.store.success") as String)
            redirect(controller: "projectConfig")
        }
    }
}

class ProjectCreationCommand implements Serializable {
    String name
    String individualPrefix
    String directory
    String analysisDirectory
    String nameInMetadataFiles
    String unixGroup
    String costCenter
    String projectGroup
    SampleIdentifierParserBeanName sampleIdentifierParserBeanName
    QcThresholdHandling qcThresholdHandling
    TumorEntity tumorEntity
    SpeciesWithStrain speciesWithStrain
    MultipartFile projectInfoFile
    String description
    ProcessingPriority processingPriority
    boolean forceCopyFiles
    boolean fingerPrinting = true
    Set<Keyword> keywords
    LocalDate endDate
    LocalDate storageUntil
    Project.ProjectType projectType
    String relatedProjects
    String predecessorProject
    String organizationalUnit
    String fundingBody
    String grantId
    String internalNotes
    ProjectRequest projectRequest

    static constraints = {
        name(blank: false, validator: { val, obj ->
            if (Project.findByName(val)) {
                return 'duplicate'
            }
            if (Project.findByNameInMetadataFiles(val)) {
                return "duplicate.metadataFilesName"
            }
        })
        individualPrefix(blank: false, validator: { val, obj ->
            if (Project.findByIndividualPrefix(val)) {
                return "duplicate"
            }
        })
        directory(blank: false, validator: { val, obj ->
            if (Project.findByDirName(val)) {
                return 'default.not.unique.message'
            }
        })
        analysisDirectory(shared: "absolutePath")
        unixGroup(blank: false, validator: { val, obj ->
            if (val == "") {
                return 'default.blank.message'
            }
            if (!(OtpPath.isValidPathComponent(val))) {
                return 'invalid'
            }
        })
        costCenter(nullable: true)
        nameInMetadataFiles(nullable: true, validator: { val, obj ->
            if (val && Project.findByNameInMetadataFiles(val)) {
                return 'duplicate'
            }
            if (Project.findByName(val)) {
                return 'duplicate.name'
            }
        })
        tumorEntity(nullable: true)
        projectInfoFile(nullable: true, validator: { val, obj ->
            if (val?.isEmpty()) {
                return "empty"
            }

            if (val && !OtpPath.isValidPathComponent(val.originalFilename)) {
                return "invalid"
            }

            if (val?.getSize() > ProjectService.PROJECT_INFO_MAX_SIZE) {
                "size"
            }
        })
        speciesWithStrain(nullable: true)
        keywords(nullable: true)
        endDate(nullable: true)
        relatedProjects(nullable: true)
        predecessorProject(nullable: true)
        organizationalUnit(nullable: true)
        fundingBody(nullable: true)
        grantId(nullable: true)
        internalNotes(nullable: true)
        projectRequest(nullable: true)
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

    void setKeywordNames(String keywordNames) {
        keywords = []
        keywordNames.split(",")*.trim().findAll().each { String name ->
            Keyword keyword = Keyword.findOrSaveByName(name)
            keywords.add(keyword)
        }
    }

    void setRelatedProjectNames(String relatedProjectNames) {
        relatedProjects = relatedProjectNames.split(",")*.trim().findAll().join(",")
    }

    void setEndDateInput(String endDate) {
        if (endDate) {
            this.endDate = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(endDate))
        }
    }

    void setStorageUntilInput(String storageUntil) {
        this.storageUntil = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(storageUntil))
    }
}
