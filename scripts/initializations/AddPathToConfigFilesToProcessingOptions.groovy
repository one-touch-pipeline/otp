
println ctx.processingOptionService.createOrUpdate(
        "roddyBaseConfigsPath",
        null,
        null,
        "/path/to/roddyBaseConfigs",
        "Path to the baseConfig-files which are needed to execute Roddy"
)


println ctx.processingOptionService.createOrUpdate(
        "roddyApplicationIni",
        null,
        null,
        "/path/to/roddyBaseConfigs/applicationProperties.ini",
        "Path to the application.ini which is needed to execute Roddy"
)