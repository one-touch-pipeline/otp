package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures.DataFilesOnGpcfMidTerm
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators.OnlyOneRunValidatorSpec.Ilse.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class OnlyOneRunValidatorSpec extends Specification {

    @Unroll
    def "test validate"(boolean containsMultipleRuns, boolean isDataFilesOnGpcfMidTerm, Ilse ilse, boolean error) {
        given:
        String header = "${MetaDataColumn.RUN_ID}\n"
        String firstData = ""
        String secondData = ""
        if (ilse != NONE) {
            header = "${MetaDataColumn.ILSE_NO}\t" + header
            if (ilse == EMPTY) {
                firstData = "\t"
                secondData = "\t"
            } else if (ilse == DIFFERENT) {
                firstData = "ilse1\t"
                secondData = "ilse2\t"
            } else if (ilse == SAME) {
                firstData = "ilse1\t"
                secondData = "ilse1\t"
            }
        }
        firstData += "run1\n"
        secondData += containsMultipleRuns ? "run2\n" : "run1\n"

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                header + firstData + secondData,
                [directoryStructure: isDataFilesOnGpcfMidTerm ? new DataFilesOnGpcfMidTerm() : [:]],
        )

        when:
        new OnlyOneRunValidator().validate(context)

        then:
        if (error) {
            Problem problem = exactlyOneElement(context.problems)
            assert problem.level == Level.WARNING
            List<String> affectedCells = ilse == NONE ? ['A2', 'A3'] : ['B2', 'B3']
            assert containSame(problem.affectedCells*.cellAddress, affectedCells)
            assert problem.message.contains("Metadata file contains data from more than one run.")
        } else {
            assert context.problems.isEmpty()
        }

        where:
        containsMultipleRuns | isDataFilesOnGpcfMidTerm | ilse        || error
        false                | false                    | NONE        || false
        false                | false                    | EMPTY       || false
        false                | false                    | DIFFERENT   || false
        false                | false                    | SAME        || false
        false                | true                     | NONE        || false
        false                | true                     | EMPTY       || false
        false                | true                     | DIFFERENT   || false
        false                | true                     | SAME        || false
        true                 | false                    | NONE        || true
        true                 | false                    | EMPTY       || true
        true                 | false                    | DIFFERENT   || true
        true                 | false                    | SAME        || true
        true                 | true                     | NONE        || true
        true                 | true                     | EMPTY       || true
        true                 | true                     | DIFFERENT   || true
        true                 | true                     | SAME        || false
    }

    enum Ilse {    // Ilse column
        NONE,      // does not exist
        EMPTY,     // is empty
        DIFFERENT, // contains different values
        SAME,      // contains only one values
    }
}
