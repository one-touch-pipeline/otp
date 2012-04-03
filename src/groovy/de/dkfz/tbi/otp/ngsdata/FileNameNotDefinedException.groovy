package de.dkfz.tbi.otp.ngsdata

public class FileNameNotDefinedException extends MetaDataFormatException {

    public FileNameNotDefinedException(String dataFileId) {
        super("FileName not defined for id: ${dataFileId}")
    }
}
