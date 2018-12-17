package de.dkfz.tbi.otp.fileSystemConsistency

import de.dkfz.tbi.otp.fileSystemConsistency.ConsistencyStatus.Status
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService

/**
 * Service to check for consistency of Database and LSDF
 */
class ConsistencyService {

    LsdfFilesService lsdfFilesService

    /**
     * Retrieves the consistency status of the DataFile
     *
     * @param dataFile
     * @return Consistency status of the DataFile
     */
    Status checkStatus(DataFile dataFile) {
        String path = lsdfFilesService.getFileFinalPath(dataFile)
        // if there is no path to the file the status will be considered consistent
        if (!path) {
            return Status.CONSISTENT
        }
        // hack to not evaluate consistency at these files. (will be considered as consistent)
        if (path.contains("_export.txt.gz")||
            path.contains("_export.txt.tar.bz2")) {
            return Status.CONSISTENT
        }
        File file = new File(path)
        if (!file.exists()) {
            return Status.NO_FILE
        }
        if (!file.canRead()) {
            return Status.NO_READ_PERMISSION
        }
        File viewByPidFile = new File(lsdfFilesService.getFileViewByPidPath(dataFile))
        if (viewByPidFile.getCanonicalPath() != file.getCanonicalPath()) {
            return Status.VIEW_BY_PID_NO_FILE
        }
        if (file.size() != dataFile.fileSize) {
            return Status.SIZE_DIFFERENCE
        }
        return Status.CONSISTENT
    }
}
