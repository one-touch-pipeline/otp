package de.dkfz.tbi.otp.ngsdata

class BootstrapController {
	
	def bootstrapService
	def seqScanService
    
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
		seqScanService.buildSeqScans()
		render "created"
	}
}
