/**
 *
 * Script to add
 * - the path to the Roddy.sh script and
 * - the currently used version of Roddy
 * in the processing options.
 */


println ctx.processingOptionService.createOrUpdate(
        "roddyPath",
        null,
        null,
        "/path/to/roddy/2.3.115/roddy/",
        "Path to the roddy.sh on the current cluster (***REMOVED***cluster 13.1)",
)
