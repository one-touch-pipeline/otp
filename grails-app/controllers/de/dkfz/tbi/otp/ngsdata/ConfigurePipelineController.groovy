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

import grails.converters.JSON
import groovy.transform.ToString
import org.springframework.validation.FieldError

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.utils.CollectionUtils

class ConfigurePipelineController implements ConfigurePipelineHelper {

    ProjectService projectService
    ProcessingOptionService processingOptionService

    static allowedMethods = [
            // TODO: incomplete (OTP-2887)
            runYapsa        : "GET",
            updateRunYapsa  : "POST",
            invalidateConfig: "POST",
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

            flash.message = new FlashMessage(g.message(code: "configurePipeline.store.success") as String)
            redirect(controller: "projectConfig")
            return
        }

        if (cmd.copy) {
            projectService.copyPanCanAlignmentXml(cmd.basedProject, cmd.project, cmd.seqType)
            flash.message = new FlashMessage(g.message(code: "configurePipeline.copy.success") as String)
            redirect(controller: "projectConfig")
        }

        String defaultPluginName = getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_NAME, cmd.seqType.roddyName)
        String defaultPluginVersion = getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_VERSION, cmd.seqType.roddyName)
        String defaultBaseProjectConfig = getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_BASE_PROJECT_CONFIG, cmd.seqType.roddyName)
        String defaultReferenceGenome = getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME, cmd.seqType.roddyName)
        String defaultMergeTool = getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_MERGE_TOOL, cmd.seqType.roddyName)
        List<String> allMergeTools = processingOptionService.findOptionAsList(OptionName.PIPELINE_RODDY_ALIGNMENT_ALL_MERGE_TOOLS, cmd.seqType.roddyName)
        List<String> allSambambaVersions = processingOptionService.findOptionAsList(OptionName.PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_AVAILABLE)
        String defaultSambambaVersion = getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_DEFAULT)
        List<String> allBwaMemVersions = processingOptionService.findOptionAsList(OptionName.PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_AVAILABLE)
        String defaultBwaMemVersion = getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_DEFAULT)


        assert allSambambaVersions.contains(defaultSambambaVersion)
        assert allBwaMemVersions.contains(defaultBwaMemVersion)
        assert MergeConstants.ALL_MERGE_TOOLS.contains(defaultMergeTool)
        assert MergeConstants.ALL_MERGE_TOOLS.containsAll(allMergeTools)
        assert ReferenceGenome.findByName(defaultReferenceGenome)

        result << params
        result << getValues(cmd.project, cmd.seqType, pipeline)

        String referenceGenome = ReferenceGenomeProjectSeqType.findByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(
                cmd.project, cmd.seqType)?.referenceGenome?.name ?: defaultReferenceGenome
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

        String defaultPluginName = getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_NAME, cmd.seqType.roddyName)
        String defaultPluginVersion = getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_VERSION, cmd.seqType.roddyName)
        String defaultBaseProjectConfig = getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_BASE_PROJECT_CONFIG, cmd.seqType.roddyName)
        String defaultReferenceGenome = getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME, cmd.seqType.roddyName)
        String defaultGenomeStarIndex = getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_RNA_DEFAULT_GENOME_STAR_INDEX)

        assert ReferenceGenome.findByName(defaultReferenceGenome)

        String referenceGenome = ReferenceGenomeProjectSeqType.findByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(
                cmd.project, cmd.seqType)?.referenceGenome?.name ?: defaultReferenceGenome
        List<String> referenceGenomes = ReferenceGenome.list(sort: "name", order: "asc").findAll {
            ReferenceGenomeIndex.findByReferenceGenome(it) && GeneModel.findByReferenceGenome(it)
        }*.name

        List<SampleType> configuredSampleTypes = ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeIsNotNullAndDeprecatedDateIsNull(
                cmd.project, cmd.seqType)*.sampleType.unique().sort { it.name }
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
        List<SampleType> additionalPossibleSampleTypes = (SampleType.findAllBySpecificReferenceGenome(
                SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC) - configuredSampleTypes - additionalUsedSampleTypes).sort { it.name }

        assert cmd.project.getProjectDirectory().exists()

        result << [
                projects                : projects,
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
                    },
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
        data << ["defaultGenomeStarIndex": getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_RNA_DEFAULT_GENOME_STAR_INDEX)]
        data << ["data": toolNamesData]
        render data as JSON
    }

    def invalidateConfig(InvalidateConfigurationCommand cmd) {
        boolean hasErrors = cmd.hasErrors()

        if (hasErrors) {
            flash.message = new FlashMessage(g.message(code: "configurePipeline.invalidate.failure") as String, errors)
            redirect(action: cmd.originAction)
        } else {
            projectService.invalidateProjectConfig(cmd.project, cmd.seqType, cmd.pipeline)
            flash.message = new FlashMessage(g.message(code: "configurePipeline.invalidate.success") as String)
            redirect(controller: "projectConfig")
        }
    }

    private List getToolNames() {
        List<String> toolNames = ToolName.findAllByTypeAndNameNotIlike(ToolName.Type.RNA, "GENOME_STAR_INDEX%")*.name
        toolNames.add(ProjectService.GENOME_STAR_INDEX)
        return toolNames.sort()
    }


    private String getOption(OptionName name, String type = null) {
        processingOptionService.findOptionAsString(name, type)
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
        statSizeFileName(nullable: true, blank: false, matches:  ReferenceGenomeProjectSeqType.TAB_FILE_PATTERN)
        bwaMemVersion(nullable: true, validator: { val, obj ->
            obj.seqType.isWgbs() ? true : val != null
        })
        sambambaVersion(nullable: true, validator: { val, obj ->
            obj.mergeTool == MergeConstants.MERGE_TOOL_SAMBAMBA ? val != null : true
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
class ConfigureCellRangerSubmitCommand extends BaseConfigurePipelineSubmitCommand {
    ReferenceGenomeIndex referenceGenomeIndex
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
        pluginName nullable: true, blank: false, shared: 'pathComponent'
        pluginVersion nullable: true, blank: false, shared: 'pathComponent'
        baseProjectConfig nullable: false, blank: false, shared: 'pathComponent'
        config nullable: true, blank: false, validator: { val, obj ->
            if (val && !(val ==~ /^v\d+_\d+$/)) {
                return "mismatch"
            }
        }
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
