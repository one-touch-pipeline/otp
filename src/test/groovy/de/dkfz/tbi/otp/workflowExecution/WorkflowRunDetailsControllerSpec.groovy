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
package de.dkfz.tbi.otp.workflowExecution

import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.filestore.WorkFolder
import de.dkfz.tbi.otp.infrastructure.LoginFailedRemoteFileSystemException

import java.nio.file.Paths

class WorkflowRunDetailsControllerSpec extends Specification implements ControllerUnitTest<WorkflowRunDetailsController> {

    void setup() {
        controller.workflowRunService = Mock(WorkflowRunService)
        controller.filestoreService = Mock(FilestoreService)
    }

    void "index displays a dash when no work folder is attached"() {
        given:
        WorkflowRun run = new WorkflowRun(id: 1)

        when:
        Map model = controller.index(new RunShowDetailsCommand(id: run))

        then:
        model.workFolderPath == "-"
        model.workflowRun == run
        0 * controller.filestoreService._
    }

    void "index displays the resolved work folder path"() {
        given:
        WorkFolder workFolder = new WorkFolder()
        WorkflowRun run = new WorkflowRun(id: 1, workFolder: workFolder)

        when:
        Map model = controller.index(new RunShowDetailsCommand(id: run))

        then:
        1 * controller.filestoreService.getWorkFolderPath(workFolder) >> Paths.get("/work/run")
        model.workFolderPath == "/work/run"
        model.workflowRun == run
    }

    void "index remains available when filestore login fails"() {
        given:
        WorkFolder workFolder = new WorkFolder()
        WorkflowRun run = new WorkflowRun(id: 1, workFolder: workFolder)
        messageSource.addMessage("workflowRun.details.workFolder.unavailable", request.locale, "Filestore unavailable")

        when:
        Map model = controller.index(new RunShowDetailsCommand(id: run))

        then:
        1 * controller.filestoreService.getWorkFolderPath(workFolder) >> {
            throw new LoginFailedRemoteFileSystemException("Login failed")
        }
        model.workFolderPath == "Filestore unavailable"
        model.workflowRun == run
        model.combinedConfig == "{}"
    }
}
