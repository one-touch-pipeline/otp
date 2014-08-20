package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome

class OverallQualityAssessmentMergedService {

    Double calcCoverageWithoutN(OverallQualityAssessmentMerged overallQualityAssessmentMerged, ReferenceGenome referenceGenome) {
        long qcBasesMapped = overallQualityAssessmentMerged.qcBasesMapped
        long referenceGenomeLengthWithoutN = referenceGenome.lengthWithoutN
        double coverageWithoutN = qcBasesMapped / referenceGenomeLengthWithoutN
        return coverageWithoutN
    }
}
