package de.dkfz.tbi.otp.ngsdata
import de.dkfz.tbi.otp.job.processing.*

class SampleInconsistentException extends ProcessingException {
    public SampleInconsistentException(String sample, String track) {
        super("Inconsistent sample: ${sample} for track ${track}")
    }
}
