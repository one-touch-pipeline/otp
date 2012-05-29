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
        String name = createSessionFileObjectAndReturnName(text)
        String url = buildURLString(request, name)
        return url
    }

    Map<Integer,Boolean> createMapOfIgvEnabledScans(List<SeqScan> scans) {
        Map<Integer,Boolean> map = [:]
        for(SeqScan scan in scans) {
            boolean enabled = igvEnabled(scan)
            map.put(scan.id, enabled)
        }
        return map
    }

    boolean igvEnabled(SeqScan scan) {
        List<String> paths = getFileSystemPathForScan(scan)
        for(String path in paths) {
            String baiPath = "${path}.bai"
            File baiFile =new File(baiPath)
            if (baiFile.canRead()) {
                return true
            }
        }
        return false
    }

    private String createSessionFileObjectAndReturnName(String text) {
        String hash = Integer.toHexString(text.hashCode())
        String name = "${hash}.xml"
        IgvSessionFile sessionFile = new IgvSessionFile(name: name, content: text)
        sessionFile.save(flush: true)
        return name
    }

    private String buildURLString(HttpServletRequest request, String name) {
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
                    List<String> paths = getPathForScan(scan)
                    for(String path in paths) {
                        Resource(path: path)
                    }
                }
                String myURL = getMyURL(request)
                Resource(path: "${myURL}igvSessionFile/mutFile/${indId}.mut")
            }
        }
        String text = header + writer.toString()
        return text 
    }

    private List<String> getPathForScan(SeqScan scan) {
        MergingLog merging = MergingLog.findBySeqScan(scan)
        if (merging) {
            return getPathForMergedScan(merging)
        }
        return getPathForSingleFiles(scan)
    }

    private List<String> getFileSystemPathForScan(SeqScan scan) {
        MergingLog merging = MergingLog.findBySeqScan(scan)
        if (merging) {
            return getFileSystemPathForMergedScan(merging)
        }
        return getFileSystemPathForSingleFiles(scan)
    }

    private List<String> getPathForMergedScan(MergingLog merging) {
        MergedAlignmentDataFile dataFile = MergedAlignmentDataFile.findByMergingLog(merging)
        String dataWebServer = dataWebServer(merging)
        String path = "${dataWebServer}/${dataFile.filePath}/${dataFile.fileName}"
        return [path]
    }

    private List<String> getFileSystemPathForMergedScan(merging) {
        MergedAlignmentDataFile dataFile = MergedAlignmentDataFile.findByMergingLog(merging)
        String path = "${dataFile.fileSystem}/${dataFile.filePath}/${dataFile.fileName}"
        return [path]
    }

    private List<String> getPathForSingleFiles(SeqScan seqScan) {
        List<DataFile> files = mergedAlignmentDataFileService.alignmentSequenceFiles(seqScan)
        String dataWebServer = dataWebServer(seqScan)
        List<String> paths = []
        for(DataFile file in files) {
            // TODO: add selection based of alignment type
            String path = lsdfFilesService.getFileViewByPidRelativePath(file)
            String projectPath = file.project.dirName
            String fullPath = "${dataWebServer}/${projectPath}/sequencing/${path}"
            paths << fullPath
        }
        return paths
    }

    private List<String> getFileSystemPathForSingleFiles(SeqScan seqScan) {
        List<DataFile> files = mergedAlignmentDataFileService.alignmentSequenceFiles(seqScan)
        List<String> paths = []
        for(DataFile file in files) {
            String path = lsdfFilesService.getFileViewByPidPath(file)
            paths << path
        }
        return paths
    }

    private String dataWebServer(MergingLog merging) {
        return dataWebServer(merging.seqScan)
    }

    private String dataWebServer(SeqScan scan) {
        Realm realm = scan.sample.individual.project.realm
        return realm.webHost
    }
}
