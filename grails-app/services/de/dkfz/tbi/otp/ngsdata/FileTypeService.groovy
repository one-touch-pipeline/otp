package de.dkfz.tbi.otp.ngsdata

class FileTypeService {

    boolean fastqcReady(DataFile file) {
        if (file.fileName.endsWith(".bz2")) {
            return false
        }
        return true
    }

    boolean isGoodSequenceDataFile(DataFile dataFile) {
        if (dataFile.fileWithdrawn) {
            return false
        }
        if (dataFile.fileType.type != FileType.Type.SEQUENCE) {
            return false
        }
        return true
    }

    List<FileType> alignmentSequenceTypes() {
        List<String> extensions = ["bam", "bwt"]
        return FileType.findAllByTypeAndSubTypeInList(FileType.Type.ALIGNMENT, extensions)
    }

    /**
     * Provides object from file name and known type
     *
     * to make a difference between 'stats' from sequence and alignment
     *
     * @param filename
     * @param type
     * @return FileType
     */
    static FileType getFileType(String filename, FileType.Type type) {
        List<FileType> types = FileType.findAllByType(type, [sort: "id", order: "asc"])
        for (FileType subType in types) {
            if (filename.contains(subType.signature)) {
                return subType
            }
        }
        throw new FileTypeUndefinedException(filename)
    }
}
