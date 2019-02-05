package de.dkfz.tbi.otp.ngsdata.metadatavalidation

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*

import java.nio.charset.*
import java.nio.file.*
import java.security.*

import static de.dkfz.tbi.otp.utils.HelperUtils.*

abstract class AbstractMetadataValidationContext extends ValidationContext {

    static final Charset CHARSET = Charset.forName('UTF-8')
    static long MAX_METADATA_FILE_SIZE_IN_MIB = 1
    static long MAX_ADDITIONAL_FILE_SIZE_IN_GIB = 1

    final Path metadataFile
    final String metadataFileMd5sum
    final byte[] content

    protected AbstractMetadataValidationContext(Path metadataFile, String metadataFileMd5sum, Spreadsheet spreadsheet, Problems problems, byte[] content) {
        super(spreadsheet, problems)
        this.metadataFile = metadataFile
        this.content = content
        this.metadataFileMd5sum = metadataFileMd5sum
    }

    static Map readAndCheckFile(Path metadataFile, Closure<Boolean> dataRowFilter = { true }) {
        Problems problems = new Problems()
        String metadataFileMd5sum = null
        Spreadsheet spreadsheet = null
        byte[] bytes = null
        if (!OtpPath.isValidAbsolutePath(metadataFile.toString())) {
            problems.addProblem(Collections.emptySet(), Level.ERROR, "'${metadataFile}' is not a valid absolute path.")
        } else if (!metadataFile.toString().endsWith('.tsv')) {
            problems.addProblem(Collections.emptySet(), Level.ERROR, "The file name of '${metadataFile}' does not end with '.tsv'.")
        } else if (!Files.isRegularFile(metadataFile)) {
            if (!Files.exists(metadataFile)) {
                problems.addProblem(Collections.emptySet(), Level.ERROR, "${pathForMessage(metadataFile)} does not exist or cannot be accessed by OTP.")
            } else {
                problems.addProblem(Collections.emptySet(), Level.ERROR, "${pathForMessage(metadataFile)} is not a file.")
            }
        } else if (!Files.isReadable(metadataFile)) {
            problems.addProblem(Collections.emptySet(), Level.ERROR, "${pathForMessage(metadataFile)} is not readable.")
        } else if (Files.size(metadataFile) == 0L) {
            problems.addProblem(Collections.emptySet(), Level.ERROR, "${pathForMessage(metadataFile)} is empty.")
        } else if (Files.size(metadataFile) > MAX_METADATA_FILE_SIZE_IN_MIB * 1024L * 1024L) {
            problems.addProblem(Collections.emptySet(), Level.ERROR, "${pathForMessage(metadataFile)} is larger than ${MAX_METADATA_FILE_SIZE_IN_MIB} MiB.")
        } else {
            try {
                bytes = Files.readAllBytes(metadataFile)
                metadataFileMd5sum = byteArrayToHexString(MessageDigest.getInstance('MD5').digest(bytes))
                String document = new String(bytes, CHARSET)
                if (document.getBytes(CHARSET) != bytes) {
                    problems.addProblem(Collections.emptySet(), Level.WARNING, "The content of ${pathForMessage(metadataFile)} is not properly encoded with ${CHARSET.name()}. Characters might be corrupted.")
                }
                spreadsheet = new FilteredSpreadsheet(document.replaceFirst(/[\t\r\n]+$/, ''), dataRowFilter)
                if (spreadsheet.dataRows.size() < 1) {
                    spreadsheet = null
                    problems.addProblem(Collections.emptySet(), Level.ERROR, "${pathForMessage(metadataFile)} contains less than two lines.")
                }
            } catch (Exception e) {
                problems.addProblem(Collections.emptySet(), Level.ERROR, e.message)
            }
        }
        return [
                metadataFileMd5sum: metadataFileMd5sum,
                spreadsheet       : spreadsheet,
                bytes             : bytes,
                problems          : problems,
        ]
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
