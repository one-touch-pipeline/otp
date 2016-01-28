package de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.Specification

class DataFilesInSameDirectorySpec extends Specification {

    void test() {

        given:
        File directory = TestCase.uniqueNonExistentPath
        DirectoryStructure directoryStructure = new DataFilesInSameDirectory()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\n" +
                "foo.fastq\n" +
                "foo(bar).fastq\n",
                [metadataFile: new File(directory, 'metadata.tsv')]
        )
        Set<Cell> validCells = [context.spreadsheet.dataRows.get(0).cells.get(0)] as Set
        Set<Cell> invalidCells = [context.spreadsheet.dataRows.get(1).cells.get(0)] as Set


        when:
        File dataFilePath1 = directoryStructure.getDataFilePath(context, new ValueTuple(
                [(MetaDataColumn.FASTQ_FILE.name()): 'foo.fastq'], validCells))

        then:
        dataFilePath1 == new File(directory, 'foo.fastq')
        context.problems.isEmpty()

        when:
        File dataFilePath2 = directoryStructure.getDataFilePath(context, new ValueTuple(
                [(MetaDataColumn.FASTQ_FILE.name()): 'foo(bar).fastq'], invalidCells))

        then:
        dataFilePath2 == null
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        problem.affectedCells == invalidCells
        problem.message.contains('is not a valid file name')
    }
}
