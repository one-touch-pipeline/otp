package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.*
import java.util.regex.*

class RoddyWorkflowConfigService {

    FileSystemService fileSystemService

    void loadPanCanConfigAndTriggerAlignment(Project project, SeqType seqType, String pluginVersionToUse, Pipeline pipeline, String configFilePath, String configVersion, boolean adapterTrimmingNeeded, Individual individual) {
        assert individual : "The individual is not allowed to be null"

        RoddyBamFile.withTransaction {
            importProjectConfigFile(project, seqType, pluginVersionToUse, pipeline, configFilePath, configVersion, adapterTrimmingNeeded, individual)

            List<MergingWorkPackage> mergingWorkPackages = MergingWorkPackage.createCriteria().list {
                eq('seqType', seqType)
                eq('pipeline', pipeline)
                sample {
                    eq('individual', individual)
                }
            }

            assert mergingWorkPackages : "no MWP found"

            println "Old roddyBamFiles are marked as withdrawn"
            RoddyBamFile.findAllByWorkPackageInList(mergingWorkPackages)*.withdraw()

            println "Realignment will be triggered for bam files of ${individual}"
            mergingWorkPackages*.needsProcessing = true
            mergingWorkPackages*.save(flush: true)
        }
    }

    RoddyWorkflowConfig importProjectConfigFile(Project project, SeqType seqType, String pluginVersionToUse, Pipeline pipeline, String configFilePath, String configVersion, boolean adapterTrimmingNeeded = false, Individual individual = null) {
        assert project : "The project is not allowed to be null"
        assert seqType : "The seqType is not allowed to be null"
        assert pipeline : "The pipeline is not allowed to be null"
        assert pluginVersionToUse:"The pluginVersionToUse is not allowed to be null"
        assert configFilePath : "The configFilePath is not allowed to be null"
        assert configVersion : "The configVersion is not allowed to be null"

        RoddyWorkflowConfig roddyWorkflowConfig = RoddyWorkflowConfig.getLatest(project, individual, seqType, pipeline)

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
                nameUsedInConfig: RoddyWorkflowConfig.getNameUsedInConfig(pipeline.name, seqType, pluginVersionToUse, configVersion)
        )
        validateConfig(config)
        config.createConfigPerProject()

        return config
    }


    void validateConfig(RoddyWorkflowConfig config) {
        FileSystem fs = fileSystemService.getFilesystemForConfigFileChecksForRealm(config.project.realm)
        Path configFile = fs.getPath(config.configFilePath)

        FileService.ensureFileIsReadableAndNotEmpty(configFile)
        String pattern = /^${Pattern.quote(config.pipeline.name.name())}_${Pattern.quote(config.seqType.roddyName)}_${Pattern.quote(config.seqType.libraryLayout)}_(.+)_${Pattern.quote(config.configVersion)}\.xml$/
        Matcher matcher = configFile.fileName.toString() =~ pattern
        assert matcher.matches(): "The file name '${configFile.toString()}' does not match the pattern '${pattern}'"
        assert config.pluginVersion.endsWith(":${matcher.group(1)}")
        def configuration = new XmlParser().parseText(configFile.text)
        assert configuration.@name == config.getNameUsedInConfig()
        if (config.individual) {
            assert config.configFilePath.contains(config.individual.pid)
        }
    }
}
