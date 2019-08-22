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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.validation.ObjectError

import de.dkfz.tbi.otp.parser.*
import de.dkfz.tbi.util.spreadsheet.Row
import de.dkfz.tbi.util.spreadsheet.Spreadsheet
import de.dkfz.tbi.util.spreadsheet.validation.Problem
import de.dkfz.tbi.util.spreadsheet.validation.ValidationContext

import java.text.MessageFormat

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@Transactional
class SampleIdentifierService {

    enum BulkSampleCreationHeader {
        PROJECT,
        PID,
        SAMPLE_TYPE,
        SAMPLE_IDENTIFIER,

        static final String getHeaders(Spreadsheet.Delimiter delimiter = Spreadsheet.Delimiter.TAB) {
            return values().join(delimiter.delimiter as String)
        }
    }

    static final List<String> XENOGRAFT_SAMPLE_TYPE_PREFIXES = ["XENOGRAFT", "PATIENT-DERIVED-CULTURE", "PATIENT_DERIVED_CULTURE", "ORGANOID"]

    @Autowired
    ApplicationContext applicationContext

    SampleIdentifierParser getSampleIdentifierParser(SampleIdentifierParserBeanName sampleIdentifierParserBeanName) {
        return applicationContext.getBean(sampleIdentifierParserBeanName.beanName, SampleIdentifierParser)
    }

    SampleIdentifier parseAndFindOrSaveSampleIdentifier(String sampleIdentifier, Project project) {
        ParsedSampleIdentifier identifier = parseSampleIdentifier(sampleIdentifier, project)
        if (identifier) {
            SampleType.SpecificReferenceGenome specificReferenceGenome = deriveSpecificReferenceGenome(identifier.sampleTypeDbName)
            return findOrSaveSampleIdentifier(identifier, specificReferenceGenome)
        } else {
            return null
        }
    }

    ParsedSampleIdentifier parseSampleIdentifier(String sampleIdentifier, Project project) {
        if (!project || project.sampleIdentifierParserBeanName == SampleIdentifierParserBeanName.NO_PARSER) {
            return null
        }

        SampleIdentifierParser sampleIdentifierParser = getSampleIdentifierParser(project.sampleIdentifierParserBeanName)

        return sampleIdentifierParser.tryParse(sampleIdentifier)
    }

    /**
     * Derives the SpecificReferenceGenome to use based on some information.
     *
     * This function encapsulates the knowledge on how to derive the SpecificReferenceGenome from
     * the given information, which is required for the automatic SampleType creation during the
     * import.
     *
     * @param sampleTypeName
     * @return the derived SpecificReferenceGenome
     */
    SampleType.SpecificReferenceGenome deriveSpecificReferenceGenome(String sampleTypeName) {
        String uppercase = sampleTypeName.toUpperCase(Locale.ENGLISH)
        boolean isXenograft = XENOGRAFT_SAMPLE_TYPE_PREFIXES.any { uppercase.startsWith(it) }

        if (isXenograft) {
            return SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
        } else {
            return SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        }
    }

    String parseCellPosition(String sampleIdentifier, Project project) {
        if (!project || project.sampleIdentifierParserBeanName == SampleIdentifierParserBeanName.NO_PARSER) {
            return null
        }

        SampleIdentifierParser sampleIdentifierParser = getSampleIdentifierParser(project.sampleIdentifierParserBeanName)

        return sampleIdentifierParser.tryParseCellPosition(sampleIdentifier)
    }

    List<String> createBulkSamples(String sampleText, Spreadsheet.Delimiter delimiter, Project project, SampleType.SpecificReferenceGenome specificReferenceGenome) {
        Spreadsheet spreadsheet = new Spreadsheet(sampleText, delimiter)
        List<String> output = []
        ValidationContext context = new ValidationContext(spreadsheet)
        getBulkSampleCreationValidatorBean().validate(context)
        Set<Problem> problems = context.getProblems()
        if (problems) {
            return problems*.message
        }

        for (Row row : spreadsheet.dataRows) {
            Closure<String> getCell = { BulkSampleCreationHeader header ->
                return row.getCellByColumnTitle(header.name())?.text?.trim()
            }
            try {
                DefaultParsedSampleIdentifier identifier = new DefaultParsedSampleIdentifier(
                        projectName     : getCell(BulkSampleCreationHeader.PROJECT) ?: project.name,
                        pid             : getCell(BulkSampleCreationHeader.PID),
                        sampleTypeDbName: getCell(BulkSampleCreationHeader.SAMPLE_TYPE),
                        fullSampleName  : getCell(BulkSampleCreationHeader.SAMPLE_IDENTIFIER),
                )
                findOrSaveSampleIdentifier(identifier, specificReferenceGenome)
            } catch (ValidationException e) {
                e.errors.allErrors.each { ObjectError err ->
                    output << "${MessageFormat.format(err.defaultMessage, err.arguments)}: ${err.code}"
                }
            } catch (RuntimeException e) {
                output << e.message
            }
        }
        return output
    }

