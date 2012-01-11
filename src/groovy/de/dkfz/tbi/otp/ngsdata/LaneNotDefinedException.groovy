package de.dkfz.tbi.otp.ngsdata

public class LaneNotDefinedException extends MetaDataFormatException {

    public LaneNotDefinedException(String runName, String fileName) {
        super("Lane is not defined for file: ${fileName} in run: ${runName}")
    }
}
