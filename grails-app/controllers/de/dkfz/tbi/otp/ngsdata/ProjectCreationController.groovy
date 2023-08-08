/*
 * Copyright 2011-2023 The OTP authors
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
import grails.validation.Validateable
import org.springframework.context.ApplicationContext
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.multipart.MultipartFile

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.project.*
import de.dkfz.tbi.otp.project.additionalField.AbstractFieldDefinition
import de.dkfz.tbi.otp.project.additionalField.ProjectPageType
import de.dkfz.tbi.otp.project.projectRequest.*
import de.dkfz.tbi.otp.searchability.Keyword
import de.dkfz.tbi.otp.utils.StaticApplicationContextWrapper
import de.dkfz.tbi.otp.utils.StringUtils
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriorityService
import de.dkfz.tbi.util.MultiObjectValueSource

import java.time.LocalDate
import java.time.format.DateTimeFormatter

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class ProjectCreationController {
    static allowedMethods = [
            index: "GET",
            save : "POST",
    ]

    ProjectService projectService
    ProjectRequestService projectRequestService
    ProjectGroupService projectGroupService
    ProcessingPriorityService processingPriorityService
    UserProjectRoleService userProjectRoleService

    private void handleSubmitEvent(ProjectCreationCommand cmd) {
        flash.cmd = cmd
        redirect(action: "index")
    }

    def index(ProjectCreationBasisCommand cmd) {
        List<ProjectRequest> projectRequests = ProjectRequest.createCriteria().listDistinct {
            state {
                eq("beanName", ProjectRequestStateProvider.getStateBeanName(Approved))
            }
        }

        List<UserProjectRole> usersToCopyFromBaseProject = []
        Map<String, ?> baseProjectOverride = [:]
        Map<String, String> abstractValues = [:]
        if (cmd.baseProject) {
            usersToCopyFromBaseProject = projectService.getUsersToCopyFromBaseProject(cmd.baseProject)
            baseProjectOverride << [
                    name                      : "",
                    individualPrefix          : "",
                    dirName                   : "",
                    dirAnalysis               : "",
                    unixGroup                 : "",
                    relatedProjects           : ((cmd.baseProject.relatedProjects?.split(",") ?: []) + cmd.baseProject.name).join(","),
                    speciesWithStrains        : cmd.baseProject.speciesWithStrains,
                    internalNotes             : "",
                    usersToCopyFromBaseProject: usersToCopyFromBaseProject,
                    nameInMetadataFiles       : "",
            ]
            abstractValues = projectRequestService.listAdditionalFieldValues(cmd.baseProject)
        }

        if (cmd.projectRequest) {
            abstractValues = projectRequestService.listAdditionalFieldValues(cmd.projectRequest)
        }

        Map<String, ?> defaults = [
                processingPriority             : processingPriorityService.defaultPriority(),
                storageUntil                   : "3000-01-01",
                projectType                    : Project.ProjectType.SEQUENCING,
                fingerPrinting                 : true,
                sendProjectCreationNotification: true,
                projectRequestAvailable        : true,
        ]

        ProjectCreationCommand projectCreationCmd = flash.cmd as ProjectCreationCommand
        boolean showSharedUnixGroupAlert = flash.showSharedUnixGroupAlert as boolean
        boolean showIgnoreUsersFromBaseObjects = flash.showIgnoreUsersFromBaseObjects as boolean

        MultiObjectValueSource multiObjectValueSource = new MultiObjectValueSource(
                projectCreationCmd, cmd.projectRequest, baseProjectOverride, cmd.baseProject, defaults
        )

        Set<ProjectInfo> baseProjectInfos = projectService.getSelectableBaseProjectInfos(cmd.baseProject)

        List<AbstractFieldDefinition> fieldDefinitions = projectService.projectRequestService
                .listAndFetchAbstractFields(multiObjectValueSource.getByFieldName("projectType") as Project.ProjectType,
                        ProjectPageType.PROJECT_CREATION)

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
                processingPriorities           : processingPriorityService.allSortedByPriority(),
                projectTypes                   : Project.ProjectType.values(),
                allSpeciesWithStrains          : SpeciesWithStrain.list().sort { it.toString() },
                keywords                       : Keyword.listOrderByName() ?: [],
                showSharedUnixGroupAlert       : showSharedUnixGroupAlert,
                showIgnoreUsersFromBaseObjects : showIgnoreUsersFromBaseObjects,
                source                         : multiObjectValueSource,
                abstractFields                 : fieldDefinitions,
                abstractValues                 : abstractValues,
        ]
    }

    def save(ProjectCreationCommand cmd) {
        Map basisCommandProperties = [
                "projectRequest.id": cmd.projectRequest?.id,
                "baseProject.id"   : cmd.baseProject?.id,
        ]
        List<Project> projects = Project.findAllByUnixGroup(cmd.unixGroup)

        if (cmd.save) {
            if (cmd.hasErrors()) {
                flash.cmd = cmd
                flash.message = new FlashMessage(g.message(code: "projectCreation.store.failure") as String, cmd.errors)
                redirect(action: "index", params: basisCommandProperties)
            } else if (projects && !cmd.ignoreUsersFromBaseObjects) {
                flash.cmd = cmd
                flash.showSharedUnixGroupAlert = true
                String baseText
                if (cmd.projectRequest || cmd.baseProject) {
                    baseText = g.message(code: "projectCreation.store.error.sharedUnixGroup.base")
                    flash.showIgnoreUsersFromBaseObjects = true
                } else {
                    baseText = g.message(code: "projectCreation.store.error.sharedUnixGroup.new", args: [cmd.unixGroup, projects.join(' and ')])
                }
                flash.message = new FlashMessage(g.message(code: "projectCreation.store.failure") as String, [
                        baseText,
                        g.message(code: "projectCreation.store.error.sharedUnixGroup.force") as String,
                ])
                redirect(action: "index", params: basisCommandProperties)
            } else {
                try {
                    Project project = projectService.createProject(cmd)
                    flash.message = new FlashMessage(g.message(code: "projectCreation.store.success") as String)
                    if (cmd.sendProjectCreationNotification) {
                        List<String> copiedUsers = cmd.projectRequest ? [cmd.projectRequest.requester.email] : []
                        List<String> notifiedUsers = userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers([project]).sort().unique()
                        projectRequestService.sendCreatedEmail(cmd.projectRequest, project, notifiedUsers, copiedUsers)
                    } else {
                        projectRequestService.sendCreatedEmail(cmd.projectRequest, project, [], [])
                    }
                    redirect(controller: "projectConfig", params: [(ProjectSelectionService.PROJECT_SELECTION_PARAMETER): project.name])
                    return
                } catch (RuntimeException e) {
                    log.error(e.message, e)
                    flash.message = new FlashMessage(g.message(code: "projectCreation.fieldstore.failure", args: [cmd?.name ?: '']) as String, e.message)
                }
                flash.cmd = cmd
                redirect(action: "index", params: basisCommandProperties)
                return
            }
        } else {
            handleSubmitEvent(cmd)
            return
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
            getPropertyOrNullRecursive(baseObject, propertyName.split('\\.').iterator())
        } catch (MissingPropertyException e) {
            return null
        }
    }

    private Object getPropertyOrNullRecursive(Object baseObject, Iterator<String> propertyNameIterator) {
        if (propertyNameIterator.hasNext()) {
            if (baseObject != null) {
                return getPropertyOrNullRecursive(baseObject[propertyNameIterator.next()], propertyNameIterator)
            }
            return null
        }
        return baseObject
    }

    @SuppressWarnings("ReturnNullFromCatchBlock")
    String getAddPropertyValue(ProjectPropertiesGivenWithRequest baseObject, String propertyId) {
        return baseObject?.projectFields?.find {
            it.definition.id.toString() == propertyId
        }?.displayValue ?: ""
    }

    Object getProjectRequestProperty(String propertyName) {
        return getPropertyOrNull(projectRequest, propertyName)
    }

    Object getBaseProjectProperty(String propertyName) {
        return getPropertyOrNull(baseProject, propertyName)
    }

    String getProjectRequestAddProperty(String propertyId) {
        return getAddPropertyValue(projectRequest, propertyId)
    }

    String getBaseProjectAddProperty(String propertyId) {
        return getAddPropertyValue(baseProject, propertyId)
    }
}

class ProjectCreationCommand extends ProjectCreationBasisCommand {
    String save
    String name
    String individualPrefix
    String dirName
    String dirAnalysis
    String nameInMetadataFiles
    String unixGroup
    String projectGroup

    @BindUsing({ ProjectCreationCommand obj, SimpleMapDataBindingSource source ->
        Object id = source['usersToCopyFromBaseProject']?.id
        List<Long> ids = (id instanceof String[] ? id : [id]) as List<Long>
        return UserProjectRole.findAllByIdInList(ids, [fetch: [user: 'join']]) as Set<UserProjectRole>
    })
    Set<UserProjectRole> usersToCopyFromBaseProject
    SampleIdentifierParserBeanName sampleIdentifierParserBeanName
    TumorEntity tumorEntity

    @BindUsing({ ProjectCreationCommand obj, SimpleMapDataBindingSource source ->
        Object id = source['speciesWithStrains'].id
        List<Long> ids = id instanceof String[] ? id : [id]
        return ids.collect { SpeciesWithStrain.get(it) }.findAll()
    })
    List<SpeciesWithStrain> speciesWithStrains = []
    MultipartFile projectInfoFile
    ProjectInfo projectInfoToCopy
    String description
    ProcessingPriority processingPriority
    boolean fingerPrinting = true
    Set<Keyword> keywords
    LocalDate endDate
    LocalDate storageUntil
    Project.ProjectType projectType
    String relatedProjects
    String internalNotes
    boolean ignoreUsersFromBaseObjects
    boolean sendProjectCreationNotification
    boolean publiclyAvailable
    boolean projectRequestAvailable

    List<String> additionalFieldName = []
    Map<String, String> additionalFieldValue = [:]

    static constraints = {
        name(blank: false, validator: { val, obj ->
            if (Project.findAllByName(val)) {
                return "duplicate"
            }
            if (Project.findAllByNameInMetadataFiles(val)) {
                return "duplicate.metadataFilesName"
            }
        })
        individualPrefix(blank: false, validator: { val, obj ->
            if (Project.findAllByIndividualPrefix(val)) {
                return "duplicate"
            }
        })
        dirName(blank: false, validator: { val, obj ->
            if (Project.findAllByDirName(val)) {
                return "default.not.unique.message"
            }
            if (val && !OtpPathValidator.isValidRelativePath(val)) {
                return "validator.relative.path"
            }
        })
        dirAnalysis(shared: "absolutePath")
        unixGroup(blank: false, validator: { val, obj ->
            if (val == "") {
                return "default.blank.message"
            }
            if (!(OtpPathValidator.isValidPathComponent(val))) {
                return "invalid"
            }
        })
        nameInMetadataFiles(nullable: true, validator: { val, obj ->
            if (val && Project.findAllByNameInMetadataFiles(val)) {
                return "duplicate"
            }
            if (Project.findAllByName(val)) {
                return "duplicate.name"
            }
        })
        usersToCopyFromBaseProject(nullable: true)
        tumorEntity(nullable: true)
        projectInfoFile(nullable: true, validator: { val, obj ->
            if (val?.empty) {
                return "empty"
            }
            if (val && !OtpPathValidator.isValidPathComponent(val.originalFilename)) {
                return "invalid"
            }
            if (val?.size > ProjectService.PROJECT_INFO_MAX_SIZE) {
                return "size"
            }
        })
        projectInfoToCopy(nullable: true)
        speciesWithStrains(nullable: true)
        keywords(nullable: true)
        endDate(nullable: true)
        relatedProjects(nullable: true)
        internalNotes(nullable: true)
    }

    void setName(String name) {
        this.name = StringUtils.trimAndShortenWhitespace(name)
    }

    void setDirectory(String directory) {
        this.dirName = StringUtils.trimAndShortenWhitespace(directory)
    }

    void setKeywords() { }

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
        ApplicationContext context = StaticApplicationContextWrapper.context
        KeywordService keywordService = context.keywordService
        keywordNames.split(",")*.trim().findAll().each { String name ->
            Keyword keyword = keywordService.findOrSaveByName(name)
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
