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
package de.dkfz.tbi.otp.project

import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
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
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeIndexService
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeProjectSeqTypeService
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.project.additionalField.*
import de.dkfz.tbi.otp.project.exception.unixGroup.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator

import java.nio.file.*
import java.nio.file.attribute.PosixFileAttributes
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Transactional
class ProjectService {

    static final long FACTOR_1024 = 1024

    static final String PHIX_INFIX = 'PhiX'

    //constants for rna configurations
    static final String ARRIBA_KNOWN_FUSIONS = "ARRIBA_KNOWN_FUSIONS"
    static final String ARRIBA_BLACKLIST = "ARRIBA_BLACKLIST"
    static final String GENOME_GATK_INDEX = "GENOME_GATK_INDEX"
    static final String GENOME_KALLISTO_INDEX = "GENOME_KALLISTO_INDEX"
    static final String GENOME_STAR_INDEX = "GENOME_STAR_INDEX"
    static final String RUN_ARRIBA = "RUN_ARRIBA"
    static final String RUN_FEATURE_COUNTS_DEXSEQ = "RUN_FEATURE_COUNTS_DEXSEQ"

    static final String PROJECT_INFO = "projectInfo"
    static final Long PROJECT_INFO_MAX_SIZE = 20 * FACTOR_1024 * FACTOR_1024

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
    ReferenceGenomeIndexService referenceGenomeIndexService
    ReferenceGenomeService referenceGenomeService
    RoddyWorkflowConfigService roddyWorkflowConfigService
    SophiaService sophiaService
    UserProjectRoleService userProjectRoleService
    WorkflowConfigService workflowConfigService
    MailHelperService mailHelperService
    MessageSourceService messageSourceService
    CreateNotificationTextService createNotificationTextService

    /**
     * @return List of all available Projects
     */
    @PostFilter("hasRole('ROLE_OPERATOR') or hasPermission(filterObject, 'OTP_READ_ACCESS')")
    List<Project> getAllProjects() {
        return Project.list(sort: "name", order: "asc", fetch: [projectGroup: 'join'])
    }

    List<Project> getAllPublicProjects() {
        return Project.findAllByPubliclyAvailable(true, [sort: "name", order: "asc"])
    }

