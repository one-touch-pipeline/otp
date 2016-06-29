package de.dkfz.tbi.otp.ngsdata.metadatavalidation

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*

import java.nio.charset.*
import java.nio.file.*
import java.security.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.HelperUtils.*

class MetadataValidationContext extends ValidationContext {

    static final Charset CHARSET = Charset.forName('UTF-8')
    static long MAX_METADATA_FILE_SIZE_IN_MIB = 1

    final File metadataFile
    final String metadataFileMd5sum
    final DirectoryStructure directoryStructure

    private MetadataValidationContext(File metadataFile, String metadataFileMd5sum, Spreadsheet spreadsheet, Problems problems, DirectoryStructure directoryStructure) {
        super(spreadsheet, problems)
        this.metadataFile = metadataFile
        this.metadataFileMd5sum = metadataFileMd5sum
        this.directoryStructure = directoryStructure
    }

    static MetadataValidationContext createFromFile(File metadataFile, DirectoryStructure directoryStructure) {
        Problems problems = new Problems()
        String metadataFileMd5sum = null
        Spreadsheet spreadsheet = null
        if (!OtpPath.isValidAbsolutePath(metadataFile.path)) {
            problems.addProblem(Collections.emptySet(), Level.ERROR, "'${metadataFile}' is not a valid absolute path.")
        } else if (!metadataFile.name.endsWith('.tsv')) {
            problems.addProblem(Collections.emptySet(), Level.ERROR, "The file name of '${metadataFile}' does not end with '.tsv'.")
        } else if (!metadataFile.isFile()) {
            if (!metadataFile.exists()) {
                problems.addProblem(Collections.emptySet(), Level.ERROR, "${pathForMessage(metadataFile)} does not exist or cannot be accessed by OTP.")
            } else {
                problems.addProblem(Collections.emptySet(), Level.ERROR, "${pathForMessage(metadataFile)} is not a file.")
            }
        } else if (!metadataFile.canRead()) {
            problems.addProblem(Collections.emptySet(), Level.ERROR, "${pathForMessage(metadataFile)} is not readable.")
        } else if (metadataFile.length() == 0L) {
            problems.addProblem(Collections.emptySet(), Level.ERROR, "${pathForMessage(metadataFile)} is empty.")
        } else if (metadataFile.length() > MAX_METADATA_FILE_SIZE_IN_MIB * 1024L * 1024L) {
            problems.addProblem(Collections.emptySet(), Level.ERROR, "${pathForMessage(metadataFile)} is larger than ${MAX_METADATA_FILE_SIZE_IN_MIB} MiB.")
        } else {
            try {
                byte[] bytes = metadataFile.bytes
                metadataFileMd5sum = byteArrayToHexString(MessageDigest.getInstance('MD5').digest(bytes))
                String document = new String(bytes, CHARSET)
                if (document.getBytes(CHARSET) != bytes) {
                    problems.addProblem(Collections.emptySet(), Level.WARNING, "The content of ${pathForMessage(metadataFile)} is not properly encoded with ${CHARSET.name()}. Characters might be corrupted.")
                }
                if (document.contains('"')) {
                    problems.addProblem(Collections.emptySet(), Level.WARNING, "The content of ${pathForMessage(metadataFile)} contains one or more quotation marks. OTP might not parse the file as expected.")
                }
                spreadsheet = new FilteredSpreadsheet(document.replaceFirst(/[\t\r\n]+$/, ''), { Row row ->
                    !row.getCellByColumnTitle(FASTQ_FILE.name())?.text?.startsWith('Undetermined') ||
                    !row.getCellByColumnTitle(SAMPLE_ID.name())?.text?.startsWith('Undetermined')
                })
                if (spreadsheet.dataRows.size() < 1) {
                    spreadsheet = null
                    problems.addProblem(Collections.emptySet(), Level.ERROR, "${pathForMessage(metadataFile)} contains less than two lines.")
                }
            } catch (Exception e) {
                problems.addProblem(Collections.emptySet(), Level.ERROR, e.message)
            }
        }
        return new MetadataValidationContext(metadataFile, metadataFileMd5sum, spreadsheet, problems, directoryStructure)
    }

    static String pathForMessage(File file) {
        return pathForMessage(file.toPath())
    }

    static String pathForMessage(Path path) {
        Path canonicalPath = canonicalPath(path)
        if (canonicalPath == path) {
            return "'${path}'"
        } else {
            return "'${canonicalPath}' (linked from '${path}')"
        }
    }

    /**
     * Replacement for {@link File#getCanonicalPath()}, which does not work when the target does not exist
     */
    static Path canonicalPath(Path path) {
        if (Files.isSymbolicLink(path)) {
            return canonicalPath(Files.readSymbolicLink(path))
        } else {
            return path
        }
    }
}
