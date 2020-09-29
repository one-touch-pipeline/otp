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

import de.dkfz.tbi.otp.administration.*
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
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import java.nio.file.*
import java.nio.file.attribute.PosixFileAttributes
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@SuppressWarnings('MethodCount')
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
        assert OtpPath.isValidPathComponent(projectParams.unixGroup): "unixGroup '${projectParams.unixGroup}' contains invalid characters"
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
                costCenter                    : projectParams.costCenter,
                tumorEntity                   : projectParams.tumorEntity,
                speciesWithStrain             : projectParams.speciesWithStrain,
                endDate                       : projectParams.endDate,
                keywords                      : projectParams.keywords,
                relatedProjects               : projectParams.relatedProjects,
                internalNotes                 : projectParams.internalNotes,
                organizationalUnit            : projectParams.organizationalUnit,
                fundingBody                   : projectParams.fundingBody,
                grantId                       : projectParams.grantId,
                publiclyAvailable             : projectParams.publiclyAvailable,
                projectRequestAvailable       : projectParams.projectRequestAvailable,
        ])
        assert project.save(flush: true)

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
        return (baseProject ? baseProject.projectInfos.findAll { !it.isDta() } : []) as Set<ProjectInfo>
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

    void sendProjectCreationNotificationEmail(Project project) {
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
        mailHelperService.sendEmail(subject, content, (userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers(project).sort() +
                processingOptionService.findOptionAsList(OptionName.EMAIL_RECIPIENT_NOTIFICATION)).unique())
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    <T> void updateProjectField(T fieldValue, String fieldName, Project project) {
        assert fieldName && [
                "costCenter",
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
                "speciesWithStrain",
                "publiclyAvailable",
                "closed",
                "projectRequestAvailable",
                "individualPrefix",
                "projectType",
                "relatedProjects",
                "organizationalUnit",
                "fundingBody",
                "grantId",
                "internalNotes",
        ].contains(fieldName)

        project."${fieldName}" = fieldValue
        project.save(flush: true)

        if (fieldName == 'dirAnalysis' && project.dirAnalysis) {
            createAnalysisDirectoryIfPossible(project)
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

        assert OtpPath.isValidPathComponent(panCanAlignmentConfiguration.pluginName):
                "pluginName '${panCanAlignmentConfiguration.pluginName}' is an invalid path component"
        assert OtpPath.isValidPathComponent(panCanAlignmentConfiguration.programVersion):
                "programVersion '${panCanAlignmentConfiguration.programVersion}' is an invalid path component"
        assert OtpPath.isValidPathComponent(panCanAlignmentConfiguration.baseProjectConfig):
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
        assert OtpPath.isValidPathComponent(rnaAlignmentConfiguration.pluginName): "pluginName '${rnaAlignmentConfiguration.pluginName}' " +
                "is an invalid path component"
        assert OtpPath.isValidPathComponent(rnaAlignmentConfiguration.programVersion): "programVersion '${rnaAlignmentConfiguration.programVersion}' " +
                "is an invalid path component"
        assert OtpPath.isValidPathComponent(rnaAlignmentConfiguration.baseProjectConfig): "baseProjectConfig " +
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
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.getConfiguredReferenceGenomeProjectSeqType(project, seqType)

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
        ReferenceGenomeProjectSeqType baseRefGenSeqType = ReferenceGenomeProjectSeqType.getConfiguredReferenceGenomeProjectSeqType(baseProject, seqType)
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
        assert OtpPath.isValidPathComponent(configuration.pluginName): "pluginName '${configuration.pluginName}' is an invalid path component"
        assert OtpPath.isValidPathComponent(configuration.programVersion): "programVersion '${configuration.programVersion}' is an invalid path component"
        assert OtpPath.isValidPathComponent(configuration.baseProjectConfig): "baseProjectConfig '${configuration.baseProjectConfig}' " +
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
