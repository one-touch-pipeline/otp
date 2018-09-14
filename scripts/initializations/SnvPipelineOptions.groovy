import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

ProcessingOptionService processingOptionService = ctx.getBean("processingOptionService")

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_NAME,
        'SNVCallingWorkflow'
)

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION,
        '1.2.166-1',
        SeqType.wholeGenomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION,
        '1.2.166-1',
        SeqType.exomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_SNV_DEFAULT_BASE_PROJECT_CONFIG,
        'otpSNVCallingWorkflowWGS-1.0',
        SeqType.wholeGenomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_SNV_DEFAULT_BASE_PROJECT_CONFIG,
        'otpSNVCallingWorkflowWGS-1.0',
        SeqType.exomePairedSeqType.roddyName,
)