    private BulkSampleCreationValidator getBulkSampleCreationValidatorBean() {
        return applicationContext.getBean(BulkSampleCreationValidator)
    }

    private static boolean parsedIdentifierMatchesFoundIdentifier(ParsedSampleIdentifier parsedIdentifier, SampleIdentifier foundIdentifier) {
        return  foundIdentifier.project.name == parsedIdentifier.projectName &&
                foundIdentifier.individual.pid == parsedIdentifier.pid &&
                foundIdentifier.sampleType.name == parsedIdentifier.sampleTypeDbName
    }

    static String getSanitizedSampleTypeDbName(String sampleTypeDbName) {
        return sampleTypeDbName.replaceAll('_', '-')
    }

    DefaultParsedSampleIdentifier sanitizeParsedSampleIdentifier(ParsedSampleIdentifier identifier) {
        return new DefaultParsedSampleIdentifier(
                projectName     : identifier.getProjectName(),
                pid             : identifier.getPid(),
                sampleTypeDbName: getSanitizedSampleTypeDbName(identifier.getSampleTypeDbName()),
                fullSampleName  : identifier.getFullSampleName(),
        )
    }

    SampleIdentifier findOrSaveSampleIdentifier(ParsedSampleIdentifier identifier, SampleType.SpecificReferenceGenome specificReferenceGenome) {
        SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(identifier.fullSampleName))
        if (sampleIdentifier) {
            if (!parsedIdentifierMatchesFoundIdentifier(identifier, sampleIdentifier)) {
                throw new RuntimeException(
                        "A sample identifier ${identifier.fullSampleName} already exists, " +
                        "but belongs to sample ${sampleIdentifier.sample} in project " +
                        "${sampleIdentifier.project.name} which does not match the expected properties"
                )
            }
        } else {
            Sample sample = findOrSaveSample(identifier, specificReferenceGenome)
            sampleIdentifier = new SampleIdentifier(
                    sample: sample,
                    name: identifier.fullSampleName,
            )
            sampleIdentifier.save(flush: true)
        }
        return sampleIdentifier
    }

    Sample findOrSaveSample(ParsedSampleIdentifier identifier, SampleType.SpecificReferenceGenome specificReferenceGenome) {
        Individual individual = findOrSaveIndividual(identifier)
        SampleType sampleType = findOrSaveSampleType(identifier.sampleTypeDbName, specificReferenceGenome)

        Sample sample = Sample.findByIndividualAndSampleType(individual, sampleType)
        if (!sample) {
            sample = new Sample(individual: individual, sampleType: sampleType)
            sample.save(flush: true)
        }
        return sample
    }

    SampleType findOrSaveSampleType(String sampleTypeName, SampleType.SpecificReferenceGenome specificReferenceGenome) {
        SampleType sampleType = atMostOneElement(SampleType.findAllByName(sampleTypeName))
        if (!sampleType) {
            String sanitizedSampleTypeDbName = getSanitizedSampleTypeDbName(sampleTypeName)
            sampleType = atMostOneElement(SampleType.findAllByName(sanitizedSampleTypeDbName))
            if (!sampleType) {
                if (specificReferenceGenome == null) {
                    throw new RuntimeException("SampleType '${sampleTypeName}' does not exist")
                } else {
                    sampleType = new SampleType(
                            name: sanitizedSampleTypeDbName,
                            specificReferenceGenome: specificReferenceGenome,
                    )
                    assert sampleType.save(flush: true)
                }
            }
        }
        return sampleType
    }

    Individual findOrSaveIndividual(ParsedSampleIdentifier identifier) {
        Individual individual = atMostOneElement(Individual.findAllByPid(identifier.pid))
        if (individual) {
            if (individual.project.name != identifier.projectName) {
                throw new RuntimeException(
                        "An individual with PID ${individual.pid} already exists, but belongs " +
                        "to project ${individual.project.name} instead of ${identifier.projectName}"
                )
            }
        } else {
            individual = new Individual(
                    pid: identifier.pid,
                    mockPid: identifier.pid,
                    mockFullName: identifier.pid,
                    project: findProject(identifier),
                    type: Individual.Type.REAL
            )
            assert individual.save(flush: true)
        }
        return individual
    }

    Project findProject(ParsedSampleIdentifier identifier) {
        Project result = atMostOneElement(Project.findAllByName(identifier.projectName))
        if (result) {
            return result
        } else {
            throw new RuntimeException("Project ${identifier.projectName} does not exist.")
        }
    }

    /**
     * Sanitizes a character delimited text.
     *
     * The sanitation includes trimming the entire string, combining consecutive
     * spaces into one and removing spaces around delimiters and newlines.
     *
     * @param text to be sanitized
     * @param delimiter which separates columns
     * @return sanitized text
     */
    String removeExcessWhitespaceFromCharacterDelimitedText(String text, Spreadsheet.Delimiter delimiter) {
        String columnDelimiter = delimiter.delimiter.toString()
        return text
                .trim()
                .replaceAll(/ *${columnDelimiter} */, columnDelimiter)
                .replaceAll(/ +/, " ")
                .replaceAll(/ *\n */, "\n")
    }
}
