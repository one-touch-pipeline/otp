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
package de.dkfz.tbi.otp.project

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.FileSystemService

import java.nio.file.*
import java.nio.file.attribute.PosixFilePermission

@Transactional
class ProjectInfoService {

    ExecutionHelperService executionHelperService
    FileSystemService fileSystemService
    FileService fileService
    ProjectService projectService

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<ProjectInfo> getAllProjectInfosSortedByDateDesc(Project project) {
        return project.projectInfos.sort { a, b ->
            b.dateCreated <=> a.dateCreated
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    ProjectInfo createProjectInfoAndUploadFile(Project project, AddProjectInfoCommand cmd) {
        cmd.validate()
        assert !cmd.errors.hasErrors()
        assert project: "project must not be null"
        ProjectInfo projectInfo = new ProjectInfo(
                fileName: cmd.projectInfoFile.originalFilename,
                comment: cmd.comment,
        )

        project.addToProjectInfos(projectInfo)
        project.save(flush: true)
        projectInfo.save(flush: true)

        uploadProjectInfoToProjectFolder(projectInfo, cmd.projectInfoFile.bytes)

        return projectInfo
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    ProjectInfo createProjectInfoByPath(Project project, Path path) {
        ProjectInfo projectInfo = new ProjectInfo(
                fileName: path.fileName.toString(),
                comment: "File copied from ${path.toAbsolutePath()}",
        )

        project.addToProjectInfos(projectInfo)
        project.save(flush: true)
        projectInfo.save(flush: true)

        uploadProjectInfoToProjectFolder(projectInfo, path.bytes)

        return projectInfo
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    void deleteProjectInfo(ProjectInfoCommand cmd) {
        cmd.validate()
        assert !cmd.errors.hasErrors()
        ProjectInfo projectInfo = cmd.projectInfo
        Path path = getPath(projectInfo)
        fileService.deleteDirectoryRecursively(path)
        projectInfo.project = null
        projectInfo.delete(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    byte[] getProjectInfoContent(ProjectInfo projectInfo) {
        assert projectInfo: "No ProjectInfo given"
        Path file = getPath(projectInfo)
        return Files.exists(file) ? file.bytes : [] as byte[]
    }

    private Path uploadProjectInfoToProjectFolder(ProjectInfo projectInfo, byte[] content) {
        Path file = getPath(projectInfo)

        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(file.parent,
                '', FileService.OWNER_DIRECTORY_PERMISSION_STRING)
        fileService.createFileWithContent(file, content, [PosixFilePermission.OWNER_READ] as Set)
        fileService.setGroupViaBash(file, projectInfo.project.unixGroup)

        return file
    }

    Path getPath(ProjectInfo projectInfo) {
        return projectService.getProjectDirectory(projectInfo.project).resolve(ProjectService.PROJECT_INFO).resolve(projectInfo.fileName)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    ProjectInfo updateProjectInfoComment(ProjectInfo projectInfo, String comment) {
        projectInfo.comment = comment
        return projectInfo.save(flush: true)
    }
}
