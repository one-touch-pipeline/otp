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
package de.dkfz.tbi.otp.parser

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.springframework.context.ApplicationContext
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.OtpRuntimeException
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.util.spreadsheet.Delimiter

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class SampleIdentifierServiceSpec extends Specification implements DataTest, ServiceUnitTest<SampleIdentifierService>, DomainFactoryCore {

    private static final Delimiter DEFAULT_DELIMITER = Delimiter.COMMA
    private static final SampleType.SpecificReferenceGenome DEFAULT_SPECIFIC_REF_GEN = SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
    private static final String HEADER = SampleIdentifierService.BulkSampleCreationHeader.getHeaders(DEFAULT_DELIMITER)
    private static final String SAMPLE_IDENTIFIER_NAME = "New name"

    @Override
    Class[] getDomainClassesToMock() {
        [
                Individual,
                ProcessingOption,
                Project,
                Realm,
                Sample,
                SampleIdentifier,
                SampleType,
        ]
    }

    private static DefaultParsedSampleIdentifier makeParsedSampleIdentifier(Map<String, String> properties = [:]) {
        Closure<String> get = { String key ->
            return properties[key] ?: HelperUtils.uniqueString.toLowerCase()
        }
        return new DefaultParsedSampleIdentifier(get('projectName'), get('pid'), get('sampleTypeDbName'), get('fullSampleName'),
                properties.containsKey('useSpecificReferenceGenome') ? properties.useSpecificReferenceGenome : DEFAULT_SPECIFIC_REF_GEN)
    }

    void "test findProject, when project exists, should return it"() {
        given:
        Project project = createProject()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(projectName: project.name)

        when:
        Project result = service.findProject(identifier)

        then:
        result.name == identifier.projectName
    }

    void "test findProject, project doesn't exist, should fail"() {
        given:
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier()

        when:
        service.findProject(identifier)

        then:
        RuntimeException e = thrown()
        e.message == "Project ${identifier.projectName} does not exist."
    }

    private void callAndAssertFindOrSaveIndividual(ParsedSampleIdentifier identifier) {
        Individual result = service.findOrSaveIndividual(identifier)

        assert result.pid == identifier.pid
        assert result.project.name == identifier.projectName
        assert containSame(Individual.list(), [result])
    }

    void "test findOrSaveIndividual, individual exists in correct project, should return it"() {
        given:
        Individual individual = createIndividual()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(projectName: individual.project.name, pid: individual.pid)

        expect:
        callAndAssertFindOrSaveIndividual(identifier)
    }

    void "test findOrSaveIndividual, individual doesn't exist, should create it"() {
        given:
        Project project = createProject()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(projectName: project.name)

        expect:
        callAndAssertFindOrSaveIndividual(identifier)
    }

    void "test findOrSaveIndividual, individual exists in other project, should fail"() {
        given:
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier()
        createIndividual(
                pid: identifier.pid,
        )

        when:
        service.findOrSaveIndividual(identifier)

        then:
        RuntimeException e = thrown()
        e.message.contains("already exists, but belongs to project")
    }

    private void callAndAssertFindOrSaveSample(ParsedSampleIdentifier identifier) {
        Sample result = service.findOrSaveSample(identifier)

        assert result.sampleType.name == identifier.sampleTypeDbName
        assert result.individual.pid == identifier.pid
        assert result.project.name == identifier.projectName
        assert containSame(Sample.list(), [result])
    }

    void "test findOrSaveSample, when sample exists, should return it"() {
        given:
        Sample sample = createSample()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(
                projectName: sample.project.name,
                pid: sample.individual.pid,
                sampleTypeDbName: sample.sampleType.name,
                useSpecificReferenceGenome: null,
        )

        expect:
        callAndAssertFindOrSaveSample(identifier)
    }

    void "test findOrSaveSample, when sample does not exist, should create it"() {
        given:
        Project project = createProject()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier([
                projectName               : project.name,
                useSpecificReferenceGenome: DEFAULT_SPECIFIC_REF_GEN,
        ])

        expect:
        callAndAssertFindOrSaveSample(identifier)
    }

    void "test findOrSaveSample, when sample does not exist but sample type does, should create it"() {
        given:
        Project project = createProject()
        SampleType sampleType = createSampleType()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(
                projectName: project.name,
                pid: HelperUtils.uniqueString,
                sampleTypeDbName: sampleType.name,
                useSpecificReferenceGenome: null,
        )

        expect:
        callAndAssertFindOrSaveSample(identifier)
    }

    void "test findOrSaveSample, when requested sample type with underscore exist, then use it and create sample with it"() {
        given:
        Project project = createProject()
        SampleType sampleType = createSampleType()
        sampleType.name = 'underscore_test'
        sampleType.save(flush: true)

        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(
                projectName: project.name,
                pid: HelperUtils.uniqueString,
                sampleTypeDbName: sampleType.name,
                useSpecificReferenceGenome: null,
        )

        expect:
        callAndAssertFindOrSaveSample(identifier)
    }

    void "test findOrSaveSample, when requested sample type with underscore does not exist, should create it with minus instead of underscore"() {
        given:
        Project project = createProject()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(
                projectName: project.name,
                pid: HelperUtils.uniqueString,
                sampleTypeDbName: 'underscore_test',
        )

        when:
        Sample result = service.findOrSaveSample(identifier)

        then:
        result.sampleType.name == 'underscore-test'
        result.individual.pid == identifier.pid
        result.project.name == identifier.projectName
        containSame(Sample.list(), [result])
    }

    void "test findOrSaveSample, when requested sample type with different case exists, should use it"() {
        given:
        Project project = createProject()
        SampleType sampleType = createSampleType()
        sampleType.name = 'blood'
        sampleType.save(flush: true)
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(
                projectName: project.name,
                pid: HelperUtils.uniqueString,
                sampleTypeDbName: 'blood',
        )

        when:
        Sample result = service.findOrSaveSample(identifier)

        then:
        result.sampleType == sampleType
        result.individual.pid == identifier.pid
        result.project.name == identifier.projectName
        containSame(Sample.list(), [result])
    }

    @Unroll
    void "findOrSaveSampleType, non existing sample type name is created with given specificReferenceGenome (#specificReferenceGenome)"() {
        given:
        String sampleTypeName = "to-be-created"
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier([
                sampleTypeDbName          : sampleTypeName,
                useSpecificReferenceGenome: specificReferenceGenome,
        ])

        when:
        SampleType result = service.findOrSaveSampleType(identifier)

        then:
        result.name == sampleTypeName
        result.specificReferenceGenome == specificReferenceGenome

        where:
        specificReferenceGenome << SampleType.SpecificReferenceGenome.values()
    }

    void "findOrSaveSampleType, when non existing sample type name containing underscore, then create sample type using minus instead of underscore"() {
        given:
        String sampleTypeName = "to_be_created"
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier([
                sampleTypeDbName          : sampleTypeName,
                useSpecificReferenceGenome: SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT,
        ])

        when:
        SampleType result = service.findOrSaveSampleType(identifier)

        then:
        result.name == "to-be-created"
    }

    void "findOrSaveSampleType, when existing sample type name containing underscore, then return this sample type"() {
        given:
        String sampleTypeName = "to_be_created"
        SampleType sampleType = createSampleType()
        sampleType.name = sampleTypeName
        sampleType.save(flush: true)

        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier([
                sampleTypeDbName          : sampleTypeName,
                useSpecificReferenceGenome: SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT,
        ])

        when:
        SampleType result = service.findOrSaveSampleType(identifier)

        then:
        result.name == sampleTypeName
    }

    void "findOrSaveSampleType, when sample type name containing underscore not exist, but with minus exist, then return this sample type"() {
        given:
        String sampleTypeName1 = "to_be_created"
        String sampleTypeName2 = "to-be-created"
        SampleType sampleType = createSampleType()
        sampleType.name = sampleTypeName2
        sampleType.save(flush: true)

        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier([
                sampleTypeDbName          : sampleTypeName1,
                useSpecificReferenceGenome: SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT,
        ])

        when:
        SampleType result = service.findOrSaveSampleType(identifier)

        then:
        result.name == sampleTypeName2
    }

    void "findOrSaveSampleType, non existing sample type name causes exception without specificReferenceGenome"() {
        given:
        String sampleTypeName = "does-not-exist-yet"
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier([
                sampleTypeDbName          : sampleTypeName,
                useSpecificReferenceGenome: null,
        ])

        when:
        service.findOrSaveSampleType(identifier)

        then:
        RuntimeException e = thrown()
        e.message == "SampleType \'${sampleTypeName}\' does not exist and useSpecificReferenceGenome is not defined"
    }

    @Unroll
    void "findOrSaveSampleType, sampleType is found and returned regardless of given specificReferenceGenome (#specificReferenceGenome)"() {
        given:
        String sampleTypeName = "existing"
        SampleType sampleType = createSampleType([
                name                   : sampleTypeName,
                specificReferenceGenome: SampleType.SpecificReferenceGenome.UNKNOWN,
        ])
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier([
                sampleTypeDbName          : sampleTypeName,
                useSpecificReferenceGenome: specificReferenceGenome,
        ])

        when:
        SampleType result = service.findOrSaveSampleType(identifier)

        then:
        result == sampleType
        result.specificReferenceGenome == SampleType.SpecificReferenceGenome.UNKNOWN

        where:
        specificReferenceGenome << [
                SampleType.SpecificReferenceGenome.values().toList(),
                null,
        ].flatten()
    }

    private SampleIdentifier getSampleIdentifier(String projectName, String pid, String sampleTypeName) {
        return createSampleIdentifier(
                sample: createSample(
                        individual: createIndividual(
                                pid: pid,
                                project: createProject(name: projectName),
                        ),
                        sampleType: createSampleType(name: sampleTypeName),
                )
        )
    }

    private void callAndAssertFindOrSaveSampleIdentifier(ParsedSampleIdentifier identifier) {
        SampleIdentifier result = service.findOrSaveSampleIdentifier(identifier)

        assert result.name == identifier.fullSampleName
        assert result.sampleType.name == identifier.sampleTypeDbName
        assert result.individual.pid == identifier.pid
        assert result.project.name == identifier.projectName
        assert containSame(SampleIdentifier.list(), [result])
    }

    void "test findOrSaveSampleIdentifier, when sample identifier exist, then should return it"() {
        given:
        SampleIdentifier sampleIdentifier = createSampleIdentifier()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier([
                projectName     : sampleIdentifier.project.name,
                pid             : sampleIdentifier.individual.pid,
                sampleTypeDbName: sampleIdentifier.sampleType.name,
                fullSampleName  : sampleIdentifier.name,
        ])

        expect:
        callAndAssertFindOrSaveSampleIdentifier(identifier)
    }

    void "test findOrSaveSampleIdentifier, when sample identifier does not exist, should create it"() {
        given:
        Project project = createProject()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier([
                projectName               : project.name,
                useSpecificReferenceGenome: DEFAULT_SPECIFIC_REF_GEN,
        ])

        expect:
        callAndAssertFindOrSaveSampleIdentifier(identifier)
    }

    void "test parseAndFindOrSaveSampleIdentifier, not parsable, should return null"() {
        when:
        SampleIdentifier result = service.parseAndFindOrSaveSampleIdentifier(HelperUtils.uniqueString, null)

        then:
        result == null
        SampleIdentifier.count() == 0
        Sample.count() == 0
        SampleType.count() == 0
        Individual.count() == 0
        Project.count() == 0
    }

    void "parseAndFindOrSaveSampleIdentifier properly integrates sanitation of sample type name"() {
        given:
        SampleIdentifierService sampleIdentifierService = Spy(SampleIdentifierService) {
            1 * parseSampleIdentifier(_, _) >> { String identifier, Project project ->
                return makeParsedSampleIdentifier(sampleTypeDbName: identifier, projectName: project.name)
            }
        }

        when:
        SampleIdentifier result = sampleIdentifierService.parseAndFindOrSaveSampleIdentifier(nameIn, createProject())

        then:
        result.sample.sampleType.name == nameOut

        where:
        nameIn            || nameOut
        "typical"         || "typical"
        "with_underscore" || "with-underscore"
    }

    private SampleIdentifierService createSampleIdentifierService() {
        SampleIdentifierService sampleIdentifierService = new SampleIdentifierService()
        sampleIdentifierService.applicationContext = Mock(ApplicationContext) {
            getBean(_) >> {
                return new BulkSampleCreationValidator()
            }
        }
        return sampleIdentifierService
    }

    void "getSanitizedSampleTypeDbName, converts underscores to dashes"() {
        given:
        SampleIdentifierService sampleIdentifierService = createSampleIdentifierService()

        when:
        String result = sampleIdentifierService.getSanitizedSampleTypeDbName(input)

        then:
        result == expected

        where:
        input        || expected
        "sin_gle"    || "sin-gle"
        "mul_ti_ple" || "mul-ti-ple"
    }

    @Unroll
    void "test createBulkSamples with correct values (useName=#useName)"() {
        given:
        SampleIdentifierService sampleIdentifierService = createSampleIdentifierService()
        List<String> output
        Project project = createProject()
        String context = "${HEADER}\n${useName ? project.name : ''},test,test,test"

        when:
        output = sampleIdentifierService.createBulkSamples(context, DEFAULT_DELIMITER, project, DEFAULT_SPECIFIC_REF_GEN)

        then:
        output == []

        where:
        useName << [true, false]
    }

    void "test createBulkSamples when headers are missing"() {
        given:
        SampleIdentifierService sampleIdentifierService = createSampleIdentifierService()
        List<String> output
        Project project = createProject()

        when:
        output = sampleIdentifierService.createBulkSamples('invalidHeader\ntest,test,test', DEFAULT_DELIMITER, project, DEFAULT_SPECIFIC_REF_GEN)

        then:
        containSame(output, [
                "Required column '${SampleIdentifierService.BulkSampleCreationHeader.PID.name()}' is missing.",
                "Required column '${SampleIdentifierService.BulkSampleCreationHeader.SAMPLE_TYPE.name()}' is missing.",
                "Required column '${SampleIdentifierService.BulkSampleCreationHeader.SAMPLE_IDENTIFIER.name()}' is missing.",
        ])
    }

    void "test createBulkSamples when given wrong projectName"() {
        given:
        SampleIdentifierService sampleIdentifierService = createSampleIdentifierService()
        List<String> output
        Project project = createProject()

        when:
        output = sampleIdentifierService.createBulkSamples("${HEADER}\ninvalidProject,test,test,test", DEFAULT_DELIMITER, project, DEFAULT_SPECIFIC_REF_GEN)

        then:
        containSame(output, ["Could not find Project 'invalidProject'"])
    }

    void "test createBulkSamples when given unknown HEADER"() {
        given:
        SampleIdentifierService sampleIdentifierService = createSampleIdentifierService()
        List<String> output
        Project project = createProject()

        when:
        output = sampleIdentifierService.createBulkSamples("${HEADER},UNKNOWN_HEADER\n${project.name},test,test,test,test", DEFAULT_DELIMITER, project, DEFAULT_SPECIFIC_REF_GEN)

        then:
        containSame(output, ["The column header 'UNKNOWN_HEADER' is unknown"])
    }

    @Unroll
    void "removeExcessWhitespaceFromCharacterDelimitedText, common cases with all delimiters (delimiter=#delimiter)"() {
        given:
        SampleIdentifierService sampleIdentifierService = createSampleIdentifierService()

        String d = delimiter.delimiter

        String input = " front${d}back ${d}mid dle${d} m u l t i ${d}  consec  utive\n new line front${d} new line middle ${d}new line back \na${d}b${d}c"
        String expected = "front${d}back${d}mid dle${d}m u l t i${d}consec utive\nnew line front${d}new line middle${d}new line back\na${d}b${d}c"

        when:
        String result = sampleIdentifierService.removeExcessWhitespaceFromCharacterDelimitedText(input, delimiter)

        then:
        result == expected

        where:
        delimiter << Delimiter.simpleValues()
    }

    void "checkSampleIdentifier, when identifier and parsed identifier match, succeed"() {
        given:
        SampleIdentifier sampleIdentifier = createSampleIdentifier()
        ParsedSampleIdentifier parsedSampleIdentifier = new DefaultParsedSampleIdentifier([
                projectName               : sampleIdentifier.individual.project.name,
                pid                       : sampleIdentifier.individual.pid,
                sampleTypeDbName          : sampleIdentifier.sampleType.name,
                fullSampleName            : sampleIdentifier.name,
                useSpecificReferenceGenome: null,
        ])

        when:
        createSampleIdentifierService().checkSampleIdentifier(parsedSampleIdentifier, sampleIdentifier)

        then:
        noExceptionThrown()
    }

    @Unroll
    void "checkSampleIdentifier, when identifier and parsed identifier not match in #property, throw exception"() {
        given:
        SampleIdentifier sampleIdentifier = createSampleIdentifier()
        ParsedSampleIdentifier parsedSampleIdentifier = new DefaultParsedSampleIdentifier([
                projectName               : sampleIdentifier.individual.project.name,
                pid                       : sampleIdentifier.individual.pid,
                sampleTypeDbName          : sampleIdentifier.sampleType.name,
                fullSampleName            : sampleIdentifier.name,
                useSpecificReferenceGenome: null,
        ] + [
                (property): value
        ])

        when:
        createSampleIdentifierService().checkSampleIdentifier(parsedSampleIdentifier, sampleIdentifier)

        then:
        OtpRuntimeException e = thrown()
        e.message.contains(message)

        where:
        property           | value             || message
        'projectName'      | 'otherProject'    || 'The sample identifier already exist, but is connected to project'
        'pid'              | 'otherPid'        || 'The sample identifier already exist, but is connected to individual'
        'sampleTypeDbName' | 'otherSampleType' || 'The sample identifier already exist, but is connected to sample type'
    }

    void "check if updateSampleIdentifierName, save new name of sample identifier"() {
        given:
        SampleIdentifier sampleIdentifier = createSampleIdentifier()
        SampleIdentifierService sampleIdentifierService = new SampleIdentifierService()

        when:
        sampleIdentifierService.updateSampleIdentifierName(sampleIdentifier, SAMPLE_IDENTIFIER_NAME)

        then:
        sampleIdentifier.name == SAMPLE_IDENTIFIER_NAME
    }

    void "check if deleteSampleIdentifier, deletes sample identifier"() {
        given:
        SampleIdentifier sampleIdentifier = createSampleIdentifier()
        SampleIdentifierService sampleIdentifierService = new SampleIdentifierService()

        when:
        sampleIdentifierService.deleteSampleIdentifier(sampleIdentifier)

        then:
        sampleIdentifier.findAll().size() == 0
    }

    void "check if createSampleIdentifier, creates a new sample identifier with sample and name"() {
        given:
        Sample sample = createSample()
        SampleIdentifierService sampleIdentifierService = new SampleIdentifierService()

        when:
        SampleIdentifier newSampleIdentifier = sampleIdentifierService.createSampleIdentifier(SAMPLE_IDENTIFIER_NAME, sample)

        then:
        newSampleIdentifier.name == SAMPLE_IDENTIFIER_NAME
        newSampleIdentifier.sample == sample
    }

    void "check if getOrCreateSampleIdentifier, creates a new sample identifier when params not don't match"() {
        given:
        SampleIdentifierService sampleIdentifierService = new SampleIdentifierService()
        Sample sample = createSample()

        when:
        SampleIdentifier newSampleIdentifier = sampleIdentifierService.getOrCreateSampleIdentifier(SAMPLE_IDENTIFIER_NAME, sample)

        then:
        newSampleIdentifier.name == SAMPLE_IDENTIFIER_NAME
        newSampleIdentifier.sample == sample
    }

    void "check if getOrCreateSampleIdentifier, returns the existing sample identifier when params match"() {
        given:
        SampleIdentifierService sampleIdentifierService = new SampleIdentifierService()
        SampleIdentifier existingSampleIdentifier = createSampleIdentifier()

        when:
        SampleIdentifier oldSampleIdentifier = sampleIdentifierService.getOrCreateSampleIdentifier(existingSampleIdentifier.name, existingSampleIdentifier.sample)

        then:
        existingSampleIdentifier == oldSampleIdentifier
    }
}
