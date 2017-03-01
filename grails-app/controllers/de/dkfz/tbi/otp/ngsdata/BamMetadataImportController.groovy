package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import org.springframework.validation.FieldError

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
            Map results = bamMetadataImportService.validateAndImport(new File(cmd.path), cmd.ignoreWarnings, cmd.md5, cmd.replaceWithLink, cmd.triggerSnv, cmd.triggerIndel, cmd.triggerAceseq)
            bamMetadataValidationContext = results.context
            if (results.project != null) {
                redirect(controller: "projectOverview", action: "laneOverview", params: [project: results.project.name])
            }
        } else if (cmd.submit != null) {
            bamMetadataValidationContext = bamMetadataImportService.validate(new File(cmd.path))
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
    boolean replaceWithLink
    boolean triggerSnv
    boolean triggerIndel
    boolean triggerAceseq
    boolean ignoreWarnings

    static constraints = {
        path nullable: true
        submit nullable: true
        md5 nullable:true
    }

    void setPath(String path) {
        this.path = path?.trim()
    }
}
