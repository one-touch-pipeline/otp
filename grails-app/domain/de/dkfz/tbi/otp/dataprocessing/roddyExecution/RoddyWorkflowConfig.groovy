package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

import java.util.regex.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

/**
 * Each roddy call is configured by a config file. This config file can be different between the different pipelines,
 * projects, seqTypes, possible sub-analyses and several other points.
 * To have a clear structure of the config files it was decided to have one config file per project, seqType and pipeline.
 * The other possible distinctions will be grouped within this config file.
 * The information about the config file is store in the "RoddyWorkflowConfig"-Domain.
 *
 * The script 'scripts/operations/pancan/LoadPanCanConfig.groovy' can be used to load a roddy config.
 *
 */
class RoddyWorkflowConfig extends ConfigPerProject implements AlignmentConfig {

    //dot makes problems in roddy config identifiers, therefore an underscore is used
    final static String CONFIG_VERSION_PATTERN =  /^v\d+_\d+$/

    final static String CONFIG_PATH_ELEMENT = 'configFiles'

    SeqType seqType

    /**
     * the full path to the config file which is used in this project and pipeline. The name of the config file contains the version number.
     *
     * The file should be located in: $OTP_ROOT_PATH/$Project/configFiles/${Pipeline}/
     * The file should be named as: ${Pipeline}_${seqType.roddyName}_${PluginVersion}_${configVersion}.xml
     *
     * for example: $OTP_ROOT_PATH/$PROJECT/configFiles/PANCAN_ALIGNMENT/PANCAN_ALIGNMENT_WES_1.0.177_v1_0.xml
     */
    String configFilePath

    String pluginVersion

    String configVersion

    /**
     * In general this field should not be used but only in cases where the standard configuration does not fit.
     */
    Individual individual

    boolean adapterTrimmingNeeded = false

    static constraints = {
        configFilePath unique: true, validator: { OtpPath.isValidAbsolutePath(it) }
        seqType nullable: true, //needs to be nullable because of old data, should never be null for new data
                validator: {val, obj ->
                    obj.id ? true : val != null
                }
        pluginVersion blank: false
        obsoleteDate validator: { obsolete, config ->
            if (!obsolete) {
                // This validator asserts that the config is unique for the given properties.
                // Unique constraint can't be used since individual is optional and can be null.
                Long id = atMostOneElement(RoddyWorkflowConfig.findAllWhere(
                        project: config.project,
                        seqType: config.seqType,
                        pipeline: config.pipeline,
                        individual: config.individual,
                        obsoleteDate: null,
                ))?.id
                !id || id == config.id
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

    static RoddyWorkflowConfig importProjectConfigFile(Project project, SeqType seqType, String pluginVersionToUse, Pipeline pipeline, String configFilePath, String configVersion, boolean adapterTrimmingNeeded = false, Individual individual = null) {
        assert project : "The project is not allowed to be null"
        assert seqType : "The seqType is not allowed to be null"
        assert pipeline : "The pipeline is not allowed to be null"
        assert pluginVersionToUse:"The pluginVersionToUse is not allowed to be null"
        assert configFilePath : "The configFilePath is not allowed to be null"
        assert configVersion : "The configVersion is not allowed to be null"

        RoddyWorkflowConfig roddyWorkflowConfig = getLatest(project, individual, seqType, pipeline)

        RoddyWorkflowConfig config = new RoddyWorkflowConfig(
                project: project,
                seqType: seqType,
                pipeline: pipeline,
                configFilePath: configFilePath,
                pluginVersion: pluginVersionToUse,
                previousConfig: roddyWorkflowConfig,
                configVersion: configVersion,
                individual: individual,
                adapterTrimmingNeeded: adapterTrimmingNeeded,
        )
        config.validateConfig()
        config.createConfigPerProject()

        return config
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

    void validateConfig() {
        File configFile = configFilePath as File
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(configFile)
        String pattern = /^${Pattern.quote(pipeline.name.name())}_${Pattern.quote(seqType.roddyName)}_(.+)_${Pattern.quote(configVersion)}\.xml$/
        Matcher matcher = configFile.name =~ pattern
        assert matcher.matches(): "The file name '${configFile.name}' does not match the pattern '${pattern}'"
        assert pluginVersion.endsWith(":${matcher.group(1)}")
        def configuration = new XmlParser().parseText(configFile.text)
        assert configuration.@name == getNameUsedInConfig()
        if (individual) {
            assert configFilePath.contains(individual.pid)
        }
    }

    String getNameUsedInConfig() {
        assert configVersion != null : 'Config version is not set'
        return getNameUsedInConfig(pipeline.name, seqType, pluginVersion, configVersion)
    }

    static String getNameUsedInConfig(Pipeline.Name pipelineName, SeqType seqType, String pluginNameAndVersion, String configVersion) {
        assert pipelineName
        assert seqType
        assert pluginNameAndVersion
        assert configVersion

        return "${pipelineName.name()}_${seqType.roddyName}_${pluginNameAndVersion}_${configVersion}"
    }

    static String getNameUsedInConfig(Pipeline.Name pipelineName, SeqType seqType, String pluginName, String pluginVersion, String configVersion) {
        getNameUsedInConfig(pipelineName, seqType, "${pluginName}:${pluginVersion}", configVersion)
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
