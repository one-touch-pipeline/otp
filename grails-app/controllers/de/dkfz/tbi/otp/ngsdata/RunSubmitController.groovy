package de.dkfz.tbi.otp.ngsdata

import grails.plugins.springsecurity.Secured

@Secured(['ROLE_ADMIN'])
class RunSubmitController {

    def runSubmitService

    def index() {
        def centers = SeqCenter.list()
        def seqPlatform = SeqPlatform.list()*.fullName()
        [centers: centers, seqPlatform: seqPlatform]
    }

    def submit(SubmitCommand cmd) {
        long runId = runSubmitService.submit(cmd.runName, cmd.seqPlatform, cmd.seqCenter,
                cmd.initialFormat, cmd.dataPath, cmd.align)
        redirect(controller: "run", action: "show", id: runId)
    }
}

class SubmitCommand implements Serializable {
    String runName
    String seqPlatform
    String seqCenter
    String initialFormat
    String dataPath
    boolean align
}
