package de.dkfz.tbi.otp.ngsdata

class RunSubmitService {

    long submit(def params) {
        Run run = findOrCreateRun(params)
        safeSave(run)
        RunSegment segment = createSegment(params, run)
        safeSave(segment)
        return run.id
    }

    private Run findOrCreateRun(def params) {
        Run run = Run.findByName(params.runName)
        if (run) {
            return run
        }
        SeqPlatform platform = getPlatform(params.seqPlatform)
        run = new Run(
            name: params.runName,
            seqCenter: SeqCenter.findByName(params.seqCenter),
            seqPlatform: platform
        )
        return run
    }

    private RunSegment createSegment(def params, Run run) {
        RunSegment.DataFormat format = RunSegment.DataFormat."${params.initialFormat}"
        RunSegment segment = new RunSegment(
            dataPath: params.dataPath,
            mdPath: params.dataPath,
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

    private SeqPlatform getPlatform(String fullName) {
        int idx = fullName.indexOf(" ")
        String name = fullName.substring(0, idx)
        String model = fullName.substring(idx+1)
        return SeqPlatform.findByNameAndModel(name, model)
    }

    private boolean safeSave(Object obj) {
        if (obj.validate() && obj.save(flush: true)) {
            return true
        } else {
            println obj.errors
            return false
        }
    }
}

