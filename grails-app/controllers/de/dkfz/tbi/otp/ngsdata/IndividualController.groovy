package de.dkfz.tbi.otp.ngsdata

class IndividualController {

    def mergingService
    def igvSessionFileService

    def scaffold = Individual

    def display = {

        log.debug(params.id)
        Individual ind = Individual.findByMockFullName(params.id)
        redirect(action: show, id: ind.id)
    }

    def show = {
        int id = params.id as int
        flash.ind=id
        Individual ind = Individual.get(id)
        if (!ind) {
            render ("Individual with id=${id} does not exist")
            return
        }
        List<SeqType> seqTypes= getSeqTypes(ind)
        List<SeqScan> seqScans = new ArrayList<SeqScan>()
        Sample.findAllByIndividual(ind).each {Sample sample ->
            SeqScan.findAllBySample(sample).each {SeqScan seqScan -> seqScans << seqScan }
        }
        int prevId = findPrevious(ind)
        int nextId = findNext(ind)

        List<Mutation> muts = Mutation.findAllByIndividual(ind)

        [ind: ind, seqTypes: seqTypes, seqScans: seqScans, prevId: prevId, nextId: nextId, muts: muts]
    }

    private int findPrevious(Individual ind) {
        List<Individual> inds =
             Individual.findAllByIdLessThan(ind.id, [sort: "id", order: "desc", max: 1])
        return (inds.size()>0) ? inds.get(0).id : ind.id
    }

    private int findNext(Individual ind) {
        List<Individual> inds =
            Individual.findAllByIdGreaterThan(ind.id, [sort: "id", order: "asc", max: 1])
        return (inds.size()>0) ? inds.get(0).id : ind.id
    }

    private List<SeqType> getSeqTypes(Individual ind) {
        List<SeqType> allSeqTypes = SeqType.list([sort: "id", order:"asc"])
        List<SeqType> seqTypes = new ArrayList<SeqType>()
        allSeqTypes.each {SeqType type ->
            Sample.findAllByIndividual(ind).each {Sample sample ->
                SeqScan.findAllBySample(sample).each {SeqScan scan -> 
                    if (scan.seqType.equals(type)) {
                        if (!seqTypes.contains(type)) {
                            seqTypes << type 
                        }
                    }
                }
            }
        }
        return seqTypes
    }

    def igvStart = {
        println params
        println flash

        List<SeqScan> scans = new ArrayList<SeqScan>()
        params.each {
            if (it.value == "on") {
                SeqScan scan = SeqScan.get(it.key as int)
                scans << scan
            }
        }
        String url = igvSessionFileService.buildSessionFile(scans, request)
        println "Redirecting: ${url}"
        redirect(url: url)
    }

    def igvDownload = {
        println "Download"
        render "downloging ..."
    }
}
