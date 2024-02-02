/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.SampleIdentifierService.BulkSampleCreationHeader.*

@Component
class BulkSampleCreationValidator extends AbstractValueTuplesValidator<ValidationContext> {

    @Override
    List<String> getRequiredColumnTitles(ValidationContext context) {
        return [PID, SAMPLE_TYPE, SAMPLE_IDENTIFIER]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(ValidationContext context) {
        return [PROJECT]*.name()
    }

    @Override
    void checkMissingOptionalColumn(ValidationContext context, String columnTitle) { }

    @Override
    void validateValueTuples(ValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each {
            String projectName = it.getValue(PROJECT.name())?.trim()
            if (projectName && !ProjectService.findByNameOrNameInMetadataFiles(projectName)) {
                context.addProblem(it.cells, LogLevel.ERROR, "Could not find Project '${projectName}'")
            }
        }
        List<String> foundHeaders = context.spreadsheet.header.cells*.text
        List<String> allowedHeaders = SampleIdentifierService.BulkSampleCreationHeader.values()*.name()
        List<String> unknownHeaders = foundHeaders - allowedHeaders
        unknownHeaders.each {
            context.addProblem([] as Set, LogLevel.ERROR, "The column header '${it}' is unknown")
        }
    }
}
