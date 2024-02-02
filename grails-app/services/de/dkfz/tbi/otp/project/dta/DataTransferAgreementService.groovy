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
package de.dkfz.tbi.otp.project.dta

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.multipart.MultipartFile

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.FileNameGenerator
import de.dkfz.tbi.otp.FileIsEmptyException
import de.dkfz.tbi.otp.FileNotFoundException

import java.nio.file.*
import java.nio.file.attribute.PosixFilePermission

@Transactional
class DataTransferAgreementService {

    static final String DTA_DIRECTORY = "dta"
    static final String DTA_DIR_PREFIX = "dta-"

    FileService fileService
    FileSystemService fileSystemService
    ExecutionHelperService executionHelperService
    ProjectService projectService

    /**
     * Create a new DataTransferAgreement (DTA) and persist it in the database.
     *
     * @param dta data transfer agreement
     * @param files of dta, will be created in their own domain class
     * @return persisted DataTransferAgreement
     * @throws FileIsEmptyException when files contains a file without content
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    DataTransferAgreement persistDtaWithDtaDocuments(DataTransferAgreement dta, List<MultipartFile> files) throws FileIsEmptyException {
        assert dta.project : "Project can not be empty."

        dta.project.addToDataTransferAgreements(dta).save(flush: true)
        return addFilesToDta(dta, files)
    }

    /**
     * Add a document file to an existing DataTransferAgreement (DTA). It will be added in the
     * database as well as in the filesystem.
     *
     * @param dta, as reference
     * @param file, new document to add
     * @return updated DataTransferAgreement
     * @throws FileIsEmptyException when file has no content
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    DataTransferAgreement addFileToDta(DataTransferAgreement dta, MultipartFile file) throws FileIsEmptyException {
        if (!file.bytes) {
            throw new FileIsEmptyException("File ${file?.originalFilename} has no content.")
        }

        DataTransferAgreementDocument dtaFile = new DataTransferAgreementDocument([
                fileName: FileNameGenerator.getUniqueFileNameWithTimestamp(file.originalFilename),
        ])

        dta.addToDataTransferAgreementDocuments(dtaFile).save(flush: true)
        uploadDataTransferAgreementToRemoteFileSystem(dtaFile, file.bytes)

        return dta
    }

    /**
     * Add multiple documents to an existing DataTransferAgreement (DTA). Those documents will be added in the
     * database as well as in the filesystem.
     *
     * @param dta, as reference
     * @param files, list of the new documents
     * @return updated DataTransferAgreement
     * @throws DataTransferAgreementNotFoundException when the given dta does not exist
     * @throws FileIsEmptyException when one of the files is empty
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    DataTransferAgreement addFilesToDta(DataTransferAgreement dta, List<MultipartFile> files)
            throws DataTransferAgreementNotFoundException, FileIsEmptyException {
        if (files.any { !it.bytes }) {
            throw new FileIsEmptyException("One or more empty files are found.")
        }

        if (!dta || !dta.id) {
            throw new DataTransferAgreementNotFoundException("DTA not found.")
        }

        if (!DataTransferAgreement.get(dta.id)) {
            throw new DataTransferAgreementNotFoundException("DTA not found.")
        }

        DataTransferAgreement resultDta = dta

        files.each { MultipartFile file ->
            resultDta = addFileToDta(resultDta, file)
        }

        return resultDta
    }

    @CompileDynamic
    List<DataTransferAgreement> getSortedDataTransferAgreementsByProject(Project project) {
        return DataTransferAgreement.findAllByProject(project, [sort: "dateCreated", order: "desc"])
    }

    /**
     * Load a specific DTA document from the remote filesystem and return its content as byte array.
     *
     * @param dtaDocument to load
     * @return document content
     * @throws FileNotFoundException when the file was not found in the db or on the filesystem
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    byte[] getDataTransferAgreementDocumentContent(DataTransferAgreementDocument dtaDocument) throws FileNotFoundException {
        if (!dtaDocument || !DataTransferAgreementDocument.get(dtaDocument.id)) {
            throw new FileNotFoundException("The DTA document ${dtaDocument?.fileName} was not found.")
        }

        Path file = fileSystemService.remoteFileSystem.getPath(getPathOnRemoteFileSystem(dtaDocument).toString())

        if (!Files.exists(file)) {
            throw new FileNotFoundException("The DTA document ${dtaDocument.fileName} was not found.")
        }

        return file.bytes
    }

    /**
     * Update the comment text of a DTA object.
     *
     * @param dta to update
     * @param newComment new comment text
     * @return updated dta
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    DataTransferAgreement updateDataTransferAgreementComment(DataTransferAgreement dta, String newComment) {
        dta.comment = newComment
        return dta.save(flush: true)
    }

    /**
     * Delete a DTA and it's transfers from the file system and also from the DB permanently.
     * This action cannot be undone.
     *
     * @param dataTransferAgreement which should be deleted
     * @throws IOException when deletion on the filesystem fails
     * @throws AssertionError when a validator fails
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void deleteDataTransferAgreement(DataTransferAgreement dataTransferAgreement) throws IOException, AssertionError {
        Path pathToDelete = getPathOnRemoteFileSystem(dataTransferAgreement)
        fileService.deleteDirectoryRecursively(pathToDelete)
        dataTransferAgreement.project = null
        dataTransferAgreement.delete(flush: true)
    }

    /**
     * Upload a given dta file to the remote file system.
     *
     * @param dtaFile database representation of the dta file
     * @param fileContent content of the file
     * @return path where the file was saved
     */
    @SuppressWarnings("JavaIoPackageAccess")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    private Path uploadDataTransferAgreementToRemoteFileSystem(DataTransferAgreementDocument dtaFile, byte[] fileContent) {
        Path absoluteFilePath = getPathOnRemoteFileSystem(dtaFile)

        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(absoluteFilePath.parent, '', FileService.OWNER_DIRECTORY_PERMISSION_STRING)
        fileService.createFileWithContent(absoluteFilePath, fileContent, [PosixFilePermission.OWNER_READ] as Set<PosixFilePermission>)
        fileService.setGroupViaBash(absoluteFilePath, dtaFile.dataTransferAgreement.project.unixGroup)

        return absoluteFilePath
    }

    /**
     * Get the path to the folder of a specific DTA on the remote file system as String.
     *
     * @param dta for which the path is requested
     * @return path to the specific dta
     */
    Path getPathOnRemoteFileSystem(DataTransferAgreement dta) {
        return projectService.getProjectDirectory(dta.project).resolve(DTA_DIRECTORY).resolve(DTA_DIR_PREFIX.concat(dta.id as String))
    }

    /**
     * Get the path of a specific DTA document on the remote file system as String.
     *
     * @param dtaDocument for which the path is requested
     * @return path to the specific dta document
     */
    Path getPathOnRemoteFileSystem(DataTransferAgreementDocument dtaDocument) {
        return getPathOnRemoteFileSystem(dtaDocument.dataTransferAgreement).resolve(dtaDocument.fileName)
    }
}
