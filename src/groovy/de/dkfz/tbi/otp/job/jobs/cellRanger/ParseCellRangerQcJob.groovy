package de.dkfz.tbi.otp.job.jobs.cellRanger

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerService
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightValue

@Component
@Scope("prototype")
@UseJobLog
class ParseCellRangerQcJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Autowired
    CellRangerService cellRangerService

    @Autowired
    QcTrafficLightService qcTrafficLightService

    @Override
    void execute() throws Exception {
        SingleCellBamFile singleCellBamFile = getProcessParameterObject() as SingleCellBamFile

        SingleCellBamFile.withTransaction {
            CellRangerQualityAssessment qa = cellRangerService.parseCellRangerQaStatistics(singleCellBamFile)
            qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(singleCellBamFile, (QcTrafficLightValue) qa)

            singleCellBamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.FINISHED
            singleCellBamFile.save(flush: true)
            succeed()
        }
    }
}
