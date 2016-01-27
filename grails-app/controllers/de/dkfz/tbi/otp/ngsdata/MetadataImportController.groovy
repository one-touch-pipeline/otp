package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Problems


class MetadataImportController {

    MetadataImportService metadataImportService

    def index(MetadataImportControllerSubmitCommand cmd) {
        MetadataValidationContext metadataValidationContext
        if (cmd.submit == "Validate") {
            metadataValidationContext = metadataImportService.validate(new File(cmd.path), cmd.directory)
        } else if (cmd.submit == "Import") {
            ValidateAndImportResult validateAndImportResult = metadataImportService.validateAndImport(new File(cmd.path), cmd.directory, cmd.align, cmd.ignoreWarnings, cmd.md5)
            metadataValidationContext = validateAndImportResult.context
            if (validateAndImportResult.runId != null) {
                redirect(controller: "run", action: "show", id: validateAndImportResult.runId)
            }
        } else {
            cmd = null
        }
        return [
                directoryStructures: metadataImportService.getSupportedDirectoryStructures(),
                cmd                : cmd,
                context            : metadataValidationContext,
                implementedValidations: metadataImportService.getImplementedValidations()
        ]
    }
}

class MetadataImportControllerSubmitCommand implements Serializable {
    String path
    String directory
    String md5
    String submit
    boolean align
    boolean ignoreWarnings
}
