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

import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.dataprocessing.BamImportInstance
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext

@PreAuthorize("hasRole('ROLE_OPERATOR')")
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
                    cmd.linkOperation == BamImportInstance.LinkOperation.LINK_SOURCE)
        }

        return [
                cmd                   : cmd,
                furtherFiles          : cmd.furtherFilePaths ?: [""],
                context               : bamMetadataValidationContext,
                implementedValidations: bamMetadataImportService.implementedValidations,
        ]
    }

    def validateOrImport(BamMetadataControllerSubmitCommand cmd) {
        boolean hasRedirected = false
        BamMetadataValidationContext bamMetadataValidationContext
        withForm {
            if (cmd.hasErrors()) {
                flash.message = new FlashMessage("Error", cmd.errors)
            } else if (cmd.submit == "Import") {
                Map results = bamMetadataImportService.validateAndImport(cmd.path, cmd.ignoreWarnings, cmd.md5,
                        cmd.linkOperation, cmd.triggerAnalysis, cmd.furtherFilePaths, cmd.addDefaultRoddyBamFilePaths)
                bamMetadataValidationContext = results.context
                if (results.project != null) {
                    redirect(controller: "sampleOverview", action: "index", params: [project: results.project.name])
                    hasRedirected = true
                    return
                }
            }
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "default.message.error") as String, g.message(code: "default.invalid.session") as String)
        }
        if (!hasRedirected) {
            flash.mvc = bamMetadataValidationContext
            redirect(action: "index", params: [
                    path            : cmd.path,
                    furtherFilePaths: cmd.furtherFilePaths,
                    linkOperation   : cmd.linkOperation,
                    triggerAnalysis : cmd.triggerAnalysis,
                    addDefaultRoddyBamFilePaths: cmd.addDefaultRoddyBamFilePaths
            ])
        }
    }
}

class BamMetadataControllerSubmitCommand {
    String path
    String submit
    String md5
    List<String> furtherFilePaths
    BamImportInstance.LinkOperation linkOperation
    boolean triggerAnalysis
    boolean ignoreWarnings
    boolean addDefaultRoddyBamFilePaths

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
