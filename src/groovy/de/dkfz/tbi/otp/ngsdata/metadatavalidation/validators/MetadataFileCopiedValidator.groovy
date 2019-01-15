package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import com.jcraft.jsch.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*

import java.nio.file.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

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
