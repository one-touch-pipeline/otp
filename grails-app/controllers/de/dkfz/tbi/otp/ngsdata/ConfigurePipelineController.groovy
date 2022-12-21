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
import org.springframework.security.access.annotation.Secured
import groovy.transform.ToString

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.ngsdata.referencegenome.*
import de.dkfz.tbi.otp.project.*
import de.dkfz.tbi.otp.utils.StringUtils

@Secured("hasRole('ROLE_OPERATOR')")
class ConfigurePipelineController implements ConfigurePipelineHelper {

    ProcessingOptionService processingOptionService
    ProjectService projectService
    ReferenceGenomeService referenceGenomeService
    ReferenceGenomeProjectSeqTypeService referenceGenomeProjectSeqTypeService
    ReferenceGenomeIndexService referenceGenomeIndexService
    GeneModelService geneModelService
    SampleTypeService sampleTypeService
    SeqTypeService seqTypeService
    ToolNameService toolNameService

    static allowedMethods = [
            alignment                  : "GET",
            saveAlignment              : "POST",
            copyAlignment              : "POST",
            rnaAlignment               : "GET",
            rnaAlignmentConfig         : "POST",
            rnaAlignmentReferenceGenome: "POST",
            getStatSizeFileNames       : "GET",
            getGeneModels              : "GET",
            getToolVersions            : "GET",
            invalidateConfig           : "POST",
    ]

