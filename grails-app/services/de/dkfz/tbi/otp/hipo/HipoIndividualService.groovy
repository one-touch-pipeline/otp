package de.dkfz.tbi.otp.hipo

import de.dkfz.tbi.otp.ngsdata.*
import static org.springframework.util.Assert.*

class HipoIndividualService {

    final static Map<String, String> tissueMap = [
        "T": "TUMOR",
        "M": "METASTASIS",
        "S": "SPHERE",
        "X": "XENOGRAFT",
        "Y": "BIOPSY",
        "B": "BLOOD",
        "N": "CONTROL"
    ]

    Individual createHipoIndividual(String sampleName) {
        if (!checkIfHipoName(sampleName)) {
            return null
        }
        if (SampleIdentifier.findByName(sampleName)) {
            return null
        }
        assureSampleType(sampleName)
        assureProject(sampleName)
        Individual individual = createOrReturnIndividual(sampleName)
        addSample(individual, sampleName)
        return individual
    }

    private boolean checkIfHipoName(String sampleName) {
        String hipoRegex = /H\d\d\d-\w\w\w\w-[TMSXYBN]\d-[DRP]\d/
        return (sampleName =~ hipoRegex)
    }

    private void assureSampleType(String sampleName) {
        String tissueType = tissueType(sampleName)
        SampleType.findOrSaveByName(tissueType)
    }

    private String tissueType(String sampleName) {
        notNull(sampleName, "The input for the method tissueType was null")
        final int TYPE_POS = 10
        final int SAMPLE_NUMBER_POS = 11
        String tissueKey = sampleName.substring(TYPE_POS, TYPE_POS + 1)
        int sampleNumber = sampleName.substring(SAMPLE_NUMBER_POS, SAMPLE_NUMBER_POS + 1) as Integer
        if (sampleNumber == 1) {
            return "${tissueMap.get(tissueKey)}"
        } else if (sampleNumber < 10) {
            return "${tissueMap.get(tissueKey)}0${sampleNumber}"
        } else {
            return "${tissueMap.get(tissueKey)}${sampleNumber}"
        }
    }

    private void assureProject(String sampleName) {
        Project project = findProject(sampleName)
        if (!project) {
            throw new Exception("Project for sample ${sampleName} does not exists")
        }
    }

    private Project findProject(String sampleName) {
        final int NUMBER_POS = 1
        String number = sampleName.substring(NUMBER_POS, NUMBER_POS + 3)
        String projectName = "hipo_${number}"
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

    private void addSample(Individual ind, String sampleName) {
        SampleType type = SampleType.findByName(tissueType(sampleName))
        Sample sample = Sample.findOrSaveByIndividualAndSampleType(ind, type)
        SampleIdentifier si = new SampleIdentifier(name: sampleName, sample: sample)
        si.save(flush: true)
    }
}
