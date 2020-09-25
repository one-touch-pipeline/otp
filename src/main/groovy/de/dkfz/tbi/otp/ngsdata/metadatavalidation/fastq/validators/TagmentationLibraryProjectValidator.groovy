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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.TAGMENTATION_LIBRARY
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.PROJECT

@Component
class TagmentationLibraryProjectValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SampleIdentifierService sampleIdentifierService


    @Override
    Collection<String> getDescriptions() {
        return ['The tagmentation library names which match the same normalized tagmentation library should be' +
                        ' the same within the metadata file and the same as the one already registered in OTP.']
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return []
    }

    @Override
    List<String> getOptionalColumnTitles(MetadataValidationContext context) {
        return [TAGMENTATION_LIBRARY, PROJECT]*.name()
    }

    @Override
    void checkMissingRequiredColumn(MetadataValidationContext context, String columnTitle) { }

    @Override
    void checkMissingOptionalColumn(MetadataValidationContext context, String columnTitle) { }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        List<ExtractedValues> extractedValuesList = []
        valueTuples.each { ValueTuple valueTuple ->
            String projectName = valueTuple.getValue(PROJECT.name())
            Project project = Project.getByNameOrNameInMetadataFiles(projectName)
            if (!project) {
                return
            }

            String libraryName = valueTuple.getValue(TAGMENTATION_LIBRARY.name())
            String normalizedLibraryName = SeqTrack.normalizeLibraryName(libraryName)
            extractedValuesList.add(new ExtractedValues(project: project, libraryName: libraryName, normalizedLibraryName: normalizedLibraryName, cells: valueTuple.cells))
        }
        extractedValuesList.groupBy { it.project }.each { Project currentProject, List<ExtractedValues> rowsById ->
            rowsById.groupBy {
                it.normalizedLibraryName
            }.each { String normalizedLibraryName, List<ExtractedValues> rows ->
                Set<Cell> cells = rows*.cells.sum() as Set<Cell>

                if (rows*.libraryName.unique().size() > 1) {
                    context.addProblem(cells, Level.WARNING, "All rows for project '${currentProject.name}' which look similar to '${normalizedLibraryName}' should have the same value in column '${TAGMENTATION_LIBRARY}'.", "All rows for one project which have a similar tagmentation library should have the same value in column '${TAGMENTATION_LIBRARY}'.")
                }

                List<String> result = SeqTrack.createCriteria().list {
                    eq('normalizedLibraryName', normalizedLibraryName)
                    sample {
                        individual {
                            eq('project', currentProject)
                        }
                    }
                    projections {
                        distinct('libraryName')
                    }
                }
                if (result) {
                    //This is done, to remove correct rows from cells. Correct rows are rows where result.size is 1 and equal to the library name.
                    if (result.size() == 1) {
                        cells = rows.findAll { it.libraryName != result[0] }*.cells.sum() as Set<Cell>
                    }
                    if (cells) {
                        context.addProblem(cells, Level.WARNING, "In project '${currentProject.name}' the following tagmentation library names which look similar to '${normalizedLibraryName}' are already registered: '${result.join("', '")}'.", "For at least one project tagmentation library names which look similar to entries in the metadata file are already registered.")
                    }
                }
            }
        }
    }
}

class ExtractedValues {
    Project project
    String libraryName
    String normalizedLibraryName
    Set<Cell> cells
}
