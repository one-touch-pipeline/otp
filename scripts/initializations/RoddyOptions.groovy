import de.dkfz.tbi.otp.dataprocessing.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*


ProcessingOptionService processingOptionService = ctx.processingOptionService

String path = "/path/to/roddy"

processingOptionService.createOrUpdate(
        RODDY_PATH,
        "${path}/roddy/release"
)

processingOptionService.createOrUpdate(
        RODDY_BASE_CONFIGS_PATH,
        "${path}/configs/"
)

processingOptionService.createOrUpdate(
        RODDY_APPLICATION_INI,
        "${path}/configs/applicationProperties.ini"
)

processingOptionService.createOrUpdate(
        RODDY_FEATURE_TOGGLES_CONFIG_PATH,
        "${path}/configs/featureToggles.ini"
)
