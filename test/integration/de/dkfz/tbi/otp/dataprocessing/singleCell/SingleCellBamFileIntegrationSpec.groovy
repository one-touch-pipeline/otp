package de.dkfz.tbi.otp.dataprocessing.singleCell

import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerQualityAssessment
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory

class SingleCellBamFileIntegrationSpec extends Specification implements CellRangerFactory {

    void "test getOverallQualityAssessment"() {
        given:
        SingleCellBamFile singleCellBamFile = createBamFile()

        when:
        CellRangerQualityAssessment expected = createQa(singleCellBamFile)

        then:
        expected == singleCellBamFile.overallQualityAssessment
    }
}
