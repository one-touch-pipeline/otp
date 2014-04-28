package de.dkfz.tbi.otp.ngsdata

class RunSubmitService {

    long submit(String runName, String seqPlatform, String seqCenter, String initialFormat, String dataPath) {
        Run run = findOrCreateRun(runName, seqPlatform, seqCenter)
        safeSave(run)
        RunSegment segment = createSegment(initialFormat, dataPath, run)
        safeSave(segment)
        return run.id
    }

    private static Run findOrCreateRun(String runName, String seqPlatform, String seqCenter) {
        Run run = Run.findByName(runName)
        if (run) {
            return run
        }
        SeqPlatform platform = getPlatform(seqPlatform)
        run = new Run(
            name: runName,
            seqCenter: SeqCenter.findByName(seqCenter),
            seqPlatform: platform
        )
        return run
    }

    private RunSegment createSegment(String initialFormat, String dataPath, Run run) {
        RunSegment.DataFormat format = RunSegment.DataFormat."${initialFormat}"
        RunSegment segment = new RunSegment(
            dataPath: dataPath,
            mdPath: dataPath,
            metaDataStatus: RunSegment.Status.NEW,
            initialFormat: format,
            currentFormat: format,
            filesStatus: defineFilesStatus(format.toString()),
            run: run
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

    private static SeqPlatform getPlatform(String fullName) {
        int idx = fullName.indexOf(" ")
        String name = fullName.substring(0, idx)
        String model = fullName.substring(idx+1)
        return SeqPlatform.findByNameAndModel(name, model)
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
