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
package de.dkfz.tbi.otp.project.dta

import grails.gorm.transactions.Transactional
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.multipart.MultipartFile

import de.dkfz.tbi.otp.FileIsEmptyException
import de.dkfz.tbi.otp.FileNotFoundException
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.utils.FileNameGenerator

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

@Transactional
class DataTransferService {

    static final String TRANSFER_DIR_PREFIX = "transfer-"

    DataTransferAgreementService dataTransferAgreementService
    FileService fileService
    FileSystemService fileSystemService
    ExecutionHelperService executionHelperService
    SecurityService securityService

    /**
     * Constructs a full DataTransfer object with its files as DataTransferDocuments in the database layer
     * and also on the filesystem.
     *
     * @param dataTransfer to persist
     * @param files as list of files connected to the transfer
     * @return persisted DataTransfer
     * @throws FileIsEmptyException when files contains a file without content
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    DataTransfer persistDataTransferWithTransferDocuments(DataTransfer dataTransfer, List<MultipartFile> files) throws FileIsEmptyException {
        dataTransfer.performingUser = securityService.currentUser
        dataTransfer.dataTransferAgreement.addToTransfers(dataTransfer).save(flush: true)
        return addFilesToDataTransfer(dataTransfer, files)
    }

    /**
     * Add one new file to an data transfer object.
     *
     * @param dataTransfer to update
     * @param file, the new one
     * @return updated data transfer
     * @throws FileIsEmptyException when the file has no content
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    DataTransfer addFileToDataTransfer(DataTransfer dataTransfer, MultipartFile file) throws FileIsEmptyException {
        if (!file.bytes) {
            throw new FileIsEmptyException("File ${file?.originalFilename} has no content.")
        }

        DataTransferDocument dataTransferDocument = new DataTransferDocument([
                fileName: FileNameGenerator.getUniqueFileNameWithTimestamp(file.originalFilename),
        ])

        dataTransfer.addToDataTransferDocuments(dataTransferDocument).save(flush: true)
        uploadDataTransferDocumentToRemoteFileSystem(dataTransferDocument, file.bytes)

        return dataTransfer
    }

    /**
     * Add multiple documents to an existing data transfer. Those documents will be added in the
     * database as well as in the filesystem.
     *
     * @param dataTransfer to update
     * @param files, list of the new documents
     * @return updated DataTransfer
     * @throws DataTransferNotFoundException when the given transfer does not exist
     * @throws FileIsEmptyException when one of the files is empty
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    DataTransfer addFilesToDataTransfer(DataTransfer dataTransfer, List<MultipartFile> files) throws DataTransferNotFoundException, FileIsEmptyException {
        if (files.any { !it.bytes }) {
            throw new FileIsEmptyException("One or more empty files are found.")
        }

        if (!dataTransfer || !dataTransfer.id) {
            throw new DataTransferNotFoundException("Data transfer not found.")
        }

        if (!DataTransfer.get(dataTransfer.id)) {
            throw new DataTransferNotFoundException("Data transfer not found.")
        }

        DataTransfer resultDataTransfer = dataTransfer

        files.each { MultipartFile file ->
            resultDataTransfer = addFileToDataTransfer(dataTransfer, file)
        }

        return resultDataTransfer
    }

    /**
     * Upload a given data transfer file to the remote file system.
     *
     * @param transferDocument database representation of the transfer file
     * @param fileContent content of the file
     * @return path where the file was saved
     */
    @SuppressWarnings("JavaIoPackageAccess")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    private Path uploadDataTransferDocumentToRemoteFileSystem(DataTransferDocument transferDocument, byte[] fileContent) {
        Path path = getPathOnRemoteFileSystem(transferDocument)
        Realm realm = transferDocument.dataTransfer.dataTransferAgreement.project.realm

        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(path.parent, realm, '', FileService.OWNER_DIRECTORY_PERMISSION_STRING)
        fileService.createFileWithContent(path, fileContent, realm, [PosixFilePermission.OWNER_READ] as Set<PosixFilePermission>)
        fileService.setGroupViaBash(path, realm, transferDocument.dataTransfer.dataTransferAgreement.project.unixGroup)

        return path
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void markTransferAsCompleted(DataTransfer transfer) {
        assert !transfer.completionDate: "DataTransfer already completed"
        transfer.completionDate = new Date()
        transfer.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    DataTransfer updateDataTransferComment(DataTransfer dataTransfer, String comment) {
        dataTransfer.comment = comment
        return dataTransfer.save(flush: true)
    }

    /**
     * Load a specific transfer document from the remote filesystem and return its content as byte array.
     *
     * @param transferDocument to load
     * @return document content
     * @throws FileNotFoundException when the file was not found in the db or on the filesystem
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    byte[] getDataTransferDocumentContent(DataTransferDocument transferDocument) throws FileNotFoundException {
        if (!transferDocument || !DataTransferDocument.get(transferDocument.id)) {
            throw new FileNotFoundException("The transfer document ${transferDocument?.fileName} was not found.")
        }

        Path file = getPathOnRemoteFileSystem(transferDocument)

        if (!Files.exists(file)) {
            throw new FileNotFoundException("The transfer document ${transferDocument.fileName} was not found.")
        }

        return file.bytes
    }

    /**
     * Get the path to the folder of a specific data transfer on the remote file system as String.
     *
     * @param transfer for which the path is requested
     * @return path to the specific data transfer
     */
    Path getPathOnRemoteFileSystem(DataTransfer transfer) {
        return dataTransferAgreementService.getPathOnRemoteFileSystem(transfer.dataTransferAgreement).resolve(TRANSFER_DIR_PREFIX.concat(transfer.id as String))
    }

    /**
     * Get the path of a specific data transfer document on the remote file system as String.
     *
     * @param transferDocument for which the path is requested
     * @return path to the specific data transfer document
     */
    Path getPathOnRemoteFileSystem(DataTransferDocument transferDocument) {
        return getPathOnRemoteFileSystem(transferDocument.dataTransfer).resolve(transferDocument.fileName)
    }
}
