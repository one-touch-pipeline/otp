package de.dkfz.tbi.otp.ngsdata

import static org.springframework.util.Assert.notNull

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

    public String md5FileName(String fileName) {
        return "${fileName}.md5sum"
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

    public String firstMD5ChecksumFromFile(String file) {
        return firstMD5ChecksumFromFile(new File(file))
    }

    /**
     * @param file, the checksum file
     * @return the checksum of the first file, which is included in the checksum file
     */
    public String firstMD5ChecksumFromFile(File file) {
        notNull(file, "the input file for the method firstMD5ChecksumFromFile is null")
        if (file.canRead() && file.size() != 0) {
            return file.readLines().get(0).tokenize().get(0)
        } else {
            throw new RuntimeException("Unable to read digest from MD5 file \"${file.path}\"")
        }

    }
}
