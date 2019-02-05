package de.dkfz.tbi.otp.ngsdata

import static de.dkfz.tbi.otp.utils.CollectionUtils.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.utils.HelperUtils
import grails.test.mixin.TestFor
import org.junit.*
import grails.buildtestdata.mixin.Build

@Build([
        ProcessingOption,
        SampleIdentifier,
])
@TestFor(SampleIdentifierService)
class SampleIdentifierServiceUnitTests {

    private ParsedSampleIdentifier makeParsedSampleIdentifier(
            String projectName = HelperUtils.uniqueString,
            String pid = HelperUtils.uniqueString,
            String sampleTypeDbName = HelperUtils.uniqueString,
            String fullSampleName = HelperUtils.uniqueString) {
        return new DefaultParsedSampleIdentifier(projectName, pid, sampleTypeDbName, fullSampleName)
    }

    @Test
    void testFindProject_ProjectExists_ShouldReturnIt() {
        Project project = DomainFactory.createProject()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(project.name)

        Project result = service.findProject(identifier)

        assert result.name == identifier.projectName
    }

    @Test
    void testFindProject_ProjectDoesNotExist_ShouldThrow() {
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier()

        assert shouldFail {
            service.findProject(identifier)
        } == "Project ${identifier.projectName} does not exist."
    }

    private void testFindOrSaveIndividual_ShouldReturnIt(ParsedSampleIdentifier identifier) {
        Individual result = service.findOrSaveIndividual(identifier)

        assert result.pid == identifier.pid
        assert result.project.name == identifier.projectName
        assert containSame(Individual.list(), [result])
    }

    @Test
    void testFindOrSaveIndividual_IndividualExitsInCorrectProject_ShouldReturnIt() {
        Individual individual = DomainFactory.createIndividual()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(individual.project.name, individual.pid)

        testFindOrSaveIndividual_ShouldReturnIt(identifier)
    }

    @Test
    void testFindOrSaveIndividual_IndividualDoesNotExist_ShouldCreateNew() {
        Project project = DomainFactory.createProject()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(project.name)

        testFindOrSaveIndividual_ShouldReturnIt(identifier)
    }

    @Test
    void testFindOrSaveIndividual_IndividualExistsInOtherProject_ShouldThrow() {
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier()
        DomainFactory.createIndividual(
                pid: identifier.pid,
        )

        assert shouldFail {
            service.findOrSaveIndividual(identifier)
        }.contains("already exists, but belongs to project")
    }


    @Test
    void testCreateSampleTypeXenograftDepending_SampleIsNoXenograft_ShouldReturnSampleUsingProjectDefaultReferenceGenome() {
        SampleType sampleType = service.createSampleTypeXenograftDepending(HelperUtils.uniqueString)

        assert sampleType.specificReferenceGenome == SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
    }

    @Test
    void testCreateSampleTypeXenograftDepending_SampleIsXenograft_ShouldReturnSampleUsingSampleTypeSpecificReferenceGenome() {
        SampleType sampleType = service.createSampleTypeXenograftDepending(SampleIdentifierService.XENOGRAFT + HelperUtils.uniqueString)

        assert sampleType.specificReferenceGenome == SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
    }

    @Test
    void testCreateSampleTypeXenograftDepending_SampleIsPatientDerivedCulture_ShouldReturnSampleUsingSampleTypeSpecificReferenceGenome() {
        SampleType sampleType = service.createSampleTypeXenograftDepending(SampleIdentifierService.CULTURE + HelperUtils.uniqueString)

        assert sampleType.specificReferenceGenome == SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
    }

    @Test
    void testCreateSampleTypeXenograftDepending_SampleIsOrganoid_ShouldReturnSampleUsingSampleTypeSpecificReferenceGenome() {
        SampleType sampleType = service.createSampleTypeXenograftDepending(SampleIdentifierService.ORGANOID + HelperUtils.uniqueString)

        assert sampleType.specificReferenceGenome == SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
    }


    private void testFindOrSaveSample_ShouldReturnIt(ParsedSampleIdentifier identifier) {
        Sample result = service.findOrSaveSample(identifier)

        assert result.sampleType.name == identifier.sampleTypeDbName
        assert result.individual.pid == identifier.pid
        assert result.project.name == identifier.projectName
        assert containSame(Sample.list(), [result])
    }

