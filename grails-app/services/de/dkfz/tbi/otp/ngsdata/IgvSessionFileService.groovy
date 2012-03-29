package de.dkfz.tbi.otp.ngsdata

import groovy.xml.MarkupBuilder

class IgvSessionFileService {

    def configService

    private final String header = '<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n'

    //private final String myMut = "https://otp.local/otpdevel/igvSessionFile/mutFile/"

    String buildSessionFile(List<SeqScan> scans) {
        String text = buildContent(scans)
        String hash = Integer.toHexString(text.hashCode())
        String name = "${hash}.xml"
        IgvSessionFile sessionFile = new IgvSessionFile(name: name, content: text)
        sessionFile.save(flush: true)
        String igvBase = configService.igvPath()
        String myURL = configService.otpWebServer()
        String url = "${igvBase}${myURL}igvSessionFile/file/${name}"
        return url
    }

    private String buildContent(List<SeqScan> scans) {
        StringWriter writer = new StringWriter()
        MarkupBuilder xml = new MarkupBuilder(writer)
        int indId = scans.get(0).sample.individual.id

        xml.Session(genome:"hg19", locus:"All", version:"4") {
            Resources {
                scans.each {SeqScan scan ->
                    String path = getPathForScan(scan)
                    Resource(path: path)
                }
                String myURL = configService.otpWebServer()
                Resource(path: "${myURL}igvSessionFile/mutFile/${indId}.mut")
            }
        }
        String text = header + writer.toString()
        return text 
    }

    private String getPathForScan(SeqScan scan) {
        MergingLog merging = MergingLog.findBySeqScan(scan)
        if (!merging) {
            return "noMerging"
        }
        MergedAlignmentDataFile dataFile = MergedAlignmentDataFile.findByMergingLog(merging)
        String dataWebServer = configService.dataWebServer()
        String path = "${dataWebServer}/${dataFile.filePath}/${dataFile.fileName}"
        return path
        /*
        String path = ""
        String projPath = scan.sample.individual.project.dirName
        String typePath = scan.seqType.dirName
        String pid = scan.sample.individual.pid
        String sampleType = scan.sample.type.toString().toLowerCase()
        String layout = scan.seqType.libraryLayout.toLowerCase()
        DataFile bamFile = MergedAlignmentDataFile.findByMergingLog(merging)
        String fileName = bamFile.fileName
        path = "${webServer}/${projPath}/sequencing/${typePath}/view-by-pid/${pid}/${sampleType}/${layout}/merged-alignment/${fileName}"
        return path
        */
    }
}
