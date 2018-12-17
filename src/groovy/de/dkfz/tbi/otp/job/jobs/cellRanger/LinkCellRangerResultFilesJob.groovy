package de.dkfz.tbi.otp.job.jobs.cellRanger

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerService
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl

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
