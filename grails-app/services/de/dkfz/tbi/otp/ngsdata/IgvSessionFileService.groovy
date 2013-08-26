package de.dkfz.tbi.otp.ngsdata

import groovy.xml.MarkupBuilder

class IgvSessionFileService {

    def configService
    def mergedAlignmentDataFileService
    def lsdfFilesService

    private final String header = '<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n'

    String buildSessionFile(List<SeqScan> scans) {
        String mutFileName = createMutationFile(scans)
        String text = buildContent(scans, mutFileName)
        String name = storeSessionFile(text)
        String url = buildURLString(name)
        return url
    }

    Map<Integer,Boolean> createMapOfIgvEnabledScans(List<SeqScan> scans) {
        Map<Integer,Boolean> map = [:]
        for (SeqScan scan in scans) {
            boolean enabled = isIgvEnabled(scan)
            map.put(scan.id, enabled)
        }
        return map
    }

    boolean isIgvEnabled(SeqScan scan) {
        List<String> paths = getFileSystemPathForScan(scan)
        for (String path in paths) {
            String baiPath = "${path}.bai"
            File baiFile = new File(baiPath)
            if (baiFile.canRead()) {
                return true
            }
        }
        return false
    }

    private String createMutationFile(List<SeqScan> scans) {
        Individual ind = scans.get(0).sample.individual
        String text = mutationFileText(ind)
        String name = text.encodeAsMD5() + ".mut"
        String path = configService.igvSessionFileDirectory()
        assertPathExists(path)
        File file = new File("${path}/${name}")
        file.setText(text)
        return name
    }

    private String mutationFileText(Individual ind) {
        List<Mutation> muts = Mutation.findAllByIndividual(ind)
        String text = "chr\tstart\tend\tsample\ttype\n"
        for(Mutation mut in muts) {
            text += mut.chromosome + "\t"
            text += mut.startPosition + "\t"
            text += mut.endPosition + "\t"
            text += mut.individual.mockPid + "\t"
            text += mut.type + "\n"
        }
        return text
    }

    private void assertPathExists(String path) {
        File file = new File(path)
        if (file.isDirectory()) {
            return
        }
        file.mkdirs()
    }

    private String storeSessionFile(String text) {
        String name = text.encodeAsMD5() + ".xml"
        String path = configService.igvSessionFileDirectory()
        assertPathExists(path)
        File file = new File("${path}/${name}")
        file.setText(text)
        return name
    }

    /**
     * Builds URL to IGV including web start address and path to a session file
     *
     * @param name name of the session file
     * @return URL
     */
    private String buildURLString(String name) {
        String igvBase = configService.igvPath()
        String sessionFileURL = "${configService.igvSessionFileServer()}${name}"
        return "${igvBase}${sessionFileURL}"
    }

    private String buildContent(List<SeqScan> scans, String mutFileName) {
        StringWriter writer = new StringWriter()
        MarkupBuilder xml = new MarkupBuilder(writer)

        xml.Session(genome:"hg19", locus:"All", version:"4") {
            Resources {
                scans.each {SeqScan scan ->
                    List<String> paths = getPathForScan(scan)
                    for (String path in paths) {
                        Resource(path: path)
                    }
                }
                String mutFileURL = "${configService.igvSessionFileServer()}${mutFileName}"
                Resource(path: mutFileURL)
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
        for (DataFile file in files) {
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
        for (DataFile file in files) {
            String path = lsdfFilesService.getFileViewByPidPath(file)
            paths << path
        }
        return paths
    }

    private String dataWebServer(MergingLog merging) {
        return dataWebServer(merging.seqScan)
    }

    private String dataWebServer(SeqScan scan) {
        Realm realm = configService.getRealmDataManagement(scan.sample.individual.project)
        return realm.webHost
    }
}
