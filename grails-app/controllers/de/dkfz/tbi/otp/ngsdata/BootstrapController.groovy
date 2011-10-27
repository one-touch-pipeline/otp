package de.dkfz.tbi.otp.ngsdata

class BootstrapController {
	
	def metaDataService
	def bootstrapService
	
    def index() {		
	}
	
	def bootstrap = {
		bootstrapService.bootstrap()
		render "bootstraping ..."
	}
	
	def loadRuns = {
		bootstrapService.importRuns()	
		render "loading runs"
	}
	
	
	def buildSeqScans = {
		metaDataService.buildSeqScans()
		render "created"
	}
}