    def alignment(BaseConfigurePipelineSubmitCommand cmd) {
        if (cmd.seqType in seqTypeService.seqTypesNewWorkflowSystem) {
            return response.sendError(404)
        }

        Pipeline pipeline = Pipeline.Name.PANCAN_ALIGNMENT.pipeline

        Project project = projectSelectionService.selectedProject
        List<Project> projects = projectService.getAllProjectsWithConfigFile(cmd.seqType, pipeline)

        String defaultPluginName = getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_NAME, cmd.seqType.roddyName)
        String defaultProgramVersion = getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_VERSION, cmd.seqType.roddyName)
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
        assert defaultMergeTool in MergeTool.ALL_MERGE_TOOLS*.name
        assert MergeTool.ALL_MERGE_TOOLS*.name.containsAll(allMergeTools)
        assert referenceGenomeService.findByName(defaultReferenceGenome)

        Map result = [:]
        result << getValues(project, cmd.seqType, pipeline)

        String referenceGenome =
                referenceGenomeProjectSeqTypeService.findReferenceGenomeProjectSeqTypeNoSampleType(project, cmd.seqType)?.referenceGenome?.name ?:
                        defaultReferenceGenome
        List<String> referenceGenomes = ReferenceGenome.list(sort: "name", order: "asc")*.name

        result << [
                projects                : projects,

                referenceGenome         : flash?.cmd?.referenceGenome ?: referenceGenome,
                referenceGenomes        : referenceGenomes,
                defaultReferenceGenome  : defaultReferenceGenome,
                statSizeFileName        : flash?.cmd?.statSizeFileName,

                mergeTool               : flash?.cmd?.mergeTool ?: defaultMergeTool,
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

                programVersion          : defaultProgramVersion,
                defaultProgramVersion   : defaultProgramVersion,

                baseProjectConfig       : defaultBaseProjectConfig,
                defaultBaseProjectConfig: defaultBaseProjectConfig,
        ]
        return result
    }

    def saveAlignment(ConfigureAlignmentPipelineSubmitCommand cmd) {
        if (cmd.seqType in seqTypeService.seqTypesNewWorkflowSystem) {
            return response.sendError(404)
        }

        Project project = projectSelectionService.requestedProject
        if (!cmd.validate()) {
            flash.message = new FlashMessage(g.message(code: "configurePipeline.store.failure") as String, cmd.errors)
            flash.cmd = cmd
            redirect(action: "alignment", params: ["seqType.id": cmd.seqType.id])
            return
        }

        if (!validateUniqueness(cmd, project, Pipeline.Name.PANCAN_ALIGNMENT.pipeline)) {
            flash.message = new FlashMessage(g.message(code: "configurePipeline.store.failure") as String,
                    [g.message(code: "configurePipeline.store.failure.duplicate") as String])
            flash.cmd = cmd
            redirect(action: "alignment", params: ["seqType.id": cmd.seqType.id])
            return
        }

        PanCanAlignmentConfiguration panCanAlignmentConfiguration = new PanCanAlignmentConfiguration([
                project              : project,
                seqType              : cmd.seqType,
                referenceGenome      : cmd.referenceGenome,
                statSizeFileName     : cmd.statSizeFileName,
                mergeTool            : cmd.mergeTool,
                pluginName           : cmd.pluginName,
                programVersion       : cmd.programVersion,
                baseProjectConfig    : cmd.baseProjectConfig,
                configVersion        : cmd.config,
                bwaMemVersion        : cmd.bwaMemVersion,
                sambambaVersion      : cmd.sambambaVersion,
                adapterTrimmingNeeded: cmd.adapterTrimmingNeeded,
        ])
        projectService.configurePanCanAlignmentDeciderProject(panCanAlignmentConfiguration)

        flash.message = new FlashMessage(g.message(code: "configurePipeline.store.success") as String)
        redirect(controller: "alignmentConfigurationOverview")
    }

    def copyAlignment(CopyAlignmentPipelineSubmitCommand cmd) {
        if (cmd.seqType in seqTypeService.seqTypesNewWorkflowSystem) {
            return response.sendError(404)
        }

        Project project = projectSelectionService.requestedProject
        if (!cmd.validate()) {
            flash.message = new FlashMessage(g.message(code: "configurePipeline.store.failure") as String, cmd.errors)
            flash.cmd = cmd
            redirect(action: "alignment", params: ["seqType.id": cmd.seqType.id])
            return
        }

        projectService.copyPanCanAlignmentXml(cmd.basedProject, project, cmd.seqType)
        flash.message = new FlashMessage(g.message(code: "configurePipeline.copy.success") as String)
        redirect(controller: "alignmentConfigurationOverview")
    }

    def rnaAlignment(BaseConfigurePipelineSubmitCommand cmd) {
        Closure sampleTypeSort = { SampleType a, SampleType b -> b.dateCreated <=> a.dateCreated }

        Pipeline pipeline = Pipeline.Name.RODDY_RNA_ALIGNMENT.pipeline

        Project project = projectSelectionService.selectedProject
        List<Project> projects = projectService.getAllProjectsWithConfigFile(cmd.seqType, pipeline)

        Map result = [:]

        String defaultPluginName = getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_NAME, cmd.seqType.roddyName)
        String defaultProgramVersion = getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_VERSION, cmd.seqType.roddyName)
        String defaultBaseProjectConfig = getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_BASE_PROJECT_CONFIG, cmd.seqType.roddyName)
        String defaultReferenceGenome = getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME, cmd.seqType.roddyName)
        String defaultGenomeStarIndex = getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_RNA_DEFAULT_GENOME_STAR_INDEX)

        assert referenceGenomeService.findByName(defaultReferenceGenome)

        String referenceGenome = referenceGenomeProjectSeqTypeService.findReferenceGenomeProjectSeqTypeNoSampleType(project, cmd.seqType)?.referenceGenome?.name
                ?: defaultReferenceGenome
        List<String> referenceGenomes = referenceGenomeService.list().findAll {
            referenceGenomeIndexService.findAllByReferenceGenome(it) && geneModelService.findAllByReferenceGenome(it)
        }*.name

        List<SampleType> configuredSampleTypes = referenceGenomeProjectSeqTypeService.findReferenceGenomeProjectSeqTypesWithSampleType(
                project, cmd.seqType)*.sampleType.unique().sort(sampleTypeSort)

        List<SampleType> additionalUsedSampleTypes = (sampleTypeService.findAllUsedOnceByProjectAndSeqType(project, cmd.seqType) -
                configuredSampleTypes).sort(sampleTypeSort)

        List<SampleType> additionalPossibleSampleTypes = (
                sampleTypeService.findAllBySpecificReferenceGenome(SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC) -
                        configuredSampleTypes - additionalUsedSampleTypes).sort(sampleTypeSort)

        result << [
                projects                : projects,
                toolNames               : toolNameService.findAllToolNamesForRNA(),

                referenceGenome         : referenceGenome,
                referenceGenomes        : referenceGenomes,
                defaultReferenceGenome  : defaultReferenceGenome,
                defaultGenomeStarIndex  : defaultGenomeStarIndex,
                indexToolVersion        : null,
                geneModel               : null,

                pluginName              : defaultPluginName,
                defaultPluginName       : defaultPluginName,

                programVersion          : defaultProgramVersion,
                defaultProgramVersion   : defaultProgramVersion,

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
        result << getValues(project, cmd.seqType, pipeline)

        return result
    }

    def rnaAlignmentConfig(ConfigurePipelineSubmitCommand cmd) {
        Project project = projectSelectionService.requestedProject
        if (!cmd.validate()) {
            flash.message = new FlashMessage(g.message(code: "configurePipeline.store.failure") as String, cmd.errors)
            flash.cmd = cmd
            redirect(action: "rnaAlignment", params: ["seqType.id": cmd.seqType.id])
            return
        }

        if (!validateUniqueness(cmd, project, Pipeline.Name.RODDY_RNA_ALIGNMENT.pipeline)) {
            flash.message = new FlashMessage(g.message(code: "configurePipeline.store.failure") as String,
                    [g.message(code: "configurePipeline.store.failure.duplicate") as String])
            flash.cmd = cmd
            redirect(action: "rnaAlignment", params: ["seqType.id": cmd.seqType.id])
            return
        }

        RoddyConfiguration rnaAlignmentConfiguration = new RoddyConfiguration([
                project          : project,
                baseProjectConfig: cmd.baseProjectConfig,
                seqType          : cmd.seqType,
                pluginName       : cmd.pluginName,
                programVersion   : cmd.programVersion,
                configVersion    : cmd.config,
        ])
        projectService.configureRnaAlignmentConfig(rnaAlignmentConfiguration)
        flash.message = new FlashMessage(g.message(code: "configurePipeline.store.success") as String)
        redirect(action: "rnaAlignment", params: ["seqType.id": cmd.seqType.id])
    }

    def rnaAlignmentReferenceGenome(ConfigureRnaAlignmentSubmitCommand cmd) {
        Project project = projectSelectionService.requestedProject

        List<ReferenceGenomeIndex> referenceGenomeIndex = cmd.toolVersionValue.collect {
            it ? referenceGenomeIndexService.findById(it as long) : null
        }.findAll()

        if (!cmd.validate()) {
            log.error("{}", errors)
            flash.message = new FlashMessage(g.message(code: "configurePipeline.store.failure") as String, cmd.errors)
            flash.cmd = cmd
            redirect(action: "rnaAlignment", params: ["seqType.id": cmd.seqType.id])
            return
        }

        RnaAlignmentReferenceGenomeConfiguration rnaConfiguration = new RnaAlignmentReferenceGenomeConfiguration([
                project                : project,
                seqType                : cmd.seqType,
                referenceGenome        : cmd.referenceGenome,
                geneModel              : cmd.geneModel,
                referenceGenomeIndex   : referenceGenomeIndex,
                mouseData              : cmd.mouseData,
                deprecateConfigurations: cmd.deprecateConfigurations,
                sampleTypes            : cmd.sampleTypeIds.collect { return sampleTypeService.findById(it as long) },
        ])
        projectService.configureRnaAlignmentReferenceGenome(rnaConfiguration)
        flash.message = new FlashMessage(g.message(code: "configurePipeline.store.success") as String)
        redirect(action: "rnaAlignment", params: ["seqType.id": cmd.seqType.id])
    }

    JSON getStatSizeFileNames(String referenceGenomeName) {
        ReferenceGenome referenceGenome = referenceGenomeService.findByName(referenceGenomeName)
        Map data = [:]

        if (referenceGenome) {
            data = [
                    data: ReferenceGenomeService.getStatSizeFileNames(referenceGenome)*.name,
            ]
        }

        render data as JSON
    }

    JSON getGeneModels(String referenceGenome) {
        Map data = [
                data: geneModelService.findAllByReferenceGenomeName(referenceGenome)
        ]
        render data as JSON
    }

    JSON getToolVersions(String referenceGenome) {
        List<ToolName> toolNames = toolNameService.findAllToolNamesByType(ToolName.Type.RNA)
        ReferenceGenome refGenome = referenceGenomeService.findByName(referenceGenome)
        Map data = [:]
        Map toolNamesData = [:]
        toolNames.each {
            toolNamesData << [
                    (it.name): referenceGenomeIndexService.findAllByReferenceGenomeAndToolName(refGenome, it)
            ]
        }
        data << ["defaultGenomeStarIndex": getOption(OptionName.PIPELINE_RODDY_ALIGNMENT_RNA_DEFAULT_GENOME_STAR_INDEX)]
        data << ["data": toolNamesData]
        render data as JSON
    }

    def invalidateConfig(InvalidateConfigurationCommand cmd) {
        if (cmd.seqType in seqTypeService.seqTypesNewWorkflowSystem) {
            return response.sendError(404)
        }

        boolean hasErrors = cmd.hasErrors()

        if (hasErrors) {
            flash.message = new FlashMessage(g.message(code: "configurePipeline.invalidate.failure") as String, errors)
            redirect(action: cmd.originAction, params: ["seqType.id": cmd.seqType.id])
        } else {
            projectService.invalidateProjectConfig(projectSelectionService.requestedProject, cmd.seqType, cmd.pipeline)
            flash.message = new FlashMessage(g.message(code: "configurePipeline.invalidate.success") as String)
            redirect(controller: cmd.overviewController)
        }
    }

    private String getOption(OptionName name, String type = null) {
        processingOptionService.findOptionAsString(name, type)
    }
}

