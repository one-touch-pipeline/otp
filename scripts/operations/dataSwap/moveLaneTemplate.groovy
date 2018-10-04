import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.ngsdata.*

/**
 * Template to move/rename a patient call. It can also change the SampleTypes of the patient and rename the file names.
 *
 * Therefore the following input are needed:
 * - OldProject: The name of the project the patient is in currently
 * - NewProject: The new project name, may the same
 * - OldPid: The old patient
 * - NewPid: The new patient pid, may the same if the project is differ
 * - OldSampleTypeName: The old sample type name
 * - NewSampleTypeName: The new sample type name or the same as the old.
 * - OldSeqTypeName: The old seqType name
 * - NewSeqTypeName: The new seqType name
 * - OldLibraryLayout: The old libraryLayout
 * - NewLibraryLayout: The new libraryLayout. Should have the same file count, otherwise the swap to not work.
 * - RunName: the name of the run the lanes are on.
 * - list of otp lanes (lane with barcode): The lane ids of the run for the migration
 * - Map of all fastq file names for given run and laneids:
 *   - OldFileName: The old name of the fastq file
 *   - NewFileName: The new name of the fastq file. May be empty if it should not change
 * - uniqueScriptName: The name used for the generated scripts. That are a bash script to change the file system and a groovy script for trigger the alignment.
 *
 * You can use multiple 'dataSwapService.swapLane' calls in the script, duplicate therefore the call.
 */

DataSwapService dataSwapService = ctx.dataSwapService

StringBuilder outputStringBuilder = new StringBuilder()

final String scriptOutputDirectory = "${ConfigService.getInstance().getScriptOutputPath()}/sample_swap/"

boolean linkedFilesVerified = false

boolean failOnMissingFiles = true

try {
    Individual.withTransaction {

        dataSwapService.swapLane([
                'oldProjectName'   : 'OldProject',
                'newProjectName'   : 'NewProject',
                'oldPid'           : 'OldPid',
                'newPid'           : 'NewPid',
                'oldSampleTypeName': 'OldSampleType',
                'newSampleTypeName': 'NewSampleType',
                'oldSeqTypeName'   : 'OldSeqTypeName',
                'newSeqTypeName'   : 'NewSeqTypeName',
                'oldSingleCell'    : 'OldSingleCell',
                'newSingleCell'    : 'NewSingleCell',
                'oldLibraryLayout' : 'OldLibraryLayout',
                'newLibraryLayout' : 'NewLibraryLayout',
                'runName'          : 'RunName',
                'lane'             : [
                        'lane1',
                        'lane2',
                        //...
                ],
        ], [
                'OldFileName1': 'NewFileName1',
                'OldFileName2': 'NewFileName2',
                'OldFileName3': '',
                'OldFileName4': '',
        ],
                'uniqueScriptName',
                outputStringBuilder,
                failOnMissingFiles,
                scriptOutputDirectory,
                linkedFilesVerified
        )

        assert false
    }
} finally {
    println outputStringBuilder
}
