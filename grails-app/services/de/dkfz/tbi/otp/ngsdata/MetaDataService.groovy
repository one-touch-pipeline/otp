package de.dkfz.tbi.otp.ngsdata

class MetaDataService {

	def fileTypeService
	
	static transactional = true
	
	/////////////////////////////////////////////////////////////////////////////////////
	
	void registerInputFiles(long runId) {
		//
		// look int directory pointed by mdPath of a run
		// and register files that could be meta-data files
		//
		
		Run run = Run.get(runId)
	
		println "registering run ${run.name} from ${run.seqCenter}"
	
		String runDir = run.mdPath + "/run" + run.name
		File dir = new File(runDir)
		
		if (!dir.canRead() || !dir.isDirectory()) {
			println "not readable directory ${dir}"
			return
		}	
		
		def fileNames = dir.list()
	
		FileType fileType = FileType.findByType(FileType.Type.METADATA)
		DataFile dataFile
		fileNames.each {
			
			if (it.count("wrong")) return
			if (it.count("fastq") > 0 || it.count("align") > 0) {
				
				dataFile = new DataFile(pathName: runDir, fileName: it)
				run.addToDataFiles(dataFile)
				fileType.addToDataFiles(dataFile)
				
				safeSave(run)
				safeSave(dataFile)
			}
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	
	void loadMetaData(long runId) {
		//
		// this function loads
		//
		//
		//
		//

		Run run = Run.get(runId)

		println "loading metadata for run ${run.name}"

		def listOfMDFiles = []
		run.dataFiles.each {listOfMDFiles << it}

				
		DataFile dataFile
		listOfMDFiles.each {

			if (it.fileType.type != FileType.Type.METADATA) return

			println "\tfound md souce file ${it.fileName}"

			// hint to determine file type

			FileType.Type type = FileType.Type.UNKNOWN
			if (it.fileName.contains("fastq"))
				type = FileType.Type.SEQUENCE

			if (it.fileName.contains("align"))
				type = FileType.Type.ALIGNMENT

			File mdFile = new File(it.pathName + "/" + it.fileName)
				if (!mdFile.canRead()) {
				println "\tcan not read ${it.fileName}"
				return
			}

			def tokens
			def values
			def keys

			mdFile.eachLine { line, no ->

				if (no == 1) {

					long start = new Date().getTime()
					// parse the header
					tokens = tokenize(line, '\t');
					keys = getKeysFromTokens(tokens)
					long stop = new Date().getTime()
						
					//println "\theader ${stop - start}"

				} else {
					
					// match values with the header
					// new entry in MetaData

					long start = new Date().getTime()

					dataFile = new DataFile() // set-up later
					run.addToDataFiles(dataFile)

					safeSave(dataFile)

					values = tokenize(line, '\t')
					for(int i=0; i<keys.size(); i++) {

						MetaDataKey key = keys.getAt(i)
						MetaDataEntry entry = new MetaDataEntry (
							value: values.getAt(i) ?: "",
							source : MetaDataEntry.Source.MDFILE
						)

						key.addToMetaDataEntries(entry)                                                      
						dataFile.addToMetaDataEntries(entry);
					}

					long middle1 = new Date().getTime()

					// fill-up important fields
					assignFileName(dataFile)
					//assignFileType(dataFile, type)
					//addKnownMissingMetaData(run, dataFile)

					long middle2 = new Date().getTime()

					//dbGateService.safeSave(dataFile)
					//dbGateService.safeSave(run)

					long stop = new Date().getTime()
					
					//println "\tline1 ${middle1 - start}"
					//println "\tline2 ${middle2 - middle1}"
					//println "\tline3 ${stop - middle2}"
					//println "\tline ${stop-start}\n"
					
				}
			}

			// save the keys
			keys.each { key ->
				safeSave(key)
			}

			safeSave(run)

		}
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	
	private def tokenize(String line, String tab) {
		//
		// tokenize the string
		// a set of two tabs gives an empty token
		//

		//println ""

		def tokens = []
		def chars = line.getChars()

		int idx = 0
		for(int i=0; i<line.length(); i++) {

			if (chars[i] == tab || i == line.length()-1) {

				int end = (i==line.length()-1)? i+1 : i
				String token = line.substring(idx, end);
				token = token.replaceAll('\"', '');
				token = token.replaceAll(tab, '');
				tokens << token
				//println "${idx} ${i} ${token}"
				idx = i+1;
			}
		}

		tokens
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	
	private def getKeysFromTokens(List tokens) {
		//
		// this function builds list of meta-data keys
		// from string tokens
		//

		def keys = []
		MetaDataKey key

		tokens.each {

			String token = correctedKey(it)

			key = MetaDataKey.findByName(token)
			if (key == null) {
				key = new MetaDataKey(name: token)
				safeSave(key)
			}

			keys << key
		}

		keys
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	
	private void assignFileName(DataFile dataFile) {
		//
		// creates file name and md5sum
		// based on metadata entries
		//
		
		long start = new Date().getTime()
		
		def keyNames = ["FASTQ_FILE", "ALIGN_FILE"]

		keyNames.each { 

			MetaDataKey key = MetaDataKey.findByName(it)
			
			println "query start \n \n \n \n \n"
			
			long startDF = new Date().getTime()
			MetaDataEntry entry =
				MetaDataEntry.findByDataFileAndKey(dataFile, key)
				
			println "query stop \n \n \n \n \n"
			
			//MetaDataEntry entry	= getMetaDataEntry(dataFile, key)
			//dataFile.metaDataEntries.each { iEntry ->
			//	if (iEntry.key.name == it)
			//		entry = iEntry
			//}	
				
			long stopDF = new Date().getTime()
			//println "\tdynamic finder time ${stopDF - startDF}"
			
			if (!entry) return

			int idx = entry.value.lastIndexOf("/");

			dataFile.pathName = (idx == -1)? "" : entry.value.substring(0, idx)
			dataFile.fileName = entry.value.substring(idx+1) ?: "error"
		}

		// md5 check sum
		//MetaDataKey key = MetaDataKey.findByName("MD5")
		//MetaDataEntry entry = MetaDataEntry.findByDataFileAndKey(dataFile, key)
		MetaDataEntry entry = getMetaDataEntry(dataFile, "MD5")
		dataFile.md5sum = entry?.value


		if (dataFile.fileName == null) {

			dataFile.fileName = "errorNoHeader"
			dataFile.pathName = "errorNoHeader"

			dataFile.metaDataEntries.each {
				println "${it.key} ${it.value}"
			}
		}	
		
		long stop = new Date().getTime()	
		//println "\tfunction time ${stop-start}"
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
		
	private MetaDataEntry getMetaDataEntry(DataFile file, MetaDataKey key) {
		//
		// 
		//
		getMetaDataEntry(file, key.name)	
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	
	private MetaDataEntry getMetaDataEntry(DataFile file, String key) {
		//
		//	
		//
		
		MetaDataEntry entry = null
		
		file.metaDataEntries.each { MetaDataEntry iEntry ->
			if (iEntry.key.name == key) 
				entry = iEntry
		}
		
		return entry
	}

	/////////////////////////////////////////////////////////////////////////////////////
	
	private void assignFileType(DataFile dataFile, FileType.Type type) {
		
		//println dataFile.fileName
		FileType tt = fileTypeService.getFileType(dataFile.fileName, type)
		tt.addToDataFiles(dataFile)
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	
	private void addKnownMissingMetaData(Run run, DataFile dataFile) {
		//
		// solving known problems by manual addition of MD entries
		// missing entries are build from a directory name
		//

		String keyName = "SEQUENCING_TYPE"
		MetaDataKey key = MetaDataKey.findByName(keyName)
		if (!key) {
				key = new MetaDataKey(name: keyName)
				dbGateService.safeSave(key)
		}


		MetaDataEntry entry =
				MetaDataEntry.findByDataFileAndKey(dataFile, key)

		if (entry) return

		def types = SeqType.findAll()

		for(int iType = 0; iType < types.size(); iType++) {

			if (run.mdPath.contains(types[iType].dirName)) {

				String value = types[iType].name
				println "\tassiginig to ${value}"

				entry = new MetaDataEntry (
					value: value,
					source: MetaDataEntry.Source.SYSTEM
				)
				key.addToMetaDataEntries(entry)
				dataFile.addToMetaDataEntries(entry);
				return
			}
		}
	}
	/////////////////////////////////////////////////////////////////////////////////////

	private String correctedKey(String token) {
		
			if (token == "lane") return "LANE_NO"
			if (token == "SLIDE_NO") return "LANE_NO"
		
			token
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	
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
		
	/////////////////////////////////////////////////////////////////////////////////////
	
}