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

    def submit() {
        long runId = runSubmitService.submit(params)
        redirect(controller: "run", action: "show", id: runId)
    }
}
