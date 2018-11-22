package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*

class BamMetadataImportController {

    static allowedMethods = [
            index           : "GET",
            validateOrImport: "POST",
    ]

    BamMetadataImportService bamMetadataImportService

    def index(BamMetadataControllerSubmitCommand cmd) {
        BamMetadataValidationContext bamMetadataValidationContext
        if (flash.mvc) {
            bamMetadataValidationContext = flash.mvc
        } else if (cmd.path) {
            bamMetadataValidationContext = bamMetadataImportService.validate(cmd.path, cmd.furtherFilePaths)
        }

        return [
                cmd                   : cmd,
                furtherFiles          : cmd.furtherFilePaths ?: [""],
                context               : bamMetadataValidationContext,
                implementedValidations: bamMetadataImportService.getImplementedValidations()
        ]
    }

    def validateOrImport(BamMetadataControllerSubmitCommand cmd) {
        BamMetadataValidationContext bamMetadataValidationContext
        if (cmd.hasErrors()) {
            flash.message = new FlashMessage("Error", cmd.errors)
        } else if (cmd.submit == "Import") {
            Map results = bamMetadataImportService.validateAndImport(cmd.path, cmd.ignoreWarnings, cmd.md5,
                    cmd.replaceWithLink, cmd.triggerAnalysis, cmd.furtherFilePaths)
            bamMetadataValidationContext = results.context
            if (results.project != null) {
                redirect(controller: "projectOverview", action: "laneOverview", params: [project: results.project.name])
                return
            }
        }
        flash.mvc = bamMetadataValidationContext
        redirect(action: "index", params: [
                path            : cmd.path,
                furtherFilePaths: cmd.furtherFilePaths,
                replaceWithLink : cmd.replaceWithLink,
                triggerAnalysis : cmd.triggerAnalysis,
        ])
    }
}

class BamMetadataControllerSubmitCommand implements Serializable {
    String path
    String submit
    String md5
    List<String> furtherFilePaths
    boolean replaceWithLink
    boolean triggerAnalysis
    boolean ignoreWarnings

    static constraints = {
        path blank: false
        submit nullable: true
        md5 nullable: true
        furtherFilePaths nullable: true
    }

    void setPath(String path) {
        this.path = path?.trim()
    }
}