    @Test
    void testFindOrSaveSample_SampleExists_ShouldReturnIt() {
        Sample sample = DomainFactory.createSample()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(sample.project.name, sample.individual.pid, sample.sampleType.name)

        testFindOrSaveSample_ShouldReturnIt(identifier)
    }

    @Test
    void testFindOrSaveSample_SampleDoesNotExist_ShouldCreateNew() {
        Project project = DomainFactory.createProject()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(project.name)

        testFindOrSaveSample_ShouldReturnIt(identifier)
    }

    @Test
    void testFindOrSaveSample_SampleDoesNotExistButSampleTypeDoes_ShouldCreateNew() {
        Project project = DomainFactory.createProject()
        SampleType sampleType = DomainFactory.createSampleType()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(project.name, HelperUtils.uniqueString, sampleType.name)

        testFindOrSaveSample_ShouldReturnIt(identifier)
    }

    private void testFindOrSaveSampleIdentifier_ShouldReturnIt(ParsedSampleIdentifier identifier) {
        SampleIdentifier result = service.findOrSaveSampleIdentifier(identifier)

        assert result.name == identifier.fullSampleName
        assert result.sampleType.name == identifier.sampleTypeDbName
        assert result.individual.pid == identifier.pid
        assert result.project.name == identifier.projectName
        assert containSame(SampleIdentifier.list(), [result])
    }

    @Test
    void testFindOrSaveSampleIdentifier_SampleIdentifierBelongsToCorrectSample_ShouldReturnIt() {
        SampleIdentifier sampleIdentifier = DomainFactory.createSampleIdentifier()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(
                sampleIdentifier.project.name,
                sampleIdentifier.individual.pid,
                sampleIdentifier.sampleType.name,
                sampleIdentifier.name,
        )

        testFindOrSaveSampleIdentifier_ShouldReturnIt(identifier)
    }

    @Test
    void testFindOrSaveSampleIdentifier_SampleIdentifierDoesNotExist_ShouldCreateNew() {
        Project project = DomainFactory.createProject()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(project.name)

        testFindOrSaveSampleIdentifier_ShouldReturnIt(identifier)
    }

    @Test
    void testFindOrSaveSampleIdentifier_SampleIdentifierBelongsToOtherSample_ShouldThrow() {
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier()
        DomainFactory.createSampleIdentifier(
                name: identifier.fullSampleName,
        )

        assert shouldFail {
            service.findOrSaveSampleIdentifier(identifier)
        }.contains("already exists, but belongs to sample")
    }

    @Test
    void testParseAndFindOrSaveSampleIdentifier_NoParserCanParse_ShouldReturnNull() {
        SampleIdentifier result = service.parseAndFindOrSaveSampleIdentifier(HelperUtils.uniqueString, null)

        assert result == null
        assert SampleIdentifier.count() == 0
        assert Sample.count() == 0
        assert SampleType.count() == 0
        assert Individual.count() == 0
        assert Project.count() == 0
    }

    @Test
    void testfindOrSaveSampleIdentifierUnderscoreReplace() {
        Project project = DomainFactory.createProject()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(project.name, HelperUtils.uniqueString, "sampleTypeName_3")
        SampleIdentifier result = service.findOrSaveSampleIdentifier(identifier)
        assert result.sampleType.name == "sampleTypeName-3"
    }

    @Test
    void testfindOrSaveSampleUnderscoreReplace() {
        Sample sample = DomainFactory.createSample()
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(sample.project.name, sample.individual.pid, "sampleTypeName_3")
        Sample result = service.findOrSaveSample(identifier)
        assert result.sampleType.name == "sampleTypeName-3"
    }

    @Test
    void testfindOrSaveSampleWithUnderscore() {
        Sample sample = DomainFactory.createSample()
        new SampleType(name: 'sampleTypeName_3').save(validate: false)
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(sample.project.name, sample.individual.pid, "sampleTypeName_3")
        Sample result = service.findOrSaveSample(identifier)
        assert result.sampleType.name == "sampleTypeName_3"
    }

    @Test
    void testfindOrSaveSampleIdentifierWithUnderscore() {
        Project project = DomainFactory.createProject()
        new SampleType(name: 'sampleTypeName_3').save(validate: false)
        ParsedSampleIdentifier identifier = makeParsedSampleIdentifier(project.name, HelperUtils.uniqueString, "sampleTypeName_3")
        SampleIdentifier result = service.findOrSaveSampleIdentifier(identifier)
        assert result.sampleType.name == "sampleTypeName_3"
    }
}
