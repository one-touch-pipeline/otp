package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddy.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.utils.*
import grails.converters.*
import org.springframework.validation.*

class ConfigurePipelineController {

    ProjectService projectService

    def alignment(ConfigureAlignmentPipelineSubmitCommand cmd) {
        Pipeline pipeline = CollectionUtils.exactlyOneElement(Pipeline.findAllByTypeAndName(
                Pipeline.Type.ALIGNMENT,
                Pipeline.Name.PANCAN_ALIGNMENT,
        ))

        List<Project> projects = projectService.getAllProjectsWithCofigFile(cmd.seqType, pipeline)

        Map result = checkErrorsIfSubmitted(cmd, pipeline)
        if (!result) {
            PanCanAlignmentConfiguration panCanAlignmentConfiguration = new PanCanAlignmentConfiguration([
                    project          : cmd.project,
                    seqType          : cmd.seqType,
                    referenceGenome  : cmd.referenceGenome,
                    statSizeFileName : cmd.statSizeFileName,
                    mergeTool        : cmd.mergeTool,
                    pluginName       : cmd.pluginName,
                    pluginVersion    : cmd.pluginVersion,
                    baseProjectConfig: cmd.baseProjectConfig,
                    configVersion    : cmd.config,
                    bwaMemVersion    : cmd.bwaMemVersion,
                    sambambaVersion  : cmd.sambambaVersion,
            ])
            projectService.configurePanCanAlignmentDeciderProject(panCanAlignmentConfiguration)
            redirect(controller: "projectOverview", action: "specificOverview", params: [project: cmd.project.name])
        }

        if (cmd.copy) {
            projectService.copyPanCanAlignmentXml(cmd.basedProject, cmd.seqType, cmd.project)
            redirect(controller: "projectOverview", action: "specificOverview", params: [project: cmd.project.name])
        }

        String defaultPluginName = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_RODDY_ALIGNMENT_PLUGIN_NAME, cmd.seqType.roddyName, null)
        String defaultPluginVersion = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_RODDY_ALIGNMENT_PLUGIN_VERSION, cmd.seqType.roddyName, null)
        String defaultBaseProjectConfig = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_BASE_PROJECT_CONFIG, cmd.seqType.roddyName, null)
        String defaultReferenceGenome = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_DEFAULT_REFERENCE_GENOME, cmd.seqType.roddyName, null)
        String defaultMergeTool = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_DEFAULT_MERGE_TOOL, cmd.seqType.roddyName, null)
        List<String> allMergeTools = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_ALL_MERGE_TOOLS, cmd.seqType.roddyName, null).split(',')*.trim()
        List<String> allSambambaVersions = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_SAMBAMBA_VERSION_AVAILABLE, null, null).split(',')*.trim()
        String defaultSambambaVersion = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_SAMBAMBA_VERSION_DEFAULT, null, null)
        List<String> allBwaMemVersions = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_BWA_VERSION_AVAILABLE, null, null).split(',')*.trim()
        String defaultBwaMemVersion = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_BWA_VERSION_DEFAULT, null, null)


        assert allSambambaVersions.contains(defaultSambambaVersion)
        assert allBwaMemVersions.contains(defaultBwaMemVersion)
        assert MergeConstants.ALL_MERGE_TOOLS.contains(defaultMergeTool)
        assert MergeConstants.ALL_MERGE_TOOLS.containsAll(allMergeTools)
        assert ReferenceGenome.findByName(defaultReferenceGenome)

        result << getValues(cmd.project, cmd.seqType, pipeline)

        String referenceGenome = ReferenceGenomeProjectSeqType.findByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(cmd.project, cmd.seqType)?.referenceGenome?.name ?: defaultReferenceGenome
        List<String> referenceGenomes = ReferenceGenome.list(sort: "name", order: "asc")*.name

        assert cmd.project.getProjectDirectory().exists()

        result << [
                projects                : projects,

                referenceGenome         : referenceGenome,
                referenceGenomes        : referenceGenomes,
                defaultReferenceGenome  : defaultReferenceGenome,
                statSizeFileName        : null,

                mergeTool               : defaultMergeTool,
                mergeTools              : allMergeTools,
                defaultMergeTool        : defaultMergeTool,

                isWgbs                  : cmd.seqType.isWgbs(),

                bwaMemVersion           : defaultBwaMemVersion,
                bwaMemVersions          : allBwaMemVersions,
                defaultBwaMemVersion    : defaultBwaMemVersion,

                sambambaVersion         : defaultSambambaVersion,
                sambambaVersions        : allSambambaVersions,
                defaultSambambaVersion  : defaultSambambaVersion,

                pluginName              : defaultPluginName,
                defaultPluginName       : defaultPluginName,

                pluginVersion           : defaultPluginVersion,
                defaultPluginVersion    : defaultPluginVersion,

                baseProjectConfig       : defaultBaseProjectConfig,
                defaultBaseProjectConfig: defaultBaseProjectConfig,
        ]

        if (cmd.submit || cmd.copy) {
            result << [
                    referenceGenome  : cmd.referenceGenome,
                    statSizeFileName : cmd.statSizeFileName,
                    mergeTool        : cmd.mergeTool,
            ]
        }
        return result
    }

    def rnaAlignment(ConfigureRnaAlignmentPipelineSubmitCommand cmd) {
        Pipeline pipeline = CollectionUtils.exactlyOneElement(Pipeline.findAllByTypeAndName(
                Pipeline.Type.ALIGNMENT,
                Pipeline.Name.RODDY_RNA_ALIGNMENT,
        ))

        List<Project> projects = projectService.getAllProjectsWithCofigFile(cmd.seqType, pipeline)

        Map result = checkErrorsIfSubmitted(cmd, pipeline)
        if (!result) {
            RnaAlignmentConfiguration rnaAlignmentConfiguration = new RnaAlignmentConfiguration([
                    project              : cmd.project,
                    seqType              : cmd.seqType,
                    referenceGenome      : cmd.referenceGenome,
                    pluginName           : cmd.pluginName,
                    pluginVersion        : cmd.pluginVersion,
                    configVersion        : cmd.config,
                    geneModels           : cmd.geneModels,
                    referenceGenomeIndex : cmd.referenceGenomeIndex,
            ])
            projectService.configureRnaAlignmentDeciderProject(rnaAlignmentConfiguration)
            redirect(controller: "projectOverview", action: "specificOverview", params: [project: cmd.project.name])
        }

        String defaultPluginName = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_RODDY_ALIGNMENT_PLUGIN_NAME, cmd.seqType.roddyName, null)
        String defaultPluginVersion = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_RODDY_ALIGNMENT_PLUGIN_VERSION, cmd.seqType.roddyName, null)
        String defaultBaseProjectConfig = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_BASE_PROJECT_CONFIG, cmd.seqType.roddyName, null)
        String defaultReferenceGenome = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_DEFAULT_REFERENCE_GENOME, cmd.seqType.roddyName, null)

        assert ReferenceGenome.findByName(defaultReferenceGenome)

        result << getValues(cmd.project, cmd.seqType, pipeline)

        String referenceGenome = ReferenceGenomeProjectSeqType.findByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(cmd.project, cmd.seqType)?.referenceGenome?.name ?: defaultReferenceGenome
        List<String> referenceGenomes = ReferenceGenome.list(sort: "name", order: "asc")*.name

        assert cmd.project.getProjectDirectory().exists()

        result << [
                projects                : projects,
                toolNames               : ToolName.findAllByType(ToolName.Type.RNA)*.name,

                referenceGenome         : referenceGenome,
                referenceGenomes        : referenceGenomes,
                defaultReferenceGenome  : defaultReferenceGenome,
                indexToolVersion        : null,
                geneModel               : null,

                pluginName              : defaultPluginName,
                defaultPluginName       : defaultPluginName,

                pluginVersion           : defaultPluginVersion,
                defaultPluginVersion    : defaultPluginVersion,

                baseProjectConfig       : defaultBaseProjectConfig,
                defaultBaseProjectConfig: defaultBaseProjectConfig,
        ]

        if (cmd.submit) {
            result << [
                    referenceGenome  : cmd.referenceGenome,
                    ]
        }
        return result
    }

    JSON getStatSizeFileNames(String referenceGenome) {
        Map data = [
                data: ReferenceGenome.findByName(referenceGenome)?.getStatSizeFileNames()*.name
        ]
        render data as JSON
    }

    JSON getGeneModels(String referenceGenome) {
        Map data = [
                data: GeneModel.findAllByReferenceGenome(ReferenceGenome.findByName(referenceGenome))
        ]
        render data as JSON
    }

    JSON getToolVersions(String referenceGenome) {
        List<ToolName> toolNames = ToolName.findAllByType(ToolName.Type.RNA)
        ReferenceGenome refGenome = ReferenceGenome.findByName(referenceGenome)
        Map data = [:]
        toolNames.each {
            data << [
                    (it.name): ReferenceGenomeIndex.findAllByReferenceGenomeAndToolName(refGenome, it)
            ]
        }
        render data as JSON
    }

    def snv(ConfigureSnvPipelineSubmitCommand cmd) {
        Pipeline pipeline = CollectionUtils.exactlyOneElement(Pipeline.findAllByTypeAndName(
                Pipeline.Type.SNV,
                Pipeline.Name.RODDY_SNV,
        ))

        Map result = checkErrorsIfSubmitted(cmd, pipeline)
        if (!result) {
            SnvPipelineConfiguration snvPipelineConfiguration = new SnvPipelineConfiguration([
                    project          : cmd.project,
                    seqType          : cmd.seqType,
                    pluginName       : cmd.pluginName,
                    pluginVersion    : cmd.pluginVersion,
                    baseProjectConfig: cmd.baseProjectConfig,
                    configVersion    : cmd.config,
            ])
            projectService.configureSnvPipelineProject(snvPipelineConfiguration)
            redirect(controller: "projectOverview", action: "specificOverview", params: [project: cmd.project.name])
        }

        String defaultPluginName = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_SNV_PIPELINE_PLUGIN_NAME, cmd.seqType.roddyName, null)
        String defaultPluginVersion = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_SNV_PIPELINE_PLUGIN_VERSION, cmd.seqType.roddyName, null)
        String defaultBaseProjectConfig = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_SNV_BASE_PROJECT_CONFIG, cmd.seqType.roddyName, null)

        result << getValues(cmd.project, cmd.seqType, pipeline)

        result << [
                pluginName              : defaultPluginName,
                defaultPluginName       : defaultPluginName,

                pluginVersion           : defaultPluginVersion,
                defaultPluginVersion    : defaultPluginVersion,

                baseProjectConfig       : defaultBaseProjectConfig,
                defaultBaseProjectConfig: defaultBaseProjectConfig,
        ]
        return result
    }


    def indel(ConfigureIndelPipelineSubmitCommand cmd) {
        Pipeline pipeline = CollectionUtils.exactlyOneElement(Pipeline.findAllByTypeAndName(
                Pipeline.Type.INDEL,
                Pipeline.Name.RODDY_INDEL,
        ))

        Map result = checkErrorsIfSubmitted(cmd, pipeline)
        if (!result) {
            IndelPipelineConfiguration indelPipelineConfiguration = new IndelPipelineConfiguration([
                    project          : cmd.project,
                    seqType          : cmd.seqType,
                    pluginName       : cmd.pluginName,
                    pluginVersion    : cmd.pluginVersion,
                    baseProjectConfig: cmd.baseProjectConfig,
                    configVersion    : cmd.config,
            ])

            projectService.configureIndelPipelineProject(indelPipelineConfiguration)
            redirect(controller: "projectOverview", action: "specificOverview", params: [project: cmd.project.name])
        }

        String defaultPluginName = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_INDEL_PIPELINE_PLUGIN_NAME, cmd.seqType.roddyName, null)
        String defaultPluginVersion = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_INDEL_PIPELINE_PLUGIN_VERSION, cmd.seqType.roddyName, null)
        String defaultBaseProjectConfig = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_INDEL_BASE_PROJECT_CONFIG, cmd.seqType.roddyName, null)

        result << getValues(cmd.project, cmd.seqType, pipeline)

        result << [

                pluginName              : defaultPluginName,
                defaultPluginName       : defaultPluginName,

                pluginVersion           : defaultPluginVersion,
                defaultPluginVersion    : defaultPluginVersion,

                baseProjectConfig       : defaultBaseProjectConfig,
                defaultBaseProjectConfig: defaultBaseProjectConfig,
        ]
        return result
    }

    private static Map checkErrorsIfSubmitted(ConfigurePipelineSubmitCommand cmd, Pipeline pipeline) {
        boolean hasErrors = false
        String message = ""

        if (cmd.submit) {
            hasErrors = cmd.hasErrors()
            boolean duplicateConfigVersion = false
            RoddyWorkflowConfig.findAllWhere([
                    project      : cmd.project,
                    seqType      : cmd.seqType,
                    pipeline     : pipeline,
                    pluginVersion: "${cmd.pluginName}:${cmd.pluginVersion}"

            ]).each({
                if (it.configVersion == cmd.config) {
                    duplicateConfigVersion = true
                }
            })
            if (hasErrors) {
                FieldError errors = cmd.errors.getFieldError()
                message = "'" + errors.getRejectedValue() + "' is not a valid value for '" + errors.getField() + "'. Error code: '" + errors.code + "'"
            } else if (duplicateConfigVersion) {
                hasErrors = true
                message = "'${cmd.config}' is not a valid value for 'Config Version'. Error code: 'duplicate'"
            } else {
                return [:] // == success
            }
            return [
                    message                 : message,
                    hasErrors               : hasErrors,
                    pluginName              : cmd.pluginName,
                    pluginVersion           : cmd.pluginVersion,
                    baseProjectConfig       : cmd.baseProjectConfig,
                    config                  : cmd.config,
            ]
        }
        return [
                message                 : message,
                hasErrors               : hasErrors,
        ]
    }

    private static Map getValues(Project project, SeqType seqType, Pipeline pipeline) {
        RoddyWorkflowConfig roddyWorkflowConfig = RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline)
        String configVersion = roddyWorkflowConfig?.configVersion
        if (configVersion) {
            Set<String> versions = configVersion.split("_")
            final int MAIN_CONFIG_VERSION_INDEX = 0
            final int SUB_CONFIG_VERSION_INDEX = 1
            configVersion = versions[MAIN_CONFIG_VERSION_INDEX] + "_" + (versions[SUB_CONFIG_VERSION_INDEX].toInteger() + 1)
        } else {
            configVersion = "v1_0"
        }

        String lastRoddyConfig = (roddyWorkflowConfig?.configFilePath as File)?.text
        assert project.getProjectDirectory().exists()
        return [
                project                 : project,
                seqType                 : seqType,
                config                  : configVersion,
                lastRoddyConfig         : lastRoddyConfig,
        ]
    }
}

