package de.dkfz.tbi.otp.ngsdata

public class MetaDataInconsistentException extends MetaDataFormatException {

    public MetaDataInconsistentException(List<DataFile> files, String entry, String reference) {
        super("metadata inconsistent for files:${files} ${entry} ${reference}")
    }
}
