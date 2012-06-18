package de.dkfz.tbi.otp.ngsdata

import grails.plugins.springsecurity.Secured

@Secured(['ROLE_ADMIN'])
class RunSubmitController {

    def index() {
        def centers = SeqCenter.list()
        def seqPlatform = SeqPlatform.list()*.fullName()
        [centers: centers, seqPlatform: seqPlatform]
    }

    def submit() {
        println "Run submitting ..."
        Run run = findOrCreateRun(params)
        safeSave(run)
        println run
        RunSegment segment = createSegment(params, run)
        safeSave(segment)
        println segment
        redirect(controller: "run", action: "show", id: run.id)
    }

    Run findOrCreateRun(def params) {
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

    RunSegment createSegment(def params, Run run) {
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

    RunSegment.FilesStatus defineFilesStatus(String format) {
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

    SeqPlatform getPlatform(String fullName) {
        int idx = fullName.indexOf(" ")
        String name = fullName.substring(0, idx)
        String model = fullName.substring(idx+1)
        return SeqPlatform.findByNameAndModel(name, model)
    }

    boolean safeSave(Object obj) {
        if (obj.validate() && obj.save(flush: true)) {
            return true
        } else {
            println obj.errors
            //render("<h1>Errors</h1><p>${obj.errors}")
            return false
        }
    }
}
