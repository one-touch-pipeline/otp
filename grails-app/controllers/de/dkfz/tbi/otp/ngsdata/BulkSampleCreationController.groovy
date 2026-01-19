/*
 * Copyright 2011-2026 The OTP authors
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

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.utils.spreadsheet.Delimiter

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class BulkSampleCreationController {

    static allowedMethods = [
            index : "GET",
            upload: "POST",
            submit: "POST",
    ]

    CommentService commentService
    ProjectSelectionService projectSelectionService
    SampleIdentifierService sampleIdentifierService

    Map index() {
        return [
                delimiters              : Delimiter.simpleValues(),
                delimiter               : flash.delimiter,
                header                  : SampleIdentifierService.BulkSampleCreationHeader.values(),
                sampleText              : flash.sampleText ?: SampleIdentifierService.BulkSampleCreationHeader.getHeaders(Delimiter.TAB),
        ]
    }

    def upload(UploadCSVCommand cmd) {
        flash.sampleText = new String(cmd.content)
        redirect(action: "index")
    }

    def submit(CreateBulkSampleCreationCommand cmd) {
        flash.sampleText = cmd.sampleText
        flash.delimiter = cmd.delimiter

        if (cmd.hasErrors()) {
            flash.message = new FlashMessage("Error", cmd.errors)
        } else {
            Sample.withTransaction { status ->
                List<String> errors = sampleIdentifierService.createBulkSamples(
                        sampleIdentifierService.removeExcessWhitespaceFromCharacterDelimitedText(cmd.sampleText, cmd.delimiter),
                        cmd.delimiter,
                        projectSelectionService.requestedProject,
                )

                if (errors) {
                    status.setRollbackOnly()
                    flash.message = new FlashMessage(g.message(code: "bulk.sample.creation.fail") as String, errors)
                } else {
                    flash.message = new FlashMessage(g.message(code: "bulk.sample.creation.succ") as String)
                }
            }
        }
        redirect(action: "index")
    }
}

class CreateBulkSampleCreationCommand {
    Delimiter delimiter
    String sampleText
}

class UploadCSVCommand {
    byte[] content
}
