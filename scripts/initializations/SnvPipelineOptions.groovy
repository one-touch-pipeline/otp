import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

ProcessingOptionService processingOptionService = ctx.getBean("processingOptionService")

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_NAME,
        'SNVCallingWorkflow'
)

List<SeqType> snvSeqTypes = SeqType.snvPipelineSeqTypes

assert snvSeqTypes.size() == 2

snvSeqTypes.each { SeqType seqType ->
    processingOptionService.createOrUpdate(
            PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION,
            '1.2.166-1',
            seqType.roddyName,
    )
}

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_SNV_DEFAULT_BASE_PROJECT_CONFIG,
        'otpSNVCallingWorkflowWGS-1.0',
        SeqType.wholeGenomePairedSeqType.roddyName,
)

processingOptionService.createOrUpdate(
        PIPELINE_RODDY_SNV_DEFAULT_BASE_PROJECT_CONFIG,
        'otpSNVCallingWorkflowWES-1.0',
        SeqType.exomePairedSeqType.roddyName,
)
