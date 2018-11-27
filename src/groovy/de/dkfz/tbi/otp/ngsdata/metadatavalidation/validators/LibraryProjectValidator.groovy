package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class LibraryProjectValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {//rename

    @Autowired
    SampleIdentifierService sampleIdentifierService


    @Override
    Collection<String> getDescriptions() {
        return ['There is only one library name for each project, where the normalized library name is the same.']
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [CUSTOMER_LIBRARY.name(), PROJECT.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        return true
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        List<ExtractedValues> extractedValuesList = []
        valueTuples.each { ValueTuple valueTuple ->
            String projectName = valueTuple.getValue(PROJECT.name())
            Project project = Project.getByNameOrNameInMetadataFiles(projectName)
            if (!project) {
                return
            }

            String libraryName = valueTuple.getValue(CUSTOMER_LIBRARY.name())
            String normalizedLibraryName = SeqTrack.normalizeLibraryName(libraryName)
            extractedValuesList.add(new ExtractedValues(project: project, libraryName: libraryName, normalizedLibraryName: normalizedLibraryName, cells: valueTuple.cells))
        }
        extractedValuesList.groupBy { it.project }.each { Project currentProject, List<ExtractedValues> rowsById ->
            rowsById.groupBy { it.normalizedLibraryName }.each { String normalizedLibraryName, List<ExtractedValues> rows ->
                Set<Cell> cells = rows*.cells.sum() as Set<Cell>

                if (rows*.libraryName.unique().size() > 1) {
                    context.addProblem(cells, Level.WARNING, "All rows for project '${currentProject.name}' which look similar to '${normalizedLibraryName}' should have the same value in column '${CUSTOMER_LIBRARY}'.", "All rows for one project which have a similar customer library should have the same value in column '${CUSTOMER_LIBRARY}'.")
                }

                List<String> result = SeqTrack.createCriteria().list {
                    eq('normalizedLibraryName', normalizedLibraryName)
                    sample {
                        individual {
                            eq ('project', currentProject)
                        }
                    }
                    projections {
                        distinct('libraryName')
                    }
                }
                if (result) {
                    //This is done, to remove correct rows from cells. Correct rows are rows where result.size is 1 and equal to the library name.
                    if (result.size() == 1) {
                        cells = rows.findAll{it.libraryName != result[0]}*.cells.sum() as Set<Cell>
                    }
                    if (cells) {
                        context.addProblem(cells, Level.WARNING, "In project '${currentProject.name}' the following library names which look similar to '${normalizedLibraryName}' are already registered: '${result.join("', '")}'.", "For at least one project library names which look similar to entries in the metadata file are already registered.")
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
