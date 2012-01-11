package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.*

public class FileNotReadableException extends ProcessingException {

    String fileName
    public FileNotReadableException(String fileName) {
        super("can not read file: ${fileName}")
        this.fileName = fileName
    }
}
