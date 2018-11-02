package de.dkfz.tbi.otp.ngsdata

class MappingFileLoader {

    private final String separator1 = ","
    private final String separator2 = "/"

    Project project = null

    /**
     * This class is parsing the mapping file in the standard:
     * sampleName,pid/sampleType
     * where sampleType is either "tumor" or "control"
     * New individuals and samples are created if needed (with unique pid)
     *
     * @param proj - project to which individuals are assigned
     * @param path - path to the mapping file
     */
    void loadMappingFile(Project proj, String path) {
        project = proj
        String text = readFile(path)
        text.eachLine { String line ->
            readLine(line)
        }
    }

    private String readFile(path) {
        File file = new File(path)
        if (!file.canRead()) {
            throw new FileNotReadableException(path)
        }
        return file.getText()
    }

    private void readLine(String line) {
        String name = parseName(line)
        String pid = parsePid(line)
        SampleType sampleType = parseType(line)
        Individual ind = findOrCreate(pid)
        if (ind.mockPid == null) {
            ind.mockPid = name
            ind.mockFullName = name
        }
        ind.save(flush: true)
        Sample sample = findOrCreateSample(ind, sampleType)
        (new SampleIdentifier(name: name, sample: sample)).save(flush: true)
    }

    private String parseName(String line) {
        return line.substring(0, line.indexOf(separator1))
    }

    private String parsePid(String line) {
        return line.substring(line.indexOf(separator1) + 1, line.indexOf(separator2))
    }

    private SampleType parseType(String line) {
        String name = line.substring(line.indexOf(separator2) + 1)
        return SampleType.findByNameIlike(name)
    }

    private Individual findOrCreate(String pid) {
        Individual ind = Individual.findByPid(pid)
        if (ind == null) {
            ind = new Individual(pid: pid)
            ind.project = project
            ind.type = Individual.Type.REAL
        }
        return ind
    }

    private Sample findOrCreateSample(Individual ind, SampleType type) {
        Sample sample = Sample.findByIndividualAndSampleType(ind, type)
        if (sample == null) {
            sample = new Sample(
               individual: ind,
               sampleType: type
            )
            sample.save(flush: true)
        }
        return sample
    }
}
