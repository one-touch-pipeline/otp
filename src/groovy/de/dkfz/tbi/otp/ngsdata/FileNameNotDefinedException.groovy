package de.dkfz.tbi.otp.ngsdata

public class FileNameNotDefinedException extends MetaDataFormatException {

    public FileNameNotDefinedException(String runName) {
        super("FileName not defined for a file in run ${runName}")
    }
}
