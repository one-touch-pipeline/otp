package de.dkfz.tbi.otp.ngsdata

class RunController {

    def lsdfFilesService
    def runService

    static scaffold = Run

    def display = {
        redirect(action: "show", id: params.id)
    }

    def show = {
        if (!params.id) {
            params.id = "0"
        }
        Run run = runService.getRun(params.id)
        if (!run) {
            response.sendError(404)
            return
        }
        List<MetaDataKey> keys = []
        keys[0] = MetaDataKey.findByName("SAMPLE_ID")
        keys[1] = MetaDataKey.findByName("WITHDRAWN")

        return [run: run,
                finalPaths: lsdfFilesService.getAllPathsForRun(run),
                keys: keys,
                processParameters: runService.retrieveProcessParameters(run),
                metaDataFiles: runService.retrieveMetaDataFilesByInitialPath(run),
                seqTracks: runService.retrieveSequenceTrackInformation(run),
                errorFiles: runService.dataFilesWithError(run),
                nextRun: runService.nextRun(run),
                previousRun: runService.previousRun(run)
        ]
    }
}
