package de.dkfz.tbi.otp.job.jobs.cellRanger

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.qcTrafficLight.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

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
            qcTrafficLightService.setQcTrafficLightStatusBasedOnThreshold(singleCellBamFile, (QcTrafficLightValue) qa)

            singleCellBamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.FINISHED
            singleCellBamFile.save(flush: true)
            succeed()
        }
    }
}
