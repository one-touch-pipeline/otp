import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.ConfigService

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*


ProcessingOptionService processingOptionService = ctx.processingOptionService

String roddy_base_path = ConfigService.getInstance().getRoddyPath().toString()

processingOptionService.createOrUpdate(
        RODDY_PATH,
        "${roddy_base_path}/roddy/release"
)

processingOptionService.createOrUpdate(
        RODDY_BASE_CONFIGS_PATH,
        "${roddy_base_path}/configs"
)

processingOptionService.createOrUpdate(
        RODDY_APPLICATION_INI,
        "${roddy_base_path}/configs/applicationProperties.ini"
)

processingOptionService.createOrUpdate(
        RODDY_FEATURE_TOGGLES_CONFIG_PATH,
        "${roddy_base_path}/configs/featureToggles.ini"
)
