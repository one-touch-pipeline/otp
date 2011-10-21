package de.dkfz.tbi.otp.ngsdata

class IndividualService {

	String resourcesPath = "${home}STS/ngsTest/resources/"
	
	
	///////////////////////////////////////////////////////////////////////////
	
	void loadIndividuals() {
		//
		// this function loads individuals name from the provided list
		//

		// PROJECT_NAME

		Project proj = Project.findByName("PROJECT_NAME")

		File file = new File(resourcesPath + "listInd.txt")
		if (!file.canRead()) {
			// shall be exception
			println "can not read ${file}"
			return
		}
						
		println "reading list of individuals from ${file}"
		
		Individual ind
		file.eachLine { line, no ->
			
			// magic standard to DB relation
			String pid = line.substring(0, line.indexOf(','))
			String name = line.substring(line.indexOf(',') + 1)
			String mockPid = (no as String).padLeft(3, '0')

			ind = new Individual(mockFullName: name, pid: pid, mockPid: mockPid)
			ind.type = Individual.Type.REAL
			
			// hack
			if (name.indexOf("Pool") != -1) ind.type = Individual.Type.POOL

			proj.addToIndividuals(ind)
			safeSave(ind)
		}
	}

	///////////////////////////////////////////////////////////////////////////
	
	def loadSamples() {
		//
		// this function load samples from the provided list
		// and attache samples to individuals
		//

		// PROJECT_NAME

		String prefix = "${home}"

		def files = [
			"$DATA_HOME/PROJECT_NAME/map-pid-tumor-control.csv",
			"$DATA_HOME/PROJECT_NAME/map-pid-tumor-control-with-prefix.csv"
		]

		Sample sample

		files.each {
			
			File file = new File(prefix + it);
			if (!file.canRead()) {
				println "can not read ${file}"
				return
				
			}

			println "reading samples from file ${it}"

			file.eachLine { line, no ->

				// magic file format
				String name = line.substring(0, line.indexOf(','));
				String pid  = line.substring(line.indexOf(',') + 1, line.indexOf('/'))

				// find out type
				String typeText  = line.substring(line.indexOf('/') + 1)
				Sample.Type type = Sample.Type.UNKNOWN
				if (typeText.indexOf("tumor") != -1) type = Sample.Type.TUMOR
				if (typeText.indexOf("control") != -1) type = Sample.Type.CONTROL

				Individual ind = Individual.findByPid(pid);

				sample = Sample.findByIndividualAndType(ind, type)

				if (sample == null) {
					sample = new Sample(type: type)
					ind.addToSamples(sample)
					safeSave(sample)
				}

				SampleIdentifier identifier = new SampleIdentifier(
					name: name,
					sample: sample
				)
				//sample.addToSampleIdentifiers(name);

				safeSave(identifier)
				safeSave(ind)
				safeSave(sample)
			}
		}
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
