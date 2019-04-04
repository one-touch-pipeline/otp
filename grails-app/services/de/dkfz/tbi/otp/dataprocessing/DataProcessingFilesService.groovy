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

package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.threadLog
import static org.springframework.util.Assert.notNull

@Transactional
class DataProcessingFilesService {

    ConfigService configService
    LsdfFilesService lsdfFilesService

    enum OutputDirectories {
        BASE,
        ALIGNMENT,
        MERGING,
        COVERAGE,
        FASTX_QC,
        FLAGSTATS,
        INSERTSIZE_DISTRIBUTION,
        SNPCOMP,
        STRUCTURAL_VARIATION
    }

    String getOutputDirectory(Individual individual) {
        return getOutputDirectory(individual, OutputDirectories.BASE)
    }

    String getOutputDirectory(Individual individual, String dir) {
        return dir ? getOutputDirectory(individual, dir.toUpperCase() as OutputDirectories) : getOutputDirectory(individual)
    }

    String getOutputDirectory(Individual individual, OutputDirectories dir) {
        String postfix = (!dir || dir == OutputDirectories.BASE) ? "" : "${dir.toString().toLowerCase()}/"
        return "${individual.resultsPerPidPath.absoluteDataProcessingPath}/${postfix}"
    }

    long deleteOldProcessingFiles(Object passService, String passTypeName, Date createdBefore, long millisMaxRuntime, Closure<Collection> passesFunc) {
        notNull passService
        notNull passTypeName
        notNull createdBefore
        notNull passesFunc
        threadLog.info "Deleting processing files of ${passTypeName} passes created before ${createdBefore}."
        final Collection passes = passesFunc()
        threadLog.info "Found ${passes.size()} ${passTypeName} passes for processing files deletion."
        final long startTimestamp = System.currentTimeMillis()
        long freedBytes = 0L
        long processedPasses = 0L
        try {
            for (final Object pass : passes) {
                if (System.currentTimeMillis() - startTimestamp > millisMaxRuntime) {
                    threadLog.info "Exiting because the maximum runtime (${millisMaxRuntime} ms) has elapsed."
                    break
                }
                if (passService.mayProcessingFilesBeDeleted(pass, createdBefore)) {
                    freedBytes += passService.deleteProcessingFiles(pass)
                    processedPasses++
                } else {
                    threadLog.error "May not delete processing files of ${pass}."
                }
            }
        } finally {
            threadLog.info "${freedBytes} bytes have been freed by deleting the processing files of ${processedPasses} ${passTypeName} " +
                    "passes created before ${createdBefore}."
        }
        return freedBytes
    }

    /**
     * If there are inconsistencies, details are logged to the thread log (see {@link de.dkfz.tbi.otp.utils.logging.LogThreadLocal}).
     *
     * @param dbFile Any object with properties fileExists, deletionDate and dateFromFileSystem.
     * @return true if there is no serious inconsistency.
     */
    boolean checkConsistencyWithDatabaseForDeletion(final def dbFile, final File fsFile) {
        notNull dbFile
        notNull fsFile
        if (!dbFile.fileExists) {
            threadLog.warn "fileExists is already false for ${dbFile} (${fsFile})."
        }
        if (dbFile.deletionDate) {
            threadLog.warn "deletionDate is already set (to ${dbFile.deletionDate}) for ${dbFile} (${fsFile})."
        }
        if (fsFile.exists()) {
            boolean consistent = true
            final long fsSize = fsFile.length()
            if (fsSize != dbFile.fileSize) {
                threadLog.error "File size in database (${dbFile.fileSize}) and on the file system (${fsSize}) are different for " +
                        "${dbFile} (${fsFile}). Will not delete the file."
                consistent = false
            }
            final Date fsDate = new Date(fsFile.lastModified())
            if (fsDate != dbFile.dateFromFileSystem) {
                threadLog.error "File date in database (${dbFile.dateFromFileSystem}) and on the file system (${fsDate}) are different for " +
                        "${dbFile} (${fsFile}). Will not delete the file."
                consistent = false
            }
            return consistent
        } else {
            threadLog.error "File does not exist on the file system: ${dbFile} (${fsFile}). Will not mark the file as deleted in the database."
            return false
        }
    }

