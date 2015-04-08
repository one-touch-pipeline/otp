package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.otp.dataprocessing.ConfigPerProject
import de.dkfz.tbi.otp.dataprocessing.Workflow
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService

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

    String configFilePath

    static constraints = {
        configFilePath validator: { path ->
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(new File(path))
        }
    }

    static mapping = {
        workflow index: 'roddy_workflow_config_workflow_idx'
    }

    String getWorkflowVersion () {
        return this.externalScriptVersion
    }

}
