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


import de.dkfz.tbi.otp.*
import de.dkfz.tbi.util.spreadsheet.Spreadsheet

class BulkSampleCreationController {

    static allowedMethods = [
            index: "GET",
            upload: "POST",
            submit: "POST",
    ]

    ProjectService projectService
    CommentService commentService
    SampleIdentifierService sampleIdentifierService
    ProjectSelectionService projectSelectionService

    Map index() {

        List<Project> projects = projectService.allProjects
        ProjectSelection selection = projectSelectionService.selectedProject
        Project project = projectSelectionService.getProjectFromProjectSelectionOrAllProjects(selection)

        return [
                projects: projects,
                project: project,
                delimiters: Spreadsheet.Delimiter.values(),
                header: SampleIdentifierService.BulkSampleCreationHeader.values(),
                sampleText: flash.sampleText ?: flash.sampleText ?: SampleIdentifierService.BulkSampleCreationHeader.getHeaders(Spreadsheet.Delimiter.COMMA),
                delimiter: flash.delimiter,
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
            Realm.withTransaction { status ->
                List<String> errors = sampleIdentifierService.createBulkSamples(
                                cmd.sampleText.replaceAll(/ *${cmd.delimiter.delimiter.toString()} */,cmd.delimiter.delimiter.toString()),
                                cmd.delimiter,
                                cmd.project
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
    Spreadsheet.Delimiter delimiter
    Project project
    String sampleText
}

class UploadCSVCommand {
    byte[] content
}
