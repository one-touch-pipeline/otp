package de.dkfz.tbi.otp.ngsdata

class MergedAlignmentDataFileService {

    def configService
    def fileTypeService

    /**
     * This function returns all alignment files (from project folder)
     * belonging to a given SeqScan. Files are selected based on their FileType.
     * List of fileTypes with alignment is provided by FileTypeService
     *
     * @param scan
     * @return
     */

    List<DataFile> alignmentSequenceFiles(SeqScan scan) {
        List<DataFile> files = []
        List<FileType> types = fileTypeService.alignmentSequenceTypes()
        List<SeqTrack> tracks = MergingAssignment.findAllBySeqScan(scan)*.seqTrack
        for (SeqTrack track in tracks) {
            List<AlignmentLog> alignLogs = AlignmentLog.findAllBySeqTrack(track)
            for (AlignmentLog alignLog in alignLogs) {
                List<DataFile> f = DataFile.findAllByFileTypeInListAndAlignmentLog(types, alignLog)
                for (DataFile file in f) {
                    files << file
                }
            }
        }
        return files
    }

    String getFullPath(MergedAlignmentDataFile dataFile) {
        String basePath = pathToHost(dataFile.mergingLog.seqScan)
        String filePath = dataFile.filePath
        String fileName = dataFile.fileName
        String path = "${basePath}/${filePath}/${fileName}"
        return path
    }

    String pathToHost(SeqScan scan) {
        return configService.getProjectRootPath(scan.sample.project)
    }

    String buildRelativePath(SeqType type, Sample sample) {
        // this method is also used in the ProcessedMergedBamFileService,
        // if this method is changed make sure that the path in the ProcessedMergedBamFileService is still correct
        String sampleType = sample.sampleType.name.toLowerCase()
        String layout = type.libraryLayout.toLowerCase()
        return "${sample.individual.getViewByPidPath(type).relativePath}/${sampleType}/${layout}/merged-alignment/"
    }

    String buildRelativePath(MergingLog mergingLog) {
        return buildRelativePath(mergingLog.seqScan.seqType, mergingLog.seqScan.sample)
    }

    //This method is not used at the moment but is kept because of historical reasons and may be reused later on.
    //It is only called in the Job "CreateSingleBamDataFileJob", which is not used anymore
    @Deprecated
    String buildFileName(MergingLog mergingLog) {
        File dir = fullDirectory(mergingLog)
        int n =  numberOfFiles(dir, ".bam")
        return fileName(mergingLog.seqScan, n)
    }

    private File fullDirectory(MergingLog mergingLog) {
        String basePath = pathToHost(mergingLog.seqScan)
        String path = buildRelativePath(mergingLog)
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

    //This method is not used at the moment but is kept because of historical reasons and may be reused later on.
    @Deprecated
    private String fileName(SeqScan scan, int n) {
        String type = scan.sample.sampleType.name.toLowerCase()
        String pid = scan.sample.individual.pid
        return "v${n}.${type}_${pid}_merged.bam.rmdup.bam"
    }
}
