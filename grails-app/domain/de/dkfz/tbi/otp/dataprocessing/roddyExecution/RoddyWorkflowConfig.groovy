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
package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigFragment

import java.nio.file.Paths

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

/**
 * Each roddy call is configured by a config file. This config file can be different between the different pipelines,
 * projects, seqTypes, possible sub-analyses and several other points.
 * To have a clear structure of the config files it was decided to have one config file per project, seqType and pipeline.
 * The other possible distinctions will be grouped within this config file.
 * The information about the config file is store in the "RoddyWorkflowConfig"-Domain.
 *
 * The script 'scripts/operations/pancan/LoadPanCanConfig.groovy' can be used to load a roddy config.
 *
 * @deprecated class is part of the old workflow system, use {@link ExternalWorkflowConfigFragment} instead
 */
@Deprecated
@ManagedEntity
class RoddyWorkflowConfig extends ConfigPerProjectAndSeqType implements AlignmentConfig {

    // dot makes problems in roddy config identifiers, therefore an underscore is used
    @Deprecated
    final static String CONFIG_VERSION_PATTERN = /^v\d+_\d+$/

    @Deprecated
    final static String CONFIG_PATH_ELEMENT = 'configFiles'

    /**
     * the full path to the config file which is used in this project and pipeline. The name of the config file contains the version number.
     *
     * The file should be located in: ${OtpProperty#PATH_PROJECT_ROOT}/${project}/configFiles/${Pipeline}/
     * The file should be named as: ${Pipeline}_${seqType.roddyName}_${seqType.libraryLayout}_${programVersion}_${configVersion}.xml
     *
     * for example: ${OtpProperty#PATH_PROJECT_ROOT}/${project}/configFiles/PANCAN_ALIGNMENT/PANCAN_ALIGNMENT_WES_1.0.177_v1_0.xml
     */
    @Deprecated
    String configFilePath

    @Deprecated
    String configVersion

    /**
     * In general this field should not be used but only in cases where the standard configuration does not fit.
     */
    @Deprecated
    Individual individual

    @Deprecated
    String nameUsedInConfig

    @Deprecated
    boolean adapterTrimmingNeeded = false

    /** md5sum of the config file */
    @Deprecated
    String md5sum

    static constraints = {
        configFilePath unique: true, blank: false, shared: "absolutePath"
        obsoleteDate validator: { val, obj ->
            if (!val) {
                // This validator asserts that the config is unique for the given properties.
                // Unique constraint can't be used since individual is optional and can be null.
                Long id = atMostOneElement(RoddyWorkflowConfig.findAllWhere(
                        project: obj.project,
                        seqType: obj.seqType,
                        pipeline: obj.pipeline,
                        individual: obj.individual,
                        obsoleteDate: null,
                ))?.id
                !id || id == obj.id
            }
        }
        configVersion nullable: true, // needs to be nullable because of old data
                blank: false, matches: CONFIG_VERSION_PATTERN, validator: { version, config ->
            if (version) {
                // This validator asserts that the config is unique for the given properties.
                // Unique constraint can't be used since individual is optional and can be null.
                Long id = atMostOneElement(RoddyWorkflowConfig.findAllWhere(
                        project: config.project,
                        seqType: config.seqType,
                        pipeline: config.pipeline,
                        individual: config.individual,
                        programVersion: config.programVersion,
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
            if (config.pipeline?.type == Pipeline.Type.ALIGNMENT &&
                    (config.seqType?.isRna() || config.seqType?.isWgbs() || config.seqType?.isChipSeq()) && !adapterTrimmingNeeded) {
                return "required"
            }
            return config.pipeline?.type != Pipeline.Type.ALIGNMENT && adapterTrimmingNeeded ? "not.allowed" : true
        }
        md5sum nullable: true, matches: /^[0-9a-f]{32}$/
    }

    static mapping = {
        configFilePath type: "text"
    }

    @Override
    @Deprecated
    AlignmentInfo getAlignmentInformation() {
        throw new UnsupportedOperationException("RoddyWorkflowConfig can not yet provide its own AlignmentInfo")
    }

    @Deprecated
    protected static RoddyWorkflowConfig getLatest(final Project project, final Individual individual, final SeqType seqType, final Pipeline pipeline) {
        assert project: "The project is not allowed to be null"
        assert seqType: "The seqType is not allowed to be null"
        assert pipeline: "The pipeline is not allowed to be null"
        assert individual == null || individual.project == project
        return (RoddyWorkflowConfig) atMostOneElement(findAllByProjectAndSeqTypeAndPipelineAndObsoleteDateAndIndividual(
                project, seqType, pipeline, null, individual), "Found more than one RoddyWorkflowConfig for Project ${project}, " +
                "SeqType ${seqType}, " +
                "Individual ${individual} and Pipeline ${pipeline}.")
    }

    @Deprecated
    static RoddyWorkflowConfig getLatestForProject(final Project project, final SeqType seqType, final Pipeline pipeline) {
        return getLatest(project, null, seqType, pipeline)
    }

    @Deprecated
    static RoddyWorkflowConfig getLatestForIndividual(final Individual individual, final SeqType seqType, final Pipeline pipeline) {
        assert individual: "The individual is not allowed to be null"
        return getLatest(individual.project, individual, seqType, pipeline) ?: getLatestForProject(individual.project, seqType, pipeline)
    }

    @Deprecated
    static String getNameUsedInConfig(Pipeline.Name pipelineName, SeqType seqType, String pluginNameAndVersion, String configVersion) {
        assert pipelineName
        assert seqType
        assert pluginNameAndVersion
        assert configVersion

        return [
                "${pipelineName.name()}",
                "${seqType.roddyName}",
                "${seqType.libraryLayout}",
                "${seqType.singleCell ? 'SingleCell_' : ''}${pluginNameAndVersion}",
                "${configVersion}",
        ].join("_")
    }

    @Deprecated
    static String getNameUsedInConfig(Pipeline.Name pipelineName, SeqType seqType, String pluginName, String programVersion, String configVersion) {
        return getNameUsedInConfig(pipelineName, seqType, "${pluginName}:${programVersion}", configVersion)
    }

    @Deprecated
    static File getStandardConfigDirectory(Project project, Pipeline.Name pipelineName) {
        assert project && pipelineName
        return Paths.get(
                project.projectDirectory.path,
                CONFIG_PATH_ELEMENT,
                pipelineName.name(),
        ).toFile()
    }

    @Deprecated
    static String getConfigFileName(Pipeline.Name pipelineName, SeqType seqType, String pluginNameAndVersion, String configVersion) {
        assert pipelineName
        assert seqType
        assert pluginNameAndVersion
        assert configVersion

        assert seqType.roddyName
        assert configVersion ==~ CONFIG_VERSION_PATTERN
        return "${getNameUsedInConfig(pipelineName, seqType, pluginNameAndVersion, configVersion)}.xml"
    }

    @Deprecated
    static File getStandardConfigFile(Project project, Pipeline.Name pipelineName, SeqType seqType, String programVersion, String configVersion) {
        return new File(getStandardConfigDirectory(project, pipelineName), getConfigFileName(pipelineName, seqType, programVersion, configVersion))
    }
}
