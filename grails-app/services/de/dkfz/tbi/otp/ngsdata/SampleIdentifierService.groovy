package de.dkfz.tbi.otp.ngsdata

import static de.dkfz.tbi.otp.utils.CollectionUtils.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

class SampleIdentifierService {

    @Autowired
    ApplicationContext applicationContext

    Collection<? extends SampleIdentifierParser> getSampleIdentifierParsers() {
        return applicationContext.getBeansOfType(SampleIdentifierParser).values()
    }

    SampleIdentifier parseAndFindOrSaveSampleIdentifier(String sampleIdentifier,
            Collection<? extends SampleIdentifierParser> sampleIdentifierParsers = getSampleIdentifierParsers()) {
        ParsedSampleIdentifier identifier = parseSampleIdentifier(sampleIdentifier, sampleIdentifierParsers)
        if (identifier) {
            return findOrSaveSampleIdentifier(identifier)
        } else {
            return null
        }
    }

    ParsedSampleIdentifier parseSampleIdentifier(String sampleIdentifier,
             Collection<? extends SampleIdentifierParser> sampleIdentifierParsers = getSampleIdentifierParsers()) {
        Map<SampleIdentifierParser, ParsedSampleIdentifier> results = [:]
        sampleIdentifierParsers.each {
            ParsedSampleIdentifier identifier = it.tryParse(sampleIdentifier)
            if (identifier != null) {
                assert identifier.fullSampleName == sampleIdentifier
                results.put(it, identifier)
            }
        }
        if (results.size() <= 1) {
            return atMostOneElement(results.values())
        } else {
            throw new RuntimeException("${sampleIdentifier} is ambiguous. It can be parsed by ${results.keySet()}.")
        }
    }

    SampleIdentifier findOrSaveSampleIdentifier(ParsedSampleIdentifier identifier) {
        SampleIdentifier result = atMostOneElement(SampleIdentifier.findAllByName(identifier.fullSampleName))
        if (result) {
            if (result.project.name != identifier.projectName ||
                    result.individual.pid != identifier.pid ||
                    result.sampleType.name != identifier.sampleTypeDbName) {
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
        return Sample.findOrSaveWhere(
                individual: findOrSaveIndividual(identifier),
                sampleType: SampleType.findOrSaveWhere(name: identifier.sampleTypeDbName),
        )
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
