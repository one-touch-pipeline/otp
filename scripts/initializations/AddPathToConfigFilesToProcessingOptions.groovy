import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName

println ctx.processingOptionService.createOrUpdate(
        OptionName.RODDY_BASE_CONFIGS_PATH,
        null,
        null,
        "/path/to/roddy/release/configs/",
        "Path to the baseConfig-files which are needed to execute Roddy"
)


println ctx.processingOptionService.createOrUpdate(
        OptionName.RODDY_APPLICATION_INI,
        null,
        null,
        "/path/to/roddy/release/configs/applicationProperties.ini",
        "Path to the application.ini which is needed to execute Roddy"
)

println ctx.processingOptionService.createOrUpdate(
        OptionName.RODDY_FEATURE_TOGGLES_CONFIG_PATH,
        null,
        null,
        "/path/to/roddy/release/configs/featureToggles.ini",
        "Path to featureToggles.ini which contains feature toggles for Roddy",
)
