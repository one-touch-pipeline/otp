import de.dkfz.tbi.otp.dataprocessing.ProcessingOption

/**
 *
 * Script to add
 * - the path to the Roddy.sh script and
 * - the currently used version of Roddy
 * in the processing options.
 */


println ctx.processingOptionService.createOrUpdate(
        ProcessingOption.OptionName.RODDY_PATH,
        null,
        null,
        "/path/to/roddy/release/roddy/"
)
