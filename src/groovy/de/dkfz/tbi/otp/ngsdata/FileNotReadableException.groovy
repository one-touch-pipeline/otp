package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.*

class FileNotReadableException extends ProcessingException {

    String fileName
    FileNotReadableException(String fileName) {
        super("can not read file: ${fileName}")
        this.fileName = fileName
    }

    FileNotReadableException(File file) {
        this(file as String)
    }
}
