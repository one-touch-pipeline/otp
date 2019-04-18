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

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.DirectoryStructure
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple

import java.nio.file.Path
import java.util.regex.Matcher

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.FASTQ_FILE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.RUN_ID

@Component
class DataFilesOnGpcfMidTerm implements DirectoryStructure {

    @Override
    String getDescription() {
        return 'data files on GPCF ILSe MidTerm'
    }

    @Override
    List<String> getRequiredColumnTitles() {
        return [
                FASTQ_FILE,
                RUN_ID,
        ]*.name()
    }

    @Override
    Path getDataFilePath(MetadataValidationContext context, ValueTuple valueTuple) {
        String fileName = valueTuple.getValue(FASTQ_FILE.name())
        String runId = valueTuple.getValue(RUN_ID.name())
        Matcher matcher = fileName =~ /^(.*)_R[12]\.fastq\.gz$/
        if (!OtpPath.isValidPathComponent(fileName) || !OtpPath.isValidPathComponent(runId) || !matcher || !OtpPath.isValidPathComponent(matcher.group(1))) {
            context.addProblem(valueTuple.cells, Level.ERROR, "Cannot construct a valid GPCF midterm storage path from run name '${runId}' and filename '${fileName}'.", "Cannot construct a valid GPCF midterm storage path for all rows.")
            return null
        } else {
            String dir = matcher.group(1)
            return context.metadataFile.resolveSibling("${runId}/${dir}/fastq/${fileName}")
        }
    }
}
