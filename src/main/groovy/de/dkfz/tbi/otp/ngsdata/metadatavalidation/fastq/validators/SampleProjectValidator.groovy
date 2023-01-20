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

import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.parser.ParsedSampleIdentifier
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.PROJECT
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SAMPLE_NAME
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@Component
class SampleProjectValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SampleIdentifierService sampleIdentifierService

    @CompileDynamic
    @Override
    Collection<String> getDescriptions() {
        return ["The value in the column '${PROJECT}' should be consistent with the parsed „project“ value from the sample name."]
    }
    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return [SAMPLE_NAME, PROJECT]*.name()
    }

    @Override
    void checkMissingRequiredColumn(MetadataValidationContext context, String columnTitle) {
        if (columnTitle == PROJECT.name()) {
            addWarningForMissingOptionalColumn(context, columnTitle)
        }
    }

    @CompileDynamic
    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each {
            String sampleName = it.getValue(SAMPLE_NAME.name())
            String projectName = it.getValue(PROJECT.name())
            Project project = ProjectService.findByNameOrNameInMetadataFiles(projectName)

            SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleName))
            if (sampleIdentifier) {
                if (![sampleIdentifier.project.name, sampleIdentifier.project.nameInMetadataFiles].contains(projectName)) {
                    context.addProblem(it.cells, LogLevel.WARNING, "Sample name '${sampleName}' is already registered in OTP with project '${sampleIdentifier.project.name}', not with project '${projectName}'. If you ignore this warning, OTP will keep the assignment of the sample name to project '${sampleIdentifier.project.name}' and ignore the value '${project?.name ?: projectName}' in the '${PROJECT}' column.", "At least one sample name is already registered in OTP but with another project.")
                }
            } else if (project) {
                if (project.sampleIdentifierParserBeanName != SampleIdentifierParserBeanName.NO_PARSER) {
                    ParsedSampleIdentifier parsedIdentifier = sampleIdentifierService.parseSampleIdentifier(sampleName, project)
                    if (!parsedIdentifier) {
                        context.addProblem(it.cells, LogLevel.WARNING, "Sample name '${sampleName}' can not be parsed with the sampleIdentifierParser '${project.sampleIdentifierParserBeanName.displayName}' given by project '${project}'.", "At least one sample name looks like it does not belong to the project in the '${PROJECT}' column.")
                    }
                }
            }
        }
    }
}
