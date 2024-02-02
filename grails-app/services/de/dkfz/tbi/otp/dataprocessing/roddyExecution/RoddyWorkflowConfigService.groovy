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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileDynamic
@Transactional
class RoddyWorkflowConfigService {

    FileService fileService
    FileSystemService fileSystemService
    WorkflowConfigService workflowConfigService
    ConfigService configService

    RoddyWorkflowConfig importProjectConfigFile(Project project, SeqType seqType, String programVersionToUse, Pipeline pipeline, String configFilePath,
                                                String configVersion, String md5sum, boolean adapterTrimmingNeeded = false, Individual individual = null) {
        assert project : "The project is not allowed to be null"
        assert seqType : "The seqType is not allowed to be null"
        assert pipeline : "The pipeline is not allowed to be null"
        assert programVersionToUse: "The programVersionToUse is not allowed to be null"
        assert configFilePath : "The configFilePath is not allowed to be null"
        assert configVersion : "The configVersion is not allowed to be null"

        RoddyWorkflowConfig roddyWorkflowConfig = RoddyWorkflowConfig.getLatest(project, individual, seqType, pipeline)

        RoddyWorkflowConfig config = new RoddyWorkflowConfig(
                project: project,
                seqType: seqType,
                pipeline: pipeline,
                configFilePath: configFilePath,
                programVersion: programVersionToUse,
                previousConfig: roddyWorkflowConfig,
                configVersion: configVersion,
                individual: individual,
                adapterTrimmingNeeded: adapterTrimmingNeeded,
                nameUsedInConfig: RoddyWorkflowConfig.getNameUsedInConfig(pipeline.name, seqType, programVersionToUse, configVersion),
                md5sum: md5sum,
        )
        validateConfig(config)
        workflowConfigService.createConfigPerProjectAndSeqType(config)

        return config
    }

    void createConfigPerProjectAndSeqType(ConfigPerProjectAndSeqType configPerProjectAndSeqType) {
        Project.withTransaction {
            if (configPerProjectAndSeqType.previousConfig) {
                makeObsolete(configPerProjectAndSeqType.previousConfig)
            }
            assert configPerProjectAndSeqType.save(flush: true)
        }
    }

    void makeObsolete(ConfigPerProjectAndSeqType configPerProjectAndSeqType) {
        configPerProjectAndSeqType.obsoleteDate = new Date()
        assert configPerProjectAndSeqType.save(flush: true)
    }

    String formatPluginVersion(String pluginName, String programVersion) {
        return (String) "${pluginName}:${programVersion}"
    }

    void validateConfig(RoddyWorkflowConfig config) {
        FileSystem fs = fileSystemService.remoteFileSystem
        Path configFile = fs.getPath(config.configFilePath)

        fileService.ensureFileIsReadableAndNotEmpty(configFile)
        List<String> patternHelper = [
                "${Pattern.quote(config.pipeline.name.name())}",
                "${Pattern.quote(config.seqType.roddyName)}",
                "${Pattern.quote(config.seqType.libraryLayout.name())}",
                "${config.seqType.singleCell ? 'SingleCell_' : ''}(.+)",
                "${Pattern.quote(config.configVersion)}",
        ]
        String pattern = /^${patternHelper.join("_")}\.xml$/
        Matcher matcher = configFile.fileName.toString() =~ pattern
        assert matcher.matches(): "The file name '${configFile}' does not match the pattern '${pattern}'"
        assert config.programVersion.endsWith(":${matcher.group(1)}")
        def configuration = new XmlParser().parseText(configFile.text)
        assert configuration.@name == config.nameUsedInConfig
        if (config.individual) {
            assert config.configFilePath.contains(config.individual.pid)
        }
    }

    ConfigState getCurrentFilesystemState(Project project, SeqType seqType, Pipeline pipeline) {
        RoddyWorkflowConfig config = RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline)
        if (config) {
            FileSystem fs = fileSystemService.remoteFileSystem
            String currentConfigContent = fs.getPath(config.configFilePath).text
            return new ConfigState(currentConfigContent, currentConfigContent.encodeAsMD5() != config.md5sum)
        }
        return new ConfigState("", false)
    }

    @TupleConstructor
    /**
     * Current filesystem contents of a {@link RoddyWorkflowConfig}, including checksum verification results.
     *
     * @see RoddyWorkflowConfig
     * @see RoddyWorkflowConfigService#getCurrentFilesystemState(Project project, SeqType seqType, Pipeline pipeline)
     */
    static class ConfigState {
        String content
        boolean changed
    }

    List<RoddyWorkflowConfig> findAllByProjectAndSeqTypeAndPipelineAndProgramVersionAndConfigVersion(Project project, SeqType seqType, Pipeline pipeline,
                                                                                                     String programVersion, String configVersion) {
        return RoddyWorkflowConfig.findAllWhere([
                project       : project,
                seqType       : seqType,
                pipeline      : pipeline,
                programVersion: programVersion,
                configVersion : configVersion,
        ])
    }

    RoddyWorkflowConfig findLatestConfigByProjectAndSeqTypeAndPipeline(Project project, SeqType seqType, Pipeline pipeline) {
        return CollectionUtils.atMostOneElement(RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineAndIndividualIsNull(
                project, seqType, pipeline, [sort: 'id', order: 'desc', max: 1,]
        ))
    }
}
