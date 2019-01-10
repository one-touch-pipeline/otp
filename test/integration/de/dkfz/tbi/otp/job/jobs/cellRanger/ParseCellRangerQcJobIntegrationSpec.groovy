package de.dkfz.tbi.otp.job.jobs.cellRanger

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.*
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.*
import grails.test.spock.*
import org.junit.*
import org.junit.rules.*

class ParseCellRangerQcJobIntegrationSpec extends IntegrationSpec implements CellRangerFactory {

    @Rule
    TemporaryFolder temporaryFolder

    File qaFile
    SingleCellBamFile singleCellBamFile
    ParseCellRangerQcJob job

    void setup() {
        qaFile = temporaryFolder.newFile(SingleCellBamFile.METRICS_SUMMARY_CSV_FILE_NAME)
        createQaFileOnFileSystem(qaFile)
        singleCellBamFile = createBamFile()
        singleCellBamFile.metaClass.getQualityAssessmentCsvFile = { -> qaFile }
        job = [
                getProcessParameterObject: { -> singleCellBamFile },
        ] as ParseCellRangerQcJob
        job.cellRangerService = new CellRangerService()
        job.cellRangerService.fileSystemService = new TestFileSystemService()
        job.qcTrafficLightService = new QcTrafficLightService()
        job.qcTrafficLightService.commentService = new CommentService()
    }

    void "ParseCellRangerQcJob sets QcTrafficLight depending on exceeded thresholds"() {
        given:
        createQaFileOnFileSystem(qaFile, ["Estimated Number of Cells": estimatedNumberOfCells])
        DomainFactory.createQcThreshold([
                qcProperty1          : "estimatedNumberOfCells",
                errorThresholdLower  : 0.0,
                warningThresholdLower: 2.0,
                warningThresholdUpper: 10.0,
                errorThresholdUpper  : 20.0,
                compare              : QcThreshold.ThresholdStrategy.ABSOLUTE_LIMITS,
                qcClass              : CellRangerQualityAssessment.name,
                seqType              : singleCellBamFile.seqType,
                project              : singleCellBamFile.project,
        ])

        when:
        job.execute()

        then:
        singleCellBamFile.qcTrafficLightStatus == resultStatus

        where:
        estimatedNumberOfCells || resultStatus
        5.0                    || AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
        25.0                   || AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED
    }

    void "ParseCellRangerQcJob sets qualityAssessmentStatus of BamFile to FINISHED"() {
        when:
        job.execute()

        then:
        singleCellBamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
    }
}