@ToString(includeNames = true, includeSuper = true)
class ConfigureAlignmentPipelineSubmitCommand extends ConfigurePipelineSubmitCommand implements Serializable {
    String referenceGenome
    String statSizeFileName
    String mergeTool
    String bwaMemVersion
    String sambambaVersion
    boolean adapterTrimmingNeeded

    static constraints = {
        statSizeFileName(nullable: true, blank: false, matches:  ReferenceGenomeProjectSeqType.TAB_FILE_PATTERN)
        bwaMemVersion(nullable: true, validator: { val, obj ->
            obj.seqType.isWgbs() ? true : val != null
        })
        sambambaVersion(nullable: true, validator: { val, obj ->
            obj.mergeTool == MergeTool.SAMBAMBA.name ? val != null : true
        })
    }
}

@ToString(includeNames = true, includeSuper = true)
class CopyAlignmentPipelineSubmitCommand extends BaseConfigurePipelineSubmitCommand implements Serializable {
    Project basedProject
}

@ToString(includeNames = true, includeSuper = true)
class ConfigureRnaAlignmentSubmitCommand extends BaseConfigurePipelineSubmitCommand {
    String referenceGenome
    boolean mouseData
    boolean deprecateConfigurations

    GeneModel geneModel
    List toolVersionValue = []

