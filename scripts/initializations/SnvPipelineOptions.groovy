import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

ProcessingOptionService processingOptionService = ctx.getBean("processingOptionService")

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_SNV_PLUGIN_NAME,
        'SNVCallingWorkflow'
)

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_SNV_PLUGIN_VERSION,
        SeqType.wholeGenomePairedSeqType.roddyName,
        null,
        '1.2.166-1'
)

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_SNV_PLUGIN_VERSION,
        SeqType.exomePairedSeqType.roddyName,
        null,
        '1.2.166-1'
)

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_SNV_BASE_PROJECT_CONFIG,
        'otpSNVCallingWorkflowWGS-1.0'
)
