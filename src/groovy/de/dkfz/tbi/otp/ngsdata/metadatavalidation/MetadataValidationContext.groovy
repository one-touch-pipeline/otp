package de.dkfz.tbi.otp.ngsdata.metadatavalidation

import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.logging.Level
import javax.xml.bind.DatatypeConverter

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.util.spreadsheet.Spreadsheet
import de.dkfz.tbi.util.spreadsheet.validation.*

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
            problems.addProblem(Collections.emptySet(), Level.SEVERE, "'${metadataFile}' is not a valid absolute path.")
        } else if (!metadataFile.exists()) {
            problems.addProblem(Collections.emptySet(), Level.SEVERE, "'${metadataFile}' could not be found by OTP.")
        } else if (!metadataFile.isFile()) {
            problems.addProblem(Collections.emptySet(), Level.SEVERE, "'${metadataFile}' is not a file.")
        } else if (!metadataFile.name.endsWith('.tsv')) {
            problems.addProblem(Collections.emptySet(), Level.SEVERE, "The file name of '${metadataFile}' does not end with '.tsv'.")
        } else if (!metadataFile.canRead()) {
            problems.addProblem(Collections.emptySet(), Level.SEVERE, "'${metadataFile}' is not readable.")
        } else if (metadataFile.length() == 0L) {
            problems.addProblem(Collections.emptySet(), Level.SEVERE, "'${metadataFile}' is empty.")
        } else if (metadataFile.length() > MAX_METADATA_FILE_SIZE_IN_MIB * 1024L * 1024L) {
            problems.addProblem(Collections.emptySet(), Level.SEVERE, "'${metadataFile}' is larger than ${MAX_METADATA_FILE_SIZE_IN_MIB} MiB.")
        } else {
            try {
                byte[] bytes = metadataFile.bytes
                metadataFileMd5sum = DatatypeConverter.printHexBinary(MessageDigest.getInstance('MD5').digest(bytes))
                String document = new String(bytes, CHARSET)
                if (document.getBytes(CHARSET) != bytes) {
                    problems.addProblem(Collections.emptySet(), Level.WARNING, "The content of '${metadataFile}' is not properly encoded with ${CHARSET.name()}. Characters might be corrupted.")
                }
                if (document.contains('"')) {
                    problems.addProblem(Collections.emptySet(), Level.WARNING, "The content of '${metadataFile}' contains one or more quotation marks. OTP might not parse the file as expected.")
                }
                spreadsheet = new Spreadsheet(document)
            } catch (Exception e) {
                problems.addProblem(Collections.emptySet(), Level.SEVERE, e.message)
            }
        }
        return new MetadataValidationContext(metadataFile, metadataFileMd5sum, spreadsheet, problems, directoryStructure)
    }
}
