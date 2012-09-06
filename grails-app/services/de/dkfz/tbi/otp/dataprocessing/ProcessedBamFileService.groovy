package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

class ProcessedBamFileService {

    def processedAlignmentFileService

    public String getFilePath(ProcessedBamFile bamFile) {
        String dir = getDirectory(bamFile)
        String filename = getFileName(bamFile)
        return "${dir}/${filename}"
    }

    public String getFilePathNoSuffix(ProcessedBamFile bamFile) {
        String dir = getDirectory(bamFile)
        String filename = getFileNameNoSuffix(bamFile)
        return "${dir}/${filename}"
    }

    public String getDirectory(ProcessedBamFile bamFile) {
        return processedAlignmentFileService.getDirectory(bamFile.alignmentPass)
    }

    public String getFileName(ProcessedBamFile bamFile) {
        String body = getFileNameNoSuffix(bamFile)
        return "${body}.bam"
    }

    public String getFileNameNoSuffix(ProcessedBamFile bamFile) {
        SeqTrack seqTrack = bamFile.alignmentPass.seqTrack
        String sampleType = seqTrack.sample.sampleType.name.toLowerCase()
        String runName = seqTrack.run.name
        String lane = seqTrack.laneId
        String layout = seqTrack.seqType.libraryLayout
        String suffix = ""
        switch (bamFile.type) {
            case AbstractBamFile.BamType.SORTED:
                suffix = ".sorted"
                break
            case AbstractBamFile.BamType.RMDUP:
                suffix = ".sorted.rmdup"
                break
        }
        return "${sampleType}_${runName}_s_${lane}_${layout}${suffix}"
    }

    public ProcessedBamFile createSortedBamFile(AlignmentPass alignmentPass) {
        return createBamFile(alignmentPass, AbstractBamFile.BamType.SORTED)
    }

    public ProcessedBamFile createRmdupBamFile(AlignmentPass alignmentPass) {
        return createBamFile(alignmentPass, AbstractBamFile.BamType.RMDUP)
    }

    private ProcessedBamFile createBamFile(AlignmentPass alignmentPass, AbstractBamFile.BamType type) {
        ProcessedBamFile pbf = new ProcessedBamFile(
            alignmentPass: alignmentPass,
            type: type
        )
        assert(pbf.save(flush: true))
        return pbf
    }

    public ProcessedBamFile findSortedBamFile(AlignmentPass alignmentPass) {
        def type = AbstractBamFile.BamType.SORTED
        return findBamFile(alignmentPass, type)
    }

    public ProcessedBamFile findRmdupBamFile(AlignmentPass alignmentPass) {
        def type = AbstractBamFile.BamType.RMDUP
        return findBamFile(alignmentPass, type)
    }

    private ProcessedBamFile findBamFile(AlignmentPass alignmentPass, AbstractBamFile.BamType type) {
        return ProcessedBamFile.findByAlignmentPassAndType(alignmentPass, type)
    }

    public ProcessedBamFile findBamFile(long alignmentPassId, String type) {
        AlignmentPass alignmentPass = AlignmentPass.get(alignmentPassId)
        return ProcessedBamFile.findByAlignmentPassAndType(alignmentPass, type)
    }

    public boolean updateBamFile(ProcessedBamFile bamFile) {
        File file = new File(getFilePath(bamFile))
        if (!file.canRead()) {
            return false
        }
        bamFile.fileExists = true
        bamFile.fileSize = file.length()
        bamFile.dateFromFileSystem = new Date(file.lastModified())
        assertSave(bamFile)
        return bamFile.fileSize
    }

    public boolean updateBamFileIndex(ProcessedBamFile bamFile) {
        String path = getFilePath(bamFile)
        File file = new File("${path}.bai")
        if (!file.canRead()) {
            return false
        }
        bamFile.hasIndexFile = true
        assertSave(bamFile)
        return true
    }

    private def assertSave(def object) {
        object = object.save(flush: true)
        if (!object) {
            throw new SavingException(object.toString())
        }
        return object
    }
}