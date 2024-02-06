/*
 * Copyright 2011-2024 The OTP authors
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

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

import java.nio.file.Path
import java.nio.file.Paths

class FilestoreServiceSpec extends Specification implements ServiceUnitTest<FilestoreService>, DataTest, WorkflowSystemDomainFactory {

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
        service.fileSystemService = new TestFileSystemService()
    }

    void "findAnyWritableBaseFolder: when call, should return any of the writable base folders"() {
        given:
        (1..3).each {
            createBaseFolder(writable: true)
            createBaseFolder(writable: false)
        }

        when:
        BaseFolder returnedBaseFolder = service.findAnyWritableBaseFolder()

        then:
        returnedBaseFolder
        returnedBaseFolder.writable
    }

    void "findAnyWritableBaseFolder: when no writable baseFolder exist, then throw an assertion."() {
        given:
        baseFolder.writable = false
        baseFolder.save(flush: true)
        (1..3).each {
            createBaseFolder(writable: false)
        }

        when:
        service.findAnyWritableBaseFolder()

        then:
        thrown(AssertionError)
    }

    void "test createUuid: should create a valid UUID"() {
        given:
        UUID uuid = service.createUuid(run)

        expect:
        uuid == run.workFolder.uuid
        run.workFolder.baseFolder.writable
    }

    void "test createWorkFolder: should create a valid uuid"() {
        given:
        WorkFolder workFolder = service.createWorkFolder(baseFolder)

        expect:
        baseFolder == workFolder.baseFolder
        UUID.fromString(workFolder.uuid.toString())
    }

    void "test getWorkFolderPath: should return the correct path"() {
        given:
        final String uuidString = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
        run.workFolder.uuid = UUID.fromString(uuidString)

        expect:
        service.getWorkFolderPath(run) == Paths.get(workFolder.baseFolder.path, "a0", "ee", "bc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    }

    void "test getWorkFolderPath: if workFolder not attached yet, an exception should be thrown"() {
        given:
        final WorkflowRun anotherRun = createWorkflowRun()

        when:
        Path path = service.getWorkFolderPath(anotherRun)

        then:
        thrown(WorkFolderNotAttachedException)
        path == null
    }

    void "test attachWorkFolder to a WorkflowRun: workFolder has been attached to another run, an exception should be thrown"() {
        given:
        WorkflowRun anotherWorkflowRun = createWorkflowRun()

        when:
        service.attachWorkFolder(anotherWorkflowRun, workFolder)

        then:
        thrown(grails.validation.ValidationException)
    }

    void "test attachWorkFolder to a WorkflowRun that already attached to a folder: should throw assertion"() {
        given:
        WorkFolder anotherWorkFolder = createWorkFolder(
                baseFolder: baseFolder
        )

        when:
        service.attachWorkFolder(run, anotherWorkFolder)

        then:
        AssertionError e = thrown()
        e.message.contains('work folder already attached and may not be changed')
    }

    void "test getWorkFolders with multiple WorkflowRuns, return the correct ones"() {
        given:
        WorkFolder anotherWorkFolder = createWorkFolder(
                baseFolder: baseFolder
        )
        List<WorkFolder> workFolders
        and: 'different project -> ignored'
        createWorkflowRun()
        and: 'no workFolder, old system -> ignored'
        createWorkflowRun(project: run.project, workFolder: null)
        and: 'different workfolder -> added'
        createWorkflowRun(project: run.project, workFolder: anotherWorkFolder)

        when:
        workFolders = service.getWorkFolders(run.project)

        then:
        TestCase.assertContainSame(workFolders, [workFolder, anotherWorkFolder])
    }
}
