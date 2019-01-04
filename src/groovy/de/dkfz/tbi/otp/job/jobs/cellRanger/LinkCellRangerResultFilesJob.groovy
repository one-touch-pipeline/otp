package de.dkfz.tbi.otp.job.jobs.cellRanger


import de.dkfz.tbi.otp.dataprocessing.cellRanger.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class LinkCellRangerResultFilesJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Autowired
    FileService fileService

    @Autowired
    CellRangerService cellRangerService

    @Override
    void execute() throws Exception {
        final SingleCellBamFile singleCellBamFile = getProcessParameterObject()

        SingleCellBamFile.withTransaction {
            cellRangerService.finishCellRangerWorkflow(singleCellBamFile)
            succeed()
        }
    }
}
