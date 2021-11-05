/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.project

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.springframework.mock.web.MockMultipartFile
import spock.lang.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.domainFactory.administration.DocumentFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.project.dta.AddTransferCommand
import de.dkfz.tbi.otp.project.dta.DataTransfer
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.ProcessOutput

import java.nio.file.*
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission

@Rollback
@Integration
class ProjectInfoServiceIntegrationSpec extends Specification implements UserAndRoles, DocumentFactory {

    ProjectInfoService projectInfoService
    TestConfigService configService

    @Rule
    TemporaryFolder temporaryFolder

    void setupData() {
        createUserAndRoles()
        projectInfoService = new ProjectInfoService(
                executionHelperService: Mock(ExecutionHelperService),
                fileSystemService     : Mock(FileSystemService) {
                    _ * getRemoteFileSystem(_) >> FileSystems.default
                    0 * _
                },
                fileService           : new FileService([
                        remoteShellHelper: Mock(RemoteShellHelper) {
                            _ * executeCommandReturnProcessOutput(_, _) >> { Realm realm, String command ->
                                return new ProcessOutput(command, '', 0)
                            }
                        }
                ]),
        )
        configService.addOtpProperties(temporaryFolder.newFolder().toPath())
    }

    void cleanup() {
        configService.clean()
    }

    void "getAllProjectInfosSortedByDateDesc, properly sorts projectInfos"() {
        given:
        setupData()
        Project project = createProject()
        List<ProjectInfo> projectInfos = [
                createProjectInfo(project: project),
                createProjectInfo(project: project, dateCreated: new Date(2)),
                createProjectInfo(project: project, dateCreated: new Date(1)),
        ]
        project.projectInfos = projectInfos as Set<ProjectInfo>
        project.save(flush: true)

        List<ProjectInfo> expected = [projectInfos[2], projectInfos[1], projectInfos[0]]
        List<ProjectInfo> result = []

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            result = projectInfoService.getAllProjectInfosSortedByDateDesc(project)
        }

