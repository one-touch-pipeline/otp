import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.ngsdata.*

ProcessingOptionService processingOptionService = ctx.getBean("processingOptionService")

println processingOptionService.createOrUpdate(
        ProcessingOption.OptionName.PIPELINE_RODDY_INDEL_PLUGIN_NAME,
        null,
        null,
        'IndelCallingWorkflow'
)
println processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_INDEL_PLUGIN_VERSION,
        SeqType.wholeGenomePairedSeqType.roddyName,
        null,
        '1.0.176-7'
)
println processingOptionService.createOrUpdate(
        OptionName.PIPELINE_RODDY_INDEL_PLUGIN_VERSION,
        SeqType.exomePairedSeqType.roddyName,
        null,
        '1.0.176-7'
)
println processingOptionService.createOrUpdate(
        ProcessingOption.OptionName.PIPELINE_RODDY_INDEL_PLUGIN_CONFIG,
        null,
        null,
        'otpIndelCallingWorkflow-1.0'
)

