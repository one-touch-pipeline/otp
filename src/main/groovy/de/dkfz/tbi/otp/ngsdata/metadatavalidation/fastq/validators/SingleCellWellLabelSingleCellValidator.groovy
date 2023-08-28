/*
 * Copyright 2011-2020 The OTP authors
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
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Slf4j
@Component
class SingleCellWellLabelSingleCellValidator extends AbstractValueTuplesValidator<AbstractMetadataValidationContext> implements MetadataValidator {

    static final String WARNING_MESSAGE = "The submission contains single cell data without a well label " +
            "(provided via column ${SINGLE_CELL_WELL_LABEL} or by project parser)"

    @Autowired
    SampleIdentifierService sampleIdentifierService

    @CompileDynamic
    @Override
    Collection<String> getDescriptions() {
        return [
                "For single cell data a ${SINGLE_CELL_WELL_LABEL} should be provided (via column ${SINGLE_CELL_WELL_LABEL} or by project parser)"
        ]
    }

    @Override
    List<String> getRequiredColumnTitles(AbstractMetadataValidationContext context) {
        return [BASE_MATERIAL]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(AbstractMetadataValidationContext context) {
        return [SINGLE_CELL_WELL_LABEL, PROJECT, SAMPLE_NAME]*.name()
    }

    @Override
    void checkMissingOptionalColumn(AbstractMetadataValidationContext context, String columnTitle) {
    }

    @Override
    void validateValueTuples(AbstractMetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        if (!context.spreadsheet.getColumn(BASE_MATERIAL.name())) {
            return // no single cell seq types
        }

        Collection<ValueTuple> singleCellWithoutWellLabel = valueTuples.findAll {
            return SeqTypeService.isSingleCell(it.getValue(BASE_MATERIAL.name())) && !(it.getValue(SINGLE_CELL_WELL_LABEL.name()) || valueByParserProvided(it))
        }

        if (singleCellWithoutWellLabel) {
            context.addProblem(singleCellWithoutWellLabel*.cells.flatten() as Set, LogLevel.WARNING, WARNING_MESSAGE)
        }
    }

    private boolean valueByParserProvided(ValueTuple valueTuple) {
        String sampleName = valueTuple.getValue(SAMPLE_NAME.name())
        String projectName = valueTuple.getValue(PROJECT.name())

        return sampleName && projectName && sampleIdentifierService.parseSingleCellWellLabel(sampleName, ProjectService.findByNameOrNameInMetadataFiles(projectName))
    }
}
