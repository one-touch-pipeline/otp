package de.dkfz.tbi.otp.ngsdata

class IndividualController {

    def mergingService

    def scaffold = Individual

    def display = {

        log.debug(params.id)
        Individual ind = Individual.findByMockFullName(params.id)
        redirect(action: show, id: ind.id)
    }

    def show = {
        int id = params.id as int
        Individual ind = Individual.get(id)
        if (!ind) {
            render ("Individual with id=${id} does not exist")
            return
        }
        Vector<SeqType> seqTypes= new Vector<SeqType>();

        seqTypes.add(SeqType.findByNameAndLibraryLayout("WHOLE_GENOME", "PAIRED"))
        seqTypes.add(SeqType.findByNameAndLibraryLayout("WHOLE_GENOME", "MATE_PAIR"))
        seqTypes.add(SeqType.findByNameAndLibraryLayout("RNA", "PAIRED"))
        seqTypes.add(SeqType.findByName("MI_RNA"))

        List<SeqScan> seqScans = new ArrayList<SeqScan>()

        ind.samples.each {Sample sample ->
            sample.seqScans.each {SeqScan seqScan -> seqScans << seqScan }
        }

        List<String> mergedBams =
                mergingService.printAllMergedBamForIndividual(ind, seqTypes)

        int prevId = (id > 1)? id-1 : 1
        int nextId = id+1

        [ind: ind, seqTypes: seqTypes, seqScans: seqScans, mergedBams: mergedBams,
                    prevId: prevId, nextId: nextId]
    }
}
