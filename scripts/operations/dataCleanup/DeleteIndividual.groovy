import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.ngsdata.*

// input area
//----------------------

String pid

boolean check = true


//script area
//-----------------------------

assert pid

DataSwapService dataSwapService = ctx.dataSwapService

String baseOutputDir = "${ConfigService.getInstance().getScriptOutputPath()}/sample_swap/"

Individual.withTransaction {
    List<String> allFilesToRemove = dataSwapService.deleteIndividual(pid, check)

    File deleteFileCmd = dataSwapService.createFileSafely(baseOutputDir, "Delete_${pid}.sh")
    File deleteFileSecondUserCmd = dataSwapService.createFileSafely(baseOutputDir, "DeleteSecondUser_${pid}.sh")

    deleteFileCmd << allFilesToRemove[0]
    deleteFileSecondUserCmd << allFilesToRemove[1]

    assert false
}
