package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.ProcessingException

class FileTypeUndefinedException extends ProcessingException {
    FileTypeUndefinedException(String fileName) {
        super("can not assign file type to filename: ${fileName}")
    }
}
