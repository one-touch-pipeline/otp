/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.withdraw

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFileService
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.DeletionService

import java.nio.file.Files
import java.nio.file.Path

@CompileDynamic
@Transactional
abstract class AbstractWithdrawBamFileService<E extends AbstractBamFile> implements ProcessingWithdrawService<E, SeqTrack> {

    static final String NON_OTP = 'nonOTP'

    AbstractBamFileService abstractBamFileService
    DeletionService deletionService
    FileSystemService fileSystemService
    FilestoreService filestoreService

    @Override
    abstract List<E> collectObjects(List<SeqTrack> entities)

    @Override
    List<String> collectPaths(List<E> entities) {
        return entities.collectMany { bamFile ->
            Path vbpFolder = abstractBamFileService.getBaseDirectory(bamFile)
            Path uuidFolder = bamFile.workflowArtefact?.producedBy?.workFolder ? filestoreService.getWorkFolderPath(bamFile.workflowArtefact.producedBy) : null

            return [
                    // ViewByPid folder: collect only its subfolders & files at the first level
                    listSubfoldersAndFiles(vbpFolder),
                    // Uuid folder: collect its root folder
                    uuidFolder,
            ].flatten()
        }.findAll { Path path ->
            // Do not collect the nonOTP folder
            path != null && Files.exists(path) && path.fileName.toString() != NON_OTP
        }.unique()*.toString()
    }

    /**
     * A helper method that returns all subfolders and files in the given folder, not recursively.
     * Note: This method uses withCloseable() to ensure that the directory stream is closed properly.
     *
     * @param the parent folder
     * @return a list of paths including directories and files
     */
    static List<Path> listSubfoldersAndFiles(Path folder) {
        if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException("Path is not a directory: $folder")
        }

        try {
            return Files.newDirectoryStream(folder).withCloseable { it.toList() }
        } catch (IOException e) {
            throw new WithdrawnException(e)
        }
    }

    @Override
    void withdrawObjects(List<E> entities) {
        entities.each {
            it.withdrawn = true
            it.save(flush: true)
        }
    }

    @Override
    void unwithdrawObjects(List<E> entities) {
        entities.each {
            it.withdrawn = false
            it.save(flush: true)
        }
    }

    @Override
    void deleteObjects(List<E> entities) {
        entities.collectMany { E bamFile ->
            bamFile.containedSeqTracks
        }.unique().each {
            deletionService.deleteAllProcessingInformationAndResultOfOneSeqTrack(it)
        }
    }
}
