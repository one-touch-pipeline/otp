package de.dkfz.tbi.otp.ngsdata

class MergedAlignmentDataFileService {

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

    static String buildRelativePath(SeqType type, Sample sample) {
        // this method is also used in the ProcessedMergedBamFileService,
        // if this method is changed make sure that the path in the ProcessedMergedBamFileService is still correct
        String sampleType = sample.sampleType.dirName
        String layout = type.libraryLayoutDirName
        return "${sample.individual.getViewByPidPath(type).relativePath}/${sampleType}/${layout}/merged-alignment/"
    }
}
