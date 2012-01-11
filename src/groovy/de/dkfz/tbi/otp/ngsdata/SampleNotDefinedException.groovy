package de.dkfz.tbi.otp.ngsdata

public class SampleNotDefinedException extends MetaDataFormatException {

    public SampleNotDefinedException(String sample) {
        super("sample with identifier: ${sample} does not exists")
    }
}
