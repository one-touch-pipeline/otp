import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService


println ctx.processingOptionService.createOrUpdate(
        "roddyBaseConfigsPath",
        null,
        null,
        "/path/to/programs/otp/RoddyBaseConfigs",
        "Path to the baseConfig-files which are needed to execute Roddy"
)


println ctx.processingOptionService.createOrUpdate(
        "roddyApplicationIni",
        null,
        null,
        "/path/to/programs/otp/RoddyBaseConfigs/applicationProperties.ini",
        "Path to the application.ini which is needed to execute Roddy"
)

println ctx.processingOptionService.createOrUpdate(
        ExecuteRoddyCommandService.FEATURE_TOGGLES_CONFIG_PATH,
        null,
        null,
        "/path/to/programs/otp/RoddyBaseConfigs/featureToggles.ini",
        "Path to featureToggles.ini which contains feature toggles for Roddy",
)
