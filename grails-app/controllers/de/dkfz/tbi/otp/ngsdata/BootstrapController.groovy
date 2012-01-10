package de.dkfz.tbi.otp.ngsdata

class BootstrapController {

    def individualService
    def seqScanService
    def mergingService
    def lsdfFilesService
    def filesCompletenessService

    def index() {
    }

    /*
    def bootstrap = {
       // individualService.loadIndividuals()
       // individualService.loadSamples()
       // render "Individual and Samples loaded"
    }
    */

    def buildSeqScans = {
        seqScanService.buildSeqScans()
        render "created"
    }

    def discoverBams = {
        List<Individual> inds = Individual.findAll()
        inds.each {Individual ind ->
            println ind
            mergingService.discoverMergedBams(ind)
        }
        render "done"
    }

    def checkProstate = {
        filesCompletenessService.checkAllRuns("PROJECT_NAME")
        render "checked"
    }
}
