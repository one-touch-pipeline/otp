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

        Sample.findAllByIndividual(ind).each {Sample sample ->
            SeqScan.findAllBySample(sample).each {SeqScan seqScan -> seqScans << seqScan }
        }

        List<String> mergedBams =
                mergingService.printAllMergedBamForIndividual(ind, seqTypes)

        int prevId = findId(ind, -1)
        int nextId = findId(ind, 1)

        [ind: ind, seqTypes: seqTypes, seqScans: seqScans, mergedBams: mergedBams,
                    prevId: prevId, nextId: nextId]
    }

    private int findId(Individual ind, int delta) {
        int pid = ind.mockPid as int
        String newMockPid = String.format("%d", (pid+delta))
        return Individual.findByMockPid(newMockPid).id
    }
}
