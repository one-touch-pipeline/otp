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
package de.dkfz.tbi.otp.administration

import grails.plugin.springsecurity.SpringSecurityService
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
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.domainFactory.administration.DocumentFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.CreateFileHelper

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
                },
                fileService           : new FileService(),
                springSecurityService : Mock(SpringSecurityService) {
                    _ * getCurrentUser() >> getUser(ADMIN)
                },
        )
        configService = new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT)   : temporaryFolder.newFolder().path,
                (OtpProperty.PATH_PROCESSING_ROOT): temporaryFolder.newFolder().path,
        ])
    }

    void cleanup() {
        configService.clean()
    }

    void "getAllProjectInfosSortedByDateDescAndGroupedByDta, properly groups and sorts projectInfos"() {
        given:
        setupData()
        Project project = createProject()
        List<ProjectInfo> projectInfos = [
                createProjectInfo(project: project, peerInstitution: null),
                createProjectInfo(project: project, peerInstitution: "peerA", dateCreated: new Date(2)),
                createProjectInfo(project: project, peerInstitution: "peerB", dateCreated: new Date(1)),
        ]
        project.projectInfos = projectInfos as Set<ProjectInfo>
        project.save(flush: true)

        Map<String, List<ProjectInfo>> expected = [
                "Dta": [projectInfos[2], projectInfos[1]],
                "NonDta": [projectInfos[0]],
        ]
        Map<String, List<ProjectInfo>> result

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            result = projectInfoService.getAllProjectInfosSortedByDateDescAndGroupedByDta(project)
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
        projectInfo.dtaId == cmd.dtaId
        projectInfo.peerInstitution == cmd.peerInstitution
        projectInfo.legalBasis == cmd.legalBasis
        projectInfo.validityDate == cmd.validityDate
        projectInfo.comment == cmd.comment
        projectInfo.transfers == [] as Set<DataTransfer>
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
        'validityDate'    | null
        'dtaId'           | null
        'dtaId'           | ''
        'legalBasis'      | null
        'peerInstitution' | null
        'peerInstitution' | ''
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

        //TODO: otp-163
        /*
        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.deleteProjectInfo(new ProjectInfoCommand(projectInfo: CollectionUtils.exactlyOneElement(project.projectInfos)))
        }

        then:
        project.projectInfos.size() == 0
        ProjectInfo.count == 0
        */
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

    void "addTransferToProjectInfo, creates DataTransfer and links it to ProjectInfo"() {
        given:
        setupData()
        DataTransfer dataTransferA
        DataTransfer dataTransferB
        ProjectInfo projectInfo = createProjectInfo()

        AddTransferCommand transferCmdA = createAddTransferCommand(parentDocument: projectInfo)
        AddTransferCommand transferCmdB = createAddTransferCommand(parentDocument: projectInfo)

        when: "adding a first transfer"
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            dataTransferA = projectInfoService.addTransferToProjectInfo(transferCmdA)
        }
        projectInfo.refresh()

        then:
        projectInfo.transfers == [dataTransferA] as Set<DataTransfer>
        dataTransferA.projectInfo == projectInfo

        when: "adding a second transfer"
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            dataTransferB = projectInfoService.addTransferToProjectInfo(transferCmdB)
        }
        projectInfo.refresh()

        then:
        projectInfo.transfers == [dataTransferA, dataTransferB] as Set<DataTransfer>
        dataTransferB.projectInfo == projectInfo
    }

    void "markTransferAsCompleted, completes uncompleted transfer"() {
        given:
        setupData()
        DataTransfer dataTransfer = createDataTransfer(completionDate: null)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            projectInfoService.markTransferAsCompleted(dataTransfer)
        }

        then:
        dataTransfer.completionDate != null
    }

    void "markTransferAsCompleted, fails when completing an already completed transfer"() {
        given:
        setupData()
        DataTransfer dataTransfer = createDataTransfer(completionDate: new Date())

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            projectInfoService.markTransferAsCompleted(dataTransfer)
        }

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("DataTransfer already completed")
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

    void "updateDataTransferComment, updates comment"() {
        given:
        setupData()
        DataTransfer dataTransfer = createDataTransfer(comment: "outdated")

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            projectInfoService.updateDataTransferComment(dataTransfer, "updated")
        }

        then:
        dataTransfer.comment == "updated"
    }

    AddProjectInfoCommand createAddProjectInfoCommand(Map properties = [:]) {
        return new AddProjectInfoCommand([
                projectSelectionService: [getRequestedProject: { -> }] as ProjectSelectionService,
                projectInfoFile        : createMultipartFile(),
                comment                : "comment_${nextId}",
                dtaId                  : "dtaId_${nextId}",
                legalBasis             : ProjectInfo.LegalBasis.DTA,
                peerInstitution        : "peerInstitution_${nextId}",
                validityDate           : new Date(),
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