    List<String> sampleTypeIds = []

    static constraints = {
        referenceGenome(nullable: false)
        geneModel(nullable: false)
    }
}

@ToString(includeNames = true, includeSuper = true)
class ConfigureRunYapsaSubmitCommand extends BaseConfigurePipelineSubmitCommand {
    String programVersion
    String overviewController
}

@ToString(includeNames = true, includeSuper = true)
class ConfigureCellRangerSubmitCommand extends BaseConfigurePipelineSubmitCommand {
    ReferenceGenomeIndex referenceGenomeIndex
    String programVersion
    String overviewController
}

@ToString(includeNames = true, includeSuper = true)
class ConfigurePipelineSubmitCommand extends BaseConfigurePipelineSubmitCommand {
    String pluginName
    String programVersion
    String baseProjectConfig
    String config
    String submit

    static constraints = {
        pluginName nullable: true, blank: false, shared: 'pathComponent'
        programVersion nullable: true, blank: false, shared: 'pathComponent'
        baseProjectConfig nullable: false, blank: false, shared: 'pathComponent'
        config nullable: true, blank: false, validator: { val, obj ->
            if (val && !(val ==~ /^v\d+_\d+$/)) {
                return "mismatch"
            }
        }
    }

    void setConfig(String config) {
        this.config = StringUtils.trimAndShortenWhitespace(config)
    }
}

@ToString(includeNames = true, includeSuper = true)
class InvalidateConfigurationCommand extends BaseConfigurePipelineSubmitCommand {
    Pipeline pipeline
    String originAction
    String overviewController

    static constraints = {
        pipeline(nullable: false)
        originAction(nullable: false)
        overviewController(nullable: false)
    }
}

@ToString(includeNames = true)
class BaseConfigurePipelineSubmitCommand implements Serializable {
    SeqType seqType

    static constraints = {
        seqType(nullable: false)
    }
}
