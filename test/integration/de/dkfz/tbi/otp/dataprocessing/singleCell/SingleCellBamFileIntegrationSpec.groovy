package de.dkfz.tbi.otp.dataprocessing.singleCell

import de.dkfz.tbi.otp.dataprocessing.cellRanger.*
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.*
import de.dkfz.tbi.otp.ngsdata.*
import spock.lang.*

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
