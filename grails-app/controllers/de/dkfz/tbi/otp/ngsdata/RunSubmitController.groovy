package de.dkfz.tbi.otp.ngsdata

import grails.plugins.springsecurity.Secured

@Secured(['ROLE_ADMIN'])
class RunSubmitController {

    def index() {
        def centers = SeqCenter.list()
        def seqPlatform = SeqPlatform.list()
        [centers: centers, seqPlatform: seqPlatform]
    }

    def submit() {

        SeqPlatform platform = getPlatform(params.seqPlatform)

        Run run = new Run(
            name: params.runName,
            multipleSource: false,
            seqCenter: SeqCenter.findByName(params.seqCenter),
            seqPlatform: platform
        )
        if (!safeSave(run)) {
            return
        }
        RunInitialPath path = new RunInitialPath(
            dataPath: params.dataPath, 
            mdPath: params.dataPath,
            run: run
        )
        safeSave(path)
        redirect(controller: "run", action: "show", id: run.id)
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
            render("<h1>Errors</h1><p>${obj.errors}")
            return false
        }
    }
}
