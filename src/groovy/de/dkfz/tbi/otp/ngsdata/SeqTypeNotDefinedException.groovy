package de.dkfz.tbi.otp.ngsdata

public class SeqTypeNotDefinedException extends MetaDataFormatException {

    public SeqTypeNotDefinedException(String type, String layout) {
        super("sequencing type not defiened: ${type} ${layout}")
    }
}
