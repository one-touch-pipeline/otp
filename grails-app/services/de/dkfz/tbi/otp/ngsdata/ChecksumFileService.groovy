package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.infrastructure.FileService

import java.nio.file.Files
import java.nio.file.Path

import static org.springframework.util.Assert.notNull

class ChecksumFileService {

    LsdfFilesService lsdfFilesService

    String pathToMd5File(DataFile file) {
        String path = lsdfFilesService.getFileFinalPath(file)
        return "${path}.md5sum"
    }

    String md5FileName(DataFile file) {
        return "${file.fileName}.md5sum"
    }

    /**
     * example: BLOOD_SomePid_WHOLE_GENOME_PAIRED_merged.mdup.bam.md5sum
    */
    String md5FileName(String fileName) {
        return "${fileName}.md5sum"
    }

    /**
     * example: BLOOD_SomePid_WHOLE_GENOME_PAIRED_merged.mdup.bam.md5
     */
    String picardMd5FileName(String fileName) {
        return "${fileName}.md5"
    }

    boolean compareMd5(DataFile file) {
        String path = pathToMd5File(file)
        File md5File = new File(path)
        FileService.ensureFileIsReadableAndNotEmpty(md5File.toPath())
        String md5sum
        try {
            List<String> lines = md5File.readLines()
            List<String> tokens = lines.get(0).tokenize()
            md5sum = tokens.get(0)
        } catch (final Exception e) {
            throw new RuntimeException("Failed to parse MD5 file ${path}", e)
        }
        return (md5sum.trim().toLowerCase(Locale.ENGLISH) == file.md5sum)
    }


    /**
     * @param file, the checksum file
     * @return the checksum of the first file, which is included in the checksum file
     */
    String firstMD5ChecksumFromFile(Path file) {
        notNull(file, "the input file for the method firstMD5ChecksumFromFile is null")
        if (!Files.isReadable(file)) {
            throw new RuntimeException("MD5 file \"${file}\" is not readable or does not exist")
        }
        if (Files.size(file) == 0) {
            throw new RuntimeException("MD5 file \"${file}\" is empty")
        }
        String md5sum = file.readLines().get(0).tokenize().get(0)
        if (!(md5sum ==~ /^[0-9a-fA-F]{32}$/)) {
            throw new RuntimeException("The format of the MD5sum of the MD5 file \"${file}\" is wrong: value=${md5sum}")
        }
        return md5sum
    }
}
