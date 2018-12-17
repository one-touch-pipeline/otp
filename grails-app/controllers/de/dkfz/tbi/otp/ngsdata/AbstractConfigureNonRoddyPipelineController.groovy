package de.dkfz.tbi.otp.ngsdata

import org.springframework.validation.Errors

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.dataprocessing.WithProgramVersion

abstract class AbstractConfigureNonRoddyPipelineController extends AbstractConfigurePipelineController {

    Map index(BaseConfigurePipelineSubmitCommand cmd) {
        WithProgramVersion config = getLatestConfig(cmd.project, cmd.seqType)
        String currentVersion = config?.programVersion

        String defaultVersion = getDefaultVersion()
        List<String> availableVersions = getAvailableVersions()

        return [
                project: cmd.project,
                seqType: cmd.seqType,
                pipeline: getPipeline(),

                defaultVersion: defaultVersion,
                currentVersion: currentVersion,
                availableVersions: availableVersions,
        ] + getAdditionalProperties(cmd.project, cmd.seqType)
    }

    void updatePipeline(Errors errors, Project project, SeqType seqType) {
        if (errors) {
            flash.message = new FlashMessage(g.message(code: "configurePipeline.store.failure") as String, errors)
            redirect action: "index", params: ['project.id': project.id, 'seqType.id': seqType.id]
        } else {
            flash.message = new FlashMessage(g.message(code: "configurePipeline.store.success") as String)
            redirect controller: "projectConfig"
        }
    }

    @SuppressWarnings("UnusedMethodParameter")
    Map getAdditionalProperties(Project project, SeqType seqType) {
        return [:]
    }

    abstract WithProgramVersion getLatestConfig(Project project, SeqType seqType)

    abstract String getDefaultVersion()

    abstract List<String> getAvailableVersions()
}
