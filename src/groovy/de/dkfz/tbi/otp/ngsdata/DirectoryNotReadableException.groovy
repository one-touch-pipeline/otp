package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.*

public class DirectoryNotReadableException extends ProcessingException {

    String dirName
    public DirectoryNotReadableException(String dirName) {
        super("can not read directory: ${dirName}")
        this.dirName = dirName
    }
}
