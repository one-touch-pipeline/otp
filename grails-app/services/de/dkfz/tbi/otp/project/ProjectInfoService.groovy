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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.FileSystemService

import java.nio.file.*
import java.nio.file.attribute.PosixFilePermission

@CompileDynamic
@Transactional
class ProjectInfoService {

    ExecutionHelperService executionHelperService
    FileSystemService fileSystemService
    FileService fileService
    ProjectService projectService

    @SuppressWarnings('PropertyName') // static helper closure, not a normal property, more like a constant.
    static Closure SORT_DATE_CREATED_DESC = { a, b ->
        b.dateCreated <=> a.dateCreated
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<ProjectInfo> getAllProjectInfosSortedByDateDesc(Project project) {
        return project.projectInfos.sort(SORT_DATE_CREATED_DESC)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    ProjectInfo createProjectInfoAndUploadFile(Project project, AddProjectInfoCommand cmd) {
        cmd.validate()
        assert !cmd.errors.hasErrors()
        assert project : "project must not be null"
        ProjectInfo projectInfo = new ProjectInfo(
                fileName: cmd.projectInfoFile.originalFilename,
                comment : cmd.comment,
        )

        project.addToProjectInfos(projectInfo)
        project.save(flush: true)
        projectInfo.save(flush: true)

        uploadProjectInfoToProjectFolder(projectInfo, cmd.projectInfoFile.bytes)

        return projectInfo
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    ProjectInfo createProjectInfoByPath(Project project, Path path) {
        ProjectInfo projectInfo = new ProjectInfo(
                fileName: path.fileName.toString(),
                comment : "File copied from ${path.toAbsolutePath()}" ,
        )

        project.addToProjectInfos(projectInfo)
        project.save(flush: true)
        projectInfo.save(flush: true)

        uploadProjectInfoToProjectFolder(projectInfo, path.bytes)

        return projectInfo
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
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

        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(file.parent, projectInfo.project.realm,
                '', FileService.OWNER_DIRECTORY_PERMISSION_STRING)
        fileService.createFileWithContent(file, content, projectInfo.project.realm, [PosixFilePermission.OWNER_READ] as Set)
        fileService.setGroupViaBash(file, projectInfo.project.realm, projectInfo.project.unixGroup)

        return file
    }

    FileSystem getRemoteFileSystemForProject(Project project) {
        return fileSystemService.getRemoteFileSystem(project.realm)
    }

    Path getPath(ProjectInfo projectInfo) {
        return projectService.getProjectDirectory(projectInfo.project).resolve(ProjectService.PROJECT_INFO).resolve(projectInfo.fileName)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    ProjectInfo updateProjectInfoComment(ProjectInfo projectInfo, String comment) {
        projectInfo.comment = comment
        projectInfo.save(flush: true)
    }
}
