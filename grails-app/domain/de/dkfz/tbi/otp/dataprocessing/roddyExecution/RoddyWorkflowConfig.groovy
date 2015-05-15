package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.otp.dataprocessing.ConfigPerProject
import de.dkfz.tbi.otp.dataprocessing.Workflow
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.ExternalScript

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

/**
 * Each roddy call is configured by a config file. This config file can be different between the different workflows,
 * projects, seqTypes, possible sub-analyses and several other points.
 * To have a clear structure of the config files it was decided to have one config file per project and workflow.
 * The other possible distinctions will be grouped within this config file.
 * The information about the config file is store in the "RoddyWorkflowConfig"-Domain.
 *
 */
class RoddyWorkflowConfig extends ConfigPerProject {

    Workflow workflow

    // Path to the config file which is used in this project and workflow. The name of the configFile contains the version number.
    String configFilePath

    static constraints = {
        configFilePath validator: { path ->
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(new File(path))
        }
        workflow unique: ['project', 'configFilePath']
    }

    static mapping = {
        workflow index: 'roddy_workflow_config_workflow_idx'
    }

    String getWorkflowVersion () {
        return this.externalScriptVersion
    }


    static void importProjectConfigFile(Project project, String pluginVersionToUse, Workflow workflow, String configFilePath) {
        assert project : "The project is not allowed to be null"
        assert workflow : "The workflow is not allowed to be null"
        assert pluginVersionToUse:"The pluginVersionToUse is not allowed to be null"
        assert configFilePath : "The configFilePath is not allowed to be null"

        validateNewConfigFile(pluginVersionToUse, workflow, configFilePath,)

        RoddyWorkflowConfig roddyWorkflowConfig = getLatest(project, workflow)

        RoddyWorkflowConfig config = new RoddyWorkflowConfig(
                project: project,
                workflow: workflow,
                configFilePath: configFilePath,
                externalScriptVersion: pluginVersionToUse,
                previousConfig: roddyWorkflowConfig
        )
        config.createConfigPerProject()
    }


    static void validateNewConfigFile(String pluginVersionToUse, Workflow workflow, String configFilePath) {
        assert workflow : "The workflow is not allowed to be null"
        CollectionUtils.exactlyOneElement(ExternalScript.findAllByScriptIdentifierAndScriptVersionAndDeprecatedDateIsNull(workflow.name.name(), pluginVersionToUse))
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(new File(configFilePath))
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

}
