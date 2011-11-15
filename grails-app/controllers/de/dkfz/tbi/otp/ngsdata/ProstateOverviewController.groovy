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
        Individual[] inds = 
            Individual.findAllByProject(project, [sort: "mockPid"])
        def table = [:]
        def names = []

        inds.each { Individual individual ->

            names << individual.mockFullName

            def indTable = [:]
            individual.samples.each {Sample sample ->

                def sampleTable = [:]
 
                sample.seqScans.each {SeqScan seqScan ->

                     // only current seqScans
                    if (seqScan.state == SeqScan.State.OBSOLETE) return 

                    String key;
                    def scanTable = [:]

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
                    scanTable["qa"] = getStringFromNumber(seqScan.nBasePairs)

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

        String[] types = ["TUMOR", "CONTROL"]
        String[] keys = ["WGP", "WGMP", "RNA", "miRNA"]


        [header: header, table: table, names: names, types: types, keys: keys]    
    }


    /**
     * 
     * 
     */

    private String getStringFromNumber(long number) {

        final long billion = 1000000000
        final long million = 1000000
        final long kilo = 1000

        if (number/billion > 0) return String.format("%.1f G",(number/billion))
        if (number/million > 0) return String.format("%.1f M", (number/million))
        if (number/kilo > 0) return String.format("%.1f k", (number/kilo)) 

        return (number as String)  
    }

    /**
     * 
     * @param n
     * @return
     */

    private String getBullets(int n) {

        String str = ""
        for(int i=0; i<n; i++)
            str += " &curren; "

        return str
    }

}

/*
SeqScanSet {
    
    String type
    String WGP
    String WGMP
    String RNA
}
*/
