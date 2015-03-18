package de.dkfz.tbi.otp.ngsdata

class RunSubmitService {

    long submit(String runName, String seqPlatformId, String seqCenter, String dataPath, boolean align) {
        Run run = findOrCreateRun(runName, seqPlatformId, seqCenter)
        safeSave(run)
        RunSegment segment = createSegment(dataPath, align, run)
        safeSave(segment)
        return run.id
    }

    private static Run findOrCreateRun(String runName, String seqPlatformId, String seqCenter) {
        Run run = Run.findByName(runName)
        if (run) {
            return run
        }
        SeqPlatform platform = SeqPlatform.get(seqPlatformId)
        run = new Run(
                name: runName,
                seqCenter: SeqCenter.findByName(seqCenter),
                seqPlatform: platform
                )
        return run
    }

    private RunSegment createSegment(String dataPath, boolean align, Run run) {
        RunSegment.DataFormat format = RunSegment.DataFormat.FILES_IN_DIRECTORY
        RunSegment segment = new RunSegment(
                dataPath: dataPath,
                mdPath: dataPath,
                metaDataStatus: RunSegment.Status.NEW,
                initialFormat: format,
                currentFormat: format,
                filesStatus: defineFilesStatus(format.toString()),
                run: run,
                align: align,
                )
        return segment
    }

    private RunSegment.FilesStatus defineFilesStatus(String format) {
        RunSegment.DataFormat dataFormat = RunSegment.DataFormat."${format}"
        switch(dataFormat) {
            case RunSegment.DataFormat.FILES_IN_DIRECTORY:
                return RunSegment.FilesStatus.NEEDS_INSTALLATION
            case RunSegment.DataFormat.TAR_IN_DIRECTORY:
            case RunSegment.DataFormat.TAR:
                return RunSegment.FilesStatus.NEEDS_UNPACK
        }
        throw new Exception("Unknown files format")
    }

    private static boolean safeSave(Object obj) {
        if (obj.validate() && obj.save(flush: true)) {
            return true
        } else {
            println obj.errors
            return false
        }
    }
}
