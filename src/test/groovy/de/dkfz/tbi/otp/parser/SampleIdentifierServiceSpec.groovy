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

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.util.spreadsheet.Spreadsheet

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class SampleIdentifierServiceSpec extends Specification implements DataTest, ServiceUnitTest<SampleIdentifierService>, DomainFactoryCore {

    private static Spreadsheet.Delimiter defaultDelimiter = Spreadsheet.Delimiter.COMMA
    private static SampleType.SpecificReferenceGenome defaultSpecRefGen = SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
    private static String header = SampleIdentifierService.BulkSampleCreationHeader.getHeaders(defaultDelimiter)

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
            return properties[key] ?: HelperUtils.uniqueString
        }
        return new DefaultParsedSampleIdentifier(get('projectName'), get('pid'), get('sampleTypeDbName'), get('fullSampleName'))
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
        def e = thrown(RuntimeException)
        e.message == "Project ${identifier.projectName} does not exist."
    }

    @Unroll
    void "deriveSpecificReferenceGenome, test xenograft prefix recognition (sampleTypeName=#sampleTypeName)"() {
        when:
        SampleType.SpecificReferenceGenome result = service.deriveSpecificReferenceGenome(sampleTypeName)

        then:
        result == expectedSpecificReferenceGenome

        where:
        sampleTypeName            | expectedSpecificReferenceGenome
        "XENOGRAFT"               | SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
        "PATIENT-DERIVED-CULTURE" | SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
        "ORGANOID"                | SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
        "XENOGRAFT_01"            | SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
        "patient-derived-culture" | SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
        "patient_derived_culture" | SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
        "ORGAN"                   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        "TUMOR01"                 | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        "CONTROL_01"              | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        "METASTASIS-02"           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
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
        def e = thrown(RuntimeException)
        e.message.contains("already exists, but belongs to project")
    }

    private void callAndAssertFindOrSaveSample(ParsedSampleIdentifier identifier, SampleType.SpecificReferenceGenome specificReferenceGenome) {
        Sample result = service.findOrSaveSample(identifier, specificReferenceGenome)

        assert result.sampleType.name == identifier.sampleTypeDbName
        assert result.individual.pid == identifier.pid
        assert result.project.name == identifier.projectName
        assert containSame(Sample.list(), [result])
    }

    void "test findOrSaveSample, when sample exists, should return it"() {
        given:
        Sample sample = createSample()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(
                projectName     : sample.project.name,
                pid             : sample.individual.pid,
                sampleTypeDbName: sample.sampleType.name,
        )

        expect:
        callAndAssertFindOrSaveSample(identifier, null)
    }

    void "test findOrSaveSample, when sample does not exist, should create it"() {
        given:
        Project project = createProject()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(projectName: project.name)

        expect:
        callAndAssertFindOrSaveSample(identifier, defaultSpecRefGen)
    }

    void "test findOrSaveSample, when sample does not exist but sample type does, should create it"() {
        given:
        Project project = createProject()
        SampleType sampleType = createSampleType()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(
                projectName     : project.name,
                pid             : HelperUtils.uniqueString,
                sampleTypeDbName: sampleType.name,
        )


        expect:
        callAndAssertFindOrSaveSample(identifier, null)
    }

    void "findOrSaveSampleType, non existing sample type name is created with given specificReferenceGenome"() {
        given:
        String sampleTypeName = "to-be-created"

        when:
        SampleType result = service.findOrSaveSampleType(sampleTypeName, specificReferenceGenome)

        then:
        result.name == sampleTypeName
        result.specificReferenceGenome == specificReferenceGenome

        where:
        specificReferenceGenome << SampleType.SpecificReferenceGenome.values()
    }

    void "findOrSaveSampleType, non existing sample type name causes exception without specificReferenceGenome"() {
        given:
        String sampleTypeName = "does-not-exist-yet"

        when:
        service.findOrSaveSampleType(sampleTypeName, null)

        then:
        RuntimeException e = thrown()
        e.message == "SampleType \'${sampleTypeName}\' does not exist"
    }

    void "findOrSaveSampleType, sampleType is found and returned regardless of given specificReferenceGenome"() {
        given:
        String sampleTypeName = "existing"
        SampleType sampleType = createSampleType(name: sampleTypeName)

        when:
        SampleType result = service.findOrSaveSampleType(sampleTypeName, specificReferenceGenome)

        then:
        result == sampleType

        where:
        specificReferenceGenome << [
                defaultSpecRefGen,
                null,
        ]
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

    void "parsedIdentifierMatchesFoundIdentifier, matching identifiers"() {
        given:
        String projectName = "projectName"
        String pid = "pid"
        String sampleTypeName = "sampleTypeName"
        DefaultParsedSampleIdentifier parsedIdentifier = makeParsedSampleIdentifier(
                projectName     : projectName,
                pid             : pid,
                sampleTypeDbName: sampleTypeName,
        )
        SampleIdentifier foundIdentifier = getSampleIdentifier(projectName, pid, sampleTypeName)

        when:
        boolean result = service.parsedIdentifierMatchesFoundIdentifier(parsedIdentifier, foundIdentifier)

        then:
        result
    }

    void "parsedIdentifierMatchesFoundIdentifier, mismatching identifiers"() {
        given:
        String projectName = "projectName"
        String pid = "pid"
        String sampleTypeName = "sampleTypeName"

        DefaultParsedSampleIdentifier parsedIdentifier = makeParsedSampleIdentifier(
                projectName     : projectName + suffix[0],
                pid             : pid + suffix[1],
                sampleTypeDbName: sampleTypeName + suffix[2],
        )
        SampleIdentifier foundIdentifier = getSampleIdentifier(projectName, pid, sampleTypeName)

        when:
        boolean result = service.parsedIdentifierMatchesFoundIdentifier(parsedIdentifier, foundIdentifier)

        then:
        !result

        where:
        suffix << [
                ["a", "", ""],
                ["", "b", ""],
                ["", "", "c"],
        ]
    }

    private void callAndAssertFindOrSaveSampleIdentifier(ParsedSampleIdentifier identifier, SampleType.SpecificReferenceGenome specificReferenceGenome) {
        SampleIdentifier result = service.findOrSaveSampleIdentifier(identifier, specificReferenceGenome)

        assert result.name == identifier.fullSampleName
        assert result.sampleType.name == identifier.sampleTypeDbName
        assert result.individual.pid == identifier.pid
        assert result.project.name == identifier.projectName
        assert containSame(SampleIdentifier.list(), [result])
    }

    void "test findOrSaveSampleIdentifier, when sample identifier belongs to correct sample, should return it"() {
        given:
        SampleIdentifier sampleIdentifier = createSampleIdentifier()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(
                projectName     : sampleIdentifier.project.name,
                pid             : sampleIdentifier.individual.pid,
                sampleTypeDbName: sampleIdentifier.sampleType.name,
                fullSampleName  : sampleIdentifier.name,
        )

        expect:
        callAndAssertFindOrSaveSampleIdentifier(identifier, null)
    }

    void "test findOrSaveSampleIdentifier, when sample identifier does not exist, should create it"() {
        given:
        Project project = createProject()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(projectName: project.name)

        expect:
        callAndAssertFindOrSaveSampleIdentifier(identifier, defaultSpecRefGen)
    }

    void "test findOrSaveSampleIdentifier, when sample identifier belongs to other sample, should fail"() {
        given:
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier()
        createSampleIdentifier(
                name: identifier.fullSampleName,
        )

        when:
        service.findOrSaveSampleIdentifier(identifier, null)

        then:
        def e = thrown(RuntimeException)
        e.message.contains("already exists, but belongs to sample")
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
        nameIn            | nameOut
        "typical"         | "typical"
        "with_underscore" | "with-underscore"
    }

    private SampleIdentifierService createSampleIdentifierService() {
        SampleIdentifierService sampleIdentifierService = Spy(SampleIdentifierService) {
            findOrSaveSampleIdentifier(_, _) >> { }
        }
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
        input        | expected
        "sin_gle"    | "sin-gle"
        "mul_ti_ple" | "mul-ti-ple"
    }

    void "sanitizeParsedSampleIdentifier returns sanitized identifier"() {
        given:
        DefaultParsedSampleIdentifier parsedIdentifier = makeParsedSampleIdentifier(sampleTypeDbName: "UNDER_SCORE")

        when:
        DefaultParsedSampleIdentifier sanitized = service.sanitizeParsedSampleIdentifier(parsedIdentifier)

        then:
        sanitized.sampleTypeDbName == "UNDER-SCORE"
    }

    @Unroll
    void "test createBulkSamples with correct values (useName=#useName)"() {
        given:
        SampleIdentifierService sampleIdentifierService = createSampleIdentifierService()
        List<String> output
        Project project = createProject()
        String context = "${header}\n${useName ? project.name : ''},test,test,test"

        when:
        output = sampleIdentifierService.createBulkSamples(context, defaultDelimiter, project, defaultSpecRefGen)

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
        output = sampleIdentifierService.createBulkSamples('invalidHeader\ntest,test,test', defaultDelimiter, project, defaultSpecRefGen)

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
        output = sampleIdentifierService.createBulkSamples("${header}\ninvalidProject,test,test,test", defaultDelimiter, project, defaultSpecRefGen)

        then:
        containSame(output, ["Could not find Project 'invalidProject'"])
    }

    void "test createBulkSamples when given unknown HEADER"() {
        given:
        SampleIdentifierService sampleIdentifierService = createSampleIdentifierService()
        List<String> output
        Project project = createProject()

        when:
        output = sampleIdentifierService.createBulkSamples("${header},UNKNOWN_HEADER\n${project.name},test,test,test,test", defaultDelimiter, project, defaultSpecRefGen)

        then:
        containSame(output, ["The column header 'UNKNOWN_HEADER' is unknown"])
    }

    @Unroll
    void "removeExcessWhitespaceFromCharacterDelimitedText, common cases with all delimiters (delimiter=#delimiter)"() {
        given:
        SampleIdentifierService sampleIdentifierService = createSampleIdentifierService()

        String d = delimiter.delimiter.toString()

        String input = " front${d}back ${d}mid dle${d} m u l t i ${d}  consec  utive\n new line front${d} new line middle ${d}new line back \na${d}b${d}c"
        String expected = "front${d}back${d}mid dle${d}m u l t i${d}consec utive\nnew line front${d}new line middle${d}new line back\na${d}b${d}c"

        when:
        String result = sampleIdentifierService.removeExcessWhitespaceFromCharacterDelimitedText(input, delimiter)

        then:
        result == expected

        where:
        delimiter << Spreadsheet.Delimiter.values()
    }
}
