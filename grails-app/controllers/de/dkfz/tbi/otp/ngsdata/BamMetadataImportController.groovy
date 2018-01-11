package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import org.springframework.validation.*

class BamMetadataImportController {

    BamMetadataImportService bamMetadataImportService

    def index(BamMetadataControllerSubmitCommand cmd) {
        BamMetadataValidationContext bamMetadataValidationContext
        String errorMessage
        if (cmd.hasErrors()) {
            FieldError fieldError = cmd.errors.getFieldError()
            errorMessage = "'${fieldError.getRejectedValue()}' is not a valid value for '${fieldError.getField()}'. Error code: '${fieldError.code}'"
        }
        if (cmd.submit == "Import" && !errorMessage) {

            Map results = bamMetadataImportService.validateAndImport(cmd.path, cmd.ignoreWarnings, cmd.md5,
                    cmd.replaceWithLink, cmd.triggerSnv, cmd.triggerIndel, cmd.triggerAceseq, cmd.furtherFilePaths)
            bamMetadataValidationContext = results.context
            if (results.project != null) {
                redirect(controller: "projectOverview", action: "laneOverview", params: [project: results.project.name])
            }
        } else if (cmd.submit != null) {
            bamMetadataValidationContext = bamMetadataImportService.validate(cmd.path, cmd.furtherFilePaths)
        }

        return [
                cmd: cmd,
                errorMessage: errorMessage,
                context: bamMetadataValidationContext,
                implementedValidations: bamMetadataImportService.getImplementedValidations()
        ]
    }
}

class BamMetadataControllerSubmitCommand implements Serializable {
    String path
    String submit
    String md5
    List<String> furtherFilePaths
    boolean replaceWithLink
    boolean triggerSnv
    boolean triggerIndel
    boolean triggerAceseq
    boolean ignoreWarnings

    static constraints = {
        path nullable: true
        submit nullable: true
        md5 nullable: true
        furtherFilePaths nullable: true
    }

    void setPath(String path) {
        this.path = path?.trim()
    }
}
