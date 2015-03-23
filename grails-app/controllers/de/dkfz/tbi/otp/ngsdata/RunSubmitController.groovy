package de.dkfz.tbi.otp.ngsdata

import grails.plugins.springsecurity.Secured

@Secured(['ROLE_ADMIN'])
class RunSubmitController {

    def runSubmitService

    def index() {
        def centers = SeqCenter.list()
        def seqPlatform = SeqPlatform.list(sort: "name", order: "asc")*.fullName()
        return [centers: centers, seqPlatform: seqPlatform, cmd: flash.params]
    }

    def submit(SubmitCommand cmd) {
        if(cmd.hasErrors()) {
            flash.params = cmd
            redirect(action: "index")
        } else {
            long runId = runSubmitService.submit(cmd.name, cmd.seqPlatform, cmd.seqCenter, cmd.dataPath, cmd.align)
            redirect(controller: "run", action: "show", id: runId)
        }
    }
}

class SubmitCommand implements Serializable {
    String name
    String seqPlatform
    String seqCenter
    String dataPath
    boolean align

    static constraints = {
        importFrom Run, include: ["name", "seqPlatform", "seqCenter"]
        importFrom RunSegment, include: ["dataPath", "align"]
    }
}
