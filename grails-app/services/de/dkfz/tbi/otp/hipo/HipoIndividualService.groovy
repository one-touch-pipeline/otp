package de.dkfz.tbi.otp.hipo

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.ngsdata.*

class HipoIndividualService {

    /**
     * Validation if the sampleName is a valid HIPO-Identifier.
     * Therefore it is checked if the name, the sampleType and the project are valid.
     * For project HIPO_035 the name is checked twice, since only blood and cell are allowed as sample types.
     *
     * @param sampleName, which has to be validated
     * @return An Individual, when the sampleName is a valid HIPO/POP identifier
     */
    Individual createHipoIndividual(String sampleName) {
        HipoSampleIdentifier identifier = HipoSampleIdentifier.tryParse(sampleName)
        if (!identifier) {
            // not a valid HIPO sample name, skip
            return null
        }
        assureSampleType(identifier)
        assureProject(identifier)
        if (!SampleIdentifier.findByName(sampleName)) {
            Individual individual = createOrReturnIndividual(identifier)
            addSample(individual, identifier)
            return individual
        } else {
            return Individual.findByPid(identifier.pid)
        }
    }

    private void assureSampleType(HipoSampleIdentifier identifier) {
        SampleType.findOrSaveByName(identifier.sampleTypeDbName)
    }

    private Project assureProject(HipoSampleIdentifier identifier) {
        Project project = findProject(identifier)
        if (project) {
            return project
        } else {
            throw new RuntimeException("Project for sample ${identifier.fullSampleName} does not exists")
        }
    }

    private Project findProject(HipoSampleIdentifier identifier) {
        String projectName = "hipo_" + String.format("%03d", identifier.projectNumber)
        return Project.findByName(projectName)
    }

    private Individual createOrReturnIndividual(HipoSampleIdentifier identifier) {
        Individual ind = Individual.findByPid(identifier.pid)
        if (ind) {
            return ind
        }
        ind = new Individual(
                pid: identifier.pid,
                mockPid: identifier.pid,
                mockFullName: identifier.pid,
                project: findProject(identifier),
                type: Individual.Type.REAL
                )
        ind.save(flush: true)
        return ind
    }

    private void addSample(Individual ind, HipoSampleIdentifier identifier) {
        SampleType type = SampleType.findByName(identifier.sampleTypeDbName)
        Sample sample = Sample.findOrSaveByIndividualAndSampleType(ind, type)
        SampleIdentifier si = new SampleIdentifier(name: identifier.fullSampleName, sample: sample)
        si.save(flush: true)
    }
}
