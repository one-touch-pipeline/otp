import de.dkfz.tbi.otp.dataprocessing.*

ProcessingOptionService processingOptionService = ctx.processingOptionService

processingOptionService.createOrUpdate(
        de.dkfz.tbi.otp.dataprocessing.roddy.RoddyConstants.OPTION_KEY_ACESEQ_PIPELINE_PLUGIN_NAME,
        null,
        null,
        'ACEseqWorkflow',
        'Name of the AceSeq pipeline plugin'
)
processingOptionService.createOrUpdate(
        de.dkfz.tbi.otp.dataprocessing.roddy.RoddyConstants.OPTION_KEY_ACESEQ_PIPELINE_PLUGIN_VERSION,
        null,
        null,
        '1.2.6',
        'The version of the AceSeq pipeline plugin'
)
processingOptionService.createOrUpdate(
        de.dkfz.tbi.otp.dataprocessing.roddy.RoddyConstants.OPTION_KEY_ACESEQ_BASE_PROJECT_CONFIG,
        null,
        null,
        'otpACEseq-1.0',
        'The base project file for AceSeq pipeline'
)

