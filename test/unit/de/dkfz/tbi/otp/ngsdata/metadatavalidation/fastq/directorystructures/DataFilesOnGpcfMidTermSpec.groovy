package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

import java.nio.file.Path
import java.nio.file.Paths

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*

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
