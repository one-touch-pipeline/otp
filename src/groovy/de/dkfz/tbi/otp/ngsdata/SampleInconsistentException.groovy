package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.*

class SampleInconsistentException extends ProcessingException {
    SampleInconsistentException(List<DataFile> files, Sample sample, Sample fileSample) {
        super("Inconsistent sample: ${files} ${sample} ${fileSample}")
    }
}