class ConfigureAlignmentPipelineSubmitCommand extends ConfigurePipelineSubmitCommand implements Serializable {
    Project basedProject

    String referenceGenome
    String statSizeFileName
    String mergeTool
    String bwaMemVersion
    String sambambaVersion
    String copy

    static constraints = {
        basedProject(nullable: true)
        copy(nullable: true)
        statSizeFileName(nullable: true, validator: { val, obj ->
            if (!val) {
                return 'Invalid statSizeFileName'
            }
            if (!(val ==~ ReferenceGenomeProjectSeqType.TAB_FILE_PATTERN)) {
                return 'Invalid file name pattern'
            }
        })
        bwaMemVersion(nullable: true, validator: { val, obj ->
            obj.seqType.isWgbs() ? true : val != null
        })
        sambambaVersion(nullable: true, validator: { val, obj ->
            obj.mergeTool != MergeConstants.MERGE_TOOL_SAMBAMBA ? true : val != null
        })
    }
}

class ConfigureRnaAlignmentPipelineSubmitCommand extends ConfigurePipelineSubmitCommand implements Serializable {
    String referenceGenome

    List<GeneModel> geneModels = []
    List<ReferenceGenomeIndex> referenceGenomeIndex = []

    void setGeneModelValues(List geneModelValues) {
        if (geneModelValues) {
            geneModelValues.each {
                if (it) {
                    geneModels.add(GeneModel.findById(it as long))
                }
            }
        }
    }