        then:
        TestCase.assertContainSame(result, expected)
    }

    void "createProjectInfoAndUploadFile, succeeds"() {
        given:
        setupData()
        Project project = createProject()

        AddProjectInfoCommand cmd = createAddProjectInfoCommand()
        ProjectInfo projectInfo

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            projectInfo = projectInfoService.createProjectInfoAndUploadFile(project, cmd)
        }

        then:
        projectInfo.fileName == cmd.projectInfoFile.originalFilename
    }

    void "createProjectInfoAndUploadFile, with same fileName for different projects, succeeds"() {
        given:
        setupData()

        MockMultipartFile file = createMultipartFile()
        AddProjectInfoCommand cmd1 = createAddProjectInfoCommand(projectInfoFile: file)
        AddProjectInfoCommand cmd2 = createAddProjectInfoCommand(projectInfoFile: file)

        ProjectInfo projectInfo1, projectInfo2

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            projectInfo1 = projectInfoService.createProjectInfoAndUploadFile(createProject(), cmd1)
            projectInfo2 = projectInfoService.createProjectInfoAndUploadFile(createProject(), cmd2)
        }

        then:
        projectInfo1.fileName == file.name
        projectInfo2.fileName == file.name
    }

    void "createProjectInfoAndUploadFile, with same fileName for same project, fails"() {
        given:
        setupData()

        Project project = createProject()
        MockMultipartFile file = createMultipartFile()
        ProjectSelectionService projectSelectionService = [getRequestedProject: { -> project }] as ProjectSelectionService
        AddProjectInfoCommand cmd1 = createAddProjectInfoCommand(projectSelectionService: projectSelectionService, projectInfoFile: file)
        AddProjectInfoCommand cmd2 = createAddProjectInfoCommand(projectSelectionService: projectSelectionService, projectInfoFile: file)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            projectInfoService.createProjectInfoAndUploadFile(project, cmd1)
            projectInfoService.createProjectInfoAndUploadFile(project, cmd2)
        }

        then:
        AssertionError e = thrown()
        e.message.contains('duplicate')
    }

    void "createProjectInfoAndUploadFile, creates file with expected permissions in expected location"() {
        given:
        setupData()
        Project project = createProject()
        MockMultipartFile file = createMultipartFile()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectInfoService.createProjectInfoAndUploadFile(project, createAddProjectInfoCommand(projectInfoFile: file))
        }

        then:
        Path projectInfoFile = Paths.get("${project.projectDirectory}/${ProjectService.PROJECT_INFO}/${file.name}")
        PosixFileAttributes attrs = Files.readAttributes(projectInfoFile, PosixFileAttributes, LinkOption.NOFOLLOW_LINKS)

        projectInfoFile.bytes == file.bytes
        TestCase.assertContainSame(attrs.permissions(), [PosixFilePermission.OWNER_READ])
    }

    void "createProjectInfoAndUploadFile, properly build ProjectInfo from command"() {
        given:
        setupData()
        AddProjectInfoCommand cmd = createAddProjectInfoCommand()
        Project project = createProject()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectInfoService.createProjectInfoAndUploadFile(project, cmd)
        }

        then:
        Path projectInfoFile = Paths.get(project.projectDirectory.absolutePath, ProjectService.PROJECT_INFO, cmd.projectInfoFile.name)
        PosixFileAttributes attrs = Files.readAttributes(projectInfoFile, PosixFileAttributes, LinkOption.NOFOLLOW_LINKS)

        projectInfoFile.bytes == cmd.projectInfoFile.bytes
        TestCase.assertContainSame(attrs.permissions(), [PosixFilePermission.OWNER_READ])

        project.refresh()
        project.projectInfos.size() == 1

        ProjectInfo projectInfo = project.projectInfos.first()
        projectInfo.refresh()
        projectInfo.project == project
        projectInfo.fileName == cmd.projectInfoFile.originalFilename
        projectInfo.comment == cmd.comment
    }

    @Unroll
    void "createProjectInfoAndUploadFile, check constraints, when #property is '#value', then succeed"() {
        given:
        setupData()
        AddProjectInfoCommand cmd = createAddProjectInfoCommand()
        cmd[property] = value

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectInfoService.createProjectInfoAndUploadFile(createProject(), cmd)
        }

        then:
        noExceptionThrown()

        where:
        property          | value
        'comment'         | null
        'comment'         | ''
    }

    @Unroll
    void "createProjectInfoAndUploadFile, check constraints, when #property is '#value', then fail"() {
        given:
        setupData()
        AddProjectInfoCommand cmd = createAddProjectInfoCommand()
        cmd[property] = value

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectInfoService.createProjectInfoAndUploadFile(createProject(), cmd)
        }

        then:
        AssertionError e = thrown()
        e.message =~ /.*${property}.*${constraint}.*/

        where:
        property          | value || constraint
        'projectInfoFile' | null  || 'nullable'
    }

    void "createProjectInfoAndUploadFile, when project is null, then fail"() {
        given:
        setupData()
        AddProjectInfoCommand cmd = createAddProjectInfoCommand()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectInfoService.createProjectInfoAndUploadFile(null, cmd)
        }

        then:
        AssertionError e = thrown()
        e.message =~ /.*project must not be null.*/
    }

    void "add and remove a project info with createProjectInfoAndUploadFile and deleteProjectInfo"() {
        given:
        setupData()
        Project project = createProject()
        byte[] projectInfoContent
        MockMultipartFile file = createMultipartFile()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectInfoService.createProjectInfoAndUploadFile(project, createAddProjectInfoCommand(projectInfoFile: file))
            projectInfoContent = projectInfoService.getProjectInfoContent(CollectionUtils.exactlyOneElement(project.projectInfos))
        }

        then:
        projectInfoContent == file.bytes
        project.projectInfos.size() == 1
    }

    @Ignore("TODO: otp-163, cannot test this until we have a postgres DB instead of H2") // TODO otp-163
    void "deleteProjectInfo, properly removes entries from project and project info"() {
        expect:
        assert false
    }

    void "getProjectInfoContent, returns file content or empty byte array if file does not exist"() {
        given:
        setupData()
        ProjectInfo projectInfo = createProjectInfo()
        if (fileExists) {
            CreateFileHelper.createFile(new File(projectInfo.path), "content")
        }

        byte[] content

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            content = projectInfoService.getProjectInfoContent(projectInfo)
        }

        then:
        content == expected

        where:
        fileExists || expected
        true       || "content".bytes
        false      || [] as byte[]
    }

    void "uploadProjectInfoToProjectFolder, creates file"() {
        given:
        setupData()

        byte[] content = "content".bytes
        ProjectInfo projectInfo = createProjectInfo()
        Path file

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            file = projectInfoService.uploadProjectInfoToProjectFolder(projectInfo, content)
        }

        then:
        Files.exists(file)
        file.bytes == content
    }

    void "updateProjectInfoComment, updates comment"() {
        given:
        setupData()
        ProjectInfo projectInfo = createProjectInfo(comment: "outdated")

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            projectInfoService.updateProjectInfoComment(projectInfo, "updated")
        }

        then:
        projectInfo.comment == "updated"
    }

    AddProjectInfoCommand createAddProjectInfoCommand(Map properties = [:]) {
        return new AddProjectInfoCommand([
                projectSelectionService: [getRequestedProject: { -> }] as ProjectSelectionService,
                projectInfoFile        : createMultipartFile(),
                comment                : "comment_${nextId}",
        ] + properties)
    }

    AddTransferCommand createAddTransferCommand(Map properties = [:]) {
        return new AddTransferCommand([
                parentDocument: createProjectInfo(),
                peerPerson    : "peerPerson_${nextId}",
                transferMode  : DataTransfer.TransferMode.ASPERA,
                direction     : DataTransfer.Direction.OUTGOING,
                requester     : "requester_${nextId}",
                transferDate  : new Date(),
        ] + properties)
    }
}
