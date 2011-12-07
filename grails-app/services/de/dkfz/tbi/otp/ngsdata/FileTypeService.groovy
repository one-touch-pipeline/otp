package de.dkfz.tbi.otp.ngsdata

class FileTypeService {

    FileType getFileType(String filename) {
        // try to provide and object from file name
        List<FileType> fileTypeList = FileType.findAll()
        FileType tt = null
        fileTypeList.each { FileType fileType ->
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
     * to make a difference between stats from sequence and alignment
     * 
     * @param filename
     * @param type
     * @return FileType
     */
    FileType getFileType(String filename, FileType.Type type) {
        List <FileType> fileTypeList = FileType.findAll()
        FileType tt
        fileTypeList.each { FileType fileType ->
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
