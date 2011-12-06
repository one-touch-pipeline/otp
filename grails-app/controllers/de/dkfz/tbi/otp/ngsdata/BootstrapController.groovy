package de.dkfz.tbi.otp.ngsdata

class BootstrapController {

    def individualService
    def bootstrapService
    def seqScanService
    def mergingService
    def lsdfFilesService

    def index() {
    }

    def bootstrap = {
        individualService.loadIndividuals()
        individualService.loadSamples()
        render "Individual and Samples loaded"
    }

    def loadRuns = {
        bootstrapService.importRuns()
        render "loading runs"
    }


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
        lsdfFilesService.checkAllRuns("PROJECT_NAME")
        render "checked"
    }
}
