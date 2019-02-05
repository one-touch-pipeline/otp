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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import de.dkfz.tbi.otp.dataprocessing.SampleIdentifierParserBeanName

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

class SampleIdentifierService {


    static final String XENOGRAFT = "XENOGRAFT"
    static final String CULTURE = "PATIENT-DERIVED-CULTURE"
    static final String ORGANOID = "ORGANOID"


    @Autowired
    ApplicationContext applicationContext

    SampleIdentifierParser getSampleIdentifierParser(SampleIdentifierParserBeanName sampleIdentifierParserBeanName) {
        return applicationContext.getBean(sampleIdentifierParserBeanName.beanName, SampleIdentifierParser)
    }

    SampleIdentifier parseAndFindOrSaveSampleIdentifier(String sampleIdentifier, Project project) {
        ParsedSampleIdentifier identifier = parseSampleIdentifier(sampleIdentifier, project)

        if (identifier) {
            return findOrSaveSampleIdentifier(identifier)
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

    String parseCellPosition(String sampleIdentifier, Project project) {
        if (!project || project.sampleIdentifierParserBeanName == SampleIdentifierParserBeanName.NO_PARSER) {
            return null
        }

        SampleIdentifierParser sampleIdentifierParser = getSampleIdentifierParser(project.sampleIdentifierParserBeanName)

        return sampleIdentifierParser.tryParseCellPosition(sampleIdentifier)
    }

    SampleIdentifier findOrSaveSampleIdentifier(ParsedSampleIdentifier identifier) {
        SampleIdentifier result = atMostOneElement(SampleIdentifier.findAllByName(identifier.fullSampleName))
        if (result) {
            if (result.project.name != identifier.projectName ||
                    result.individual.pid != identifier.pid ||
                    (result.sampleType.name != identifier.sampleTypeDbName && result.sampleType.name != identifier.sampleTypeDbName.replace('_', '-'))) {
                throw new RuntimeException("A sample identifier ${identifier.fullSampleName} already exists, " +
                        "but belongs to sample ${result.sample} which does not match the expected properties")
            }
        } else {
            result = new SampleIdentifier(
                    sample: findOrSaveSample(identifier),
                    name: identifier.fullSampleName,
            )
            assert result.save(failOnError: true, flush: true)
        }
        return result
    }

    Sample findOrSaveSample(ParsedSampleIdentifier identifier) {
        String sampleTypeWithoutUnderscore = identifier.sampleTypeDbName.replace('_', '-')
        return Sample.findOrSaveWhere(
                individual: findOrSaveIndividual(identifier),
                sampleType: SampleType.findWhere(name: identifier.sampleTypeDbName) ?: SampleType.findWhere(
                        name: sampleTypeWithoutUnderscore) ?: createSampleTypeXenograftDepending(sampleTypeWithoutUnderscore),
        )
    }


    SampleType createSampleTypeXenograftDepending(String sampleTypeName) {
        boolean xenograft = sampleTypeName.toUpperCase(Locale.ENGLISH).startsWith(XENOGRAFT) ||
                sampleTypeName.toUpperCase(Locale.ENGLISH).startsWith(CULTURE) ||
                sampleTypeName.toUpperCase(Locale.ENGLISH).startsWith(ORGANOID)
        SampleType.SpecificReferenceGenome specificReferenceGenome =
                xenograft ? SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC : SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        SampleType sampleType = new SampleType(
                name: sampleTypeName,
                specificReferenceGenome: specificReferenceGenome,
        )
        assert sampleType.save(flush: true)
        return sampleType
    }


    Individual findOrSaveIndividual(ParsedSampleIdentifier identifier) {
        Individual result = atMostOneElement(Individual.findAllByPid(identifier.pid))
        if (result) {
            if (result.project.name != identifier.projectName) {
                throw new RuntimeException("An individual with PID ${result.pid} already exists, " +
                        "but belongs to project ${result.project.name} instead of ${identifier.projectName}")

            }
        } else {
            result = new Individual(
                    pid: identifier.pid,
                    mockPid: identifier.pid,
                    mockFullName: identifier.pid,
                    project: findProject(identifier),
                    type: Individual.Type.REAL
            )
            assert result.save(failOnError: true, flush: true)
        }
        return result
    }

    Project findProject(ParsedSampleIdentifier identifier) {
        Project result = atMostOneElement(Project.findAllByName(identifier.projectName))
        if (result) {
            return result
        } else {
            throw new RuntimeException("Project ${identifier.projectName} does not exist.")
        }
    }
}
