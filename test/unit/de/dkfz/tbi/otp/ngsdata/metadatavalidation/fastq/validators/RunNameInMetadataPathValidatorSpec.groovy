package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DataFilesWithAbsolutePath
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures.DataFilesOnGpcfMidTerm
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import java.nio.file.Paths

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class RunNameInMetadataPathValidatorSpec extends Specification {

    @Unroll
    def "test validate"(boolean containsMultipleRuns, boolean isDataFilesOnGpcfMidTerm, boolean mdFilenameContainsRunName, boolean error) {
        given:
        String data = containsMultipleRuns ? "run1\nrun2\n" : "run1\nrun1\n"

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.RUN_ID}\n" + data,
                [
                        metadataFile: mdFilenameContainsRunName ? Paths.get("run1"): Paths.get("whatever"),
                        directoryStructure: isDataFilesOnGpcfMidTerm ? new DataFilesOnGpcfMidTerm() : [:],
                ],
        )

        when:
        new RunNameInMetadataPathValidator().validate(context)

        then:
        if (error) {
            Problem problem = exactlyOneElement(context.problems)
            assert problem.level == Level.WARNING
            assert containSame(problem.affectedCells*.cellAddress, ['A2', 'A3'])
            assert problem.message.contains("The path of the metadata file should contain the run name.")
        } else {
            assert context.problems.isEmpty()
        }

        where:
        containsMultipleRuns | isDataFilesOnGpcfMidTerm | mdFilenameContainsRunName || error
        false                | false                    | false                     || true
        false                | false                    | true                      || false
        false                | true                     | false                     || false
        false                | true                     | true                      || false
        true                 | false                    | false                     || false
        true                 | false                    | true                      || false
        true                 | true                     | false                     || false
        true                 | true                     | true                      || false
    }


    @Unroll
    def "validate, when directory structure is DataFilesWithAbsolutePath, then run name does not need to be in path"() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.RUN_ID}\n${runEntry1}\n${runEntry2}",
                [
                        metadataFile: Paths.get("whatever"),
                        directoryStructure: new DataFilesWithAbsolutePath(),
                ],
        )

        when:
        new RunNameInMetadataPathValidator().validate(context)

        then:
        context.problems.isEmpty()

        where:
        runEntry1 | runEntry2
        'run1' | 'run2'
        'run1' | 'run1'
    }


}
