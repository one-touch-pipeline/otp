package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

class RoddyLibraryQa extends RoddyQualityAssessment {

    String normalizedLibraryName

    static constraints = {
        chromosome(unique: ['qualityAssessmentMergedPass', 'normalizedLibraryName'])
        normalizedLibraryName(blank: false, validator: { val -> !val || SeqTrack.normalizeLibraryName(val) == val })
    }
}
