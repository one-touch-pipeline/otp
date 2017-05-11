import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddy.*


ProcessingOptionService processingOptionService = ctx.processingOptionService

processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_SOPHIA_PIPELINE_PLUGIN_NAME,
        null,
        null,
        'SophiaWorkflow',
        'Name of the Sophia pipeline plugin'
)
processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_SOPHIA_PIPELINE_PLUGIN_VERSION,
        null,
        null,
        '1.0.15',
        'The version of the Sophia pipeline plugin'
)
processingOptionService.createOrUpdate(
        RoddyConstants.OPTION_KEY_SOPHIA_BASE_PROJECT_CONFIG,
        null,
        null,
        'otpSophia-1.0',
        'The base project file for Sophia pipeline'
)
