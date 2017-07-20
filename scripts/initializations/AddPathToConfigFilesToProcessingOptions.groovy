import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName

println ctx.processingOptionService.createOrUpdate(
        OptionName.RODDY_BASE_CONFIGS_PATH,
        null,
        null,
        "/path/to/roddy/release/configs/"
)


println ctx.processingOptionService.createOrUpdate(
        OptionName.RODDY_APPLICATION_INI,
        null,
        null,
        "/path/to/roddy/release/configs/applicationProperties.ini"
)

println ctx.processingOptionService.createOrUpdate(
        OptionName.RODDY_FEATURE_TOGGLES_CONFIG_PATH,
        null,
        null,
        "/path/to/roddy/release/configs/featureToggles.ini"
)
