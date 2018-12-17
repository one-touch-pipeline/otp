package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import com.jcraft.jsch.JSchException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.MetadataImportService
import de.dkfz.tbi.otp.ngsdata.SeqCenter
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.Level

import java.nio.file.Files
import java.nio.file.Path

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.CENTER_NAME
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.ILSE_NO

@Component
class MetadataFileCopiedValidator implements MetadataValidator {

    @Autowired
    MetadataImportService metadataImportService

    @Autowired
    FileSystemService fileSystemService

    @Autowired
    FileService fileService

    @Autowired
    ConfigService configService

    @Override
    Collection<String> getDescriptions() {
        return ["There is no incorrect metadata file already copied to the seqcenter inbox."]
    }

    @Override
    void validate(MetadataValidationContext context) {
        List<SeqCenter> seqCenters = SeqCenter.findAllByNameInList(context.spreadsheet.dataRows*.getCellByColumnTitle(CENTER_NAME.name())?.text)
        seqCenters.findAll { it?.copyMetadataFile }.unique().each { SeqCenter seqCenter ->
            try  {
                Path source = context.metadataFile
                String ilse = context.spreadsheet.dataRows[0].getCellByColumnTitle(ILSE_NO.name())?.text
                Path targetDirectory = metadataImportService.getIlseFolder(ilse, seqCenter)
                if (!targetDirectory) {
                    return
                }
                Path path = targetDirectory.resolve(source.fileName.toString())
                if (Files.exists(path) && Files.isRegularFile(path) && Files.isReadable(path)) {
                    if (path.bytes != context.content) {
                        context.addProblem([] as Set, Level.ERROR, "There is already a file in the seqcenter inbox but it is different from this metadata file.")
                    }
                }
            } catch (JSchException e)  {
                context.addProblem(Collections.emptySet(), Level.WARNING, "Could not detect if there is an old metadata file already present in the seqcenter inbox.")
            }
        }
    }
}
