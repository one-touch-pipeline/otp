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

import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.security.User

import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

@Transactional
class ProjectInfoService {

    SpringSecurityService springSecurityService
    FileSystemService fileSystemService
    FileService fileService

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void createProjectInfoAndUploadFile(AddProjectInfoCommand cmd) throws IOException {
        cmd.validate()
        assert !cmd.errors.hasErrors()
        ProjectInfo projectInfo = createProjectInfo(cmd.project, cmd.projectInfoFile.originalFilename)
        uploadProjectInfoToProjectFolder(projectInfo, cmd.projectInfoFile.bytes)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void createProjectDtaInfoAndUploadFile(AddProjectDtaCommand cmd) throws IOException {
        cmd.validate()
        assert !cmd.errors.hasErrors()
        ProjectInfo projectInfo = createProjectInfo(cmd.project, cmd.projectInfoFile.originalFilename)
        addAdditionalValuesToProjectInfo(projectInfo, cmd, springSecurityService.currentUser as User)
        uploadProjectInfoToProjectFolder(projectInfo, cmd.projectInfoFile.bytes)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void markDtaDataAsDeleted(ProjectInfoCommand cmd) throws IOException {
        cmd.validate()
        assert !cmd.errors.hasErrors()
        ProjectInfo projectInfo = cmd.projectInfo
        assert !projectInfo.deletionDate
        projectInfo.refresh()
        projectInfo.deletionDate = new Date()
        projectInfo.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void deleteProjectInfo(ProjectInfoCommand cmd) throws IOException {
        cmd.validate()
        assert !cmd.errors.hasErrors()
        ProjectInfo projectInfo = cmd.projectInfo
        FileSystem fs = fileSystemService.getRemoteFileSystem(projectInfo.project.realm)
        Path path = fs.getPath(projectInfo.getPath())
        fileService.deleteDirectoryRecursively(path)
        projectInfo.project = null
        projectInfo.delete(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    byte[] getProjectInfoContent(ProjectInfo projectInfo) {
        assert projectInfo: "No ProjectInfo given"
        FileSystem fs = fileSystemService.getFilesystemForConfigFileChecksForRealm(projectInfo.project.realm)
        Path file = fs.getPath(projectInfo.path)

        return Files.exists(file) ? file.bytes : [] as byte[]
    }

    private ProjectInfo createProjectInfo(Project project, String fileName) {
        ProjectInfo projectInfo = new ProjectInfo([fileName: fileName])
        project.addToProjectInfos(projectInfo)
        project.save(flush: true)
        return projectInfo
    }

    private void addAdditionalValuesToProjectInfo(ProjectInfo projectInfo, AddProjectDtaCommand cmd, User performingUser) {
        projectInfo.performingUser = performingUser
        cmd.values().each {
            projectInfo[it.key] = it.value
        }
        projectInfo.save(flush: true)
    }

    private void uploadProjectInfoToProjectFolder(ProjectInfo projectInfo, byte[] content) {
        FileSystem fs = fileSystemService.getFilesystemForConfigFileChecksForRealm(projectInfo.project.realm)
        Path projectDirectory = fs.getPath(projectInfo.project.projectDirectory.toString())
        Path projectInfoDirectory = projectDirectory.resolve(ProjectService.PROJECT_INFO)

        Path file = projectInfoDirectory.resolve(projectInfo.fileName)

        fileService.createFileWithContent(file, content, [
                PosixFilePermission.OWNER_READ,
        ] as Set)
    }
}
