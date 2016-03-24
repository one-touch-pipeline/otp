package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.Workflow
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.utils.CollectionUtils
import grails.converters.JSON
import org.springframework.validation.FieldError
import de.dkfz.tbi.otp.dataprocessing.OtpPath

class ConfigureAlignmentController {

    ProjectService projectService
    ReferenceGenomeService referenceGenomeService



    Map index(ConfigureAlignmentSubmitCommand cmd) {
        Project project
        if (params.projectName) {
            project = projectService.getProjectByName(params.projectName)
        } else {
            response.sendError(404)
            return
        }
        if (!project) {
            response.sendError(404)
            return
        }

        boolean hasErrors
        String message = ""
        List<String> deciders = [
                "No Alignment",
                "OTP Alignment",
                "PanCan Alignment"
        ]
        String decider
        switch (project.alignmentDeciderBeanName) {
            case "noAlignmentDecider":
                decider = "No Alignment"
                break
            case "defaultOtpAlignmentDecider":
                decider = "OTP Alignment"
                break
            case "panCanAlignmentDecider":
                decider = "PanCan Alignment"
                break
        }
        String workflowName = "QualityControlWorkflows"
        Workflow workflow = CollectionUtils.exactlyOneElement(Workflow.findAllByTypeAndName(
                Workflow.Type.ALIGNMENT,
                Workflow.Name.PANCAN_ALIGNMENT,
        ))
        String pluginVersion= "1.0.182"
        String configVersion = CollectionUtils.atMostOneElement(RoddyWorkflowConfig.findAllByWorkflowAndPluginVersionAndProjectAndObsoleteDateIsNull(workflow, "${workflowName}:${pluginVersion}", project))?.configVersion
        if (configVersion) {
            Set<String> versions = configVersion.split("_")
            configVersion = versions[0] + "_" + (versions[1].toInteger() + 1)
        } else {
            configVersion="v1_0"
        }

        if (cmd.submit == "Submit") {
            hasErrors = cmd.hasErrors()
            boolean invalidConfigVersion = false
            RoddyWorkflowConfig.findAllByWorkflowAndPluginVersionAndProject(workflow, "${workflowName}:${cmd.plugin}", project).each({
                if (it.configVersion == cmd.config) {
                    invalidConfigVersion = true
                }
            })
            if (hasErrors) {
                FieldError errors = cmd.errors.getFieldError()
                message = "'" + errors.getRejectedValue() + "' is not a valid value for '" + errors.getField() + "'. Error code: '" + errors.code + "'"
            } else if (invalidConfigVersion && cmd.decider == "PanCan Alignment+") {
                hasErrors = true
                message = "'"+cmd.config+"' is not a valid value for 'Config Version'. Error code: 'duplicate'"
            }
            else {
                message = "Successfully changed alignment decider of project '" + project.name + "' to"
                if (cmd.decider == "No Alignment") {
                    projectService.configureNoAlignmentDeciderProject(project)
                    message += " to 'No Alignment'"
                } else if (cmd.decider == "OTP Alignment") {
                    projectService.configureDefaultOtpAlignmentDecider(project, cmd.referenceGenome)
                    message += " to 'OTP Alignment'"
                } else if (cmd.decider == "PanCan Alignment") {
                    projectService.configurePanCanAlignmentDeciderProject(project, cmd.referenceGenome, cmd.plugin, cmd.statSizeFileName, cmd.unixGroup, cmd.mergeTool, cmd.config)
                    message += " to 'PanCan Alignment'"
                }
            }
        }
        return [
            projectName: project.name,
            deciders: deciders,
            decider: (cmd.submit == "Submit") ? cmd.decider : decider,
            referenceGenomes: ReferenceGenome.list(sort: "name", order: "asc")*.name,
            referenceGenome: (cmd.submit == "Submit") ? cmd.referenceGenome : ReferenceGenomeProjectSeqType.findByProjectAndDeprecatedDateIsNull(project)?.referenceGenome?.name?: "hs37d5",
            plugin: (cmd.submit == "Submit") ? cmd.plugin : pluginVersion,
            unixGroup: (cmd.submit == "Submit") ? cmd.unixGroup : "",
            mergeTools: [ProjectService.PICARD, ProjectService.BIOBAMBAM],
            mergeTool: (cmd.submit == "Submit") ? cmd.mergeTool : ProjectService.PICARD,
            config: (cmd.submit == "Submit") ? cmd.config : configVersion,
            message: message,
            hasErrors: hasErrors,
        ]
    }

    JSON getStatSizeFileNames(String referenceGenome) {
        Map data = [
                data: ReferenceGenome.findByName(referenceGenome)?.getStatSizeFileNames()*.name
        ]
        render data as JSON
    }

}

class ConfigureAlignmentSubmitCommand implements Serializable {
    String decider
    String referenceGenome
    String plugin
    String statSizeFileName
    String unixGroup
    String mergeTool
    String config
    String submit

    static constraints = {
        plugin(validator: {val, obj ->
            if (obj.decider == "PanCan Alignment") {
                if (val == "") {
                    return 'Empty'
                }
                if (!(OtpPath.isValidPathComponent(val))) {
                    return 'Invalid path component'
                }
            }
        })
        statSizeFileName(nullable: true, validator: {val, obj ->
            if (obj.decider == "PanCan Alignment") {
                if (!val) {
                    return 'Invalid statSizeFileName'
                }
            }
        })
        unixGroup(validator: {val, obj ->
            if (obj.decider == "PanCan Alignment") {
                if (val == "") {
                    return 'Empty'
                }
                if (!(OtpPath.isValidPathComponent(val))) {
                    return 'Unix group contains invalid characters'
                }
            }
        })
        config(validator: {val, obj ->
            if (obj.decider == "PanCan Alignment") {
                if (val == "") {
                    return 'Empty'
                }
                if (!(val ==~ /^v\d+_\d+$/)) {
                    return "Not a valid config version. Must look like 'v1_0'"
                }
            }
        })
    }

    void setPlugin(String plugin) {
        this.plugin = plugin?.trim()?.replaceAll(" +", " ")
    }

    void setUnixGroup(String unixGroup) {
        this.unixGroup = unixGroup?.trim()?.replaceAll(" +", " ")
    }

    void setConfig(String config) {
        this.config = config?.trim()?.replaceAll(" +", " ")
    }
}
