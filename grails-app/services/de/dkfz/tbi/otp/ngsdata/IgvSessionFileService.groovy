package de.dkfz.tbi.otp.ngsdata

import groovy.xml.MarkupBuilder
import javax.servlet.http.HttpServletRequest

class IgvSessionFileService {

    def configService
    def mergedAlignmentDataFileService
    def lsdfFilesService

    private final String header = '<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n'

    String buildSessionFile(List<SeqScan> scans, HttpServletRequest request) {
        String text = buildContent(scans, request)
        String hash = Integer.toHexString(text.hashCode())
        String name = "${hash}.xml"
        IgvSessionFile sessionFile = new IgvSessionFile(name: name, content: text)
        sessionFile.save(flush: true)
        String igvBase = configService.igvPath()
        String myURL = getMyURL(request)
        String url = "${igvBase}${myURL}igvSessionFile/file/${name}"
        return url
    }

    private String getMyURL(HttpServletRequest request) {
        String url = request.getRequestURL()
        url = url.substring(0, url.indexOf("grails"))
        return url
    }

    private String buildContent(List<SeqScan> scans, HttpServletRequest request) {
        StringWriter writer = new StringWriter()
        MarkupBuilder xml = new MarkupBuilder(writer)
        int indId = scans.get(0).sample.individual.id

        xml.Session(genome:"hg19", locus:"All", version:"4") {
            Resources {
                scans.each {SeqScan scan ->
                    String path = getPathForScan(scan)
                    Resource(path: path)
                }
                String myURL = getMyURL(request)
                Resource(path: "${myURL}igvSessionFile/mutFile/${indId}.mut")
            }
        }
        String text = header + writer.toString()
        return text 
    }

    private String getPathForScan(SeqScan scan) {
        MergingLog merging = MergingLog.findBySeqScan(scan)
        if (merging) {
            return getPathForMergedScan(merging)
        }
        if (scan.nLanes == 1) {
            return getPathForSingleLane(scan)
        }
        // TODO: selection based on platform or "better lane"
        return ""
    }

    private String getPathForMergedScan(MergingLog merging) {
        MergedAlignmentDataFile dataFile = MergedAlignmentDataFile.findByMergingLog(merging)
        String dataWebServer = configService.dataWebServer()
        String path = "${dataWebServer}/${dataFile.filePath}/${dataFile.fileName}"
        return path
    }

    private String getPathForSingleLane(SeqScan seqScan) {
        List<DataFile> files = mergedAlignmentDataFileService.alignmentSequenceFiles(seqScan)
        String dataWebServer = configService.dataWebServer()
        for(DataFile file in files) {
            // TODO: add selection based of alignment type
            String path = lsdfFilesService.getFileViewByPidRelativePath(file)
            return "${dataWebServer}/${path}"
        }
    }
}
