package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures

import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.DirectoryStructure
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.*

import java.nio.file.Path
import java.nio.file.Paths

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.FASTQ_FILE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.RUN_ID
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class DataFilesOnGpcfMidTermSpec extends Specification {

    void test(String fileName, boolean valid) {
        given:
        File directory = TestCase.uniqueNonExistentPath
        DirectoryStructure directoryStructure = new DataFilesOnGpcfMidTerm()

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${FASTQ_FILE}\t${RUN_ID}\n" +
                        "${fileName}\trun_name\n",
                [metadataFile: Paths.get(directory.path, 'metadata.tsv')]
        )
        Set<Cell> invalidCells = context.spreadsheet.dataRows.get(0).cells as Set


        when:
        Path dataFilePath = directoryStructure.getDataFilePath(context, new ValueTuple(
                [(FASTQ_FILE.name()): fileName, (RUN_ID.name()): "run_name"], invalidCells)
        )

        then:
        if (valid) {
            assert dataFilePath == Paths.get(directory.path, "run_name/asdf/fastq/asdf_R1.fastq.gz")
            assert context.problems.isEmpty()
        } else {
            assert dataFilePath == null
            Problem problem = exactlyOneElement(context.problems)
            assert problem.level == Level.ERROR
            assert problem.affectedCells == invalidCells
            assert problem.message.contains('Cannot construct a valid GPCF midterm storage path')
        }

        where:
        fileName            | valid
        'asdf_R1.fastq.gz'  | true
        'asdf_R1.fastq'     | false
        '**asdf**.fastq.gz' | false
        '.._R1.fastq.gz'    | false
    }
}
