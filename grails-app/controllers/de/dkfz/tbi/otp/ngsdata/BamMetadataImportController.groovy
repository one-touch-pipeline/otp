/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.dataprocessing.ImportProcess
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext

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
            bamMetadataValidationContext = bamMetadataImportService.validate(cmd.path, cmd.furtherFilePaths,
                    cmd.linkOperation == ImportProcess.LinkOperation.LINK_SOURCE)
        }

        return [
                cmd                   : cmd,
                furtherFiles          : cmd.furtherFilePaths ?: [""],
                context               : bamMetadataValidationContext,
                implementedValidations: bamMetadataImportService.implementedValidations,
        ]
    }

    def validateOrImport(BamMetadataControllerSubmitCommand cmd) {
        BamMetadataValidationContext bamMetadataValidationContext
        if (cmd.hasErrors()) {
            flash.message = new FlashMessage("Error", cmd.errors)
        } else if (cmd.submit == "Import") {
            Map results = bamMetadataImportService.validateAndImport(cmd.path, cmd.ignoreWarnings, cmd.md5,
                    cmd.linkOperation, cmd.triggerAnalysis, cmd.furtherFilePaths)
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
                linkOperation   : cmd.linkOperation,
                triggerAnalysis : cmd.triggerAnalysis,
        ])
    }
}

class BamMetadataControllerSubmitCommand implements Serializable {
    String path
    String submit
    String md5
    List<String> furtherFilePaths
    ImportProcess.LinkOperation linkOperation
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
