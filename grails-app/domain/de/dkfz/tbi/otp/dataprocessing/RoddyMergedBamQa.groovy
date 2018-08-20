package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightValue

class RoddyMergedBamQa extends RoddyQualityAssessment implements QcTrafficLightValue {

    static constraints = {
        chromosome(unique: ['qualityAssessmentMergedPass', 'class'])
    }
}
