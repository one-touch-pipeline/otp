package de.dkfz.tbi.otp.ngsdata

class IndividualController {
	
	def scaffold = Individual
	
	//def list = {
	//	render "test"
	//}

    def display = {

        println params.id
        Individual ind = Individual.findByMockFullName(params.id)
        redirect(action: show, id: ind.id)
    }


    def show = {

        Individual ind = Individual.get(params.id)
        Vector<SeqType> seqTypes= new Vector<SeqType>();

        seqTypes.add(SeqType.findByNameAndLibraryLayout("WHOLE_GENOME", "PAIRED"))
        seqTypes.add(SeqType.findByNameAndLibraryLayout("WHOLE_GENOME", "MATE_PAIR"))
        seqTypes.add(SeqType.findByNameAndLibraryLayout("RNA", "PAIRED"))
        seqTypes.add(SeqType.findByName("MI_RNA")) 

        List<SeqScan> seqScans = new ArrayList<SeqScan>()

        ind.samples.each {Sample sample ->
            sample.seqScans.each {SeqScan seqScan ->
                seqScans << seqScan
            }
        }


        /*
        def allRuns = [:]
        seqTypes.each {SeqType seqType ->

            Set<String> runs = new HashSet<String>()

            ind.samples.each {Sample sample ->
                sample.seqScans.each {SeqScan seqScan ->

                    if (seqScan.seqType != seqType) return
                    if (seqScan.state == SeqScan.State.OBSOLETE) return

                    seqScan.seqTracks.each {SeqTrack seqTrack->
                        runs << seqTrack.run.name
                    }
                }
            }

            String[] runNames = runs.toArray()
            Arrays.sort(runNames)
            allRuns[seqType] = runNames
        }
        */

        [ind: ind, seqTypes: seqTypes, seqScans: seqScans]
    }

}
