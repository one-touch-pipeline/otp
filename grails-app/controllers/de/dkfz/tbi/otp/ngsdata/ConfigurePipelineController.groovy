package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.utils.*
import grails.converters.*
import groovy.transform.*
import org.springframework.validation.*

import java.nio.file.*

class ConfigurePipelineController {

    ProjectService projectService
    ProjectSelectionService projectSelectionService
    FileSystemService fileSystemService

    static allowedMethods = [
            // TODO: incomplete (OTP-2887)
            runYapsa                   : "GET",
            updateRunYapsa             : "POST",
            invalidateConfig           : "POST",
    ]


    def alignment(ConfigureAlignmentPipelineSubmitCommand cmd) {
        Pipeline pipeline = Pipeline.Name.PANCAN_ALIGNMENT.pipeline

        List<Project> projects = projectService.getAllProjectsWithConfigFile(cmd.seqType, pipeline)

        Map result = checkErrorsIfSubmitted(cmd, pipeline)
        if (!result) {
            PanCanAlignmentConfiguration panCanAlignmentConfiguration = new PanCanAlignmentConfiguration([
                    project              : cmd.project,
                    seqType              : cmd.seqType,
                    referenceGenome      : cmd.referenceGenome,
                    statSizeFileName     : cmd.statSizeFileName,
                    mergeTool            : cmd.mergeTool,
                    pluginName           : cmd.pluginName,
                    pluginVersion        : cmd.pluginVersion,
                    baseProjectConfig    : cmd.baseProjectConfig,
                    configVersion        : cmd.config,
                    bwaMemVersion        : cmd.bwaMemVersion,
                    sambambaVersion      : cmd.sambambaVersion,
                    adapterTrimmingNeeded: cmd.adapterTrimmingNeeded,
            ])
            projectService.configurePanCanAlignmentDeciderProject(panCanAlignmentConfiguration)

            flash.message = g.message(code: "configurePipeline.store.success")
            redirect(controller: "projectConfig")
        }

        if (cmd.copy) {
            projectService.copyPanCanAlignmentXml(cmd.basedProject, cmd.seqType, cmd.project)
            flash.message = g.message(code: "configurePipeline.copy.success")
            redirect(controller: "projectConfig")
        }

        String defaultPluginName = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_ALIGNMENT_PLUGIN_NAME, cmd.seqType.roddyName, null)
        String defaultPluginVersion = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_ALIGNMENT_PLUGIN_VERSION, cmd.seqType.roddyName, null)
        String defaultBaseProjectConfig = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_ALIGNMENT_BASE_PROJECT_CONFIG, cmd.seqType.roddyName, null)
        String defaultReferenceGenome = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME, cmd.seqType.roddyName, null)
        String defaultMergeTool = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_MERGE_TOOL, cmd.seqType.roddyName, null)
        List<String> allMergeTools = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_ALIGNMENT_ALL_MERGED_TOOLS, cmd.seqType.roddyName, null).split(',')*.trim()
        List<String> allSambambaVersions = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_AVAILABLE, null, null).split(',')*.trim()
        String defaultSambambaVersion = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_DEFAULT, null, null)
        List<String> allBwaMemVersions = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_AVAILABLE, null, null).split(',')*.trim()
        String defaultBwaMemVersion = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_DEFAULT, null, null)


        assert allSambambaVersions.contains(defaultSambambaVersion)
        assert allBwaMemVersions.contains(defaultBwaMemVersion)
        assert MergeConstants.ALL_MERGE_TOOLS.contains(defaultMergeTool)
        assert MergeConstants.ALL_MERGE_TOOLS.containsAll(allMergeTools)
        assert ReferenceGenome.findByName(defaultReferenceGenome)

        result << params
        result << getValues(cmd.project, cmd.seqType, pipeline)

        String referenceGenome = ReferenceGenomeProjectSeqType.findByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(cmd.project, cmd.seqType)?.referenceGenome?.name ?: defaultReferenceGenome
        List<String> referenceGenomes = ReferenceGenome.list(sort: "name", order: "asc")*.name

        assert cmd.project.getProjectDirectory().exists()

        result << [
                projects                : projects,
                pipeline                : pipeline,

                referenceGenome         : referenceGenome,
                referenceGenomes        : referenceGenomes,
                defaultReferenceGenome  : defaultReferenceGenome,
                statSizeFileName        : null,

                mergeTool               : defaultMergeTool,
                mergeTools              : allMergeTools,
                defaultMergeTool        : defaultMergeTool,

                isWgbs                  : cmd.seqType.isWgbs(),
                isChipSeq               : cmd.seqType.isChipSeq(),

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
                    referenceGenome : cmd.referenceGenome,
                    statSizeFileName: cmd.statSizeFileName,
                    mergeTool       : cmd.mergeTool,
            ]
        }
        return result
    }

    def rnaAlignment(BaseConfigurePipelineSubmitCommand cmd) {
        Pipeline pipeline = Pipeline.Name.RODDY_RNA_ALIGNMENT.pipeline

        List<Project> projects = projectService.getAllProjectsWithConfigFile(cmd.seqType, pipeline)

        Map result = [:]

        String defaultPluginName = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_ALIGNMENT_PLUGIN_NAME, cmd.seqType.roddyName, null)
        String defaultPluginVersion = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_ALIGNMENT_PLUGIN_VERSION, cmd.seqType.roddyName, null)
        String defaultBaseProjectConfig = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_ALIGNMENT_BASE_PROJECT_CONFIG, cmd.seqType.roddyName, null)
        String defaultReferenceGenome = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME, cmd.seqType.roddyName, null)
        String defaultGenomeStarIndex = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_ALIGNMENT_GENOME_STAR_INDEX, cmd.seqType.roddyName, null)

        assert ReferenceGenome.findByName(defaultReferenceGenome)

        String referenceGenome = ReferenceGenomeProjectSeqType.findByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(cmd.project, cmd.seqType)?.referenceGenome?.name ?: defaultReferenceGenome
        List<String> referenceGenomes = ReferenceGenome.list(sort: "name", order: "asc").findAll {
            ReferenceGenomeIndex.findByReferenceGenome(it) && GeneModel.findByReferenceGenome(it)
        }*.name

        List<SampleType> configuredSampleTypes = ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeIsNotNullAndDeprecatedDateIsNull(cmd.project, cmd.seqType)*.sampleType.unique().sort {
            it.name
        }
        List<SampleType> additionalUsedSampleTypes = (SeqTrack.createCriteria().list {
            projections {
                sample {
                    property('sampleType')
                }
            }
            sample {
                sampleType {
                    eq('specificReferenceGenome', SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC)
                }
                individual {
                    eq('project', cmd.project)
                }
            }
            eq('seqType', cmd.seqType)
        }.unique() - configuredSampleTypes).sort {
            it.name
        }
        List<SampleType> additionalPossibleSampleTypes = (SampleType.findAllBySpecificReferenceGenome(SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC) - configuredSampleTypes - additionalUsedSampleTypes).sort {
            it.name
        }

        assert cmd.project.getProjectDirectory().exists()

        result << [
                projects                : projects,
                pipeline                : pipeline,
                toolNames               : getToolNames(),

                referenceGenome         : referenceGenome,
                referenceGenomes        : referenceGenomes,
                defaultReferenceGenome  : defaultReferenceGenome,
                defaultGenomeStarIndex  : defaultGenomeStarIndex,
                indexToolVersion        : null,
                geneModel               : null,

                pluginName              : defaultPluginName,
                defaultPluginName       : defaultPluginName,

                pluginVersion           : defaultPluginVersion,
                defaultPluginVersion    : defaultPluginVersion,

                baseProjectConfig       : defaultBaseProjectConfig,
                defaultBaseProjectConfig: defaultBaseProjectConfig,

                possibleSampleTypes     : [
                        [
                                name  : 'configurePipeline.sample.type.configured',
                                values: configuredSampleTypes,
                                info  : 'configurePipeline.sample.type.configured.info',
                        ],
                        [
                                name  : 'configurePipeline.sample.type.used.in.project',
                                values: additionalUsedSampleTypes,
                                info  : 'configurePipeline.sample.type.used.in.project.info',
                        ],
                        [
                                name  : 'configurePipeline.sample.type.possible',
                                values: additionalPossibleSampleTypes,
                                info  : 'configurePipeline.sample.type.possible.info',
                        ],
                ],
        ]
        result << params
        result << getValues(cmd.project, cmd.seqType, pipeline)

        return result
    }

    def rnaAlignmentConfig(ConfigurePipelineSubmitCommand cmd) {
        Map result = checkErrorsIfSubmitted(cmd, Pipeline.Name.RODDY_RNA_ALIGNMENT.pipeline)
        if (!result) {
            RoddyConfiguration rnaAlignmentConfiguration = new RoddyConfiguration([
                    project          : cmd.project,
                    baseProjectConfig: cmd.baseProjectConfig,
                    seqType          : cmd.seqType,
                    pluginName       : cmd.pluginName,
                    pluginVersion    : cmd.pluginVersion,
                    configVersion    : cmd.config,
            ])
            projectService.configureRnaAlignmentConfig(rnaAlignmentConfiguration)
            result << [message: 'The config settings were saved successfully']
        }
        forward(action: "rnaAlignment", params: result)
    }

    def rnaAlignmentReferenceGenome(ConfigureRnaAlignmentSubmitCommand cmd) {
        boolean hasErrors = cmd.hasErrors()
        Map result = [hasErrors: hasErrors]

        if (hasErrors) {
            FieldError errors = cmd.errors.getFieldError()
            log.error(errors)
            result << [message: "'${errors.getRejectedValue()}' is not a valid value for '${errors.getField()}'. Error code: '${errors.code}'"]

        } else {
            RnaAlignmentReferenceGenomeConfiguration rnaConfiguration = new RnaAlignmentReferenceGenomeConfiguration([
                    project                : cmd.project,
                    seqType                : cmd.seqType,
                    referenceGenome        : cmd.referenceGenome,
                    geneModel              : cmd.geneModel,
                    referenceGenomeIndex   : cmd.referenceGenomeIndex,
                    mouseData              : cmd.mouseData,
                    deprecateConfigurations: cmd.deprecateConfigurations,
                    sampleTypes            : cmd.sampleTypeIds.collect {
                        return CollectionUtils.exactlyOneElement(SampleType.findAllById(it))
                    }
            ])
            projectService.configureRnaAlignmentReferenceGenome(rnaConfiguration)
            result << [message: 'The reference genome settings were saved successfully']
        }
        forward(action: "rnaAlignment", params: result)
    }

    JSON getStatSizeFileNames(String referenceGenome) {
        Map data = [
                data: ReferenceGenome.findByName(referenceGenome)?.getStatSizeFileNames()*.name,
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
        Map toolNamesData = [:]
        toolNames.each {
            toolNamesData << [
                    (it.name): ReferenceGenomeIndex.findAllByReferenceGenomeAndToolName(refGenome, it)
            ]
        }
        data << ["defaultGenomeStarIndex": ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_ALIGNMENT_GENOME_STAR_INDEX, SeqType.rnaPairedSeqType.roddyName, null)]
        data << ["data": toolNamesData]
        render data as JSON
    }

    def snv(ConfigurePipelineSubmitCommand cmd) {
        Pipeline pipeline = Pipeline.Name.RODDY_SNV.pipeline

        String defaultPluginName = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_SNV_PLUGIN_NAME, cmd.seqType.roddyName, null)
        String defaultPluginVersion = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_SNV_PLUGIN_VERSION, cmd.seqType.roddyName, null)
        String defaultBaseProjectConfig = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_SNV_BASE_PROJECT_CONFIG, cmd.seqType.roddyName, null)

        return createAnalysisConfig(cmd, pipeline, defaultPluginName, defaultPluginVersion, defaultBaseProjectConfig)
    }

    def indel(ConfigurePipelineSubmitCommand cmd) {
        Pipeline pipeline = Pipeline.Name.RODDY_INDEL.pipeline

        String defaultPluginName = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_INDEL_PLUGIN_NAME, cmd.seqType.roddyName, null)
        String defaultPluginVersion = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_INDEL_PLUGIN_VERSION, cmd.seqType.roddyName, null)
        String defaultBaseProjectConfig = ProcessingOptionService.findOption(OptionName.PIPELINE_RODDY_INDEL_PLUGIN_CONFIG, cmd.seqType.roddyName, null)

        return createAnalysisConfig(cmd, pipeline, defaultPluginName, defaultPluginVersion, defaultBaseProjectConfig)
    }

    def sophia(ConfigurePipelineSubmitCommand cmd) {
        Pipeline pipeline = Pipeline.Name.RODDY_SOPHIA.pipeline

        String defaultPluginName = ProcessingOptionService.findOption(OptionName.PIPELINE_SOPHIA_PLUGIN_NAME, cmd.seqType.roddyName, null)
        String defaultPluginVersion = ProcessingOptionService.findOption(OptionName.PIPELINE_SOPHIA_PLUGIN_VERSIONS, cmd.seqType.roddyName, null)
        String defaultBaseProjectConfig = ProcessingOptionService.findOption(OptionName.PIPELINE_SOPHIA_BASE_PROJECT_CONFIG, cmd.seqType.roddyName, null)

        return createAnalysisConfig(cmd, pipeline, defaultPluginName, defaultPluginVersion, defaultBaseProjectConfig)
    }

    def aceseq(ConfigurePipelineSubmitCommand cmd) {
        Pipeline pipeline = Pipeline.Name.RODDY_ACESEQ.pipeline

        String defaultPluginName = ProcessingOptionService.findOption(OptionName.PIPELINE_ACESEQ_PLUGIN_NAME, cmd.seqType.roddyName, null)
        String defaultPluginVersion = ProcessingOptionService.findOption(OptionName.PIPELINE_ACESEQ_PLUGIN_VERSION, cmd.seqType.roddyName, null)
        String defaultBaseProjectConfig = ProcessingOptionService.findOption(OptionName.PIPELINE_ACESEQ_BASE_PROJECT_CONFIG, cmd.seqType.roddyName, null)

        return createAnalysisConfig(cmd, pipeline, defaultPluginName, defaultPluginVersion, defaultBaseProjectConfig)
    }

    def runYapsa(BaseConfigurePipelineSubmitCommand cmd) {
        Pipeline pipeline = Pipeline.Name.RUN_YAPSA.pipeline

        RunYapsaConfig config = projectService.getLatestRunYapsaConfig(cmd.project, cmd.seqType)
        String currentVersion = config?.programVersion

        String defaultVersion = ProcessingOptionService.findOption(OptionName.PIPELINE_RUNYAPSA_DEFAULT_VERSION, null, null)
        List<String> availableVersions = ProcessingOptionService.findOptionSafe(OptionName.PIPELINE_RUNYAPSA_AVAILABLE_VERSIONS, null, null).split(",").collect { it.trim() }

        return [
                project: cmd.project,
                seqType: cmd.seqType,
                pipeline: pipeline,

                defaultVersion: defaultVersion,
                currentVersion: currentVersion,
                availableVersions: availableVersions,
        ]
    }

    def updateRunYapsa(ConfigureRunYapsaSubmitCommand cmd) {
        Errors errors = projectService.createOrUpdateRunYapsaConfig(cmd.project, cmd.seqType, cmd.programVersion)
        if (errors) {
            flash.message = g.message(code: "configurePipeline.store.failure")
            flash.errors = errors
            redirect action: "runYapsa"
        } else {
            flash.message = g.message(code: "configurePipeline.store.success")
            redirect controller: "projectConfig"
        }
    }

    def invalidateConfig(InvalidateConfigurationCommand cmd) {
        boolean hasErrors = cmd.hasErrors()

        if (hasErrors) {
            flash.message = g.message(code: "configurePipeline.invalidate.failure")
            flash.errors = errors
            redirect(action: cmd.originAction)
        } else {
            projectService.invalidateProjectConfig(cmd.project, cmd.seqType, cmd.pipeline)
            flash.message = g.message(code: "configurePipeline.invalidate.success")
            redirect(controller: "projectConfig")
        }
    }

    private Map createAnalysisConfig(ConfigurePipelineSubmitCommand cmd, Pipeline pipeline, String defaultPluginName, String defaultPluginVersion, String defaultBaseProjectConfig) {
        Map result = checkErrorsIfSubmitted(cmd, pipeline)
        if (!result) {
            RoddyConfiguration configuration = new RoddyConfiguration([
                    project          : cmd.project,
                    seqType          : cmd.seqType,
                    pluginName       : cmd.pluginName,
                    pluginVersion    : cmd.pluginVersion,
                    baseProjectConfig: cmd.baseProjectConfig,
                    configVersion    : cmd.config,
            ])

            switch (pipeline.name) {
                case Pipeline.Name.RODDY_ACESEQ:
                    projectService.configureAceseqPipelineProject(configuration)
                    break
                case Pipeline.Name.RODDY_INDEL:
                    projectService.configureIndelPipelineProject(configuration)
                    break
                case Pipeline.Name.RODDY_SNV:
                    projectService.configureSnvPipelineProject(configuration)
                    break
                case Pipeline.Name.RODDY_SOPHIA:
                    projectService.configureSophiaPipelineProject(configuration)
                    break
            }

            flash.message = g.message(code: "configurePipeline.store.success")
            redirect(controller: "projectConfig")
        }

        result << params
        result << getValues(cmd.project, cmd.seqType, pipeline)

        result << [
                pipeline                : pipeline,

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
                    pluginVersion: "${cmd.pluginName}:${cmd.pluginVersion}",
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
                    message          : message,
                    hasErrors        : hasErrors,
                    pluginName       : cmd.pluginName,
                    pluginVersion    : cmd.pluginVersion,
                    baseProjectConfig: cmd.baseProjectConfig,
                    config           : cmd.config,
            ]
        }
        return [
                message  : message,
                hasErrors: hasErrors,
        ]
    }

    private Map getValues(Project project, SeqType seqType, Pipeline pipeline) {
        String configVersion = CollectionUtils.atMostOneElement(
                RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineAndIndividualIsNull(project, seqType, pipeline, [sort: 'id', order: 'desc', max: 1]))?.configVersion
        if (configVersion) {
            Set<String> versions = configVersion.split("_")
            final int MAIN_CONFIG_VERSION_INDEX = 0
            final int SUB_CONFIG_VERSION_INDEX = 1
            configVersion = versions[MAIN_CONFIG_VERSION_INDEX] + "_" + (versions[SUB_CONFIG_VERSION_INDEX].toInteger() + 1)
        } else {
            configVersion = "v1_0"
        }

        String latestRoddyConfig = ""
        RoddyWorkflowConfig config = RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline)
        if (config) {
            FileSystem fs = fileSystemService.getFilesystemForConfigFileChecksForRealm(project.realm)
            latestRoddyConfig = fs.getPath(config.configFilePath).text
        }
        return [
                project        : project,
                seqType        : seqType,
                config         : configVersion,
                lastRoddyConfig: latestRoddyConfig,
        ]
    }

    private List getToolNames() {
        List<String> toolNames = ToolName.findAllByTypeAndNameNotIlike(ToolName.Type.RNA, "GENOME_STAR_INDEX%")*.name
        toolNames.add(ProjectService.GENOME_STAR_INDEX)
        return toolNames.sort()
    }
}

