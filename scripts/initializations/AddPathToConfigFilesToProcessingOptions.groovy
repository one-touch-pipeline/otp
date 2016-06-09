import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService


println ctx.processingOptionService.createOrUpdate(
        "roddyBaseConfigsPath",
        null,
        null,
        "/path/to/roddy/2.3.115/configs/",
        "Path to the baseConfig-files which are needed to execute Roddy"
)


println ctx.processingOptionService.createOrUpdate(
        "roddyApplicationIni",
        null,
        null,
        "/path/to/roddy/2.3.115/configs/applicationProperties.ini",
        "Path to the application.ini which is needed to execute Roddy"
)

println ctx.processingOptionService.createOrUpdate(
        ExecuteRoddyCommandService.FEATURE_TOGGLES_CONFIG_PATH,
        null,
        null,
        "/path/to/roddy/2.3.115/configs/featureToggles.ini",
        "Path to featureToggles.ini which contains feature toggles for Roddy",
)