    void setToolVersionValue(List toolVersionValue) {
        if (toolVersionValue) {
            toolVersionValue.each {
                println it
                if (it) {
                    referenceGenomeIndex.add(ReferenceGenomeIndex.findById(it as long))
                }
            }
        }
    }
}

class ConfigureIndelPipelineSubmitCommand extends ConfigurePipelineSubmitCommand {
}

class ConfigureSnvPipelineSubmitCommand extends ConfigurePipelineSubmitCommand {
}

class ConfigurePipelineSubmitCommand implements Serializable {
    Project project
    SeqType seqType

    String pluginName
    String pluginVersion
    String baseProjectConfig
    String config
    String submit

    static constraints = {
        project(nullable: false)
        seqType(nullable: false)
        pluginName(nullable: true, validator: { val, obj ->
            if (!val) {
                return 'Empty'
            }
            if (!(OtpPath.isValidPathComponent(val))) {
                return 'Invalid path component'
            }
        })
        pluginVersion(nullable: true, validator: { val, obj ->
            if (!val) {
                return 'Empty'
            }
            if (!(OtpPath.isValidPathComponent(val))) {
                return 'Invalid path component'
            }
        })
        baseProjectConfig(nullable: false, blank: false, validator: { val, obj ->
            if (val && !OtpPath.isValidPathComponent(val)) {
                return "Invalid path component"
            }
        })
        config(nullable: true, validator: { val, obj ->
            if (!val) {
                return 'Empty'
            }
            if (!(val ==~ /^v\d+_\d+$/)) {
                return "Not a valid config version. Must look like 'v1_0'"
            }
        })
    }

    void setConfig(String config) {
        this.config = config?.trim()?.replaceAll(" +", " ")
    }
}
