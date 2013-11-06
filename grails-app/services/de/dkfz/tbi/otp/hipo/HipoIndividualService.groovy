package de.dkfz.tbi.otp.hipo

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.ngsdata.*

class HipoIndividualService {

    private static final int PROJECT_NUMBER_POS = 1
    private static final int PROJECT_NUMBER_LENGTH = 3

    final static Map<String, String> TISSUEMAP = [
        "T": "TUMOR",
        "M": "METASTASIS",
        "S": "SPHERE",
        "X": "XENOGRAFT",
        "Y": "BIOPSY",
        "B": "BLOOD",
        "N": "CONTROL",
        "C": "CELL",
        "I": "INVASIVE_MARGINS",
        "P": "PATIENT_DERIVED_CULTURE",
        "Q": "CULTURE_DERIVED_XENOGRAFT",
    ].asImmutable()

    /**
     * Validation if the sampleName is a valid HIPO-Identifier.
     * Therefore it is checked if the name, the sampleType and the project are valid.
     * For project HIPO_035 the name is checked twice, since only blood and cell are allowed as sample types.
     *
     * @param sampleName, which has to be validated
     * @return An Individual, when the sampleName is a valid HIPO/POP identifier
     */
    Individual createHipoIndividual(String sampleName) {
        int projectNumber = sampleName.substring(PROJECT_NUMBER_POS ,PROJECT_NUMBER_POS + PROJECT_NUMBER_LENGTH) as Integer
        if ((!checkIfHipoName(sampleName)) || ((projectNumber == 35) && (!checkIfHipo35Name(sampleName)))) {
            return null
        }
        if (SampleIdentifier.findByName(sampleName)) {
            return null
        }
        assureSampleType(sampleName, projectNumber)
        assureProject(sampleName)
        Individual individual = createOrReturnIndividual(sampleName)
        addSample(individual, sampleName, projectNumber)
        return individual
    }

    private boolean checkIfHipoName(String sampleName) {
        String issues = TISSUEMAP.keySet().join()
        String hipoRegex = /[HP]\d\d\d-\w\w\w\w-[${issues}]\d-[DRP]\d/
        return (sampleName =~ hipoRegex)
    }

    private boolean checkIfHipo35Name(String sampleName) {
        String hipoRegex = /[H]035-\w\w\w[KM]-[BC]\d-[DRP]\d/
        return (sampleName =~ hipoRegex)
    }

    private void assureSampleType(String sampleName, int projectNumber) {
        String tissueType = tissueType(sampleName, projectNumber)
        SampleType.findOrSaveByName(tissueType)
    }

    private String tissueType(String sampleName, int projectNumber) {
        notNull(sampleName, "The input sampleName for the method tissueType was null")
        final int TYPE_POS = 10
        final int SAMPLE_NUMBER_POS = 11
        String tissueKey = sampleName.substring(TYPE_POS, TYPE_POS + 1)
        int sampleNumber = sampleName.substring(SAMPLE_NUMBER_POS, SAMPLE_NUMBER_POS + 1) as Integer
        if (projectNumber != 35 && sampleNumber == 1) {
            return "${TISSUEMAP.get(tissueKey)}"
        } else {
            return "${TISSUEMAP.get(tissueKey)}${String.format('%02d', sampleNumber, 'd')}"
        }
    }

    private void assureProject(String sampleName) {
        Project project = findProject(sampleName)
        if (!project) {
            throw new Exception("Project for sample ${sampleName} does not exists")
        }
    }

    private Project findProject(String sampleName) {
        String number = sampleName.substring(PROJECT_NUMBER_POS, PROJECT_NUMBER_POS + PROJECT_NUMBER_LENGTH)
        String projectName = "hipo_" + number
        return Project.findByName(projectName)
    }

    private Individual createOrReturnIndividual(String sampleName) {
        final int INDIVIDUAL_POS = 9
        String pid = sampleName.substring(0, INDIVIDUAL_POS)
        Individual ind = Individual.findByPid(pid)
        if (ind) {
            return ind
        }
        ind = new Individual(
                        pid: pid,
                        mockPid: pid,
                        mockFullName: pid,
                        project: findProject(sampleName),
                        type: Individual.Type.REAL
                        )
        ind.save(flush: true)
        return ind
    }

    private void addSample(Individual ind, String sampleName, int projectNumber) {
        notNull(ind, "the input individual of the method addSample is null")
        SampleType type = SampleType.findByName(tissueType(sampleName, projectNumber))
        Sample sample = Sample.findOrSaveByIndividualAndSampleType(ind, type)
        SampleIdentifier si = new SampleIdentifier(name: sampleName, sample: sample)
        si.save(flush: true)
    }
}
