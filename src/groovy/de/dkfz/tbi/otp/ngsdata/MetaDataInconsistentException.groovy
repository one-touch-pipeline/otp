package de.dkfz.tbi.otp.ngsdata

public class MetaDataInconsistentException extends MetaDataFormatException {

    public MetaDataInconsistentException(List<DataFile> files) {
        super("metadata inconsistent for files:${files}")
    }
}