    int getProjectCount() {
        return Project.count()
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
     * Returns the Project in an acl aware manner
     * @param id The Id of the Project
     * @return The Project
     */
    @PostAuthorize("hasRole('ROLE_OPERATOR') or returnObject == null or hasPermission(returnObject, 'OTP_READ_ACCESS')")
    Project getProject(Long id) {
        return Project.get(id)
    }

    @PostAuthorize("hasRole('ROLE_OPERATOR') or returnObject == null or hasPermission(returnObject, 'OTP_READ_ACCESS')")
    Project getProjectByName(String name) {
        return Project.findByName(name)
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
        assert rootPathElements.every { !projectParams.dirName.startsWith("${it}${File.separator}") } :
                "project directory (${projectParams.dirName}) contains (partial) data processing root path (${rootPath})"

        Project project = new Project([
                name                          : projectParams.name,
                dirName                       : projectParams.dirName,
                individualPrefix              : projectParams.individualPrefix,
                realm                         : configService.defaultRealm,
                qcThresholdHandling           : projectParams.qcThresholdHandling,
                projectType                   : projectParams.projectType,
                storageUntil                  : projectParams.storageUntil,
                projectGroup                  : ProjectGroup.findByName(projectParams.projectGroup),
                dirAnalysis                   : projectParams.dirAnalysis,
                processingPriority            : projectParams.processingPriority,
                forceCopyFiles                : projectParams.forceCopyFiles,
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

        if (project.dirAnalysis) {
            createAnalysisDirectoryIfPossible(project)
        }

        if (projectParams.projectRequest) {
            projectRequestService.finish(projectParams.projectRequest, project)
            if (!projectParams.ignoreUsersFromBaseObjects) {
                projectRequestService.addProjectRequestUsersToProject(projectParams.projectRequest)
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
                    project        : project,
                    projectInfoFile: projectParams.projectInfoFile,
            )
            projectInfoService.createProjectInfoAndUploadFile(project, projectInfoCmd)
        }
        if (projectParams.projectInfoToCopy) {
            Path path = fileSystemService.remoteFileSystemOnDefaultRealm.getPath(projectParams.projectInfoToCopy.path)
            projectInfoService.createProjectInfoByPath(project, path)
        }

        return project
    }

    void updateAllRelatedProjects(Project project) {
        List<Project> relatedProjectList = project.relatedProjects.split(',')*.trim().findAll().unique().collect { String relatedProjectName ->
            return atMostOneElement(Project.findAllByName(relatedProjectName))
        }

        //update all the related projects' relatedProjects field with the newly created project name
        relatedProjectList.findAll().each { Project baseProject ->
            addProjectToRelatedProjects(baseProject, project)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void saveAdditionalFieldValuesForProject(String fieldValue, String fieldId, Project project) {
        AbstractFieldDefinition afd = AbstractFieldDefinition.findById(fieldId as Long)
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
        File projectDirectory = project.projectDirectory
        if (projectDirectory.exists()) {
            PosixFileAttributes attrs = Files.readAttributes(projectDirectory.toPath(), PosixFileAttributes, LinkOption.NOFOLLOW_LINKS)
            if (attrs.group().toString() == project.unixGroup) {
                return
            }
        }
        executeScript(buildCreateProjectDirectory(project.unixGroup, projectDirectory.absolutePath), project, "0022")
        FileSystem fs = fileSystemService.filesystemForConfigFileChecksForRealm
        FileService.waitUntilExists(fs.getPath(projectDirectory.absolutePath))
    }

    private void createAnalysisDirectoryIfPossible(Project project) {
        assert project.dirAnalysis
        FileSystem fs = fileSystemService.filesystemForConfigFileChecksForRealm
        Path analysisDirectory = fs.getPath(project.dirAnalysis)
        if (Files.exists(analysisDirectory)) {
            return
        }

        try {
            executeScript(buildCreateProjectDirectory(project.unixGroup, project.dirAnalysis, '2770', '2775'), project, "0002")
            FileService.waitUntilExists(analysisDirectory)
        } catch (AssertionError e) {
            String recipientsString = processingOptionService.findOptionAsString(OptionName.EMAIL_RECIPIENT_ERRORS)
            String header = "Could not automatically create analysisDir '${project.dirAnalysis}' for Project '${project.name}'."
            if (recipientsString) {
                mailHelperService.sendEmail(
                        header,
                        "Automatic creation of analysisDir '${project.dirAnalysis}' for Project '${project.name}' failed. " +
                                "Make sure that the share exists and that OTP has write access to the already existing base directory," +
                                " so that the new subfolder can be created.\n ${e.localizedMessage}",
                        recipientsString)
            } else {
                log.warn("ProcessingOption 'EMAIL_RECIPIENT_ERRORS' is not set.")
                log.warn(header, e)
                assert false: "ProcessingOption EMAIL_RECIPIENT_ERRORS has to be set so OTP can sent emails about needed manual steps"
            }
        }
    }

    /**
     * Send project creation email to the ticket system.
     *
     * @param project which was just created
     */
    void sendProjectCreationMailOnlyToTicketSystem(Project project) {
        List<String> ticketSystemMails = processingOptionService.findOptionAsList(OptionName.EMAIL_RECIPIENT_NOTIFICATION)
        sendProjectCreationMail(project, ticketSystemMails, [])
    }

    /**
     * Send project creation email to the added users and the ticket system in CC. If no user mail address for
     * the given project exists, then the mail will be sent to the ticket system only.
     *
     * @param project which was just created
     */
    void sendProjectCreationMailToUserAndTicketSystem(Project project) {
        List<String> userMails = userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers(project).sort().unique()
        List<String> ccMails = processingOptionService.findOptionAsList(OptionName.EMAIL_RECIPIENT_NOTIFICATION)

        if (userMails.empty) {
            sendProjectCreationMailOnlyToTicketSystem(project)
        } else {
            sendProjectCreationMail(project, userMails, ccMails)
        }
    }

    /**
     * Generate project creation email and send it to the given receivers and ccs.
     *
     * @param project which was just created
     * @param receivers mail addresses
     * @param cc mail addresses
     */
    private void sendProjectCreationMail(Project project, List<String> receivers, List<String> cc) {
        String subject = messageSourceService.createMessage("notification.projectCreation.subject", [projectName: project.displayName])
        String content = messageSourceService.createMessage("notification.projectCreation.message",
                [
                        projectName               : project.displayName,
                        linkProjectConfig         : createNotificationTextService.createOtpLinks([project], 'projectConfig', 'index'),
                        projectFolder             : LsdfFilesService.getPath(configService.rootPath.path, project.dirName),
                        analysisFolder            : project.dirAnalysis,
                        linkUserManagementConfig  : createNotificationTextService.createOtpLinks([project], 'projectUser', 'index'),
                        clusterName               : processingOptionService.findOptionAsString(OptionName.CLUSTER_NAME),
                        clusterAdministrationEmail: processingOptionService.findOptionAsString(OptionName.EMAIL_CLUSTER_ADMINISTRATION),
                        contactEmail              : processingOptionService.findOptionAsString(OptionName.EMAIL_OTP_MAINTENANCE),
                        teamSignature             : processingOptionService.findOptionAsString(OptionName.EMAIL_SENDER_SALUTATION),
                ])

        mailHelperService.sendEmail(subject, content, receivers, cc)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    <T> void updateProjectField(T fieldValue, String fieldName, Project project) {
        assert fieldName && [
                "description",
                "dirAnalysis",
                "nameInMetadataFiles",
                "processingPriority",
                "tumorEntity",
                "projectGroup",
                "sampleIdentifierParserBeanName",
                "qcThresholdHandling",
                "unixGroup",
                "forceCopyFiles",
                "speciesWithStrains",
                "publiclyAvailable",
                "closed",
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

        if (fieldName == 'dirAnalysis' && project.dirAnalysis) {
            createAnalysisDirectoryIfPossible(project)
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
    void configureNoAlignmentDeciderProject(Project project) {
        deprecateAllReferenceGenomesByProject(project)
        project.alignmentDeciderBeanName = AlignmentDeciderBeanName.NO_ALIGNMENT
        project.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void configureDefaultOtpAlignmentDecider(Project project, String referenceGenomeName) {
        deprecateAllReferenceGenomesByProject(project)
        project.alignmentDeciderBeanName = AlignmentDeciderBeanName.OTP_ALIGNMENT
        project.save(flush: true)
        ReferenceGenome referenceGenome = exactlyOneElement(ReferenceGenome.findAllByName(referenceGenomeName))
        SeqType seqTypeWgp = SeqTypeService.wholeGenomePairedSeqType
        SeqType seqTypeExome = SeqTypeService.exomePairedSeqType
        [seqTypeWgp, seqTypeExome].each { seqType ->
            ReferenceGenomeProjectSeqType refSeqType = new ReferenceGenomeProjectSeqType()
            refSeqType.project = project
            refSeqType.seqType = seqType
            refSeqType.referenceGenome = referenceGenome
            refSeqType.sampleType = null
            refSeqType.save(flush: true)
        }
    }

    @SuppressWarnings('JavaIoPackageAccess')
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void configurePanCanAlignmentDeciderProject(PanCanAlignmentConfiguration panCanAlignmentConfiguration) {
        deprecatedReferenceGenomeProjectSeqTypeAndSetDecider(panCanAlignmentConfiguration)

        ReferenceGenome referenceGenome = exactlyOneElement(ReferenceGenome.findAllByName(panCanAlignmentConfiguration.referenceGenome))

        assert panCanAlignmentConfiguration.mergeTool in MergeConstants.ALL_MERGE_TOOLS:
                "Invalid merge tool: '${panCanAlignmentConfiguration.mergeTool}', possible values: ${MergeConstants.ALL_MERGE_TOOLS}"

        assert OtpPathValidator.isValidPathComponent(panCanAlignmentConfiguration.pluginName):
                "pluginName '${panCanAlignmentConfiguration.pluginName}' is an invalid path component"
        assert OtpPathValidator.isValidPathComponent(panCanAlignmentConfiguration.programVersion):
                "programVersion '${panCanAlignmentConfiguration.programVersion}' is an invalid path component"
        assert OtpPathValidator.isValidPathComponent(panCanAlignmentConfiguration.baseProjectConfig):
                "baseProjectConfig '${panCanAlignmentConfiguration.baseProjectConfig}' is an invalid path component"
        assert panCanAlignmentConfiguration.configVersion ==~ RoddyWorkflowConfig.CONFIG_VERSION_PATTERN:
                "configVersion '${panCanAlignmentConfiguration.configVersion}' has not expected pattern: ${RoddyWorkflowConfig.CONFIG_VERSION_PATTERN}"

        if (panCanAlignmentConfiguration.seqType.wgbs) {
            panCanAlignmentConfiguration.adapterTrimmingNeeded = true
        } else {
            List<String> allBwaMemVersions = processingOptionService.findOptionAsList(OptionName.PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_AVAILABLE)
            assert panCanAlignmentConfiguration.bwaMemVersion in allBwaMemVersions:
                    "Invalid bwa_mem version: '${panCanAlignmentConfiguration.bwaMemVersion}', possible values: ${allBwaMemVersions}"
        }
        if (panCanAlignmentConfiguration.seqType.chipSeq) {
            panCanAlignmentConfiguration.adapterTrimmingNeeded = true
        }
        if (panCanAlignmentConfiguration.mergeTool == MergeConstants.MERGE_TOOL_SAMBAMBA) {
            List<String> allSambambaVersions = processingOptionService.findOptionAsList(OptionName.PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_AVAILABLE)
            assert panCanAlignmentConfiguration.sambambaVersion in allSambambaVersions:
                    "Invalid sambamba version: '${panCanAlignmentConfiguration.sambambaVersion}', possible values: ${allSambambaVersions}"
        }

        //Reference genomes with PHIX_INFIX only works with sambamba
        if (referenceGenome.name.contains(PHIX_INFIX)) {
            assert panCanAlignmentConfiguration.mergeTool == MergeConstants.MERGE_TOOL_SAMBAMBA: "Only sambamba supported for reference genome with Phix"
        }

        File statDir = referenceGenomeService.pathToChromosomeSizeFilesPerReference(referenceGenome)
        File statSizeFile = new File(statDir, panCanAlignmentConfiguration.statSizeFileName)
        assert statSizeFile.exists(): "The statSizeFile '${panCanAlignmentConfiguration.statSizeFileName}' could not be found in ${statDir}"

        Pipeline pipeline = CollectionUtils.exactlyOneElement(Pipeline.findAllByTypeAndName(
                Pipeline.Type.ALIGNMENT,
                Pipeline.Name.PANCAN_ALIGNMENT,
        ))

        ReferenceGenomeProjectSeqType refSeqType = createReferenceGenomeProjectSeqType(panCanAlignmentConfiguration, referenceGenome)
        refSeqType.statSizeFileName = panCanAlignmentConfiguration.statSizeFileName
        refSeqType.save(flush: true)

        alignmentHelper(panCanAlignmentConfiguration, pipeline, RoddyPanCanConfigTemplate.createConfig(panCanAlignmentConfiguration),
                panCanAlignmentConfiguration.adapterTrimmingNeeded)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void configureRnaAlignmentConfig(RoddyConfiguration rnaAlignmentConfiguration) {
        assert OtpPathValidator.isValidPathComponent(rnaAlignmentConfiguration.pluginName): "pluginName '${rnaAlignmentConfiguration.pluginName}' " +
                "is an invalid path component"
        assert OtpPathValidator.isValidPathComponent(rnaAlignmentConfiguration.programVersion): "programVersion '${rnaAlignmentConfiguration.programVersion}'" +
                " is an invalid path component"
        assert OtpPathValidator.isValidPathComponent(rnaAlignmentConfiguration.baseProjectConfig): "baseProjectConfig " +
                "'${rnaAlignmentConfiguration.baseProjectConfig}' is an invalid path component"
        assert rnaAlignmentConfiguration.configVersion ==~ RoddyWorkflowConfig.CONFIG_VERSION_PATTERN: "configVersion " +
                "'${rnaAlignmentConfiguration.configVersion}' has not expected pattern: ${RoddyWorkflowConfig.CONFIG_VERSION_PATTERN}"

        Pipeline pipeline = CollectionUtils.exactlyOneElement(Pipeline.findAllByTypeAndName(
                Pipeline.Type.ALIGNMENT,
                Pipeline.Name.RODDY_RNA_ALIGNMENT,
        ))

        alignmentHelper(rnaAlignmentConfiguration, pipeline, RoddyRnaConfigTemplate.createConfig(
                rnaAlignmentConfiguration, pipeline.name), true)
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
        Pipeline pipeline = Pipeline.findByName(Pipeline.Name.RUN_YAPSA)
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
        Pipeline pipeline = Pipeline.findByName(Pipeline.Name.CELL_RANGER)
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
        return RunYapsaConfig.findByProjectAndSeqTypeAndObsoleteDateIsNull(project, seqType)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    CellRangerConfig getLatestCellRangerConfig(Project project, SeqType seqType) {
        return CellRangerConfig.findByProjectAndSeqTypeAndObsoleteDateIsNull(project, seqType)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void configureRnaAlignmentReferenceGenome(RnaAlignmentReferenceGenomeConfiguration rnaAlignmentConfiguration) {
        if (rnaAlignmentConfiguration.sampleTypes) {
            deprecateAllReferenceGenomesByProjectAndSeqTypeAndSampleTypes(
                    rnaAlignmentConfiguration.project, rnaAlignmentConfiguration.seqType, rnaAlignmentConfiguration.sampleTypes)
        } else if (rnaAlignmentConfiguration.deprecateConfigurations) {
            deprecatedReferenceGenomeProjectSeqTypeAndSetDecider(rnaAlignmentConfiguration)
        } else {
            deprecateReferenceGenomeByProjectAndSeqTypeAndNoSampleType(rnaAlignmentConfiguration.project, rnaAlignmentConfiguration.seqType)
        }
        rnaAlignmentConfiguration.project.alignmentDeciderBeanName = AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT

        Map alignmentProperties = [:]

        ReferenceGenome referenceGenome = exactlyOneElement(ReferenceGenome.findAllByName(rnaAlignmentConfiguration.referenceGenome))
        boolean mouseData = rnaAlignmentConfiguration.mouseData
        GeneModel geneModel = rnaAlignmentConfiguration.geneModel

        if (mouseData) {
            alignmentProperties[RUN_ARRIBA] = 'false'
            alignmentProperties[RUN_FEATURE_COUNTS_DEXSEQ] = 'false'
        }
        rnaAlignmentConfiguration.referenceGenomeIndex.each {
            if (!(mouseData && [ARRIBA_KNOWN_FUSIONS, ARRIBA_BLACKLIST].contains(it.toolName.name))) {
                alignmentProperties[it.toolName.name.contains(GENOME_STAR_INDEX) ? GENOME_STAR_INDEX : it.toolName.name] =
                        referenceGenomeIndexService.getFile(it).absolutePath
            }
        }
        alignmentProperties[GeneModel.GENE_MODELS] = geneModelService.getFile(geneModel).absolutePath
        if (!mouseData) {
            alignmentProperties[GeneModel.GENE_MODELS_DEXSEQ] = geneModelService.getDexSeqFile(geneModel).absolutePath
        }
        if (geneModel.excludeFileName) {
            alignmentProperties[GeneModel.GENE_MODELS_EXCLUDE] = geneModelService.getExcludeFile(geneModel).absolutePath
        }
        if (geneModel.gcFileName) {
            alignmentProperties[GeneModel.GENE_MODELS_GC] = geneModelService.getGcFile(geneModel).absolutePath
        }

        List<SampleType> sampleTypes = rnaAlignmentConfiguration.sampleTypes ?: [null]
        sampleTypes.each {
            ReferenceGenomeProjectSeqType refSeqType = new ReferenceGenomeProjectSeqType(
                    project: rnaAlignmentConfiguration.project,
                    seqType: rnaAlignmentConfiguration.seqType,
                    sampleType: it,
                    referenceGenome: referenceGenome,
            )
            refSeqType.save(flush: true)
            refSeqType.alignmentProperties = alignmentProperties.collect { String key, String value ->
                new ReferenceGenomeProjectSeqTypeAlignmentProperty(name: key, value: value, referenceGenomeProjectSeqType: refSeqType)
            } as Set
            refSeqType.save(flush: true)
        }
    }

    private void deprecatedReferenceGenomeProjectSeqTypeAndSetDecider(ProjectSeqTypeReferenceGenomeConfiguration config) {
        if (config.project.alignmentDeciderBeanName == AlignmentDeciderBeanName.OTP_ALIGNMENT) {
            deprecateAllReferenceGenomesByProject(config.project)
        } else {
            deprecateAllReferenceGenomesByProjectAndSeqType(config.project, config.seqType)
        }
        config.project.alignmentDeciderBeanName = AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT
        config.project.save(flush: true)
    }

    private ReferenceGenomeProjectSeqType createReferenceGenomeProjectSeqType(RoddyConfiguration config, ReferenceGenome referenceGenome) {
        return new ReferenceGenomeProjectSeqType(
                project: config.project,
                seqType: config.seqType,
                referenceGenome: referenceGenome,
                sampleType: null,
        )
    }

    private void alignmentHelper(RoddyConfiguration configuration, Pipeline pipeline, String xmlConfig, boolean adapterTrimmingNeeded) {
        File projectDirectory = configuration.project.projectDirectory
        assert projectDirectory.exists()

        File configFilePath = RoddyWorkflowConfig.getStandardConfigFile(
                configuration.project,
                pipeline.name,
                configuration.seqType,
                configuration.programVersion,
                configuration.configVersion,
        )
        File configDirectory = configFilePath.parentFile

        executeScript(getScriptBash(configDirectory, xmlConfig, configFilePath), configuration.project)

        roddyWorkflowConfigService.importProjectConfigFile(
                configuration.project,
                configuration.seqType,
                roddyWorkflowConfigService.formatPluginVersion(configuration.pluginName, configuration.programVersion),
                pipeline,
                configFilePath.path,
                configuration.configVersion,
                (xmlConfig.encodeAsMD5() as String),
                adapterTrimmingNeeded,
        )
    }

    private ReferenceGenomeProjectSeqType copyReferenceGenomeProjectSeqType(Project baseProject, Project targetProject, SeqType seqType) {
        ReferenceGenomeProjectSeqType baseRefGenSeqType = ReferenceGenomeProjectSeqTypeService.getConfiguredReferenceGenomeProjectSeqType(baseProject, seqType)
        assert baseRefGenSeqType

        deprecateAllReferenceGenomesByProjectAndSeqType(targetProject, seqType)

        ReferenceGenomeProjectSeqType refGenProjectSeqType = new ReferenceGenomeProjectSeqType(
            project: targetProject,
            seqType: baseRefGenSeqType.seqType,
            referenceGenome: baseRefGenSeqType.referenceGenome,
            sampleType: baseRefGenSeqType.sampleType,
            statSizeFileName: baseRefGenSeqType.statSizeFileName,
        )
        refGenProjectSeqType.save(flush: true)
        return refGenProjectSeqType
    }

    /**
     * Parses the version from the String stored in the plugin version
     * e.g. AlignmentWorkflow:1.2.3 -> 1.2.3
     * It assumes a single ':' as the separator, the version being the
     * second field of the split. This complies with the Roddy convention.
     */
    private static String parseVersionFromPluginVersionString(String programVersion) {
        return programVersion.split(":")[1]
    }

    private static void adaptConfigurationNameInRoddyConfigFile(Path file, String oldName, String newName) {
        file.text = file.text.replaceFirst(oldName, newName)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void copyPanCanAlignmentXml(Project baseProject, Project targetProject, SeqType seqType) {
        copyReferenceGenomeProjectSeqType(baseProject, targetProject, seqType)

        Pipeline pipeline = exactlyOneElement(Pipeline.findAllByTypeAndName(Pipeline.Type.ALIGNMENT, Pipeline.Name.PANCAN_ALIGNMENT))

        FileSystem remoteFileSystem = fileSystemService.remoteFileSystemOnDefaultRealm

        RoddyWorkflowConfig baseProjectRoddyConfig = RoddyWorkflowConfig.getLatestForProject(baseProject, seqType, pipeline)
        RoddyWorkflowConfig targetProjectConfig = RoddyWorkflowConfig.getLatestForProject(targetProject, seqType, pipeline)

        Path targetConfigDirectory = fileService.toPath(RoddyWorkflowConfig.getStandardConfigDirectory(targetProject, pipeline.name), remoteFileSystem)

        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(targetConfigDirectory, targetProject.realm)

        String nextConfigVersion = workflowConfigService.getNextConfigVersion(targetProjectConfig?.configVersion)
        String programVersion = parseVersionFromPluginVersionString(baseProjectRoddyConfig.programVersion)
        String configFileName = RoddyWorkflowConfig.getConfigFileName(pipeline.name, seqType, programVersion, nextConfigVersion)

        Path baseProjectConfigFile = remoteFileSystem.getPath(baseProjectRoddyConfig.configFilePath)
        Path targetProjectConfigFile = remoteFileSystem.getPath(targetConfigDirectory.toString(), configFileName)
        assert Files.notExists(targetProjectConfigFile): "A file with the planned filename already exists (${targetProjectConfigFile})"

        Files.copy(baseProjectConfigFile, targetProjectConfigFile)

        fileService.setPermission(targetProjectConfigFile, FileService.OWNER_AND_GROUP_READ_WRITE_EXECUTE_PERMISSION)

        String nameUsedInConfig = RoddyWorkflowConfig.getNameUsedInConfig(pipeline.name, seqType, baseProjectRoddyConfig.programVersion, nextConfigVersion)
        adaptConfigurationNameInRoddyConfigFile(targetProjectConfigFile, baseProjectRoddyConfig.nameUsedInConfig, nameUsedInConfig)

        fileService.setPermission(targetProjectConfigFile, FileService.DEFAULT_FILE_PERMISSION)

        roddyWorkflowConfigService.importProjectConfigFile(
                targetProject,
                baseProjectRoddyConfig.seqType,
                baseProjectRoddyConfig.programVersion,
                baseProjectRoddyConfig.pipeline,
                targetProjectConfigFile.toString(),
                nextConfigVersion,
                targetProjectConfigFile.text.encodeAsMD5() as String,
                baseProjectRoddyConfig.adapterTrimmingNeeded,
        )

        targetProject.alignmentDeciderBeanName = AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT
        targetProject.save(flush: true)
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
            checkReferenceGenomeForAceseq(configuration.project, configuration.seqType).onSuccess { ReferenceGenome referenceGenome ->
                xmlConfig = roddyConfigTemplate.createConfig(
                        configuration,
                        pipeline.name,
                )
            }
        } else if (pipeline.name == Pipeline.Name.RODDY_SOPHIA) {
            checkReferenceGenomeForSophia(configuration.project, configuration.seqType).onSuccess {
                xmlConfig = roddyConfigTemplate.createConfig(configuration, pipeline.name)
            }
        } else {
            xmlConfig = roddyConfigTemplate.createConfig(configuration, pipeline.name)
        }

        File projectDirectory = configuration.project.projectDirectory
        assert projectDirectory.exists()

        File configFilePath = RoddyWorkflowConfig.getStandardConfigFile(
                configuration.project,
                pipeline.name,
                configuration.seqType,
                configuration.programVersion,
                configuration.configVersion,
        )
        File configDirectory = configFilePath.parentFile

        executeScript(getScriptBash(configDirectory, xmlConfig, configFilePath), configuration.project)

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

    @SuppressWarnings('Indentation')//auto format and codenarc does not match
    Result<ReferenceGenome, String> checkReferenceGenomeForAceseq(Project project, SeqType seqType) {
        return Result.ofNullable(project, "project must not be null")
                .map { Project p ->
                    ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(
                            project, seqType)
                }
                .ensure({ List<ReferenceGenomeProjectSeqType> rgpsts -> rgpsts.size() == 1 }, "No reference genome set.")
                .map { List<ReferenceGenomeProjectSeqType> rgpsts -> rgpsts.first().referenceGenome }
                .ensure({ ReferenceGenome referenceGenome -> referenceGenome in aceseqService.checkReferenceGenomeMap()['referenceGenomes'] },
                        "Reference genome is not compatible with ACESeq.")
                .ensure({ ReferenceGenome referenceGenome ->
                    referenceGenome.knownHaplotypesLegendFileX &&
                            referenceGenome.knownHaplotypesLegendFile &&
                            referenceGenome.knownHaplotypesFileX &&
                            referenceGenome.knownHaplotypesFile &&
                            referenceGenome.geneticMapFileX &&
                            referenceGenome.geneticMapFile &&
                            referenceGenome.gcContentFile &&
                            referenceGenome.mappabilityFile &&
                            referenceGenome.replicationTimeFile
                }, "The selected reference genome is not configured for CNV (from ACEseq) (files are missing).")
    }

    Result<ReferenceGenome, String> checkReferenceGenomeForSophia(Project project, SeqType seqType) {
        return Result.ofNullable(project, "project must not be null").map { Project p ->
            ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(project, seqType)
        }.ensure({ List<ReferenceGenomeProjectSeqType> rgpsts -> rgpsts.size() == 1 }, "No reference genome set.")
                .map { List<ReferenceGenomeProjectSeqType> rgpsts -> rgpsts.first().referenceGenome }
                .ensure({ ReferenceGenome referenceGenome -> referenceGenome in sophiaService.checkReferenceGenomeMap()['referenceGenomes'] },
                        "Reference genome is not compatible with SOPHIA.")
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

    private String buildCreateProjectDirectory(String unixGroup, String projectDirectory, String permission = '2750', String initialPermission = '2755') {
        return """\
mkdir -p -m ${initialPermission} ${projectDirectory}

chgrp -h ${unixGroup} ${projectDirectory}
chmod ${permission} ${projectDirectory}
"""
    }

    private void executeScript(String input, Project project, String mask = "0027") {
        Realm realm = project.realm
        String script = """\
#!/bin/bash
set -evx

umask ${mask}

${input}

echo 'OK'
"""
        LogThreadLocal.withThreadLog(log) {
            assert remoteShellHelper.executeCommandReturnProcessOutput(realm, script).stdout?.trim() == "OK"
        }
    }

    /**
     * Performs a small bash script on the cluster to check if the unixGroup is available.
     *
     * @param unixGroup to check
     * @return true if the unixGroup exists, otherwise false
     */
    private boolean isUnixGroupOnCluster(Realm realm, String unixGroup) {
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

        return Boolean.valueOf(remoteShellHelper.executeCommandReturnProcessOutput(realm, script).stdout?.trim())
    }

    /**
     * Validates if the UnixGroup is valid. Throws an exception if not.
     * It checks
     *      - whether a unixGroup has invalid chars
     *      - whether a unixGroup exists on the cluster
     *      - whether a unixGroup is shared by another project
     *
     * @param unixGroup to check
     * @param realm to check if the unixGroup exists on it
     * @throws UnixGroupException if a validation fails
     */
    private void validateUnixGroup(String unixGroup, Realm realm) throws UnixGroupException {
        if (!OtpPathValidator.isValidPathComponent(unixGroup)) {
            throw new UnixGroupIsInvalidException("The unixGroup '${unixGroup}' contains invalid characters.")
        }

        if (Project.countByUnixGroup(unixGroup) > 0) {
            throw new UnixGroupIsSharedException("Unix group ${unixGroup} is already used in another project.")
        }

        if (!isUnixGroupOnCluster(realm, unixGroup)) {
            throw new UnixGroupNotFoundException("Unix group ${unixGroup} does not exist on the cluster.")
        }
    }

    private void deprecateAllReferenceGenomesByProject(Project project) {
        Set<ReferenceGenomeProjectSeqType> referenceGenomeProjectSeqTypes = ReferenceGenomeProjectSeqType.findAllByProjectAndDeprecatedDateIsNull(project)
        referenceGenomeProjectSeqTypes*.deprecatedDate = new Date()
        referenceGenomeProjectSeqTypes*.save(flush: true)
    }

    private void deprecateAllReferenceGenomesByProjectAndSeqType(Project project, SeqType seqType) {
        Set<ReferenceGenomeProjectSeqType> referenceGenomeProjectSeqTypes = ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndDeprecatedDateIsNull(
                project, seqType)
        referenceGenomeProjectSeqTypes*.deprecatedDate = new Date()
        referenceGenomeProjectSeqTypes*.save(flush: true)
    }

    private void deprecateReferenceGenomeByProjectAndSeqTypeAndNoSampleType(Project project, SeqType seqType) {
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType =
                ReferenceGenomeProjectSeqType.findByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(project, seqType)
        referenceGenomeProjectSeqType?.deprecatedDate = new Date()
        referenceGenomeProjectSeqType?.save(flush: true)
    }

    private void deprecateAllReferenceGenomesByProjectAndSeqTypeAndSampleTypes(Project project, SeqType seqType, List<SampleType> sampleTypes) {
        Set<ReferenceGenomeProjectSeqType> referenceGenomeProjectSeqTypes = sampleTypes ?
                ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeInListAndDeprecatedDateIsNull(project, seqType, sampleTypes) : []
        referenceGenomeProjectSeqTypes*.deprecatedDate = new Date()
        referenceGenomeProjectSeqTypes*.save(flush: true)
    }

    /**
     * Update the unixGroup to a new value. Before the update it will validate the unixGroup.
     *
     * If the unixGroup is used by another project, the validation will throw an exception.
     * You can force to update the unixGroup in this case by the force parameter.
     *
     * @param project for which the unixGroup is
     * @param unixGroup, the new value
     * @param force, default is false
     * @throws UnixGroupException when validation fails
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateUnixGroup(Project project, String unixGroup, boolean force = false) throws UnixGroupException {
        try {
            validateUnixGroup(unixGroup, project.realm)
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
    void updateCustomFinalNotification(Project project, boolean value) {
        project.customFinalNotification = value
        assert project.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateProcessingNotification(Project project, boolean value) {
        project.processingNotification = value
        assert project.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateQcTrafficLightNotification(Project project, boolean value) {
        project.qcTrafficLightNotification = value
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
}

trait ProjectSeqTypeConfiguration {
    Project project
    SeqType seqType
}

trait ProjectSeqTypeReferenceGenomeConfiguration extends ProjectSeqTypeConfiguration {
    String referenceGenome
}

class RoddyConfiguration implements ProjectSeqTypeConfiguration {
    String pluginName
    String programVersion
    String baseProjectConfig
    String configVersion
    String resources = "xl"
}

class PanCanAlignmentConfiguration extends RoddyConfiguration implements ProjectSeqTypeReferenceGenomeConfiguration {
    String statSizeFileName
    String mergeTool
    String bwaMemVersion
    String sambambaVersion
    boolean adapterTrimmingNeeded
}

class RnaAlignmentReferenceGenomeConfiguration implements ProjectSeqTypeReferenceGenomeConfiguration {
    boolean mouseData
    boolean deprecateConfigurations
    GeneModel geneModel
    List<ReferenceGenomeIndex> referenceGenomeIndex
    List<SampleType> sampleTypes
}
