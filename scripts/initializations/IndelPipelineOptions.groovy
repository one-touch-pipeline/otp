import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

ProcessingOptionService processingOptionService = ctx.getBean("processingOptionService")

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_INDEL_PLUGIN_NAME,
        'IndelCallingWorkflow'
)

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_INDEL_PLUGIN_VERSION,
        SeqType.wholeGenomePairedSeqType.roddyName,
        null,
        '1.2.177'
)

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_INDEL_PLUGIN_VERSION,
        SeqType.exomePairedSeqType.roddyName,
        null,
        '1.2.177'
)

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_INDEL_PLUGIN_CONFIG,
        'otpIndelCallingWorkflow-1.0'
)
