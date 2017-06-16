import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName

ProcessingOptionService processingOptionService = ctx.processingOptionService

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_ACESEQ_PLUGIN_NAME,
        null,
        null,
        'ACEseqWorkflow',
        'Name of the AceSeq pipeline plugin'
)
processingOptionService.createOrUpdate(
        ProcessingOption.OptionName.PIPELINE_ACESEQ_PLUGIN_VERSION,
        null,
        null,
        '1.2.8-1',
        'The version of the AceSeq pipeline plugin'
)
processingOptionService.createOrUpdate(
        ProcessingOption.OptionName.PIPELINE_ACESEQ_BASE_PROJECT_CONFIG,
        null,
        null,
        'otpACEseq-1.0',
        'The base project file for AceSeq pipeline'
)

