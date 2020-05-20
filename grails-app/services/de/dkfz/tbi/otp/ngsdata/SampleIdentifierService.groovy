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
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.ObjectError

import de.dkfz.tbi.otp.OtpRuntimeException
import de.dkfz.tbi.otp.parser.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.*
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

        static final String getHeaders(Delimiter delimiter = Delimiter.TAB) {
            return values().join(delimiter.delimiter as String)
        }
    }

    @Autowired
    ApplicationContext applicationContext

    SampleIdentifierParser getSampleIdentifierParser(SampleIdentifierParserBeanName sampleIdentifierParserBeanName) {
        return applicationContext.getBean(sampleIdentifierParserBeanName.beanName, SampleIdentifierParser)
    }

    SampleIdentifier parseAndFindOrSaveSampleIdentifier(String sampleIdentifier, Project project) {
        ParsedSampleIdentifier identifier = parseSampleIdentifier(sampleIdentifier, project)
        if (identifier) {
            return findOrSaveSampleIdentifier(identifier)
        }
        return null
    }

    ParsedSampleIdentifier parseSampleIdentifier(String sampleIdentifier, Project project) {
        if (!project || project.sampleIdentifierParserBeanName == SampleIdentifierParserBeanName.NO_PARSER) {
            return null
        }

        SampleIdentifierParser sampleIdentifierParser = getSampleIdentifierParser(project.sampleIdentifierParserBeanName)

        return sampleIdentifierParser.tryParse(sampleIdentifier)
    }

    String parseCellPosition(String sampleIdentifier, Project project) {
        if (!project || project.sampleIdentifierParserBeanName == SampleIdentifierParserBeanName.NO_PARSER) {
            return null
        }

        SampleIdentifierParser sampleIdentifierParser = getSampleIdentifierParser(project.sampleIdentifierParserBeanName)

        return sampleIdentifierParser.tryParseCellPosition(sampleIdentifier)
    }

    List<String> createBulkSamples(String sampleText, Delimiter delimiter, Project project, SampleType.SpecificReferenceGenome specificReferenceGenome) {
        Spreadsheet spreadsheet = new Spreadsheet(sampleText, delimiter)
        List<String> output = []
        ValidationContext context = new ValidationContext(spreadsheet)
        bulkSampleCreationValidatorBean.validate(context)
        Set<Problem> problems = context.problems
        if (problems) {
            return problems*.message
        }

        for (Row row : spreadsheet.dataRows) {
            Closure<String> getCell = { BulkSampleCreationHeader header ->
                return row.getCellByColumnTitle(header.name())?.text?.trim()
            }
            try {
                DefaultParsedSampleIdentifier identifier = new DefaultParsedSampleIdentifier(
                        projectName: getCell(BulkSampleCreationHeader.PROJECT) ?: project.name,
                        pid: getCell(BulkSampleCreationHeader.PID),
                        sampleTypeDbName: getCell(BulkSampleCreationHeader.SAMPLE_TYPE),
                        fullSampleName: getCell(BulkSampleCreationHeader.SAMPLE_IDENTIFIER),
                        useSpecificReferenceGenome: specificReferenceGenome,
                )
                SampleIdentifier sampleIdentifier = findOrSaveSampleIdentifier(identifier)
                checkSampleIdentifier(identifier, sampleIdentifier)
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

    void checkSampleIdentifier(ParsedSampleIdentifier identifier, SampleIdentifier sampleIdentifier) {
        if (sampleIdentifier.individual.pid != identifier.pid) {
            throw new OtpRuntimeException("The sample identifier already exist, but is connected to individual '${sampleIdentifier.individual.pid}' " +
                    "and not '${identifier.pid}'")
        }
        if (sampleIdentifier.project.name != identifier.projectName) {
            throw new OtpRuntimeException("The sample identifier already exist, but is connected to project '${sampleIdentifier.project}' " +
                    "and not '${identifier.projectName}'")
        }
        String sanitizedSampleTypeDbName = getSanitizedSampleTypeDbName(identifier.sampleTypeDbName)
        if (!sampleIdentifier.sampleType.name.equalsIgnoreCase(identifier.sampleTypeDbName) && !sampleIdentifier.sampleType.name.equalsIgnoreCase(sanitizedSampleTypeDbName)) {
            throw new OtpRuntimeException("The sample identifier already exist, but is connected to sample type '${sampleIdentifier.sampleType}' " +
                    "and not '${identifier.sampleTypeDbName}'")
        }
    }

    static String getSanitizedSampleTypeDbName(String sampleTypeDbName) {
        return sampleTypeDbName.replaceAll('_', '-').toLowerCase()
    }

    SampleIdentifier findOrSaveSampleIdentifier(ParsedSampleIdentifier identifier) {
        SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(identifier.fullSampleName))
        if (sampleIdentifier) {
            return sampleIdentifier
        }

        Sample sample = findOrSaveSample(identifier)
        return new SampleIdentifier(
                sample: sample,
                name: identifier.fullSampleName,
        ).save(flush: true)
    }

    Sample findOrSaveSample(ParsedSampleIdentifier identifier) {
        Individual individual = findOrSaveIndividual(identifier)
        SampleType sampleType = findOrSaveSampleType(identifier)

        Sample sample = Sample.findByIndividualAndSampleType(individual, sampleType)
        if (sample) {
            return sample
        }

        return new Sample(
                individual: individual,
                sampleType: sampleType,
        ).save(flush: true)
    }

    SampleType findOrSaveSampleType(ParsedSampleIdentifier identifier) {
        SampleType sampleType = SampleType.findSampleTypeByName(identifier.sampleTypeDbName)
        if (sampleType) {
            return sampleType
        }
        String sanitizedSampleTypeDbName = getSanitizedSampleTypeDbName(identifier.sampleTypeDbName)
        SampleType sanitizedSampleType = SampleType.findSampleTypeByName(sanitizedSampleTypeDbName)
        if (sanitizedSampleType) {
            return sanitizedSampleType
        }
        if (identifier.useSpecificReferenceGenome == null) {
            throw new RuntimeException("SampleType '${identifier.sampleTypeDbName}' does not exist and useSpecificReferenceGenome is not defined")
        }
        return new SampleType(
                name: sanitizedSampleTypeDbName,
                specificReferenceGenome: identifier.useSpecificReferenceGenome,
        ).save(flush: true)
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
        }
        throw new RuntimeException("Project ${identifier.projectName} does not exist.")
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
    String removeExcessWhitespaceFromCharacterDelimitedText(String text, Delimiter delimiter) {
        String columnDelimiter = delimiter.delimiter
        return text
                .trim()
                .replaceAll(/ *${columnDelimiter} */, columnDelimiter)
                .replaceAll(/ +/, " ")
                .replaceAll(/ *\n */, "\n")
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateSampleIdentifierName(SampleIdentifier sampleIdentifier, String name) {
            sampleIdentifier.name = name
            sampleIdentifier.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void deleteSampleIdentifier(SampleIdentifier sampleIdentifier) {
        sampleIdentifier.delete(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    SampleIdentifier createSampleIdentifier(String name, Sample sample) {
        SampleIdentifier sampleIdentifier = new SampleIdentifier(name: name, sample: sample)
        sampleIdentifier.save(flush: true)
        return sampleIdentifier
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    SampleIdentifier getOrCreateSampleIdentifier(String name, Sample sample) {
        SampleIdentifier sampleIdentifier = SampleIdentifier.findByNameAndSample(name, sample)
        if (!sampleIdentifier) {
            sampleIdentifier = new SampleIdentifier(name: name, sample: sample)
            sampleIdentifier.save(flush: true)
        }
        return sampleIdentifier
    }
}
