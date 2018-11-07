package de.dkfz.tbi.otp.job.jobs.cellRanger

import de.dkfz.tbi.otp.dataprocessing.cellRanger.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.*
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.*
import de.dkfz.tbi.otp.utils.*
import grails.test.spock.*
import org.junit.*
import org.junit.rules.*

class CellRangerServiceIntegrationSpec extends IntegrationSpec implements CellRangerFactory {

    @Rule
    public TemporaryFolder temporaryFolder

    CellRangerService cellRangerService

    SingleCellBamFile singleCellBamFile
    File file

    void setup() {
        cellRangerService = new CellRangerService()
        singleCellBamFile = createBamFile()
        file = new File(temporaryFolder.newFolder(), "${HelperUtils.uniqueString}_metrics_summary.csv")
        singleCellBamFile.metaClass.getQualityAssessmentCsvFile = { return file }
    }

    void "parseCellRangerQaStatistics, all values are parsed and stored in CellRangerQualityAssessment"() {
        given:
        createQaFileOnFileSystem(file)

        when:
        CellRangerQualityAssessment cellRangerQualityAssessment = cellRangerService.parseCellRangerQaStatistics(singleCellBamFile)

        then:
        CellRangerService.MetricsSummaryCsvColumn.values().every {
            cellRangerQualityAssessment."${it.attributeName}" != null
        }
    }

    void "parseCellRangerQaStatistics, unparsable values cause an exception"() {
        given:
        createQaFileOnFileSystem(file, [(key): value])

        when:
        cellRangerService.parseCellRangerQaStatistics(singleCellBamFile)

        then:
        NumberFormatException e = thrown()
        e.message.contains("Failed to parse '${value}'")

        where:
        key                         | value
        "Estimated Number of Cells" | "12%34"
        "Fraction Reads in Cells"   | "77.7%%"
        "Mean Reads per Cell"       | "string"
    }

    void "parseCellRangerQaStatistics, missing columns cause an exception"() {
        given:
        file = CreateFileHelper.createFile(
                new File(temporaryFolder.newFolder(), "${HelperUtils.uniqueString}_metrics_summary.csv"),
                "column1,column2\ncontent1,content2"
        )

        when:
        cellRangerService.parseCellRangerQaStatistics(singleCellBamFile)

        then:
        AssertionError e = thrown()
        e.message.contains("can not be found in")
    }
}
