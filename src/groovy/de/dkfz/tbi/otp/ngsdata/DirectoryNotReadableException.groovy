package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.*

class DirectoryNotReadableException extends ProcessingException {

    String dirName
    DirectoryNotReadableException(String dirName) {
        super("can not read directory: ${dirName}")
        this.dirName = dirName
    }
}
