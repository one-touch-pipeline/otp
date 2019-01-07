package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.cellRanger.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import grails.validation.*
import org.springframework.beans.factory.annotation.*
import org.springframework.security.access.prepost.*
import org.springframework.validation.*
import org.springframework.web.multipart.*

import java.nio.file.*
import java.nio.file.attribute.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class ProjectService {

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
    static final Long PROJECT_INFO_MAX_SIZE = 20 * 1024 * 1024


    @Autowired
    RemoteShellHelper remoteShellHelper
    ReferenceGenomeService referenceGenomeService
    ExecutionHelperService executionHelperService
    ReferenceGenomeIndexService referenceGenomeIndexService
    GeneModelService geneModelService
    SophiaService sophiaService
    AceseqService aceseqService
    ConfigService configService
    FileSystemService fileSystemService
    RoddyWorkflowConfigService roddyWorkflowConfigService
    ProcessingOptionService processingOptionService

    FileService fileService

    /**
     *
     * @return List of all available Projects
     */
    @PostFilter("hasRole('ROLE_OPERATOR') or hasPermission(filterObject, 'OTP_READ_ACCESS')")
    List<Project> getAllProjects() {
        return Project.list(sort: "name", order: "asc", fetch: [projectCategories: 'join', projectGroup: 'join'])
    }

    int getProjectCount() {
        return Project.count()
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

    List<Project> projectByProjectGroup(ProjectGroup projectGroup) {
        return Project.findAllByProjectGroup(projectGroup, [sort: "name", order: "asc"])
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<Project> getAllProjectsWithConfigFile(SeqType seqType, Pipeline pipeline) {
        return RoddyWorkflowConfig.findAllBySeqTypeAndPipelineAndObsoleteDateIsNullAndIndividualIsNull(seqType, pipeline)*.project.unique().sort({
            it.name.toUpperCase()
        })
    }

    /**
     * Creates a Project and grants permissions to Groups which have read/write privileges for Projects.
     * @param name
     * @param dirName
     * @param realm
     * @return The created project
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Project createProject(String name, String dirName, Realm realm, String alignmentDeciderBeanName, List<String> categoryNames, QcThresholdHandling qcThresholdHandling) {
        // check that our dirName is relative to the configured data root.
        Path rootPath = configService.getRootPath().toPath()
        List<String> rootPathElements = rootPath.toList()*.toString()
        assert rootPathElements.every { !dirName.startsWith("${it}${File.separator}") }:
                "project directory (${dirName}) contains (partial) data processing root path (${rootPath})"

        Project project = new Project(
                name: name,
                dirName: dirName,
                realm: realm,
                alignmentDeciderBeanName: alignmentDeciderBeanName,
                projectCategories: categoryNames.collect { exactlyOneElement(ProjectCategory.findAllByName(it)) },
                qcThresholdHandling: qcThresholdHandling,
        )

        project = project.save(flush: true)
        assert (project != null)

        return project
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Project createProject(ProjectParams projectParams) {
        assert OtpPath.isValidPathComponent(projectParams.unixGroup): "unixGroup '${projectParams.unixGroup}' contains invalid characters"
        Project project = createProject(projectParams.name, projectParams.dirName, projectParams.realm, projectParams.alignmentDeciderBeanName, projectParams.categoryNames, projectParams.qcThresholdHandling)
        project.phabricatorAlias = projectParams.phabricatorAlias
        project.dirAnalysis = projectParams.dirAnalysis
        project.processingPriority = projectParams.processingPriority.priority
        project.hasToBeCopied = projectParams.copyFiles
        project.fingerPrinting = projectParams.fingerPrinting
        project.nameInMetadataFiles = projectParams.nameInMetadataFiles
        project.setProjectGroup(ProjectGroup.findByName(projectParams.projectGroup))
        project.sampleIdentifierParserBeanName = projectParams.sampleIdentifierParserBeanName
        project.description = projectParams.description
        project.unixGroup = projectParams.unixGroup
        project.costCenter = projectParams.costCenter
        project.tumorEntity = projectParams.tumorEntity
        assert project.save(flush: true, failOnError: true)

        createProjectDirectoryIfNeeded(project, projectParams)

        if (projectParams.projectInfoFile) {
            createProjectInfoAndUploadFile(project, projectParams.projectInfoFile)
        }

        return project
    }

    private void createProjectDirectoryIfNeeded(Project project, ProjectParams projectParams) {
        File projectDirectory = project.getProjectDirectory()
        if (projectDirectory.exists()) {
            PosixFileAttributes attrs = Files.readAttributes(projectDirectory.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS)
            if (attrs.group().toString() == projectParams.unixGroup) {
                return
            }
        }
        executeScript(buildCreateProjectDirectory(projectParams.unixGroup, projectDirectory), project)
        FileSystem fs = fileSystemService.getFilesystemForConfigFileChecksForRealm(project.realm)
        FileService.waitUntilExists(fs.getPath(projectDirectory.absolutePath))
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void createProjectInfoAndUploadFile(Project project, MultipartFile file) {
        assert project: "No Project given"
        assert file: "No File given"
        assert !file.isEmpty(): "Empty file"
        assert file.getSize() <= PROJECT_INFO_MAX_SIZE: "Too big"
        ProjectInfo projectInfo = createProjectInfo(project, file.originalFilename)
        uploadProjectInfoToProjectFolder(projectInfo, file.getBytes())
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    byte[] getProjectInfoContent(ProjectInfo projectInfo) {
        assert projectInfo: "No ProjectInfo given"
        FileSystem fs = fileSystemService.getFilesystemForConfigFileChecksForRealm(projectInfo.project.realm)
        Path file = fs.getPath(projectInfo.getPath())

        return Files.exists(file) ? file.bytes : []
    }

    private ProjectInfo createProjectInfo(Project project, String fileName) {
        ProjectInfo projectInfo = new ProjectInfo([fileName: fileName])
        project.addToProjectInfos(projectInfo)
        project.save(flush: true)
        return projectInfo
    }

    private void uploadProjectInfoToProjectFolder(ProjectInfo projectInfo, byte[] content) {
        FileSystem fs = fileSystemService.getFilesystemForConfigFileChecksForRealm(projectInfo.project.realm)
        Path projectDirectory = fs.getPath(projectInfo.project.getProjectDirectory().toString())
        Path projectInfoDirectory = projectDirectory.resolve(PROJECT_INFO)

        Path file = projectInfoDirectory.resolve(projectInfo.fileName)

        fileService.createFileWithContent(file, content, [
                PosixFilePermission.OWNER_READ,
        ] as Set)
    }

    static class ProjectParams {
        String name
        String phabricatorAlias
        String dirName
        String dirAnalysis
        Realm realm
        String alignmentDeciderBeanName
        List<String> categoryNames
        String unixGroup
        String projectGroup
        String nameInMetadataFiles
        boolean copyFiles
        boolean fingerPrinting
        String costCenter
        String description
        SampleIdentifierParserBeanName sampleIdentifierParserBeanName
        QcThresholdHandling qcThresholdHandling
        ProcessingPriority processingPriority
        TumorEntity tumorEntity
        MultipartFile projectInfoFile
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateCategory(List<String> categoryNames, Project project) {
        updateProjectField(
                categoryNames.collect { exactlyOneElement(ProjectCategory.findAllByName(it)) },
                "projectCategories",
                project
        )
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    <T> void updateProjectField(T fieldValue, String fieldName, Project project) {
        assert fieldName && [
                "costCenter",
                "description",
                "dirAnalysis",
                "nameInMetadataFiles",
                "processingPriority",
                "projectCategories",
                "snv",
                "tumorEntity",
                "sampleIdentifierParserBeanName",
                "qcThresholdHandling",
        ].contains(fieldName)

        project."${fieldName}" = fieldValue
        project.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    void updatePhabricatorAlias(String value, Project project) {
        project.phabricatorAlias = value
        project.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void configureNoAlignmentDeciderProject(Project project) {
        deprecateAllReferenceGenomesByProject(project)
        project.alignmentDeciderBeanName = "noAlignmentDecider"
        project.save(flush: true, failOnError: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void configureDefaultOtpAlignmentDecider(Project project, String referenceGenomeName) {
        deprecateAllReferenceGenomesByProject(project)
        project.alignmentDeciderBeanName = "defaultOtpAlignmentDecider"
        project.save(flush: true, failOnError: true)
        ReferenceGenome referenceGenome = exactlyOneElement(ReferenceGenome.findAllByName(referenceGenomeName))
        SeqType seqType_wgp = SeqTypeService.getWholeGenomePairedSeqType()
        SeqType seqType_exome = SeqTypeService.getExomePairedSeqType()
        [seqType_wgp, seqType_exome].each { seqType ->
            ReferenceGenomeProjectSeqType refSeqType = new ReferenceGenomeProjectSeqType()
            refSeqType.project = project
            refSeqType.seqType = seqType
            refSeqType.referenceGenome = referenceGenome
            refSeqType.sampleType = null
            refSeqType.save(flush: true, failOnError: true)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void configurePanCanAlignmentDeciderProject(PanCanAlignmentConfiguration panCanAlignmentConfiguration) {
        deprecatedReferenceGenomeProjectSeqTypeAndSetDecider(panCanAlignmentConfiguration)

        ReferenceGenome referenceGenome = exactlyOneElement(ReferenceGenome.findAllByName(panCanAlignmentConfiguration.referenceGenome))

        assert panCanAlignmentConfiguration.mergeTool in MergeConstants.ALL_MERGE_TOOLS: "Invalid merge tool: '${panCanAlignmentConfiguration.mergeTool}', possible values: ${MergeConstants.ALL_MERGE_TOOLS}"

        assert OtpPath.isValidPathComponent(panCanAlignmentConfiguration.pluginName): "pluginName '${panCanAlignmentConfiguration.pluginName}' is an invalid path component"
        assert OtpPath.isValidPathComponent(panCanAlignmentConfiguration.pluginVersion): "pluginVersion '${panCanAlignmentConfiguration.pluginVersion}' is an invalid path component"
        assert OtpPath.isValidPathComponent(panCanAlignmentConfiguration.baseProjectConfig): "baseProjectConfig '${panCanAlignmentConfiguration.baseProjectConfig}' is an invalid path component"
        assert panCanAlignmentConfiguration.configVersion ==~ RoddyWorkflowConfig.CONFIG_VERSION_PATTERN: "configVersion '${panCanAlignmentConfiguration.configVersion}' has not expected pattern: ${RoddyWorkflowConfig.CONFIG_VERSION_PATTERN}"

        if (!panCanAlignmentConfiguration.seqType.isWgbs()) {
            List<String> allBwaMemVersions = processingOptionService.findOptionAsList(OptionName.PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_AVAILABLE)
            assert panCanAlignmentConfiguration.bwaMemVersion in allBwaMemVersions: "Invalid bwa_mem version: '${panCanAlignmentConfiguration.bwaMemVersion}', possible values: ${allBwaMemVersions}"
        } else {
            panCanAlignmentConfiguration.adapterTrimmingNeeded = true
        }
        if (panCanAlignmentConfiguration.seqType.isChipSeq()) {
            panCanAlignmentConfiguration.adapterTrimmingNeeded = true
        }
        if (panCanAlignmentConfiguration.mergeTool == MergeConstants.MERGE_TOOL_SAMBAMBA) {
            List<String> allSambambaVersions = processingOptionService.findOptionAsList(OptionName.PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_AVAILABLE)
            assert panCanAlignmentConfiguration.sambambaVersion in allSambambaVersions: "Invalid sambamba version: '${panCanAlignmentConfiguration.sambambaVersion}', possible values: ${allSambambaVersions}"
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
        refSeqType.save(flush: true, failOnError: true)

        alignmentHelper(panCanAlignmentConfiguration, pipeline, RoddyPanCanConfigTemplate.createConfig(panCanAlignmentConfiguration), panCanAlignmentConfiguration.adapterTrimmingNeeded)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void configureRnaAlignmentConfig(RoddyConfiguration rnaAlignmentConfiguration) {
        assert OtpPath.isValidPathComponent(rnaAlignmentConfiguration.pluginName): "pluginName '${rnaAlignmentConfiguration.pluginName}' is an invalid path component"
        assert OtpPath.isValidPathComponent(rnaAlignmentConfiguration.pluginVersion): "pluginVersion '${rnaAlignmentConfiguration.pluginVersion}' is an invalid path component"
        assert OtpPath.isValidPathComponent(rnaAlignmentConfiguration.baseProjectConfig): "baseProjectConfig '${rnaAlignmentConfiguration.baseProjectConfig}' is an invalid path component"
        assert rnaAlignmentConfiguration.configVersion ==~ RoddyWorkflowConfig.CONFIG_VERSION_PATTERN: "configVersion '${rnaAlignmentConfiguration.configVersion}' has not expected pattern: ${RoddyWorkflowConfig.CONFIG_VERSION_PATTERN}"

        Pipeline pipeline = CollectionUtils.exactlyOneElement(Pipeline.findAllByTypeAndName(
                Pipeline.Type.ALIGNMENT,
                Pipeline.Name.RODDY_RNA_ALIGNMENT,
        ))

        alignmentHelper(rnaAlignmentConfiguration, pipeline, RoddyRnaConfigTemplate.createConfig(rnaAlignmentConfiguration, pipeline.name), true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void invalidateProjectConfig(Project project, SeqType seqType, Pipeline pipeline) {
        ConfigPerProjectAndSeqType config = atMostOneElement(ConfigPerProjectAndSeqType.findAllByProjectAndSeqTypeAndPipelineAndObsoleteDate(project, seqType, pipeline, null))
        if (config) {
            config.makeObsolete()
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
            latest?.makeObsolete()

            try {
                new RunYapsaConfig(
                        project: project,
                        seqType: seqType,
                        pipeline: pipeline,
                        programVersion: programVersion,
                        previousConfig: latest,
                ).save(failOnError: true)
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

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Errors createOrUpdateCellRangerConfig(Project project, SeqType seqType, String programVersion, ReferenceGenomeIndex referenceGenomeIndex) {
        Pipeline pipeline = Pipeline.findByName(Pipeline.Name.CELL_RANGER)
        ConfigPerProjectAndSeqType latest = getLatestCellRangerConfig(project, seqType)

        latest?.makeObsolete()
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
            deprecateAllReferenceGenomesByProjectAndSeqTypeAndSampleTypes(rnaAlignmentConfiguration.project, rnaAlignmentConfiguration.seqType, rnaAlignmentConfiguration.sampleTypes)
        } else if (rnaAlignmentConfiguration.deprecateConfigurations) {
            deprecatedReferenceGenomeProjectSeqTypeAndSetDecider(rnaAlignmentConfiguration)
        } else {
            deprecateReferenceGenomeByProjectAndSeqTypeAndNoSampleType(rnaAlignmentConfiguration.project, rnaAlignmentConfiguration.seqType)
        }
        rnaAlignmentConfiguration.project.alignmentDeciderBeanName = AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT.beanName

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
                alignmentProperties[it.toolName.name.contains(GENOME_STAR_INDEX) ? GENOME_STAR_INDEX : it.toolName.name] = referenceGenomeIndexService.getFile(it).absolutePath
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
            refSeqType.alignmentProperties = alignmentProperties.collect { String key, String value ->
                new ReferenceGenomeProjectSeqTypeAlignmentProperty(name: key, value: value, referenceGenomeProjectSeqType: refSeqType)
            } as Set
            refSeqType.save(flush: true, failOnError: true)
        }
    }

    private void deprecatedReferenceGenomeProjectSeqTypeAndSetDecider(ProjectSeqTypeReferenceGenomeConfiguration config) {
        if (config.project.alignmentDeciderBeanName == AlignmentDeciderBeanName.OTP_ALIGNMENT.beanName) {
            deprecateAllReferenceGenomesByProject(config.project)
        } else {
            deprecateAllReferenceGenomesByProjectAndSeqType(config.project, config.seqType)
        }
        config.project.alignmentDeciderBeanName = AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT.beanName
        config.project.save(flush: true, failOnError: true)
    }

    private ReferenceGenomeProjectSeqType createReferenceGenomeProjectSeqType(RoddyConfiguration config, ReferenceGenome referenceGenome) {
        return new ReferenceGenomeProjectSeqType(
                project: config.project,
                seqType: config.seqType,
                referenceGenome: referenceGenome,
                sampleType: null,
        )
    }

    private void alignmentHelper(RoddyConfiguration alignmentConfiguration, Pipeline pipeline, String xmlConfig, boolean adapterTrimmingNeeded) {
        File projectDirectory = alignmentConfiguration.project.getProjectDirectory()
        assert projectDirectory.exists()

        File configFilePath = RoddyWorkflowConfig.getStandardConfigFile(
                alignmentConfiguration.project,
                pipeline.name,
                alignmentConfiguration.seqType,
                alignmentConfiguration.pluginVersion,
                alignmentConfiguration.configVersion,
        )
        File configDirectory = configFilePath.parentFile

        executeScript(getScriptBash(configDirectory, xmlConfig, configFilePath), alignmentConfiguration.project)

        roddyWorkflowConfigService.importProjectConfigFile(
                alignmentConfiguration.project,
                alignmentConfiguration.seqType,
                "${alignmentConfiguration.pluginName}:${alignmentConfiguration.pluginVersion}",
                pipeline,
                configFilePath.path,
                alignmentConfiguration.configVersion,
                adapterTrimmingNeeded,
        )
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void copyPanCanAlignmentXml(Project basedProject, SeqType seqType, Project project) {
        ReferenceGenomeProjectSeqType refSeqType = ReferenceGenomeProjectSeqType.getConfiguredReferenceGenomeProjectSeqType(basedProject, seqType)
        assert refSeqType

        deprecateAllReferenceGenomesByProjectAndSeqType(project, seqType)

        ReferenceGenomeProjectSeqType refSeqType1 = new ReferenceGenomeProjectSeqType()
        refSeqType1.project = project
        refSeqType1.seqType = refSeqType.seqType
        refSeqType1.referenceGenome = refSeqType.referenceGenome
        refSeqType1.sampleType = refSeqType.sampleType
        refSeqType1.statSizeFileName = refSeqType.statSizeFileName
        refSeqType1.save(flush: true)

        File projectDirectory = project.getProjectDirectory()
        assert projectDirectory.exists()

        Pipeline pipeline = CollectionUtils.exactlyOneElement(Pipeline.findAllByTypeAndName(
                Pipeline.Type.ALIGNMENT,
                Pipeline.Name.PANCAN_ALIGNMENT,
        ))

        RoddyWorkflowConfig roddyWorkflowConfigBasedProject = RoddyWorkflowConfig.getLatestForProject(basedProject, seqType, pipeline)
        File configFilePathBasedProject = new File(roddyWorkflowConfigBasedProject.configFilePath)
        File configDirectory = RoddyWorkflowConfig.getStandardConfigDirectory(project, roddyWorkflowConfigBasedProject.pipeline.name)

        executeScript(getCopyBashScript(configDirectory, configFilePathBasedProject, executionHelperService.getGroup(projectDirectory)), project)

        File configFilePath = new File(configDirectory, configFilePathBasedProject.name)
        roddyWorkflowConfigService.importProjectConfigFile(
                project,
                roddyWorkflowConfigBasedProject.seqType,
                roddyWorkflowConfigBasedProject.pluginVersion,
                roddyWorkflowConfigBasedProject.pipeline,
                configFilePath.path,
                roddyWorkflowConfigBasedProject.configVersion,
                roddyWorkflowConfigBasedProject.adapterTrimmingNeeded,
        )
        project.alignmentDeciderBeanName = AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT.beanName
        project.save(flush: true)
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
        assert OtpPath.isValidPathComponent(configuration.pluginVersion): "pluginVersion '${configuration.pluginVersion}' is an invalid path component"
        assert OtpPath.isValidPathComponent(configuration.baseProjectConfig): "baseProjectConfig '${configuration.baseProjectConfig}' is an invalid path component"
        assert configuration.configVersion ==~ RoddyWorkflowConfig.CONFIG_VERSION_PATTERN: "configVersion '${configuration.configVersion}' has not expected pattern: ${RoddyWorkflowConfig.CONFIG_VERSION_PATTERN}"

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

        File projectDirectory = configuration.project.getProjectDirectory()
        assert projectDirectory.exists()

        File configFilePath = RoddyWorkflowConfig.getStandardConfigFile(
                configuration.project,
                pipeline.name,
                configuration.seqType,
                configuration.pluginVersion,
                configuration.configVersion,
        )
        File configDirectory = configFilePath.parentFile

        executeScript(getScriptBash(configDirectory, xmlConfig, configFilePath), configuration.project)

        return roddyWorkflowConfigService.importProjectConfigFile(
                configuration.project,
                configuration.seqType,
                "${configuration.pluginName}:${configuration.pluginVersion}",
                pipeline,
                configFilePath.path,
                configuration.configVersion,
        )
    }

    Result<ReferenceGenome, String> checkReferenceGenomeForAceseq(Project project, SeqType seqType) {
        return Result.ofNullable(project, "project must not be null")
                .map { Project p ->
            ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(
                    project, seqType)
        }
        .ensure({ List<ReferenceGenomeProjectSeqType> rgpsts -> rgpsts.size() == 1 }, "No reference genome set.")
                .map { List<ReferenceGenomeProjectSeqType> rgpsts -> rgpsts.first().referenceGenome }
                .ensure({ ReferenceGenome referenceGenome -> referenceGenome in aceseqService.checkReferenceGenomeMap()['referenceGenomes'] }, "Reference genome is not compatible with ACESeq.")
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
        return Result.ofNullable(project, "project must not be null")
                .map { Project p ->
            ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(
                    project, seqType)
        }
        .ensure({ List<ReferenceGenomeProjectSeqType> rgpsts -> rgpsts.size() == 1 }, "No reference genome set.")
                .map { List<ReferenceGenomeProjectSeqType> rgpsts -> rgpsts.first().referenceGenome }
                .ensure({ ReferenceGenome referenceGenome -> referenceGenome in sophiaService.checkReferenceGenomeMap()['referenceGenomes'] }, "Reference genome is not compatible with SOPHIA.")
    }

    private String getScriptBash(File configDirectory, String xmlConfig, File configFilePath) {
        String md5 = HelperUtils.getRandomMd5sum()
        String createConfigDirectory = ''

        if (!configDirectory.exists()) {
            createConfigDirectory = """\
mkdir -p -m 2750 ${configDirectory}
"""
        }

        return """\

${createConfigDirectory}

cat <<${md5} > ${configFilePath}
${xmlConfig.replaceAll(/\$/, /\\\$/)}
${md5}

chmod 0440 ${configFilePath}

"""
    }

    private String getCopyBashScript(File configDirectory, File configFilePathBasedProject, String unixGroup) {
        String createConfigDirectory = ''

        if (!configDirectory.exists()) {
            createConfigDirectory = """\
mkdir -p -m 2750 ${configDirectory}
"""
        }

        return """\

${createConfigDirectory}

cp -a ${configFilePathBasedProject} ${configDirectory}/

chgrp ${unixGroup} ${configDirectory}/*

"""
    }

    private String buildCreateProjectDirectory(String unixGroup, File projectDirectory) {
        return """\
mkdir -p -m 2750 ${projectDirectory}

chgrp ${unixGroup} ${projectDirectory}
chmod 2750 ${projectDirectory}
"""
    }

    private void executeScript(String input, Project project) {
        Realm realm = project.realm
        String script = """\
#!/bin/bash
set -evx

umask 0027

${input}

echo 'OK'
"""
        LogThreadLocal.withThreadLog(System.out) {
            assert remoteShellHelper.executeCommand(realm, script).trim() == "OK"
        }
    }

    private void deprecateAllReferenceGenomesByProject(Project project) {
        Set<ReferenceGenomeProjectSeqType> referenceGenomeProjectSeqTypes = ReferenceGenomeProjectSeqType.findAllByProjectAndDeprecatedDateIsNull(project)
        referenceGenomeProjectSeqTypes*.deprecatedDate = new Date()
        referenceGenomeProjectSeqTypes*.save(flush: true, failOnError: true)
    }

    private void deprecateAllReferenceGenomesByProjectAndSeqType(Project project, SeqType seqType) {
        Set<ReferenceGenomeProjectSeqType> referenceGenomeProjectSeqTypes = ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndDeprecatedDateIsNull(project, seqType)
        referenceGenomeProjectSeqTypes*.deprecatedDate = new Date()
        referenceGenomeProjectSeqTypes*.save(flush: true, failOnError: true)
    }

    private void deprecateReferenceGenomeByProjectAndSeqTypeAndNoSampleType(Project project, SeqType seqType) {
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.findByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(project, seqType)
        referenceGenomeProjectSeqType?.deprecatedDate = new Date()
        referenceGenomeProjectSeqType?.save(flush: true, failOnError: true)
    }

    private void deprecateAllReferenceGenomesByProjectAndSeqTypeAndSampleTypes(Project project, SeqType seqType, List<SampleType> sampleTypes) {
        Set<ReferenceGenomeProjectSeqType> referenceGenomeProjectSeqTypes = ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeInListAndDeprecatedDateIsNull(project, seqType, sampleTypes)
        referenceGenomeProjectSeqTypes*.deprecatedDate = new Date()
        referenceGenomeProjectSeqTypes*.save(flush: true, failOnError: true)
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
    String pluginVersion
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
