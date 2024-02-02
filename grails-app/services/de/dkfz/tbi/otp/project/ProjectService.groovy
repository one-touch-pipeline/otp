/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.project

import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.*
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfigService
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvConfig
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.OtpFileSystemException
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.project.additionalField.*
import de.dkfz.tbi.otp.project.exception.unixGroup.*
import de.dkfz.tbi.otp.project.projectRequest.ProjectRequestService
import de.dkfz.tbi.otp.project.projectRequest.ProjectRequestStateProvider
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.util.TimeFormats

import java.nio.file.*
import java.sql.Timestamp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@CompileDynamic
@Transactional
class ProjectService {

    static final long FACTOR_1024 = 1024

    static final String PHIX_INFIX = 'PhiX'

    // constants for rna configurations
    static final String ARRIBA_KNOWN_FUSIONS = "ARRIBA_KNOWN_FUSIONS"
    static final String ARRIBA_BLACKLIST = "ARRIBA_BLACKLIST"
    static final String GENOME_GATK_INDEX = "GENOME_GATK_INDEX"
    static final String GENOME_KALLISTO_INDEX = "GENOME_KALLISTO_INDEX"
    static final String GENOME_STAR_INDEX = "GENOME_STAR_INDEX"
    static final String RUN_ARRIBA = "RUN_ARRIBA"
    static final String RUN_FEATURE_COUNTS_DEXSEQ = "RUN_FEATURE_COUNTS_DEXSEQ"

    static final String PROJECT_INFO = "projectInfo"

    @Autowired
    RemoteShellHelper remoteShellHelper
    AceseqService aceseqService
    ConfigService configService
    FileService fileService
    FileSystemService fileSystemService
    GeneModelService geneModelService
    ProcessingOptionService processingOptionService
    ProjectInfoService projectInfoService
    ProjectRequestService projectRequestService
    ProjectRequestStateProvider projectRequestStateProvider
    ReferenceGenomeIndexService referenceGenomeIndexService
    ReferenceGenomeService referenceGenomeService
    RoddyWorkflowConfigService roddyWorkflowConfigService
    SophiaService sophiaService
    UserProjectRoleService userProjectRoleService
    WorkflowConfigService workflowConfigService
    MailHelperService mailHelperService
    MessageSourceService messageSourceService
    CreateNotificationTextService createNotificationTextService
    MergingCriteriaService mergingCriteriaService
    SecurityService securityService

    /**
     * This method doesn't use a @PostFilter security annotation for performance reasons
     * If you change this method, also change {@link ProjectPermissionEvaluator#checkProjectRolePermission}
     *
     * @return List of projects accessible by the current user
     */
    List<Project> getAllProjects() {
        if (securityService.ifAllGranted(Role.ROLE_OPERATOR)) {
            return Project.withCriteria {
                order('name', 'asc')
            } as List<Project>
        }
        return UserProjectRole.withCriteria {
            user {
                eq("id", securityService.currentUser.id)
                eq("enabled", true)
            }
            eq("enabled", true)
            eq("accessToOtp", true)
            project {
                ne("state", Project.State.DELETED)
            }
            projections {
                property("project")
                project {
                    order('name', 'asc')
                }
            }
        } as List<Project>
    }

    List<Project> getAllPublicProjects() {
        return Project.findAllByPubliclyAvailable(true, [sort: "name", order: "asc"])
    }

    /**
     * return the number of projects for specified period if given
     */
    int getCountOfProjectsForSpecifiedPeriod(Date startDate = null, Date endDate = null, List<Project> projects) {
        return Project.createCriteria().count {
            'in'('id', projects*.id)
            if (startDate && endDate) {
                between('dateCreated', startDate, endDate)
            }
        }
    }

    /**
     * Returns the Project in an permission aware manner
     * @param id The Id of the Project
     * @return The Project
     */
    @PostAuthorize("hasRole('ROLE_OPERATOR') or returnObject == null or hasPermission(returnObject, 'OTP_READ_ACCESS')")
    Project getProject(Long id) {
        return Project.get(id)
    }

