package de.dkfz.tbi.otp.ngsdata

class ChecksumFileService {

    def lsdfFilesService

    public String dirToMd5File(DataFile file) {
        String path = lsdfFilesService.getFileFinalPath(file)
        return path.substring(0, path.lastIndexOf("/") + 1)
    }

    public String pathToMd5File(DataFile file) {
        String path = lsdfFilesService.getFileFinalPath(file)
        return "${path}.md5sum"
    }

    public String md5FileName(DataFile file) {
        return "${file.fileName}.md5sum"
    }

    public boolean md5sumFileExists(DataFile file) {
        String path = pathToMd5File(file)
        return lsdfFilesService.fileExists(path)
    }

    public boolean compareMd5(DataFile file) {
        String path = pathToMd5File(file)
        File md5File = new File(path)
        if (!md5File.canRead()) {
            return false
        }
        List<String> lines = md5File.readLines()
        List<String> tokens = lines.get(0).tokenize()
        String md5sum = tokens.get(0)
        return (md5sum.trim() == file.md5sum.trim())
    }
}
