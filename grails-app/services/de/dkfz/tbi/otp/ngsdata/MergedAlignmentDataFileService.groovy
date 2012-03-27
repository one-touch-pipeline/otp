package de.dkfz.tbi.otp.ngsdata

class MergedAlignmentDataFileService {

    String getFullPath(MergedAlignmentDataFile dataFile) {
        String basePath = pathToHost(dataFile.mergingLog.seqScan)
        String filePath = dataFile.filePath
        String fileName = dataFile.fileName
        String path = "${basePath}/${filePath}/${fileName}"
        return path
    }

    String pathToHost(SeqScan scan) {
        String host = scan.sample.individual.project.host
        switch (host) {
            case "DKFZ":
                return "$OTP_ROOT_PATH/"
            case "BioQuant":
                return "$BQ_ROOTPATH/project"
            default: 
                throw new Exception()
        }
    }

    String buildPath(MergingLog mergingLog) {
        SeqScan scan = mergingLog.seqScan
        String path = ""
        String projPath = scan.sample.individual.project.dirName
        String typePath = scan.seqType.dirName
        String pid = scan.sample.individual.pid
        String sampleType = scan.sample.type.toString().toLowerCase()
        String layout = scan.seqType.libraryLayout.toLowerCase()
        path = "${projPath}/sequencing/${typePath}/view-by-pid/${pid}/${sampleType}/${layout}/merged-alignment/"
        return path
    }

    String buildFileName(MergingLog mergingLog) {
        File dir = fullDirectory(mergingLog)
        int n =  numberOfFiles(dir, ".bam")
        return fileName(mergingLog.seqScan, n)
    }

    private File fullDirectory(MergingLog mergingLog) {
        String basePath = pathToHost(mergingLog.seqScan)
        String path = buildPath(mergingLog)
        String fullPath = "${basePath}/${path}"
        File dir = new File(fullPath)
    }

    private int numberOfFiles(File dir, String ext) {
        int n = 0
        if (!dir.canRead() || !dir.isDirectory()) {
            return 0
        }
        String[] names = dir.list()
        for(String name in names) {
            if (name.endsWith(".bam")) {
                n++
            }
        }
        return n
    }

    private String fileName(SeqScan scan, int n) {
        String type = scan.sample.type.toString().toLowerCase()
        String pid = scan.sample.individual.pid
        return "v${n}.${type}_${pid}_merged.bam.rmdup.bam"
    }
}