    @PostAuthorize("hasRole('ROLE_OPERATOR') or returnObject == null or hasPermission(returnObject, 'OTP_READ_ACCESS')")
    Project getProjectByName(String name) {
        return CollectionUtils.atMostOneElement(Project.findAllByName(name))
    }

    @PostFilter("hasRole('ROLE_OPERATOR') or hasPermission(filterObject, 'OTP_READ_ACCESS')")
    List<Project> getProjectByNameAsList(String name) {
        return Project.findAllByName(name)
    }

    List<Project> projectByProjectGroup(ProjectGroup projectGroup) {
        return Project.findAllByProjectGroup(projectGroup, [sort: "name", order: "asc"])
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<Project> getAllProjectsWithConfigFile(SeqType seqType, Pipeline pipeline) {
        return RoddyWorkflowConfig.findAllBySeqTypeAndPipelineAndObsoleteDateIsNullAndIndividualIsNull(seqType, pipeline)*.project.unique().sort {
            it.name.toUpperCase()
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Project createProject(ProjectCreationCommand projectParams) {
        assert OtpPathValidator.isValidPathComponent(projectParams.unixGroup): "unixGroup '${projectParams.unixGroup}' contains invalid characters"
        Path rootPath = configService.rootPath.toPath()
        List<String> rootPathElements = rootPath.toList()*.toString()
        assert rootPathElements.every { !projectParams.dirName.startsWith("${it}${File.separator}") }:
                "project directory (${projectParams.dirName}) contains (partial) data processing root path (${rootPath})"

        Project project = new Project([
                name                          : projectParams.name,
                dirName                       : projectParams.dirName,
                individualPrefix              : projectParams.individualPrefix,
                projectType                   : projectParams.projectType,
                storageUntil                  : projectParams.storageUntil,
                projectGroup                  : CollectionUtils.atMostOneElement(ProjectGroup.findAllByName(projectParams.projectGroup)),
                dirAnalysis                   : projectParams.dirAnalysis,
                processingPriority            : projectParams.processingPriority,
                fingerPrinting                : projectParams.fingerPrinting,
                nameInMetadataFiles           : projectParams.nameInMetadataFiles,
                sampleIdentifierParserBeanName: projectParams.sampleIdentifierParserBeanName,
                description                   : projectParams.description,
                unixGroup                     : projectParams.unixGroup,
                tumorEntity                   : projectParams.tumorEntity,
                speciesWithStrains            : projectParams.speciesWithStrains as Set,
                endDate                       : projectParams.endDate,
                keywords                      : projectParams.keywords,
                relatedProjects               : projectParams.relatedProjects,
                internalNotes                 : projectParams.internalNotes,
                publiclyAvailable             : projectParams.publiclyAvailable,
                projectRequestAvailable       : projectParams.projectRequestAvailable,
        ])

        project.save(flush: true)

        if (projectParams.additionalFieldValue) {
            projectParams.additionalFieldValue.each {
                saveAdditionalFieldValuesForProject(it.value, it.key, project)
            }
        }

        if (project.relatedProjects) {
            updateAllRelatedProjects(project)
        }

        createProjectDirectoryIfNeeded(project)

        SessionUtils.withNewSession {
            // open new session to prevent project creation from rollback on failing creation of analysis dir
            // In this case an email is send
            if (project.dirAnalysis) {
                try {
                    createAnalysisDirectoryIfPossible(project)
                } catch (FileSystemException | OtpFileSystemException ignore) {
                }
            }
        }

        if (projectParams.projectRequest) {
            projectParams.projectRequest.project = project
            projectRequestStateProvider.getCurrentState(projectParams.projectRequest).create(projectParams.projectRequest)
            if (!projectParams.ignoreUsersFromBaseObjects) {
                projectRequestService.addProjectRequestUsersToProject(projectParams.projectRequest)
                projectRequestService.addDepartmentHeadsToProject(projectParams.additionalFieldValue, projectParams.projectRequest)
            }
        }

        if (projectParams.baseProject) {
            addProjectToRelatedProjects(projectParams.baseProject, project)
            if (!projectParams.ignoreUsersFromBaseObjects) {
                userProjectRoleService.applyUserProjectRolesOntoProject(projectParams.usersToCopyFromBaseProject as List<UserProjectRole>, project)
            }
        }

        userProjectRoleService.handleSharedUnixGroupOnProjectCreation(project, projectParams.unixGroup)

        if (projectParams.projectInfoFile) {
            AddProjectInfoCommand projectInfoCmd = new AddProjectInfoCommand(
                    project: project,
                    projectInfoFile: projectParams.projectInfoFile,
            )
            projectInfoService.createProjectInfoAndUploadFile(project, projectInfoCmd)
        }
        if (projectParams.projectInfoToCopy) {
            Path path = projectInfoService.getPath(projectParams.projectInfoToCopy)
            projectInfoService.createProjectInfoByPath(project, path)
        }

        mergingCriteriaService.createDefaultMergingCriteria(project)

        configureDefaultFastQc(project)

        return project
    }

    private void configureDefaultFastQc(Project project) {
        if (project.projectType == Project.ProjectType.SEQUENCING) {
            String defaultFastQcType = processingOptionService.findOptionAsString(OptionName.DEFAULT_FASTQC_TYPE)
            if (defaultFastQcType) {
                Workflow workflow = exactlyOneElement(Workflow.findAllByNameIlike(defaultFastQcType + " fastqc"))
                WorkflowVersion workflowVersion = exactlyOneElement(WorkflowVersion.createCriteria().list(max: 1) {
                    apiVersion {
                        eq("workflow", workflow)
                    }
                    order("lastUpdated", "desc")
                } as Collection<Object>) as WorkflowVersion
                new WorkflowVersionSelector(project: project, workflowVersion: workflowVersion).save(flush: true)
            }
        }
    }

    void updateAllRelatedProjects(Project project) {
        List<Project> relatedProjectList = project.relatedProjects.split(',')*.trim().findAll().unique().collect { String relatedProjectName ->
            return atMostOneElement(Project.findAllByName(relatedProjectName))
        }

        // update all the related projects' relatedProjects field with the newly created project name
        relatedProjectList.findAll().each { Project baseProject ->
            addProjectToRelatedProjects(baseProject, project)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void saveAdditionalFieldValuesForProject(String fieldValue, String fieldId, Project project) {
        AbstractFieldDefinition afd = AbstractFieldDefinition.get(fieldId as Long)
        if (afd.projectFieldType == ProjectFieldType.TEXT) {
            TextFieldValue tfv = new TextFieldValue()
            tfv.definition = afd
            tfv.textValue = fieldValue
            tfv.save(flush: true)
            project.projectFields.add(tfv)
        } else if (afd.projectFieldType == ProjectFieldType.INTEGER) {
            IntegerFieldValue ifv = new IntegerFieldValue()
            ifv.definition = afd
            ifv.integerValue = fieldValue.toInteger()
            ifv.save(flush: true)
            project.projectFields.add(ifv)
        }
        project.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateAdditionalFieldValuesForProject(String fieldValue, String fieldId, Project project) {
        project.projectFields.each { AbstractFieldValue afv ->
            if (afv.definition.id.toString() == fieldId) {
                if (afv.definition.projectFieldType == ProjectFieldType.TEXT) {
                    TextFieldValue tfv = afv
                    afv.definition
                    tfv.textValue = fieldValue
                    tfv.save(flush: true)
                } else if (afv.definition.projectFieldType == ProjectFieldType.INTEGER) {
                    IntegerFieldValue ifv = afv
                    ifv.integerValue = fieldValue.toInteger()
                    ifv.save(flush: true)
                }
                project.save(flush: true)
            }
        }
    }

    List<UserProjectRole> getUsersToCopyFromBaseProject(Project baseProject) {
        return UserProjectRole.withCriteria {
            eq("project", baseProject)
            projectRoles {
                eq("name", ProjectRole.Basic.PI.name())
            }
            eq("enabled", true)
        } as List<UserProjectRole>
    }

    Set<ProjectInfo> getSelectableBaseProjectInfos(Project baseProject) {
        return (baseProject ? baseProject.projectInfos : []) as Set<ProjectInfo>
    }

    private void createProjectDirectoryIfNeeded(Project project) {
        Path projectDirectory = getProjectDirectory(project)

        if (Files.exists(projectDirectory)) {
            // ensure correct permission and group
            fileService.setGroupViaBash(projectDirectory, project.unixGroup)
            fileService.setPermissionViaBash(projectDirectory, FileService.DEFAULT_DIRECTORY_PERMISSION_STRING)
            return
        }

        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(projectDirectory.parent,
                '', FileService.DIRECTORY_WITH_OTHER_PERMISSION_STRING)
        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(projectDirectory, project.unixGroup)
    }

    private void createAnalysisDirectoryIfPossible(Project project, Boolean sendMailInErrorCase = true)
            throws OtpFileSystemException, AssertionError, FileSystemException {
        assert project.dirAnalysis
        FileSystem fs = fileSystemService.remoteFileSystem
        Path analysisDirectory = fs.getPath(project.dirAnalysis)
        if (Files.exists(analysisDirectory)) {
            // ensure correct permission and group
            fileService.setGroupViaBash(analysisDirectory, project.unixGroup)
            fileService.setPermissionViaBash(analysisDirectory, FileService.OWNER_AND_GROUP_DIRECTORY_PERMISSION_STRING)
            return
        }

        try {
            fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(analysisDirectory.parent,
                    '', FileService.DIRECTORY_WITH_OTHER_PERMISSION_STRING)
            fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(analysisDirectory, project.unixGroup,
                    FileService.OWNER_AND_GROUP_DIRECTORY_PERMISSION_STRING)
        } catch (FileSystemException | OtpFileSystemException e) {
            if (sendMailInErrorCase) {
                String header = "Could not automatically create analysisDir '${project.dirAnalysis}' for Project '${project.name}'."
                mailHelperService.sendEmailToTicketSystem(
                        header,
                        "Automatic creation of analysisDir '${project.dirAnalysis}' for Project '${project.name}' failed. " +
                                "Make sure that the share exists and that OTP has write access to the already existing base directory," +
                                " so that the new subfolder can be created.\n ${e.localizedMessage}")
            }
            throw e
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    <T> void updateProjectField(T fieldValue, String fieldName, Project project) {
        assert fieldName && [
                "description",
                "nameInMetadataFiles",
                "processingPriority",
                "tumorEntity",
                "projectGroup",
                "sampleIdentifierParserBeanName",
                "speciesWithStrains",
                "publiclyAvailable",
                "projectRequestAvailable",
                "individualPrefix",
                "projectType",
                "relatedProjects",
                "internalNotes",
        ].contains(fieldName)

        if (fieldName == 'speciesWithStrains') {
            project.speciesWithStrains.clear()
            project.save(flush: true)
            project.speciesWithStrains.addAll(fieldValue.findAll().collect {
                SpeciesWithStrain.get(Long.valueOf(it))
            })
        } else {
            project."${fieldName}" = fieldValue
        }

        project.save(flush: true)

        if (fieldName == 'relatedProjects' && project.relatedProjects) {
            updateAllRelatedProjects(project)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    <T> void updateAbstractFieldValueForProject(T fieldValue, String fieldId, Project project) {
        boolean fieldDoesNotExist = true
        project.projectFields.each { AbstractFieldValue abstractFieldValue ->
            if (abstractFieldValue.definition.id.toString() == fieldId) {
                fieldDoesNotExist = false
                updateAdditionalFieldValuesForProject(fieldValue as String, fieldId, project)
            }
        }
        if (fieldDoesNotExist) {
            saveAdditionalFieldValuesForProject(fieldValue as String, fieldId, project)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateProjectFieldDate(String fieldValue, String fieldName, Project project) {
        assert fieldName && [
                "endDate",
                "storageUntil",
        ].contains(fieldName)

        project."${fieldName}" = fieldValue ? LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(fieldValue)) : null
        project.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void invalidateProjectConfig(Project project, SeqType seqType, Pipeline pipeline) {
        ConfigPerProjectAndSeqType config = atMostOneElement(ConfigPerProjectAndSeqType.findAllByProjectAndSeqTypeAndPipelineAndObsoleteDate(
                project, seqType, pipeline, null))
        if (config) {
            workflowConfigService.makeObsolete(config)
        }
        if (pipeline.name == Pipeline.Name.CELL_RANGER) {
            deprecateReferenceGenomeProjectSeqType(project, seqType)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Errors createOrUpdateRunYapsaConfig(Project project, SeqType seqType, String programVersion) {
        Pipeline pipeline = CollectionUtils.atMostOneElement(Pipeline.findAllByName(Pipeline.Name.RUN_YAPSA))
        ConfigPerProjectAndSeqType latest = getLatestRunYapsaConfig(project, seqType)

        if (latest?.programVersion != programVersion) {
            workflowConfigService.makeObsolete(latest)

            try {
                new RunYapsaConfig(
                        project: project,
                        seqType: seqType,
                        pipeline: pipeline,
                        programVersion: programVersion,
                        previousConfig: latest,
                ).save(flush: true)
            } catch (ValidationException e) {
                return e.errors
            }
        }
        return null
    }

    void deprecateReferenceGenomeProjectSeqType(Project project, SeqType seqType) {
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType =
                ReferenceGenomeProjectSeqTypeService.getConfiguredReferenceGenomeProjectSeqType(project, seqType)

        if (referenceGenomeProjectSeqType) {
            referenceGenomeProjectSeqType.deprecatedDate = new Date()
            referenceGenomeProjectSeqType.save(flush: true)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    Errors createOrUpdateCellRangerConfig(Project project, SeqType seqType, String programVersion, ReferenceGenomeIndex referenceGenomeIndex) {
        Pipeline pipeline = CollectionUtils.atMostOneElement(Pipeline.findAllByName(Pipeline.Name.CELL_RANGER))
        ConfigPerProjectAndSeqType latest = getLatestCellRangerConfig(project, seqType)

        workflowConfigService.makeObsolete(latest)
        try {
            new CellRangerConfig(
                    project: project,
                    seqType: seqType,
                    referenceGenomeIndex: referenceGenomeIndex,
                    pipeline: pipeline,
                    programVersion: programVersion,
                    previousConfig: latest,
            ).save(flush: true)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    RunYapsaConfig getLatestRunYapsaConfig(Project project, SeqType seqType) {
        return CollectionUtils.atMostOneElement(RunYapsaConfig.findAllByProjectAndSeqTypeAndObsoleteDateIsNull(project, seqType))
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    CellRangerConfig getLatestCellRangerConfig(Project project, SeqType seqType) {
        return CollectionUtils.atMostOneElement(CellRangerConfig.findAllByProjectAndSeqTypeAndObsoleteDateIsNull(project, seqType))
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    RoddyWorkflowConfig configureSnvPipelineProject(RoddyConfiguration snvPipelineConfiguration) {
        RoddyWorkflowConfig roddyWorkflowConfig = configurePipelineProject(snvPipelineConfiguration, Pipeline.Name.RODDY_SNV.pipeline, RoddySnvConfigTemplate)

        SnvConfig snvConfig = CollectionUtils.atMostOneElement(SnvConfig.findAllWhere([
                project     : snvPipelineConfiguration.project,
                seqType     : snvPipelineConfiguration.seqType,
                obsoleteDate: null,
        ]))
        if (snvConfig) {
            snvConfig.obsoleteDate = new Date()
            snvConfig.save(flush: true)
            roddyWorkflowConfig.previousConfig = snvConfig
            roddyWorkflowConfig.save(flush: true)
        }
        return roddyWorkflowConfig
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    RoddyWorkflowConfig configureIndelPipelineProject(RoddyConfiguration indelPipelineConfiguration) {
        return configurePipelineProject(indelPipelineConfiguration, Pipeline.Name.RODDY_INDEL.pipeline, RoddyIndelConfigTemplate)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    RoddyWorkflowConfig configureSophiaPipelineProject(RoddyConfiguration sophiaPipelineConfiguration) {
        return configurePipelineProject(sophiaPipelineConfiguration, Pipeline.Name.RODDY_SOPHIA.pipeline, RoddySophiaConfigTemplate)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    RoddyWorkflowConfig configureAceseqPipelineProject(RoddyConfiguration aceseqPipelineConfiguration) {
        return configurePipelineProject(aceseqPipelineConfiguration, Pipeline.Name.RODDY_ACESEQ.pipeline, RoddyAceseqConfigTemplate)
    }

    private RoddyWorkflowConfig configurePipelineProject(RoddyConfiguration configuration, Pipeline pipeline, Class roddyConfigTemplate) {
        assert OtpPathValidator.isValidPathComponent(configuration.pluginName): "pluginName '${configuration.pluginName}' is an invalid path component"
        assert OtpPathValidator.isValidPathComponent(configuration.programVersion): "programVersion '${configuration.programVersion}' " +
                "is an invalid path component"
        assert OtpPathValidator.isValidPathComponent(configuration.baseProjectConfig): "baseProjectConfig '${configuration.baseProjectConfig}' " +
                "is an invalid path component"
        assert configuration.configVersion ==~ RoddyWorkflowConfig.CONFIG_VERSION_PATTERN: "configVersion '${configuration.configVersion}' " +
                "has not expected pattern: ${RoddyWorkflowConfig.CONFIG_VERSION_PATTERN}"

        String xmlConfig
        if (pipeline.name == Pipeline.Name.RODDY_ACESEQ) {
            checkReferenceGenomeForAceseq(configuration.project, configuration.seqType).onSuccess {
                xmlConfig = roddyConfigTemplate.createConfig(configuration, pipeline.name)
            }
        } else if (pipeline.name == Pipeline.Name.RODDY_SOPHIA) {
            checkReferenceGenomeForSophia(configuration.project, configuration.seqType).onSuccess {
                xmlConfig = roddyConfigTemplate.createConfig(configuration, pipeline.name)
            }
        } else {
            xmlConfig = roddyConfigTemplate.createConfig(configuration, pipeline.name)
        }
        Path projectDirectory = getProjectDirectory(configuration.project)
        assert Files.exists(projectDirectory)

        File configFilePath = RoddyWorkflowConfig.getStandardConfigFile(
                configuration.project,
                pipeline.name,
                configuration.seqType,
                configuration.programVersion,
                configuration.configVersion,
        )
        File configDirectory = configFilePath.parentFile

        executeScript(getScriptBash(configDirectory, xmlConfig, configFilePath))

        return roddyWorkflowConfigService.importProjectConfigFile(
                configuration.project,
                configuration.seqType,
                roddyWorkflowConfigService.formatPluginVersion(configuration.pluginName, configuration.programVersion),
                pipeline,
                configFilePath.path,
                configuration.configVersion,
                (xmlConfig.encodeAsMD5() as String),
        )
    }

    Result<List<ReferenceGenome>, String> checkReferenceGenomeForAceseq(Project project, SeqType seqType) {
        return Result.ofNullable(project, "project must not be null")
                .map { Project p ->
                    ReferenceGenomeSelector.findAllByProjectAndSeqType(project, seqType)*.referenceGenome
                }
                .ensure({ List<ReferenceGenome> referenceGenomes ->
                    referenceGenomes.size() >= 1
                }, "No reference genome is configured.")
                .ensure({ List<ReferenceGenome> referenceGenomes ->
                    List<ReferenceGenome> allowedReferenceGenomes = aceseqService.checkReferenceGenomeMap()['referenceGenomes']
                    List<ReferenceGenome> filteredReferenceGenomes = allowedReferenceGenomes.findAll { ReferenceGenome referenceGenome ->
                        referenceGenome.knownHaplotypesLegendFileX &&
                                referenceGenome.knownHaplotypesLegendFile &&
                                referenceGenome.knownHaplotypesFileX &&
                                referenceGenome.knownHaplotypesFile &&
                                referenceGenome.geneticMapFileX &&
                                referenceGenome.geneticMapFile &&
                                referenceGenome.gcContentFile &&
                                referenceGenome.mappabilityFile &&
                                referenceGenome.replicationTimeFile
                    }
                    referenceGenomes.intersect(filteredReferenceGenomes)
                }, "No reference genome is compatible with ACESeq.")
    }

    Result<List<ReferenceGenome>, String> checkReferenceGenomeForSophia(Project project, SeqType seqType) {
        return Result.ofNullable(project, "project must not be null")
                .map { Project p ->
                    ReferenceGenomeSelector.findAllByProjectAndSeqType(project, seqType)*.referenceGenome
                }
                .ensure({ List<ReferenceGenome> referenceGenomes ->
                    referenceGenomes.size() >= 1
                }, "No reference genome is configured.")
                .ensure({ List<ReferenceGenome> referenceGenomes ->
                    List<ReferenceGenome> allowedReferenceGenomes = sophiaService.checkReferenceGenomeMap()['referenceGenomes']
                    referenceGenomes.intersect(allowedReferenceGenomes)
                }, "No reference genome is compatible with SOPHIA.")
    }

    private String getScriptBash(File configDirectory, String xmlConfig, File configFilePath) {
        String md5 = HelperUtils.randomMd5sum
        String createConfigDirectory = ''

        if (!configDirectory.exists()) {
            createConfigDirectory = """\
mkdir -p -m 2750 ${configDirectory}
"""
        }

        return """\

${createConfigDirectory}

cat <<${md5} > ${configFilePath}
${xmlConfig.replaceAll(/\$/, /\\\$/)}${md5}

chmod 0440 ${configFilePath}

"""
    }

    private void executeScript(String input, String mask = "0027") {
        String script = """\
#!/bin/bash
set -evx

umask ${mask}

${input}

echo 'OK'
"""
        LogThreadLocal.withThreadLog(log) {
            assert remoteShellHelper.executeCommandReturnProcessOutput(script).stdout?.trim() == "OK"
        }
    }

    /**
     * Performs a small bash script on the cluster to check if the unixGroup is available.
     *
     * @param unixGroup to check
     * @return true if the unixGroup exists, otherwise false
     */
    private boolean isUnixGroupOnCluster(String unixGroup) {
        String script = """\
        #!/bin/bash
        if
            getent group ${unixGroup} > /dev/null 2>&1;
        then
            echo 'true';
        else
            echo 'false';
        fi
        """.stripIndent()

        return Boolean.valueOf(remoteShellHelper.executeCommandReturnProcessOutput(script).stdout?.trim())
    }

    /**
     * Validates if the UnixGroup is valid. Throws an exception if not.
     * It checks
     *      - whether a unixGroup has invalid chars
     *      - whether a unixGroup exists on the cluster
     *      - whether a unixGroup is shared by another project
     *
     * @param unixGroup to check
     * @throws UnixGroupException if a validation fails
     */
    private void validateUnixGroup(String unixGroup) throws UnixGroupException {
        if (!OtpPathValidator.isValidPathComponent(unixGroup)) {
            throw new UnixGroupIsInvalidException("The unixGroup '${unixGroup}' contains invalid characters.")
        }

        if (Project.countByUnixGroup(unixGroup) > 0) {
            throw new UnixGroupIsSharedException("Unix group ${unixGroup} is already used in another project.")
        }

        if (!isUnixGroupOnCluster(unixGroup)) {
            throw new UnixGroupNotFoundException("Unix group ${unixGroup} does not exist on the cluster.")
        }
    }

    /**
     * Update the unixGroup to a new value. Before the update it will validate the unixGroup.
     *
     * If the unixGroup is used by another project, the validation will throw an exception.
     * You can force to update the unixGroup in this case by the force parameter.
     *
     * @param project for which the unixGroup is
     * @param unixGroup , the new value
     * @param force , default is false
     * @throws UnixGroupException when validation fails
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateUnixGroup(Project project, String unixGroup, boolean force = false) throws UnixGroupException {
        try {
            validateUnixGroup(unixGroup)
        } catch (UnixGroupIsSharedException isSharedException) {
            if (!force) {
                throw isSharedException
            }
        }

        project.unixGroup = unixGroup
        assert project.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateFingerPrinting(Project project, boolean value) {
        project.fingerPrinting = value
        assert project.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateAnalysisDirectory(Project project, String analysisDir, boolean force = false)
            throws FileSystemException, OtpFileSystemException, AssertionError {
        project.dirAnalysis = analysisDir
        project.save(flush: true)
        try {
            createAnalysisDirectoryIfPossible(project, force)
        } catch (FileSystemException | OtpFileSystemException | AssertionError e) {
            if (!force) {
                throw e
            }
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateProcessingNotification(Project project, boolean value) {
        project.processingNotification = value
        assert project.save(flush: true)
    }

    Map<String, List<Project>> getAllProjectsWithSharedUnixGroup() {
        return Project.list().groupBy { Project project ->
            project.unixGroup
        }.findAll { String unixGroup, List<Project> projects ->
            projects.size() > 1
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void addProjectToRelatedProjects(Project baseProject, Project newProject) {
        baseProject.relatedProjects = ("${baseProject.relatedProjects ?: ""},${newProject.name}").split(",").findAll().unique().sort().join(",")
        baseProject.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateState(Project project, Project.State state) {
        project.state = state
        assert project.save(flush: true)
    }

    Path getProjectDirectory(Project project) {
        return fileSystemService.remoteFileSystem.getPath(
                configService.rootPath.absolutePath,
                project.dirName,
        )
    }

    Path getSequencingDirectory(Project project) {
        return getProjectDirectory(project).resolve('sequencing')
    }

    String getLastReceivedDate(Project project) {
        Timestamp[] timestamps = SeqTrack.createCriteria().get {
            sample {
                individual {
                    eq("project", project)
                }
            }
            projections {
                max("dateCreated")
            }
        }
        return timestamps ? TimeFormats.DATE_TIME.getFormattedDate(timestamps[0] as Date) : ''
    }

    Project findByProjectWithFetchedKeywords(Project project) {
        return atMostOneElement(Project.findAllByName(project?.name, [fetch: [keywords: 'join']]))
    }

    Map<Project, List<User>> getExpiredProjectsWithPIs() {
        List<Project> projects = Project.findAllByStorageUntilLessThan(LocalDate.now())

        if (projects.empty) {
            return [:]
        }

        List<UserProjectRole> userProjectRoles = UserProjectRole.withCriteria {
            'in'('project', projects)
            projectRoles {
                eq('name', ProjectRole.Basic.PI.toString())
            }
            user {
                eq('enabled', true)
            }
        }

        return projects.collectEntries { Project project ->
            List<User> users = userProjectRoles.findAll { it.project == project }*.user
            return [(project): users]
        }
    }

    static Project findByNameOrNameInMetadataFiles(String name) {
        return name ? atMostOneElement(Project.findAllByNameOrNameInMetadataFiles(name, name)) : null
    }
}

trait ProjectSeqTypeConfiguration {
    Project project
    SeqType seqType
}

class RoddyConfiguration implements ProjectSeqTypeConfiguration {
    String pluginName
    String programVersion
    String baseProjectConfig
    String configVersion
    String resources = "xl"
}
