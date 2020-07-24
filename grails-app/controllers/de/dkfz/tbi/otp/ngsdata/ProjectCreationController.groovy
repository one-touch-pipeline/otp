/*
 * Copyright 2011-2020 The OTP authors
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

import grails.databinding.BindUsing
import grails.databinding.SimpleMapDataBindingSource
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.Validateable
import org.springframework.web.multipart.MultipartFile

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.administration.ProjectInfo
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.project.*
import de.dkfz.tbi.otp.searchability.Keyword
import de.dkfz.tbi.otp.utils.StringUtils
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriorityService
import de.dkfz.tbi.util.MultiObjectValueSource

import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Secured(['ROLE_OPERATOR'])
class ProjectCreationController {
    static allowedMethods = [
            index: "GET",
            save : "POST",
    ]

    ProjectService projectService
    ProjectGroupService projectGroupService
    ProcessingPriorityService processingPriorityService

    def index(ProjectCreationBasisCommand cmd) {
        List<ProjectRequest> projectRequests = ProjectRequest.findAllByStatus(ProjectRequest.Status.APPROVED_BY_PI_WAITING_FOR_OPERATOR)

        List<UserProjectRole> usersToCopyFromBaseProject = []
        Map<String, ?> baseProjectOverride = [:]
        if (cmd.baseProject) {
            usersToCopyFromBaseProject = projectService.getUsersToCopyFromBaseProject(cmd.baseProject)
            baseProjectOverride << [
                    name                      : "",
                    individualPrefix          : "",
                    dirName                   : "",
                    dirAnalysis               : "",
                    unixGroup                 : "",
                    relatedProjects           : ((cmd.baseProject.relatedProjects?.split(",") ?: []) + cmd.baseProject.name).join(","),
                    speciesWithStrain         : [id: null],
                    internalNotes             : "",
                    usersToCopyFromBaseProject: usersToCopyFromBaseProject,
                    nameInMetadataFiles       : "",
            ]
        }
        Map<String, ?> defaults = [
                qcThresholdHandling            : QcThresholdHandling.CHECK_NOTIFY_AND_BLOCK,
                processingPriority             : processingPriorityService.defaultPriority(),
                storageUntil                   : "3000-01-01",
                projectType                    : Project.ProjectType.SEQUENCING,
                forceCopyFiles                 : true,
                fingerPrinting                 : true,
                sendProjectCreationNotification: true,
                projectRequestAvailable        : false,
        ]
        ProjectCreationCommand projectCreationCmd = flash.cmd as ProjectCreationCommand
        boolean showIgnoreUsersFromBaseObjects = flash.showIgnoreUsersFromBaseObjects as boolean

        MultiObjectValueSource multiObjectValueSource = new MultiObjectValueSource(projectCreationCmd, cmd.projectRequest, baseProjectOverride, cmd.baseProject, defaults)

        Set<ProjectInfo> baseProjectInfos = projectService.getSelectableBaseProjectInfos(cmd.baseProject)

        return [
                cmd                            : cmd as ProjectCreationBasisCommand,
                projectCreationCmd             : projectCreationCmd,
                projectRequests                : projectRequests,
                projectRequest                 : cmd.projectRequest,
                projects                       : projectService.allProjects,
                baseProject                    : cmd.baseProject,
                baseProjectUsers               : usersToCopyFromBaseProject,
                baseProjectInfos               : baseProjectInfos,
                projectGroups                  : projectGroupService.availableProjectGroups()*.name,
                tumorEntities                  : TumorEntity.list().sort { it.name },
                sampleIdentifierParserBeanNames: SampleIdentifierParserBeanName.values(),
                qcThresholdHandlings           : QcThresholdHandling.values(),
                processingPriorities           : processingPriorityService.allSortedByPriority(),
                projectTypes                   : Project.ProjectType.values(),
                allSpeciesWithStrains          : SpeciesWithStrain.list().sort { it.toString() },
                keywords                       : Keyword.listOrderByName() ?: [],
                showIgnoreUsersFromBaseObjects : showIgnoreUsersFromBaseObjects,
                source                         : multiObjectValueSource,
        ]
    }

    def save(ProjectCreationCommand cmd) {
        Map basisCommandProperties = [
                "projectRequest.id": cmd.projectRequest?.id,
                "baseProject.id"   : cmd.baseProject?.id,
        ]
        if (cmd.hasErrors()) {
            flash.cmd = cmd
            flash.message = new FlashMessage(g.message(code: "projectCreation.store.failure") as String, cmd.errors)
            redirect(action: "index", params: basisCommandProperties)
        } else if (((cmd.projectRequest || cmd.baseProject) && Project.findAllByUnixGroup(cmd.unixGroup)) && !cmd.ignoreUsersFromBaseObjects) {
            flash.cmd = cmd
            flash.showIgnoreUsersFromBaseObjects = true
            flash.message = new FlashMessage(g.message(code: "projectCreation.store.failure") as String, [g.message(code: "projectCreation.store.error.sharedUnixGroup") as String])
            redirect(action: "index", params: basisCommandProperties)
        } else {
            Project project = projectService.createProject(cmd)
            flash.message = new FlashMessage(g.message(code: "projectCreation.store.success") as String)
            if (cmd.sendProjectCreationNotification) {
                projectService.sendProjectCreationNotificationEmail(project)
            }
            redirect(controller: "projectConfig", params: [(ProjectSelectionService.PROJECT_SELECTION_PARAMETER): project.name])
        }
    }
}

class ProjectCreationBasisCommand implements Validateable {
    ProjectRequest projectRequest
    Project baseProject

    static constraints = {
        projectRequest(nullable: true)
        baseProject(nullable: true)
    }

    @SuppressWarnings("ReturnNullFromCatchBlock")
    Object getPropertyOrNull(Object baseObject, String propertyName) {
        try {
            return baseObject ? baseObject."${propertyName}" : null
        } catch (MissingPropertyException e) {
            return null
        }
    }

    Object getProjectRequestProperty(String propertyName) {
        return getPropertyOrNull(projectRequest, propertyName)
    }

    Object getBaseProjectProperty(String propertyName) {
        return getPropertyOrNull(baseProject, propertyName)
    }
}

class ProjectCreationCommand extends ProjectCreationBasisCommand {
    String name
    String individualPrefix
    String dirName
    String dirAnalysis
    String nameInMetadataFiles
    String unixGroup
    String costCenter
    String projectGroup
    @BindUsing({ ProjectCreationCommand obj, SimpleMapDataBindingSource source ->
        Object id = source['usersToCopyFromBaseProject']?.id
        List<Long> ids = (id instanceof String[] ? id : [id]) as List<Long>
        return UserProjectRole.findAllByIdInList(ids, [fetch: [user: 'join']]) as Set<UserProjectRole>
    })
    Set<UserProjectRole> usersToCopyFromBaseProject
    SampleIdentifierParserBeanName sampleIdentifierParserBeanName
    QcThresholdHandling qcThresholdHandling
    TumorEntity tumorEntity
    SpeciesWithStrain speciesWithStrain
    MultipartFile projectInfoFile
    ProjectInfo projectInfoToCopy
    String description
    ProcessingPriority processingPriority
    boolean forceCopyFiles
    boolean fingerPrinting = true
    Set<Keyword> keywords
    LocalDate endDate
    LocalDate storageUntil
    Project.ProjectType projectType
    String relatedProjects
    String organizationalUnit
    String fundingBody
    String grantId
    String internalNotes
    boolean ignoreUsersFromBaseObjects
    boolean sendProjectCreationNotification
    boolean publiclyAvailable
    boolean projectRequestAvailable

    static constraints = {
        name(blank: false, validator: { val, obj ->
            if (Project.findByName(val)) {
                return "duplicate"
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
        dirName(blank: false, validator: { val, obj ->
            if (Project.findByDirName(val)) {
                return "default.not.unique.message"
            }
            if (val && !OtpPath.isValidRelativePath(val)) {
                return "validator.relative.path"
            }
        })
        dirAnalysis(shared: "absolutePath")
        unixGroup(blank: false, validator: { val, obj ->
            if (val == "") {
                return "default.blank.message"
            }
            if (!(OtpPath.isValidPathComponent(val))) {
                return "invalid"
            }
        })
        costCenter(nullable: true)
        nameInMetadataFiles(nullable: true, validator: { val, obj ->
            if (val && Project.findByNameInMetadataFiles(val)) {
                return "duplicate"
            }
            if (Project.findByName(val)) {
                return "duplicate.name"
            }
        })
        usersToCopyFromBaseProject(nullable: true)
        tumorEntity(nullable: true)
        projectInfoFile(nullable: true, validator: { val, obj ->
            if (val?.empty) {
                return "empty"
            }
            if (val && !OtpPath.isValidPathComponent(val.originalFilename)) {
                return "invalid"
            }
            if (val?.size > ProjectService.PROJECT_INFO_MAX_SIZE) {
                return "size"
            }
        })
        projectInfoToCopy(nullable: true)
        speciesWithStrain(nullable: true)
        keywords(nullable: true)
        endDate(nullable: true)
        relatedProjects(nullable: true)
        organizationalUnit(nullable: true)
        fundingBody(nullable: true)
        grantId(nullable: true)
        internalNotes(nullable: true)
    }

    void setName(String name) {
        this.name = StringUtils.trimAndShortenWhitespace(name)
    }

    void setDirectory(String directory) {
        this.dirName = StringUtils.trimAndShortenWhitespace(directory)
    }

    void setUnixGroup(String unixGroup) {
        this.unixGroup = StringUtils.trimAndShortenWhitespace(unixGroup)
    }

    void setNameInMetadataFiles(String nameInMetadataFiles) {
        this.nameInMetadataFiles = StringUtils.blankToNull(StringUtils.trimAndShortenWhitespace(nameInMetadataFiles))
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
