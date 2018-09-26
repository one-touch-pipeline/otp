package de.dkfz.tbi.otp.dataprocessing.singleCell

import spock.lang.Specification

class SingleCellBamFileIntegrationSpec extends Specification {

    void "test getOverallQualityAssessment"() {
        given:
        SingleCellBamFile singleCellBamFile = new SingleCellBamFile()

        when:
        singleCellBamFile.overallQualityAssessment

        then:
        thrown(UnsupportedOperationException)
    }

}
