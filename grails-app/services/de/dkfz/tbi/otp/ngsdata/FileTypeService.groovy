package de.dkfz.tbi.otp.ngsdata

class FileTypeService {

    boolean isRawDataFile(DataFile dataFile) {
        switch(dataFile.fileType.type) {
            case "SEQUENCE" :
            case "ALIGNMENT" :
                return true
        }
        return false
    }

    boolean isSequenceDataFile(DataFile dataFile) {
        if (dataFile.fileType.type == Type.SEQUENCE) {
            return true
        }
        return false
    }

    FileType getFileType(String filename) {
        // try to provide and object from file name
        FileType tt = null
        FileType.list().each { FileType fileType ->
            if (filename.contains(fileType.signature)) {
                tt = fileType
            }
        }
        if (!tt) {
            tt = FileType.findByType(FileType.Type.UNKNOWN)
        }
        return tt
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
    FileType getFileType(String filename, FileType.Type type) {
        FileType tt
        FileType.list().each { FileType fileType ->
            if (fileType.type != type) {
                return
            }
            if (filename.contains(fileType.signature)) {
                tt = fileType
            }
        }
        if (!tt) {
            tt = FileType.findByType(FileType.Type.UNKNOWN)
        }
        return tt
    }
}
