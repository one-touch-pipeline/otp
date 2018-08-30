package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

/**
 * Each roddy call is configured by a config file. This config file can be different between the different pipelines,
 * projects, seqTypes, possible sub-analyses and several other points.
 * To have a clear structure of the config files it was decided to have one config file per project, seqType and pipeline.
 * The other possible distinctions will be grouped within this config file.
 * The information about the config file is store in the "RoddyWorkflowConfig"-Domain.
 *
 * The script 'scripts/operations/pancan/LoadPanCanConfig.groovy' can be used to load a roddy config.
 */
class RoddyWorkflowConfig extends ConfigPerProjectAndSeqType implements AlignmentConfig {

    //dot makes problems in roddy config identifiers, therefore an underscore is used
    final static String CONFIG_VERSION_PATTERN =  /^v\d+_\d+$/

    final static String CONFIG_PATH_ELEMENT = 'configFiles'

    /**
     * the full path to the config file which is used in this project and pipeline. The name of the config file contains the version number.
     *
     * The file should be located in: ${OtpProperty#PATH_PROJECT_ROOT}/${project}/configFiles/${Pipeline}/
     * The file should be named as: ${Pipeline}_${seqType.roddyName}_${seqType.libraryLayout}_${PluginVersion}_${configVersion}.xml
     *
     * for example: ${OtpProperty#PATH_PROJECT_ROOT}/${project}/configFiles/PANCAN_ALIGNMENT/PANCAN_ALIGNMENT_WES_1.0.177_v1_0.xml
     */
    String configFilePath

    String pluginVersion

    String configVersion

    String nameUsedInConfig

    /**
     * In general this field should not be used but only in cases where the standard configuration does not fit.
     */
    Individual individual

    boolean adapterTrimmingNeeded = false

    static constraints = {
        configFilePath unique: true, validator: { OtpPath.isValidAbsolutePath(it) }
        pluginVersion blank: false
        obsoleteDate validator: { val, obj ->
            if (!val) {
                // This validator asserts that the config is unique for the given properties.
                // Unique constraint can't be used since individual is optional and can be null.
                Long id = atMostOneElement(RoddyWorkflowConfig.findAllWhere(
                        project:        obj.project,
                        seqType:        obj.seqType,
                        pipeline:       obj.pipeline,
                        individual:     obj.individual,
                        obsoleteDate:   null,
                ))?.id
                !id || id == obj.id
            }
        }
        configVersion nullable: true, //needs to be nullable because of old data
                blank: false, matches: CONFIG_VERSION_PATTERN, validator: { version, config ->
            if (version) {
                // This validator asserts that the config is unique for the given properties.
                // Unique constraint can't be used since individual is optional and can be null.
                Long id = atMostOneElement(RoddyWorkflowConfig.findAllWhere(
                        project: config.project,
                        seqType: config.seqType,
                        pipeline: config.pipeline,
                        individual: config.individual,
                        pluginVersion: config.pluginVersion,
                        configVersion: config.configVersion,
                ))?.id
                !id || id == config.id
            }
        }
        nameUsedInConfig nullable: true, blank: true
        individual nullable: true, validator: { Individual val, RoddyWorkflowConfig obj ->
            return val == null || val.project == obj.project
        }
        pipeline validator: { pipeline ->
            pipeline?.usesRoddy()
        }
        adapterTrimmingNeeded validator: { adapterTrimmingNeeded, config ->
            if (config.pipeline?.type == Pipeline.Type.ALIGNMENT && (config.seqType?.isRna() || config.seqType?.isWgbs() || config.seqType?.isChipSeq()) && !adapterTrimmingNeeded) {
                return "adapterTrimmingNeeded must be set for WGBS, ChipSeq and RNA alignment"
            }
            if (config.pipeline?.type != Pipeline.Type.ALIGNMENT && adapterTrimmingNeeded) {
                return "adapterTrimmingNeeded must not be set for non-alignment pipelines"
            }
            return true
        }
    }

    protected static RoddyWorkflowConfig getLatest(final Project project, final Individual individual, final SeqType seqType, final Pipeline pipeline) {
        assert project : "The project is not allowed to be null"
        assert seqType : "The seqType is not allowed to be null"
        assert pipeline : "The pipeline is not allowed to be null"
        assert individual == null || individual.project == project
        try {
            return (RoddyWorkflowConfig) atMostOneElement(findAllByProjectAndSeqTypeAndPipelineAndObsoleteDateAndIndividual(project, seqType, pipeline, null, individual))
        } catch (final Throwable t) {
            throw new RuntimeException("Found more than one RoddyWorkflowConfig for Project ${project}, SeqType ${seqType}, Individual ${individual} and Pipeline ${pipeline}. ${t.message ?: ''}", t)
        }
    }

    static RoddyWorkflowConfig getLatestForProject(final Project project, final SeqType seqType, final Pipeline pipeline) {
        return getLatest(project, null, seqType, pipeline)
    }

    static RoddyWorkflowConfig getLatestForIndividual(final Individual individual, final SeqType seqType, final Pipeline pipeline) {
        assert individual : "The individual is not allowed to be null"
        return getLatest(individual.project, individual, seqType, pipeline) ?: getLatestForProject(individual.project, seqType, pipeline)
    }

    static String getNameUsedInConfig(Pipeline.Name pipelineName, SeqType seqType, String pluginNameAndVersion, String configVersion) {
        assert pipelineName
        assert seqType
        assert pluginNameAndVersion
        assert configVersion

        return "${pipelineName.name()}_${seqType.roddyName}_${seqType.libraryLayout}_${seqType.singleCell ? 'SingleCell_' : ''}${pluginNameAndVersion}_${configVersion}"
    }

    static String getNameUsedInConfig(Pipeline.Name pipelineName, SeqType seqType, String pluginName, String pluginVersion, String configVersion) {
        return getNameUsedInConfig(pipelineName, seqType, "${pluginName}:${pluginVersion}", configVersion)
    }

    static File getStandardConfigDirectory(Project project, Pipeline.Name pipelineName) {
        assert project
        assert pipelineName
        LsdfFilesService.getPath(
                project.getProjectDirectory().path,
                CONFIG_PATH_ELEMENT,
                pipelineName.name(),
        )
    }

    static String getConfigFileName(Pipeline.Name pipelineName, SeqType seqType, String pluginNameAndVersion, String configVersion) {
        assert pipelineName
        assert seqType
        assert pluginNameAndVersion
        assert configVersion

        assert seqType.roddyName
        assert configVersion ==~ CONFIG_VERSION_PATTERN
        return "${getNameUsedInConfig(pipelineName, seqType, pluginNameAndVersion, configVersion)}.xml"
    }

    static File getStandardConfigFile(Project project, Pipeline.Name pipelineName, SeqType seqType, String pluginVersion, String configVersion) {
        return new File(getStandardConfigDirectory(project, pipelineName), getConfigFileName(pipelineName, seqType, pluginVersion, configVersion))
    }

}
