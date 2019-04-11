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

import org.springframework.context.ApplicationContext
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.util.spreadsheet.Spreadsheet

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class SampleIdentifierServiceSpec extends Specification implements DataTest, ServiceUnitTest<SampleIdentifierService>, DomainFactoryCore {

    private String HEADER = SampleIdentifierService.BulkSampleCreationHeader.getHeaders(Spreadsheet.Delimiter.COMMA)

    Class[] getDomainClassesToMock() {[
            Individual,
            ProcessingOption,
            Project,
            Realm,
            Sample,
            SampleIdentifier,
            SampleType,
    ]}


    private ParsedSampleIdentifier makeParsedSampleIdentifier(
            String projectName = HelperUtils.uniqueString,
            String pid = HelperUtils.uniqueString,
            String sampleTypeDbName = HelperUtils.uniqueString,
            String fullSampleName = HelperUtils.uniqueString) {
        return new DefaultParsedSampleIdentifier(projectName, pid, sampleTypeDbName, fullSampleName)
    }

    void "test findProject, when project exists, should return it"() {
        given:
        Project project = DomainFactory.createProject()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(project.name)

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

    private void findOrSaveIndividual(ParsedSampleIdentifier identifier) {
        Individual result = service.findOrSaveIndividual(identifier)

        assert result.pid == identifier.pid
        assert result.project.name == identifier.projectName
        assert containSame(Individual.list(), [result])
    }

    void "test findOrSaveIndividual, individual exists in correct project, should return it"() {
        given:
        Individual individual = DomainFactory.createIndividual()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(individual.project.name, individual.pid)

        expect:
        findOrSaveIndividual(identifier)
    }

    void "test findOrSaveIndividual, individual doesn't exist, should create it"() {
        given:
        Project project = DomainFactory.createProject()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(project.name)

        expect:
        findOrSaveIndividual(identifier)
    }

    void "test findOrSaveIndividual, individual exists in other project, should fail"() {
        given:
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier()
        DomainFactory.createIndividual(
                pid: identifier.pid,
        )

        when:
        service.findOrSaveIndividual(identifier)

        then:
        def e = thrown(RuntimeException)
        e.message.contains("already exists, but belongs to project")
    }

    void "test createSampleTypeXenograftDepending, when sample is not xenograft, should return sample using project default reference genome"() {
        when:
        SampleType sampleType = service.createSampleTypeXenograftDepending(HelperUtils.uniqueString)

        then:
        sampleType.specificReferenceGenome == SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
    }

    void "test createSampleTypeXenograftDepending, when sample is xenograft, should return sample using sample type specific reference genome"() {
        when:
        SampleType sampleType = service.createSampleTypeXenograftDepending(SampleIdentifierService.XENOGRAFT + HelperUtils.uniqueString)

        then:
        sampleType.specificReferenceGenome == SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
    }

    void "test createSampleTypeXenograftDepending, when sample is patient derived culture, should return sample using sample type specific reference genome"() {
        when:
        SampleType sampleType = service.createSampleTypeXenograftDepending(SampleIdentifierService.CULTURE + HelperUtils.uniqueString)

        then:
        sampleType.specificReferenceGenome == SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
    }

    void "test createSampleTypeXenograftDepending, when sample is organoid, should return sample using sample type specific reference genome"() {
        when:
        SampleType sampleType = service.createSampleTypeXenograftDepending(SampleIdentifierService.ORGANOID + HelperUtils.uniqueString)

        then:
        sampleType.specificReferenceGenome == SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
    }


    private void findOrSaveSample(ParsedSampleIdentifier identifier) {
        Sample result = service.findOrSaveSample(identifier)

        assert result.sampleType.name == identifier.sampleTypeDbName
        assert result.individual.pid == identifier.pid
        assert result.project.name == identifier.projectName
        assert containSame(Sample.list(), [result])
    }

    void "test findOrSaveSample, when sample exists, should return it"() {
        given:
        Sample sample = DomainFactory.createSample()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(sample.project.name, sample.individual.pid, sample.sampleType.name)

        expect:
        findOrSaveSample(identifier)
    }

    void "test findOrSaveSample, when sample does not exist, should create it"() {
        given:
        Project project = DomainFactory.createProject()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(project.name)

        expect:
        findOrSaveSample(identifier)
    }

    void "test findOrSaveSample, when sample does not exist but sample type does, should create it"() {
        given:
        Project project = DomainFactory.createProject()
        SampleType sampleType = DomainFactory.createSampleType()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(project.name, HelperUtils.uniqueString, sampleType.name)

        expect:
        findOrSaveSample(identifier)
    }

    private void findOrSaveSampleIdentifier(ParsedSampleIdentifier identifier) {
        SampleIdentifier result = service.findOrSaveSampleIdentifier(identifier)

        assert result.name == identifier.fullSampleName
        assert result.sampleType.name == identifier.sampleTypeDbName
        assert result.individual.pid == identifier.pid
        assert result.project.name == identifier.projectName
        assert containSame(SampleIdentifier.list(), [result])
    }

    void "test findOrSaveSampleIdentifier, when sample identifier belongs to correct sample, should return it"() {
        given:
        SampleIdentifier sampleIdentifier = DomainFactory.createSampleIdentifier()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(
                sampleIdentifier.project.name,
                sampleIdentifier.individual.pid,
                sampleIdentifier.sampleType.name,
                sampleIdentifier.name,
        )

        expect:
        findOrSaveSampleIdentifier(identifier)
    }

    void "test findOrSaveSampleIdentifier, when sample identifier does not exist, should create it"() {
        given:
        Project project = DomainFactory.createProject()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(project.name)

        expect:
        findOrSaveSampleIdentifier(identifier)
    }

    void "test findOrSaveSampleIdentifier, when sample identifier belongs to other sample, should fail"() {
        given:
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier()
        DomainFactory.createSampleIdentifier(
                name: identifier.fullSampleName,
        )

        when:
        service.findOrSaveSampleIdentifier(identifier)

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

    void "test findOrSaveSampleIdentifier, should replace underscore"() {
        given:
        Project project = DomainFactory.createProject()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(project.name, HelperUtils.uniqueString, "sampleTypeName_3")

        when:
        SampleIdentifier result = service.findOrSaveSampleIdentifier(identifier)

        then:
        result.sampleType.name == "sampleTypeName-3"
    }

    void "test findOrSaveSample, should replace underscore"() {
        given:
        Sample sample = DomainFactory.createSample()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(sample.project.name, sample.individual.pid, "sampleTypeName_3")

        when:
        Sample result = service.findOrSaveSample(identifier)

        then:
        result.sampleType.name == "sampleTypeName-3"
    }

    void "test findOrSaveSample, with underscore"() {
        given:
        Sample sample = DomainFactory.createSample()
        new SampleType(name: 'sampleTypeName_3').save(flush: true, validate: false)
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(sample.project.name, sample.individual.pid, "sampleTypeName_3")

        when:
        Sample result = service.findOrSaveSample(identifier)

        then:
        result.sampleType.name == "sampleTypeName_3"
    }

    void "test findOrSaveSampleIdentifier, with underscore"() {
        given:
        Project project = DomainFactory.createProject()
        new SampleType(name: 'sampleTypeName_3').save(flush: true, validate: false)
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(project.name, HelperUtils.uniqueString, "sampleTypeName_3")

        when:
        SampleIdentifier result = service.findOrSaveSampleIdentifier(identifier)

        then:
        result.sampleType.name == "sampleTypeName_3"
    }

    private SampleIdentifierService createSampleIdentifierService() {
        SampleIdentifierService sampleIdentifierService = Spy(SampleIdentifierService) {
            findOrSaveSampleIdentifier(_) >> { }
        }
        sampleIdentifierService.applicationContext = Mock(ApplicationContext) {
            getBean(_) >> {
                return new BulkSampleCreationValidator()
            }
        }
        return sampleIdentifierService
    }

    @Unroll
    void "test createBulkSamples with correct values (userName=#userName)"() {
        given:
        SampleIdentifierService sampleIdentifierService = createSampleIdentifierService()
        List<String> output
        Project project = createProject()
        String context = "${HEADER}\n${useName ? project.name : ''},test,test,test"

        when:
        output = sampleIdentifierService.createBulkSamples(context, Spreadsheet.Delimiter.COMMA, project)

        then:
        output == []

        where:
        useName | _
        true    | _
        false   | _
    }

    void "test createBulkSamples when headers are missing"() {
        given:
        SampleIdentifierService sampleIdentifierService = createSampleIdentifierService()
        List<String> output
        Project project = createProject()

        when:
        output = sampleIdentifierService.createBulkSamples('invalidHeader\ntest,test,test', Spreadsheet.Delimiter.COMMA, project)

        then:
        CollectionUtils.containSame(output, [
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
        output = sampleIdentifierService.createBulkSamples("${HEADER}\ninvalidProject,test,test,test", Spreadsheet.Delimiter.COMMA, project)

        then:
        CollectionUtils.containSame(output, ["Could not find Project 'invalidProject'"])
    }
}
