import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl;

String workflow = "PanCanWorkflow"

plan(workflow, ctx, true) {
    start("PanCanStart", "PanCanStartJob")
    job("executePanCan", "executePanCanJob")
    job("parsePanCanQc", "parsePanCanQcJob")
    job("movePanCanFilesToFinalDestination", "movePanCanFilesToFinalDestinationJob")
}

println ctx.processingOptionService.createOrUpdate(AbstractStartJobImpl.TOTAL_SLOTS_OPTION_NAME, workflow, null, '5', '')
println ctx.processingOptionService.createOrUpdate(AbstractStartJobImpl.SLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME, workflow, null, '0', '')
