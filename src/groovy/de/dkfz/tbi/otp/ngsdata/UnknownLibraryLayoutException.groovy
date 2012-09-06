package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.*

class UnknownLibraryLayoutException extends ProcessingException {
    String libraryLayout
    public UnknownLibraryLayoutException(String layout) {
        super("Unknown Library Layout Exception: ${layout}")
        this.libraryLayout = layout
    }
}
