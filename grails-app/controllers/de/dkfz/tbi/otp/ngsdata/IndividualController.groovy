package de.dkfz.tbi.otp.ngsdata

class IndividualController {

    def individualService
    def igvSessionFileService

    def scaffold = Individual

    def display = {
        redirect(action: "show", id: params.id)
    }

    def show = {
        if (!params.id) {
            params.id = "0"
        }
        Individual ind = individualService.getIndividual(params.id)
        if (!ind) {
            response.sendError(404)
            return
        }

        [
            ind: ind,
            previous: individualService.previousIndividual(ind),
            next: individualService.nextIndividual(ind),
            igvMap: igvSessionFileService.createMapOfIgvEnabledScans(ind.seqScans)
        ]
    }

    def igvStart = {
        println params
        println flash

        // TODO: this code is crashy and cannot be protected
        List<SeqScan> scans = new ArrayList<SeqScan>()
        params.each {
            if (it.value == "on") {
                SeqScan scan = SeqScan.get(it.key as int)
                scans << scan
            }
        }
        String url = igvSessionFileService.buildSessionFile(scans, request.getRequestURL().toString())
        println "Redirecting: ${url}"
        redirect(url: url)
    }

    def igvDownload = {
        println "Download"
        render "downloding ..."
    }
}