    /**
     * Checks consistency between the files in the processing directory and the files in the final destination (= project
     * folder) as a preparation for deleting the files in the processing directory.
     *
     * If there are inconsistencies, details are logged to the thread log (see {@link de.dkfz.tbi.otp.utils.logging.LogThreadLocal}).
     *
     * @return true if there is no serious inconsistency.
     */
    boolean checkConsistencyWithFinalDestinationForDeletion(File processingDirectory, File finalDestinationDirectory, Collection<String> fileNames) {
        notNull processingDirectory
        notNull finalDestinationDirectory
        notNull fileNames
        boolean consistent = true
        fileNames.each {
            final File fileToBeDeleted = new File(processingDirectory, it)
            if (fileToBeDeleted.exists()) {
                final long fileToBeDeletedSize = fileToBeDeleted.length()
                final Date fileToBeDeletedDate = new Date(fileToBeDeleted.lastModified())
                final File fileInFinalDest = new File(finalDestinationDirectory, it)
                if (!fileInFinalDest.exists()) {
                    threadLog.error "File does not exist: ${fileInFinalDest}. " +
                            "Expected it to be the same as ${fileToBeDeleted} (${fileToBeDeletedSize} bytes, last modified ${fileToBeDeletedDate})"
                    consistent = false
                } else {
                    final long fileInFinalDestSize = fileInFinalDest.length()
                    final Date fileInFinalDestDate = new Date(fileInFinalDest.lastModified())
                    // Checking the dates for equality makes no sense because they are always different (seems not to be
                    // preserved when copying).
                    if (fileToBeDeletedSize != fileInFinalDestSize || fileToBeDeletedDate > fileInFinalDestDate) {
                        threadLog.error "Files are different: " +
                                "${fileToBeDeleted} (${fileToBeDeletedSize} bytes, last modified ${fileToBeDeletedDate}) and " +
                                "${fileInFinalDest} (${fileInFinalDestSize} bytes, last modified ${fileInFinalDestDate})"
                        consistent = false
                    }
                }
            }
        }
        return consistent
    }

    /**
     * Deletes processing files. Sets fileExists to <code>false</code> and deletionDate to the current time.
     *
     * @param dbFile The database object representing the processing file that shall be deleted. Must have
     * the properties project, fileExists, deletionDate and dateFromFileSystem.
     *
     * @param fsFile The main processing file represented by dbFile.
     *
     * @param additionalFiles Additional processing files which are not represented by a separated database object,
     * for example BAI files.
     *
     * @return The number of bytes that have been freed on the file system.
     */
    long deleteProcessingFiles(final def dbFile, final File fsFile, final File... additionalFiles) {
        notNull dbFile
        notNull fsFile
        notNull additionalFiles
        if (checkConsistencyWithDatabaseForDeletion(dbFile, fsFile)) {
            long freedBytes = 0L
            final Project project = dbFile.project
            additionalFiles.each {
                freedBytes += deleteProcessingFile(project, it)
            }
            freedBytes += deleteProcessingFile(project, fsFile)
            dbFile.fileExists = false
            dbFile.deletionDate = new Date()
            assert dbFile.save(flush: true)
            return freedBytes
        } else {
            return 0L
        }
    }

    /**
     * @return The number of bytes that have been freed on the file system.
     */
    long deleteProcessingFiles(final Project project, final File processingDirectory, final Collection<String> fileNames) {
        notNull project
        notNull processingDirectory
        notNull fileNames
        long freedBytes = 0L
        fileNames.each {
            freedBytes += deleteProcessingFile(project, new File(processingDirectory, it))
        }
        return freedBytes
    }

    /**
     * @return The number of bytes that have been freed on the file system.
     */
    long deleteProcessingFilesAndDirectory(final Project project, final File processingDirectory, final Collection<String> fileNames) {
        notNull project
        notNull processingDirectory
        notNull fileNames
        long freedBytes = deleteProcessingFiles(project, processingDirectory, fileNames)
        deleteProcessingDirectory(project, processingDirectory)
        return freedBytes
    }

    /**
     * Deletes the specified file if it exists. Otherwise logs a warning.
     * @return The number of freed bytes (i.e. the size of the file if it existed, otherwise 0).
     */
    long deleteProcessingFile(final Project project, final String filePath) {
        notNull project
        notNull filePath
        return deleteProcessingFile(project, new File(filePath))
    }

    /**
     * Deletes the specified file if it exists. Otherwise logs a warning.
     * @return The number of freed bytes (i.e. the size of the file if it existed, otherwise 0).
     */
    long deleteProcessingFile(final Project project, final File file) {
        notNull project
        notNull file
        if (file.exists()) {
            final long freedBytes = file.length()
            lsdfFilesService.deleteFile(project.realm, file)
            return freedBytes
        } else {
            threadLog.warn "File has already been deleted: ${file}."
            return 0L
        }
    }

    /**
     * Deletes the specified directory if it exists and is empty. If it does not exist, logs a warning. If it is not
     * empty, logs an error.
     */
    void deleteProcessingDirectory(final Project project, final String directoryPath) {
        notNull project
        notNull directoryPath
        deleteProcessingDirectory(project, new File(directoryPath))
    }

    /**
     * Deletes the specified directory if it exists and is empty. If it does not exist, logs a warning. If it is not
     * empty, logs an error.
     */
    void deleteProcessingDirectory(final Project project, final File directory) {
        notNull project
        notNull directory
        if (!directory.exists()) {
            threadLog.warn "Directory has already been deleted: ${directory}."
        } else if (directory.list().length != 0) {
            threadLog.error "Directory ${directory} is not empty. Will not delete it."
        } else {
            lsdfFilesService.deleteDirectory(project.realm, directory)
        }
    }
}
