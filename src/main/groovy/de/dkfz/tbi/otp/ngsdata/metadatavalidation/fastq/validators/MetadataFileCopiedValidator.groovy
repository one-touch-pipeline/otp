/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

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
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel

import java.nio.file.Files
import java.nio.file.Path

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
        return ["There was no metadata file copied to the seqcenter inbox with incorrect content."]
    }

    @Override
    void validate(MetadataValidationContext context) {
        List<SeqCenter> seqCenters = MetadataImportService.getSeqCenters(context)
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
                        context.addProblem([] as Set, LogLevel.ERROR, "There is already a file in the seqcenter inbox but it is different from this metadata file.")
                    }
                }
            } catch (JSchException e)  {
                context.addProblem(Collections.emptySet(), LogLevel.WARNING, "Could not detect if there is an old metadata file already present in the seqcenter inbox.")
            }
        }
    }
}
