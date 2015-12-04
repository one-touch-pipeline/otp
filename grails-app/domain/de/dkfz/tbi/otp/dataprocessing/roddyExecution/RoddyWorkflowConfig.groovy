package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.otp.dataprocessing.ConfigPerProject
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.Workflow
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Project

import java.util.regex.Matcher
import java.util.regex.Pattern

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

/**
 * Each roddy call is configured by a config file. This config file can be different between the different workflows,
 * projects, seqTypes, possible sub-analyses and several other points.
 * To have a clear structure of the config files it was decided to have one config file per project and workflow.
 * The other possible distinctions will be grouped within this config file.
 * The information about the config file is store in the "RoddyWorkflowConfig"-Domain.
 *
 * The script 'scripts/operations/pancan/LoadPanCanConfig.groovy' can be used to load a roddy config.
 *
 */
class RoddyWorkflowConfig extends ConfigPerProject {

    //dot makes problems in roddy config identifiers, therefore an underscore is used
    final static String CONFIG_VERSION_PATTERN =  /^v\d+_\d+$/

    Workflow workflow

    /**
     * the full path to the config file which is used in this project and workflow. The name of the config file contains the version number.
     *
     * The file should be located in: $OTP_ROOT_PATH/$PROJECT/configFiles/$Workflow/
     * The file should be named as: ${Workflow}_${WorkflowVersion}_${configVersion}.xml
     *
     * for example: $OTP_ROOT_PATH/$PROJECT/configFiles/PANCAN_ALIGNMENT/PANCAN_ALIGNMENT_1.0.177_v1_0.xml
     */
    String configFilePath

    String pluginVersion

    String configVersion

    static constraints = {
        configFilePath validator: { OtpPath.isValidAbsolutePath(it) }
        workflow unique: ['project', 'configFilePath']
        pluginVersion blank: false
        obsoleteDate unique: ['project', 'workflow']  // partial index: WHERE obsolete_date IS NULL
        configVersion nullable: true, blank: false, unique: ['project', 'workflow', 'pluginVersion'], matches: CONFIG_VERSION_PATTERN //needs to be nullable because of old data
    }

    static void importProjectConfigFile(Project project, String pluginVersionToUse, Workflow workflow, String configFilePath, String configVersion) {
        assert project : "The project is not allowed to be null"
        assert workflow : "The workflow is not allowed to be null"
        assert pluginVersionToUse:"The pluginVersionToUse is not allowed to be null"
        assert configFilePath : "The configFilePath is not allowed to be null"
        assert configVersion : "The configVersion is not allowed to be null"

        RoddyWorkflowConfig roddyWorkflowConfig = getLatest(project, workflow)

        RoddyWorkflowConfig config = new RoddyWorkflowConfig(
                project: project,
                workflow: workflow,
                configFilePath: configFilePath,
                pluginVersion: pluginVersionToUse,
                previousConfig: roddyWorkflowConfig,
                configVersion: configVersion,
        )
        config.validateConfig()
        config.createConfigPerProject()
    }

    static RoddyWorkflowConfig getLatest(final Project project, final Workflow workflow) {
        assert project : "The project is not allowed to be null"
        assert workflow : "The workflow is not allowed to be null"
        try {
            return atMostOneElement(RoddyWorkflowConfig.findAllByProjectAndWorkflowAndObsoleteDate(project, workflow, null))
        } catch (final Throwable t) {
            throw new RuntimeException("Found more than one RoddyWorkflowConfig for Project ${project} and Workflow ${workflow}. ${t.message ?: ''}", t)
        }
    }

    void validateConfig() {
        File configFile = configFilePath as File
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(configFile)
        String pattern = /^${Pattern.quote(workflow.name.name())}_(.+)_${Pattern.quote(configVersion)}\.xml$/
        Matcher matcher = configFile.name =~ pattern
        assert matcher.matches(): "The file name '${configFile.name}' does not match the pattern '${pattern}'"
        assert pluginVersion.endsWith(":${matcher.group(1)}")
        def configuration = new XmlParser().parseText(configFile.text)
        assert configuration.@name == getNameUsedInConfig()
    }

    String getNameUsedInConfig() {
        String nameInConfigFile = "${workflow.name}_${pluginVersion}"
        if (configVersion) {
            nameInConfigFile = "${nameInConfigFile}_${configVersion}"
        }
        return nameInConfigFile
    }

}
