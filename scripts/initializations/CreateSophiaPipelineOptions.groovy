import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName


ProcessingOptionService processingOptionService = ctx.processingOptionService

processingOptionService.createOrUpdate(
        OptionName.PIPELINE_SOPHIA_PLUGIN_NAME,
        null,
        null,
        'SophiaWorkflow',
        'Name of the Sophia pipeline plugin'
)
processingOptionService.createOrUpdate(
        OptionName.PIPELINE_SOPHIA_PLUGIN_VERSIONS,
        null,
        null,
        '1.0.16',
        'The version of the Sophia pipeline plugin'
)
processingOptionService.createOrUpdate(
        OptionName.PIPELINE_SOPHIA_BASE_PROJECT_CONFIG,
        null,
        null,
        'otpSophia-1.0',
        'The base project file for Sophia pipeline'
)
