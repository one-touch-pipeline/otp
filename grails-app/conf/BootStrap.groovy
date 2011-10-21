
import de.dkfz.tbi.otp.ngsdata.*


class BootStrap {
	
	def individualService
	def fileTypeService
	def metaDataService
	
	// used in many finctions
	def seqNames = ["WHOLE_GENOME", "RNA", "MI_RNA", "EXON"]
	def llNames  = ["SINGLE", "PAIRED", "MATE_PAIR"]
	def seqDirs  = ["whole_genome_sequencing", "rna_sequencing",
					"mi_rna_sequencing", "exen_sequencing"]
	
	
	def projNames = ["PROJECT_NAME", "PROJECT_NAME", "PROJECT_NAME"]
	def projDirs  = ["PROJECT_NAME", "PROJECT_DIR", "PROJECT_DIR"]
	def projHost  = ["DKFZ", "BioQuant", "BioQuant"]
	
	
	def centers  = ["dkfz", "gatc", "mpimg", "embl"]
	def dirNames = ["core", "gatc", "mpimg", "embl"]
	
    def init = { servletContext ->
		
		//
		// this part will go to execution plan
		//
		
		println 'Adding basic structures ...'
		
		/*
		def seqNames = ["WHOLE_GENOME", "RNA", "MI_RNA", "EXON"]
		def llNames  = ["SINGLE", "PAIRED", "MATE_PAIR"]
		def seqDirs  = ["whole_genome_sequencing", "rna_sequencing",
						"mi_rna_sequencing", "exen_sequencing"]
		*/
		
		SeqType seq
		for(int i=0; i<seqNames.size(); i++) {
			for(int j=0; j<llNames.size(); j++) {
				seq = new SeqType(
					name: seqNames[i],
					libraryLayout: llNames[j],
					dirName: seqDirs[i]
				)
				safeSave(seq)
			}
		}
		
		/*
		def projNames = ["PROJECT_NAME", "PROJECT_NAME", "PROJECT_NAME"]
		def projDirs  = ["PROJECT_NAME", "PROJECT_DIR", "PROJECT_DIR"]
		def projHost  = ["DKFZ", "BioQuant", "BioQuant"]
		*/
		
		Project proj
		for (int i=0; i<projNames.size(); i++) {
			proj = new Project(
				name: projNames[i],
				dirName: projDirs[i],
				host: projHost[i]
			)
			safeSave(proj)
		}
		
		/*
		def centers  = ["dkfz", "gatc", "mpimg", "embl"]
		def dirNames = ["core", "gatc", "mpimg", "embl"]
		*/

		for(int i=0; i<centers.size; i++) {
			SeqCenter seqCenter = new SeqCenter(
				name: centers[i].toUpperCase(),
				dirName: dirNames[i]
			)
			safeSave(seqCenter)
		}


		def tech = ["solid", "illumina"]
		tech.each {
			SeqTech seqTech = new SeqTech(name: it)
			safeSave(seqTech)
		}

		
		fileTypeService.loadFileTypes()		
		
		individualService.loadIndividuals()
		individualService.loadSamples()
		
		importRuns()
	}		
	
	///////////////////////////////////////////////////////////////////////////
	
	void importRuns() {
		//
		// import runs from private meta-data
		//
	
		def illumina = SeqTech.findByName("illumina")
		def solid = SeqTech.findByName("solid")

		String dataPath = "$ROOT_PATH/ftp"
		String mdPath = "${home}ngs-icgc/data-tracking-private/"

		int start = 0
		int limit = 500
		int counter = 0

		for(int iProj = 0; iProj < 1 /*projDirs.size()*/; iProj++) {
			for(int iSeq = 0; iSeq < seqDirs.size(); iSeq++) {
				for(int iCenter = 0; iCenter < dirNames.size(); iCenter++) {

					SeqCenter seqCenter = SeqCenter.findByDirName(dirNames[iCenter])

					String path = mdPath + "/" + projDirs[iProj] +
						 "/sequencing/" + seqDirs[iSeq] + "/" + dirNames[iCenter]

					File dir = new File(path)

					println path

					if (!dir.canRead() || !dir.isDirectory()) continue

					dir.list().each {runDirName ->
						
						counter++  // loading only part of runs
						if (counter < start) return
						if (counter > limit) return

						File runDir = new File(path + "/" + runDirName)
						if (!runDir.canRead() || !runDir.isDirectory()) return
						if (runDirName.indexOf("run") != 0) return

						String name = runDirName.substring(3) // remove "run"

						SeqTech seqTech
						if (name.contains("solid")) seqTech = solid
						else seqTech = illumina

						Run run = new Run(
							name: name,
							seqCenter: seqCenter,
							seqTech: seqTech,
							dataPath : dataPath,
							mdPath : path
						)
						
						//seqCenter.addToRuns(run)
						//tech.addToRuns(run)
						//Project.findByName("PROJECT_NAME").addToRuns(run);

						safeSave(run)
						
						long id = run.id

						// processing and poor's man time measurement
									
						def times = []
						println run.name									
						times << new Date().getTime()
						metaDataService.registerInputFiles(id)
						times << new Date().getTime()
						metaDataService.loadMetaData(id)
						times << new Date().getTime()
						//metaDataService.validateMetadata(id)
						times << new Date().getTime()
						//metaDataService.buildExecutionDate(id)
						times << new Date().getTime()
						//metaDataService.buildSequenceTracks(id)
						times << new Date().getTime()
						//metaDataService.checkSequenceTracks(id)
						times << new Date().getTime()
									
						println "total ${times[6]-times[0]}"
						for(int i=1; i<times.size(); i++) {
							println "step ${i} ${times[i]-times[i-1]}"
						}
					}
				}
			}
		}
									
		//buildSeqScans()	
	}
	
	///////////////////////////////////////////////////////////////////////////
    
	def destroy = {
    }

	///////////////////////////////////////////////////////////////////////////
	
	private void safeSave(def obj) {
		//
		// probably will go to separate static class
		// no formal exception, information only
		//
		
		obj.validate()
		if (obj.hasErrors()) {
			println obj.errors
			return
		}

		if (!obj.save(flush: true))
			println "can not save ${obj}"
	}
		
	///////////////////////////////////////////////////////////////////////////
	
}
