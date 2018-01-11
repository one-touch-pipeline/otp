package de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.DirectoryStructure
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

import java.nio.file.Path
import java.nio.file.Paths

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class DataFilesWithAbsolutePathSpec extends Specification {

    void test() {

        given:
        File directory = TestCase.uniqueNonExistentPath
        DirectoryStructure directoryStructure = new DataFilesWithAbsolutePath()
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\n" +
                "${directory.path}/foo.fastq\n" +
                "${directory.path}/foo(bar).fastq\n",
                [metadataFile: Paths.get(directory.path, 'metadata.tsv')]
        )
        Set<Cell> validCells = [context.spreadsheet.dataRows.get(0).cells.get(0)] as Set
        Set<Cell> invalidCells = [context.spreadsheet.dataRows.get(1).cells.get(0)] as Set


        when:
        Path dataFilePath1 = directoryStructure.getDataFilePath(context, new ValueTuple(
                [(MetaDataColumn.FASTQ_FILE.name()): "${directory.path}/foo.fastq"], validCells))

        then:
        context.problems.isEmpty()
        dataFilePath1 == Paths.get(directory.path, 'foo.fastq')
        context.problems.isEmpty()

        when:
        Path dataFilePath2 = directoryStructure.getDataFilePath(context, new ValueTuple(
                [(MetaDataColumn.FASTQ_FILE.name()): "${directory.path}/foo(bar).fastq"], invalidCells))

        then:
        dataFilePath2 == null
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        problem.affectedCells == invalidCells
        problem.message.contains('is not a valid absolute path')
    }
}
