import de.dkfz.tbi.otp.ngsdata.*

// input area
//----------------------

String projectName

//script area
//-----------------------------

assert projectName

String baseOutputDir = "${ConfigService.getInstance().getScriptOutputPath()}/sample_swap/"

DataSwapService dataSwapService = ctx.dataSwapService

Project.withTransaction {

    dataSwapService.deleteProcessingFilesOfProject(projectName, baseOutputDir)

    assert false
}
