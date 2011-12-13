package de.dkfz.tbi.otp.ngsdata

class ProstateOverviewController {

    def index() {
        redirect(action: overview)
    }

    def overview = {
        def header = [
            "sample",
            "whole genome paired",
            "whole genome mate-pair",
            "RNA",
            "miRNA"
        ]
        Project project = Project.findByName("PROJECT_NAME")
        List <Individual> individuals = Individual.findAllByProject(project, [sort: "mockPid"])
        Map table = [:]
        List <String> names = []

        individuals.each { Individual individual ->
            names << individual.mockFullName
            Map indTable = [:]
            Sample.findAllByIndividual(individual).each { Sample sample ->
                Map sampleTable = [:]
                SeqScan.findAllBySample(sample).each { SeqScan seqScan ->
                    // only current seqScans
                    if (seqScan.state == SeqScan.State.OBSOLETE) {
                        return
                    }
                    String key
                    Map scanTable = [:]

                    if (seqScan.seqType.name == "WHOLE_GENOME" &&
                    seqScan.seqType.libraryLayout == "PAIRED") {
                        key = "WGP"
                    }

                    if (seqScan.seqType.name == "WHOLE_GENOME" &&
                    seqScan.seqType.libraryLayout == "MATE_PAIR") {
                        key = "WGMP"
                    }

                    if (seqScan.seqType.name == "RNA") {
                        key = "RNA"
                    }

                    if (seqScan.seqType.name == "MI_RNA") {
                        key = "miRNA"
                    }

                    scanTable["id"] = seqScan.id
                    scanTable["center"] = seqScan.seqCenters.toLowerCase()
                    scanTable["lanes"] = getBullets(seqScan.nLanes)
                    scanTable["qa"] = Tools.getStringFromNumber(seqScan.nBasePairs)
                    scanTable["merged"] = (MergingLog.countBySeqScan(seqScan) > 0)

                    if (key == "WGP") {
                        if (seqScan.nBasePairs < 110e9) scanTable["qav"] = "QA1"
                        if (seqScan.nBasePairs < 80e9) scanTable["qav"] = "QA2"
                    }
                    sampleTable[key] = scanTable
                }
                indTable[sample.type.toString()] = sampleTable
            }
            table[individual.mockFullName] = indTable
        }

        List <String> types = ["TUMOR", "CONTROL"]
        List <String> keys = [
            "WGP",
            "WGMP",
            "RNA",
            "miRNA"
        ]

        [header: header, table: table, names: names, types: types, keys: keys]
    }

    /**
     *
     * @param n
     * @return
     */
    private String getBullets(int n) {

        String str = ""
        if (n > 3) return "${n} &curren;"
        for(int i=0; i<n; i++)
            str += " &curren; "

        return str
    }
}
