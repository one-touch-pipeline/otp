package de.dkfz.tbi.otp.dataprocessing

class RoddyLibraryQa extends RoddyQualityAssessment {

    String libraryDirectoryName

    static constraints = {
        chromosome(unique: ['qualityAssessmentMergedPass', 'libraryDirectoryName'])
        libraryDirectoryName(blank: false, validator: { val -> !val || val.startsWith("lib") })
    }
}
