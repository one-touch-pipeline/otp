package de.dkfz.tbi.otp.ngsdata

import grails.plugins.springsecurity.Secured

class ResultsUploadCommand {
    String identifier
    String fileText
}

@Secured(['ROLE_ADMIN'])
class ResultsUploadController {

    def resultsParserService

    def index = {
        
    }

    def upload = {ResultsUploadCommand ruc ->
        ResultsDataFile rdf = new ResultsDataFile(
            identifier: ruc.identifier,
            text: ruc.fileText
        )
        rdf.save(flush: true)
        resultsParserService.parse(rdf)
        render("registered")
    }
}
