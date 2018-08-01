import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.ngsdata.*

/**
 * Template to move sample or change the sample type. It can also rename the file names.
 *
 * Therefore the following input are needed:
 * - OldProject: The name of the project the patient is in currently
 * - NewProject: The new project name, may the same
 * - OldPid: The old patient
 * - NewPid: The new patient pid, may the same if the project is differ
 * - OldSampleTypeName: The old sample type name
 * - NewSampleTypeName: The new sample type name or the same as the old. If changed, the patient may not have a sample of that sampleType
 * - Map of all fastq file names:
 *   - OldFileName: The old name of the fastq file
 *   - NewFileName: The new name of the fastq file. May be empty if it should not change
 * - uniqueScriptName: The name used for the generated scripts. That are a bash script to change the file system and a groovy script for trigger the alignment.
 *
 * You can use multiple 'dataSwapService.moveSample' calls in the script, duplicate therefore the call.
 */

DataSwapService dataSwapService = ctx.dataSwapService

StringBuilder outputStringBuilder = new StringBuilder()

final String scriptOutputDirectory = "${ConfigService.getInstance().getScriptOutputPath()}/sample_swap/"

boolean linkedFilesVerified = false

boolean failOnMissingFiles = true

try {
    Individual.withTransaction {

        dataSwapService.moveSample(
                'OldProject', 'NewProject',
                'OldPid', 'NewPid',
                'OldSampleType', 'NewSampleType',
                [
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


