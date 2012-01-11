package de.dkfz.tbi.otp.ngsdata


public class MetaDataFileDuplicationException extends MetaDataFormatException {

    String runName
    String fileName

    public MetaDataFileDuplicationException(String runName, String fileName) {
        super("duplication of file ${fileName} in run ${runName}")
        this.runName = runName
        this.fileName = fileName
    }
}
