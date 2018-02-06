package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

import static de.dkfz.tbi.TestCase.assertContainSame

class ProjectRunNameFileNameValidatorIntegrationSpec extends Specification {

    static final String PROJECT = 'project'
    static final String RUN_ID = "runName"
    static final String SAMPLE_ID = "sampleIdentifierName"
    static final String DATAFILE = "DataFileFileName.gz"
    static final String DATAFILE_NEW = "DataFileFileNameNew.gz"

    static DataFile dataFile

    static final String VALID_METADATA =
            "${MetaDataColumn.FASTQ_FILE}\t${MetaDataColumn.RUN_ID}\t${MetaDataColumn.SAMPLE_ID}\n" +
                    "${DATAFILE}\t${RUN_ID}\t${SAMPLE_ID}\t\n"


    void setup() {
        Run run = DomainFactory.createRun(["name": RUN_ID])
        Project project = DomainFactory.createProject(["name": PROJECT])
        Individual individual = DomainFactory.createIndividual(["project": project])
        Sample sample = DomainFactory.createSample(["individual": individual])
        SeqTrack seq = DomainFactory.createSeqTrack(["run": run, "sample": sample])
        dataFile = DomainFactory.createDataFile(["fileName": DATAFILE, "seqTrack": seq])
    }


    void 'validate, when file name does not exist for specified run and project (using parseSampleIdentifier)'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\t${MetaDataColumn.RUN_ID}\t${MetaDataColumn.SAMPLE_ID}\n" +
                        "${DATAFILE_NEW}\t${RUN_ID}\t${SAMPLE_ID}\t\n"
        )

        when:
        ProjectRunNameFileNameValidator validator = new ProjectRunNameFileNameValidator()
        validator.sampleIdentifierService = Mock(SampleIdentifierService) {
            1 * parseSampleIdentifier(_) >> {
                return new DefaultParsedSampleIdentifier(dataFile.project.name, dataFile.individual.pid, dataFile.sampleType.name, SAMPLE_ID)
            }
        }

        validator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when file name does not exist for specified run and project (not using parseSampleIdentifier)'() {

        given:
        DomainFactory.createSampleIdentifier(["name": SAMPLE_ID, "sample": dataFile.seqTrack.sample]).name


        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.FASTQ_FILE}\t${MetaDataColumn.RUN_ID}\t${MetaDataColumn.SAMPLE_ID}\n" +
                        "${DATAFILE_NEW}\t${RUN_ID}\t${SAMPLE_ID}\t\n"
        )

        when:
        ProjectRunNameFileNameValidator validator = new ProjectRunNameFileNameValidator()
        validator.sampleIdentifierService = Mock(SampleIdentifierService) {
            0 * parseSampleIdentifier(_) >> {
                return null
            }
        }

        validator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when file name already exists for specified run and project (using parseSampleIdentifier)'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA
        )

        when:
        ProjectRunNameFileNameValidator validator = new ProjectRunNameFileNameValidator()
        validator.sampleIdentifierService = Mock(SampleIdentifierService) {
            1 * parseSampleIdentifier(_) >> {
                return new DefaultParsedSampleIdentifier(dataFile.project.name, dataFile.individual.pid, dataFile.sampleType.name, SAMPLE_ID)
            }
        }

        validator.validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set,
                        Level.ERROR, "A file with name '${DATAFILE}' already exists for run '${RUN_ID}' and project '${PROJECT}'","At least one project, run and file combination already exists in OTP")
        ]
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when file name already exists for specified run and project (not using parseSampleIdentifier)'() {

        given:
        DomainFactory.createSampleIdentifier(["name": SAMPLE_ID, "sample": dataFile.seqTrack.sample]).name

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA
        )

        when:
        ProjectRunNameFileNameValidator validator = new ProjectRunNameFileNameValidator()
        validator.sampleIdentifierService = Mock(SampleIdentifierService) {
            0 * parseSampleIdentifier(_) >> {
                return null
            }
        }

        validator.validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set,
                        Level.ERROR, "A file with name '${DATAFILE}' already exists for run '${RUN_ID}' and project '${PROJECT}'","At least one project, run and file combination already exists in OTP")
        ]
        assertContainSame(context.problems, expectedProblems)
    }


}
