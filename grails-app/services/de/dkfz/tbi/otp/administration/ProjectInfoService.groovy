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

import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.security.User

import java.nio.file.*
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission

@Transactional
class ProjectInfoService {

    SpringSecurityService springSecurityService
    ExecutionHelperService executionHelperService
    FileSystemService fileSystemService
    FileService fileService

    @SuppressWarnings('PropertyName') // static helper closure, not a normal property, more like a constant.
    static Closure SORT_DATE_CREATED_DESC = { a, b ->
        b.dateCreated <=> a.dateCreated
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Map<String, List<ProjectInfo>> getAllProjectInfosSortedByDateDescAndGroupedByDta(Project project) {
        return project.projectInfos.sort(SORT_DATE_CREATED_DESC).groupBy { it.dta ? "Dta" : "NonDta" }
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
        // if this is a DTA document, see also ProjectInfo.isDta()
        if (cmd.peerInstitution) {
            projectInfo.dtaId = cmd.dtaId
            projectInfo.legalBasis = cmd.legalBasis
            projectInfo.peerInstitution = cmd.peerInstitution
            projectInfo.validityDate = cmd.validityDate
        }

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
        FileSystem fs = getRemoteFileSystemForProject(projectInfo.project)
        Path path = fs.getPath(projectInfo.path)
        fileService.deleteDirectoryRecursively(path)
        projectInfo.project = null
        projectInfo.delete(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    byte[] getProjectInfoContent(ProjectInfo projectInfo) {
        assert projectInfo: "No ProjectInfo given"
        Path file = getRemoteFileSystemForProject(projectInfo.project).getPath(projectInfo.path)

        return Files.exists(file) ? file.bytes : [] as byte[]
    }

    private Path uploadProjectInfoToProjectFolder(ProjectInfo projectInfo, byte[] content) {
        String absoluteFilePath = "${projectInfo.project.projectDirectory.absolutePath}/${ProjectService.PROJECT_INFO}/${projectInfo.fileName}"
        Path file = getRemoteFileSystemForProject(projectInfo.project).getPath(absoluteFilePath)

        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(file.parent, projectInfo.project.realm,
                '', FileService.OWNER_DIRECTORY_PERMISSION_STRING)
        fileService.createFileWithContent(file, content, projectInfo.project.realm,
                [PosixFilePermission.OWNER_READ] as Set<PosixFileAttributes>)
        executionHelperService.setGroup(projectInfo.project.realm, new File(absoluteFilePath), projectInfo.project.unixGroup)

        return file
    }

    FileSystem getRemoteFileSystemForProject(Project project) {
        return fileSystemService.getRemoteFileSystem(project.realm)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    DataTransfer addTransferToProjectInfo(AddTransferCommand cmd) {
        DataTransfer xfer = new DataTransfer(
                projectInfo   : cmd.parentDocument,
                requester     : cmd.requester,
                ticketID      : cmd.ticketID,
                performingUser: springSecurityService.currentUser as User,
                direction     : cmd.direction,
                transferMode  : cmd.transferMode,
                peerPerson    : cmd.peerPerson,
                peerAccount   : cmd.peerAccount,
                transferDate  : cmd.transferDate,
                completionDate: cmd.completionDate,
                comment       : cmd.comment,
        )
        xfer.save(flush: true)
        return xfer
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void markTransferAsCompleted(DataTransfer xfer) {
        assert !xfer.completionDate: "DataTransfer already completed"
        xfer.completionDate = new Date()
        xfer.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    ProjectInfo updateProjectInfoComment(ProjectInfo projectInfo, String comment) {
        projectInfo.comment = comment
        projectInfo.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    DataTransfer updateDataTransferComment(DataTransfer dataTransfer, String comment) {
        dataTransfer.comment = comment
        dataTransfer.save(flush: true)
    }
}
