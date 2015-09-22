import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService

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
        "/path/to/roddy/",
        "Path to the roddy.sh on the current cluster (***REMOVED***cluster 11.4)",
)

println ctx.processingOptionService.createOrUpdate(
        ExecuteRoddyCommandService.CORRECT_PERMISSION_SCRIPT_NAME,
        null,
        null,
        "/path/to/programs/otp/OtherUnixUserBashScripts/correctPathPermissionsOtherUnixUserRemoteWrapper.sh",
        "Script to correct file/directoryPermissions",
)

println ctx.processingOptionService.createOrUpdate(
        ExecuteRoddyCommandService.CORRECT_GROUP_SCRIPT_NAME,
        null,
        null,
        "/path/to/programs/otp/OtherUnixUserBashScripts/correctGroupOtherUnixUserRemoteWrapper.sh",
        "Script to correct file system groups",
)

println ctx.processingOptionService.createOrUpdate(
        ExecuteRoddyCommandService.DELETE_CONTENT_OF_OTHERUNIXUSER_DIRECTORIES_SCRIPT,
        null,
        null,
        "/path/to/programs/otp/OtherUnixUserBashScripts/deleteContentOfRoddyDirectoriesRemoteWrapper.sh",
        "Script to delete content of directories owned by OtherUnixUser",
)

println ctx.processingOptionService.createOrUpdate(
        "roddyVersion",
        null,
        null,
        "2.2.66",
        "Roddy version which is used currently to process Roddy-Pipelines"
)
