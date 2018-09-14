import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

ProcessingOptionService processingOptionService = ctx.getBean("processingOptionService")

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_INDEL_DEFAULT_PLUGIN_NAME,
        'IndelCallingWorkflow'
)

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_INDEL_DEFAULT_PLUGIN_VERSION,
        '1.2.177',
        SeqType.wholeGenomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_INDEL_DEFAULT_PLUGIN_VERSION,
        '1.2.177',
        SeqType.exomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_INDEL_DEFAULT_BASE_PROJECT_CONFIG,
        'otpIndelCallingWorkflow-1.0',
        SeqType.wholeGenomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_INDEL_DEFAULT_BASE_PROJECT_CONFIG,
        'otpIndelCallingWorkflow-1.0',
        SeqType.exomePairedSeqType.roddyName,
)
