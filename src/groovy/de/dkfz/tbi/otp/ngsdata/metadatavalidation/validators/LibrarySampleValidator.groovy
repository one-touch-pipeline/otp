package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Component
class LibrarySampleValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SampleIdentifierService sampleIdentifierService


    @Override
    Collection<String> getDescriptions() {
        return ['There is only one library name for each project, where the normalized library name is the same.']
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [CUSTOMER_LIBRARY.name(), SAMPLE_ID.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        return true
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        List<ExtractedValues> extractedValuesList = []
        valueTuples.each { ValueTuple valueTuple ->
            String sampleId = valueTuple.getValue(SAMPLE_ID.name())
            SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleId))
            Long projectId
            if (sampleIdentifier) {
                projectId = sampleIdentifier.getProject().id
            } else {
                ParsedSampleIdentifier parsedSampleIdentifier = sampleIdentifierService.parseSampleIdentifier(sampleId)
                if (!parsedSampleIdentifier) {
                    return
                }

                projectId = CollectionUtils.atMostOneElement(Project.findAllByName(parsedSampleIdentifier.projectName))?.id
                if (!projectId) {
                    return
                }
            }

            String libraryName = valueTuple.getValue(CUSTOMER_LIBRARY.name())
            String normalizedLibraryName = SeqTrack.normalizeLibraryName(libraryName)
            extractedValuesList.add(new ExtractedValues(projectId: projectId, libraryName: libraryName, normalizedLibraryName: normalizedLibraryName, cells: valueTuple.cells))
        }
        extractedValuesList.groupBy {it.projectId}.each {Long projectId, List<ExtractedValues> rowsById ->
            rowsById.groupBy {it.normalizedLibraryName}.each { String normalizedLibraryName, List<ExtractedValues> rows ->
                Set<Cell> cells = rows*.cells.sum() as Set<Cell>

                if (((rows*.libraryName).findAll().unique()).size() > 1) {
                    context.addProblem(cells, Level.WARNING, "All rows for project '${Project.get(projectId)}' which look similar to '${normalizedLibraryName}' should have the same value in column '${CUSTOMER_LIBRARY}'.")
                }

                List<String> result = SeqTrack.createCriteria().listDistinct {
                    eq('normalizedLibraryName', normalizedLibraryName)
                    sample {
                        individual {
                            project {
                                eq('id', projectId)
                            }
                        }
                    }
                    projections {
                        property('libraryName')
                    }
                }
                if (result.size() != 0) {
                    //This is done, to remove correct rows from cells. Correct rows are rows where result.size is 1 and equal to the library name.
                    if (result.size() == 1) {
                        cells = rows.findAll{it.libraryName != result[0]}*.cells.sum() as Set<Cell>
                    }
                    if (cells.size() != 0) {
                        context.addProblem(cells, Level.WARNING, "In project '${Project.get(projectId)}' the following library names which look similar to '${normalizedLibraryName}' are already registered: '${result.join("', '")}'.")
                    }
                }
            }
        }
    }
}

class ExtractedValues {
    Long projectId
    String libraryName
    String normalizedLibraryName
    Set<Cell> cells
}