@ToString(includeNames = true, includeSuper = true)
class ConfigureAlignmentPipelineSubmitCommand extends ConfigurePipelineSubmitCommand implements Serializable {
    Project basedProject

    String referenceGenome
    String statSizeFileName
    String mergeTool
    String bwaMemVersion
    String sambambaVersion
    String copy
    boolean adapterTrimmingNeeded

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

@ToString(includeNames = true, includeSuper = true)
class ConfigureRnaAlignmentSubmitCommand extends BaseConfigurePipelineSubmitCommand {
    String referenceGenome
    boolean mouseData
    boolean deprecateConfigurations

    GeneModel geneModel
    List<ReferenceGenomeIndex> referenceGenomeIndex = []

    List<String> sampleTypeIds = []

    static constraints = {
        referenceGenome(nullable: false)
        geneModel(nullable: false)
        referenceGenomeIndex(nullable: false)
    }

    //used by gui to set referenceGenomeIndex
    void setToolVersionValue(List toolVersionValue) {
        if (toolVersionValue) {
            toolVersionValue.each {
                if (it) {
                    referenceGenomeIndex.add(ReferenceGenomeIndex.findById(it as long))
                }
            }
        }
    }
}


@ToString(includeNames = true, includeSuper = true)
class ConfigureRunYapsaSubmitCommand extends BaseConfigurePipelineSubmitCommand {
    String programVersion
}

@ToString(includeNames = true, includeSuper = true)
class ConfigurePipelineSubmitCommand extends BaseConfigurePipelineSubmitCommand {
    String pluginName
    String pluginVersion
    String baseProjectConfig
    String config
    String submit

    static constraints = {
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


@ToString(includeNames = true, includeSuper = true)
class InvalidateConfigurationCommand extends BaseConfigurePipelineSubmitCommand {
    Pipeline pipeline
    String originAction

    static constraints = {
        pipeline(nullable: false)
        originAction(nullable: false)
    }
}


@ToString(includeNames = true)
class BaseConfigurePipelineSubmitCommand implements Serializable {
    Project project
    SeqType seqType

    static constraints = {
        project(nullable: false)
        seqType(nullable: false)
    }
}
