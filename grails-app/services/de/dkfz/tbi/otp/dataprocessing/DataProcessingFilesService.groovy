package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.getLog

class DataProcessingFilesService {

    ConfigService configService
    LsdfFilesService lsdfFilesService

    public enum OutputDirectories {
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

    private String getOutputRoot(Individual individual) {
        if (!individual) {
            throw new IllegalArgumentException("individual must not be null")
        }
        Project project = individual.project
        Realm realm = configService.getRealmDataProcessing(project)
        if (realm == null) {
            throw new RuntimeException("Cannot determine output root for individual ${individual}. ConfigService returned null as the realm for project ${project}.")
        }
        String rpPath = realm.processingRootPath
        String pdName = project.dirName
        return "${rpPath}/${pdName}"
    }

    public String getOutputDirectory(Individual individual) {
        return getOutputDirectory(individual, OutputDirectories.BASE)
    }

    public String getOutputDirectory(Individual individual, String dir) {
        return dir ? getOutputDirectory(individual, dir.toUpperCase() as OutputDirectories) : getOutputDirectory(individual)
    }

    public String getOutputDirectory(Individual individual, OutputDirectories dir) {
        String outputBaseDir = getOutputRoot(individual)
        String postfix = (!dir || dir == OutputDirectories.BASE) ? "" : "${dir.toString().toLowerCase()}/"
        return "${outputBaseDir}/results_per_pid/${individual.pid}/${postfix}"
    }

    /**
     * @param dbFile Any object with properties fileExists, deletionDate and dateFromFileSystem.
     * @return true if there is no serious inconsistency.
     */
    public boolean checkConsistencyForDeletion(final def dbFile, final File fsFile) {
        if (!dbFile.fileExists) {
            log.warn "fileExists is already false for ${dbFile} (${fsFile})."
        }
        if (dbFile.deletionDate) {
            log.warn "deletionDate is already set (to ${dbFile.deletionDate}) for ${dbFile} (${fsFile})."
        }
        if (fsFile.exists()) {
            boolean consistent = true
            final long fsSize = fsFile.length()
            if (fsSize != dbFile.fileSize) {
                log.error "File size in database (${dbFile.fileSize}) and on the file system (${fsSize}) are different for ${dbFile} (${fsFile}). Will not delete the file."
                consistent = false
            }
            final Date fsDate = new Date(fsFile.lastModified())
            if (fsDate != dbFile.dateFromFileSystem) {
                log.error "File date in database (${dbFile.dateFromFileSystem}) and on the file system (${fsDate}) are different for ${dbFile} (${fsFile}). Will not delete the file."
                consistent = false
            }
            return consistent
        } else {
            log.error "File does not exist on the file system: ${dbFile} (${fsFile}). Will not mark the file as deleted in the database."
            return false
        }
    }

    /**
     * Deletes the specified file if it exists. Otherwise logs a warning.
     * @return The number of freed bytes (i.e. the size of the file if it existed, otherwise 0).
     */
    public long deleteProcessingFile(final Project project, final String filePath) {
        return deleteProcessingFile(project, new File(filePath))
    }

    /**
     * Deletes the specified file if it exists. Otherwise logs a warning.
     * @return The number of freed bytes (i.e. the size of the file if it existed, otherwise 0).
     */
    public long deleteProcessingFile(final Project project, final File file) {
        if (file.exists()) {
            final long freedBytes = file.length()
            lsdfFilesService.deleteFile(configService.getRealmDataProcessing(project), file)
            return freedBytes
        } else {
            log.warn "File has already been deleted: ${file}."
            return 0L
        }
    }

    /**
     * Deletes the specified directory if it exists and is empty. If it does not exist, logs a warning. If it is not
     * empty, logs an error.
     */
    public void deleteProcessingDirectory(final Project project, final String directoryPath) {
        deleteProcessingDirectory(project, new File(directoryPath))
    }

    /**
     * Deletes the specified directory if it exists and is empty. If it does not exist, logs a warning. If it is not
     * empty, logs an error.
     */
    public void deleteProcessingDirectory(final Project project, final File directory) {
        if (!directory.exists()) {
            log.warn "Directory has already been deleted: ${directory}."
        } else if (directory.list().length != 0) {
            log.error "Directory ${directory} is not empty. Will not delete it."
        } else {
            lsdfFilesService.deleteDirectory(configService.getRealmDataProcessing(project), directory)
        }
    }
}
