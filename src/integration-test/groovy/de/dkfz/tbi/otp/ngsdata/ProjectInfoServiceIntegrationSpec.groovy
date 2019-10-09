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

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import grails.validation.ValidationException
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.springframework.mock.web.MockMultipartFile
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.*
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission

@SuppressWarnings(['JUnitPublicProperty'])
@Rollback
@Integration
class ProjectInfoServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore {

    ProjectService projectService
    ProjectInfoService projectInfoService
    TestConfigService configService

    static final String FILE_NAME = "fileName"
    static final byte[] CONTENT = 0..3

    @Rule
    TemporaryFolder temporaryFolder

    void setupData() {
        createUserAndRoles()
        configService = new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT)   : temporaryFolder.newFolder().path,
                (OtpProperty.PATH_PROCESSING_ROOT): temporaryFolder.newFolder().path,
        ])

    }

    void cleanup() {
        configService.clean()
    }

    void "test createProjectInfo, succeeds"() {
        given:
        setupData()
        Project project = createProject()

        when:
        ProjectInfo projectInfo = projectInfoService.createProjectInfo(project, FILE_NAME)

        then:
        projectInfo.fileName == FILE_NAME
    }

    void "test createProjectInfo, with same fileName for different projects, succeeds"() {
        given:
        setupData()
        Project project1 = createProject()
        Project project2 = createProject()

        when:
        ProjectInfo projectInfo1 = projectInfoService.createProjectInfo(project1, FILE_NAME)
        ProjectInfo projectInfo2 = projectInfoService.createProjectInfo(project2, FILE_NAME)

        then:
        projectInfo1.fileName == FILE_NAME
        projectInfo2.fileName == FILE_NAME
    }

    void "test createProjectInfo, with same fileName for same project, fails"() {
        given:
        setupData()
        Project project = createProject()

        when:
        projectInfoService.createProjectInfo(project, FILE_NAME)
        projectInfoService.createProjectInfo(project, FILE_NAME)

        then:
        ValidationException ex = thrown()
        ex.message.contains('unique')
    }

    void "test createProjectInfoAndUploadFile, succeeds"() {
        given:
        setupData()
        Project project = createProject()
        MockMultipartFile mockMultipartFile = new MockMultipartFile(FILE_NAME, CONTENT)
        mockMultipartFile.originalFilename = FILE_NAME

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectInfoService.createProjectInfoAndUploadFile(new AddProjectInfoCommand(project: project, projectInfoFile: mockMultipartFile))
        }

        then:
        Path projectInfoFile = Paths.get("${project.projectDirectory}/${projectService.PROJECT_INFO}/${FILE_NAME}")
        PosixFileAttributes attrs = Files.readAttributes(projectInfoFile, PosixFileAttributes, LinkOption.NOFOLLOW_LINKS)

        projectInfoFile.bytes == CONTENT
        TestCase.assertContainSame(attrs.permissions(), [PosixFilePermission.OWNER_READ])
    }

    void "test copyProjectInfoToProjectFolder, succeeds"() {
        given:
        setupData()
        Project project = createProject()
        byte[] projectInfoContent
        MockMultipartFile mockMultipartFile = new MockMultipartFile(FILE_NAME, CONTENT)
        mockMultipartFile.originalFilename = FILE_NAME

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectInfoService.createProjectInfoAndUploadFile(new AddProjectInfoCommand(project: project, projectInfoFile: mockMultipartFile))
            projectInfoContent = projectInfoService.getProjectInfoContent(CollectionUtils.exactlyOneElement(project.projectInfos))
        }

        then:
        projectInfoContent == CONTENT
        project.projectInfos.size() == 1
        /* TODO: otp-163
        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.deleteProjectInfo(new ProjectInfoCommand(projectInfo: CollectionUtils.exactlyOneElement(project.projectInfos)))
        }

        then:
        project.projectInfos.size() == 0
        ProjectInfo.count == 0
        */
    }

    void "test copyProjectInfoToProjectFolder, when no file exists, returns []"() {
        given:
        setupData()
        Project project = createProject()
        byte[] projectInfoContent = []
        MockMultipartFile mockMultipartFile = new MockMultipartFile(FILE_NAME, CONTENT)
        mockMultipartFile.originalFilename = FILE_NAME

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectInfoService.createProjectInfoAndUploadFile(new AddProjectInfoCommand(project: project, projectInfoFile: mockMultipartFile))
            ProjectInfo projectInfo = CollectionUtils.exactlyOneElement(project.projectInfos)
            FileSystem fs = projectInfoService.fileSystemService.getFilesystemForConfigFileChecksForRealm(projectInfo.project.realm)
            Path file = fs.getPath(projectInfo.path)
            Files.delete(file)

            projectInfoContent = projectInfoService.getProjectInfoContent(CollectionUtils.exactlyOneElement(project.projectInfos))
        }

        then:
        projectInfoContent == [] as byte[]
    }

    private AddProjectDtaCommand createAddProjectDtaCommand() {
        return new AddProjectDtaCommand([
                project             : createProject(),
                projectInfoFile     : new MockMultipartFile(FILE_NAME, FILE_NAME, null, CONTENT),
                recipientInstitution: "recipientInstitution_${nextId}",
                recipientPerson     : "recipientPerson_${nextId}",
                recipientAccount    : "recipientAccount_${nextId}",
                transferDate        : new Date(),
                validityDate        : new Date(),
                transferMode        : ProjectInfo.TransferMode.ASPERA,
                legalBasis          : ProjectInfo.LegalBasis.DTA,
                dtaId               : "dtaId_${nextId}",
                requester           : "requester_${nextId}",
                ticketID            : "ticket_${nextId}",
                comment             : "comment_${nextId}",
        ])
    }

    void "createProjectDtaInfoAndUploadFile, succeeds"() {
        given:
        setupData()
        AddProjectDtaCommand cmd = createAddProjectDtaCommand()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectInfoService.createProjectDtaInfoAndUploadFile(cmd)
        }

        then:
        Path projectInfoFile = Paths.get(cmd.project.projectDirectory.absolutePath, ProjectService.PROJECT_INFO, FILE_NAME)
        PosixFileAttributes attrs = Files.readAttributes(projectInfoFile, PosixFileAttributes, LinkOption.NOFOLLOW_LINKS)

        projectInfoFile.bytes == CONTENT
        TestCase.assertContainSame(attrs.permissions(), [PosixFilePermission.OWNER_READ])

        cmd.project.refresh()
        cmd.project.projectInfos.size() == 1

        ProjectInfo projectInfo = cmd.project.projectInfos.first()
        projectInfo.project
        projectInfo.fileName
        projectInfo.recipientInstitution
        projectInfo.recipientPerson
        projectInfo.recipientAccount
        projectInfo.transferDate
        projectInfo.validityDate
        projectInfo.legalBasis
        projectInfo.dtaId
        projectInfo.requester
        projectInfo.performingUser
        projectInfo.ticketID
        projectInfo.comment
    }

    @Unroll
    void "createProjectDtaInfoAndUploadFile, when #property is '#value', then succeed"() {
        given:
        setupData()
        AddProjectDtaCommand cmd = createAddProjectDtaCommand()
        cmd[property] = value

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectInfoService.createProjectDtaInfoAndUploadFile(cmd)
        }

        then:
        noExceptionThrown()

        where:
        property           | value
        'recipientAccount' | null
        'recipientAccount' | ''
        'validityDate'     | null
        'dtaId'            | null
        'dtaId'            | ''
        'ticketID'         | null
        'ticketID'         | ''
        'comment'          | null
        'comment'          | ''
    }

    @Unroll
    void "createProjectDtaInfoAndUploadFile, when #property is '#value', then fail"() {
        given:
        setupData()
        AddProjectDtaCommand cmd = createAddProjectDtaCommand()
        cmd[property] = value

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectInfoService.createProjectDtaInfoAndUploadFile(cmd)
        }

        then:
        AssertionError e = thrown()
        e.message =~ /.*${property}.*${constraint}.*/

        where:
        property               | value || constraint
        'project'              | null  || 'nullable'
        'projectInfoFile'      | null  || 'nullable'
        'recipientInstitution' | null  || 'nullable'
        'recipientInstitution' | ''    || 'blank'
        'recipientPerson'      | null  || 'nullable'
        'recipientPerson'      | ''    || 'blank'
        'transferDate'         | null  || 'nullable'
        'legalBasis'           | null  || 'nullable'
        'requester'            | null  || 'nullable'
        'requester'            | ''    || 'blank'
    }

    void "markDtaDataAsDeleted, when delete date is not yet set, then set date"() {
        given:
        setupData()
        ProjectInfoCommand cmd = new ProjectInfoCommand(projectInfo: DomainFactory.createProjectInfo())

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectInfoService.markDtaDataAsDeleted(cmd)
        }

        then:
        ProjectInfo.get(cmd.projectInfo.id).deletionDate
    }

    void "markDtaDataAsDeleted, when delete date is already set, then throw assertion"() {
        given:
        setupData()
        ProjectInfoCommand cmd = new ProjectInfoCommand(projectInfo: DomainFactory.createProjectInfo([
                deletionDate: new Date(),
        ]))

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectInfoService.markDtaDataAsDeleted(cmd)
        }

        then:
        thrown(AssertionError)
    }
}
