/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.filestore

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.administration.DocumentFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

import java.nio.file.Path
import java.nio.file.Paths

class FilestoreServiceSpec extends Specification implements ServiceUnitTest<FilestoreService>, DataTest, WorkflowSystemDomainFactory, DocumentFactory {

    WorkflowRun run
    WorkFolder workFolder
    BaseFolder baseFolder

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkFolder,
                BaseFolder,
                WorkflowRun,
        ]
    }

    void setup() {
        baseFolder = createBaseFolder([
                path: "/test"
        ])
        workFolder = createWorkFolder([
                baseFolder: baseFolder
        ])
        run = createWorkflowRun([
                workFolder: workFolder
        ])
    }

    void "createUuid, should create a valid UUID"() {
        given:
        UUID uuid = service.createUuid(run)

        expect:
        uuid == run.workFolder.uuid
        run.workFolder.baseFolder.writable
    }

    void "getWorkFolderPath, should return the correct path"() {
        given:
        WorkFolder anotherWorkFolder = service.createWorkFolder(baseFolder)
        service.attachWorkFolder(run, anotherWorkFolder)

        expect:
        Path path = service.getWorkFolderPath(run)
        path == Paths.get(anotherWorkFolder.baseFolder.path, run.workFolder.uuid.toString().split(WorkFolder.UUID_SEPARATOR))
    }

    void "attachWorkFolder to a WorkflowRun, workFolder has been attached to another run, which should throw an exception"() {
        given:
        WorkflowRun anotherWorkflowRun = createWorkflowRun()

        when:
        service.attachWorkFolder(anotherWorkflowRun, workFolder)

        then:
        WorkFolderAttachedException ex = thrown()
        ex.message == "WorkFolder ${workFolder} has been attached to WorkflowRun ${run}"
    }
}
