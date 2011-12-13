package de.dkfz.tbi.otp.ngsdata

class SampleSwapService {

    /**
     * 
     * This function disconnect data from a given sample
     * from tha sample. This functions shall be used when 
     * physical sample was discovered to be swapped.
     * 
     * New new individual is created "PooledErrors"
     * 
     * @param sample
     */
    void wrongSample(Sample sample) {

        // take real individual
        Individual oldIndividual = sample.individual

        // take or create and PooledError Individual

        Individual ind = Individual.findByPid("errors")
        if (!ind) {
            ind = new Individual (
                    pid: "errors",
                    mockPid: "errors",
                    mockFullName: "PooledErrors",
                    type = Individual.Type.POOL
                )
            ind.save(flush: true)
        }

        // move Sample to different this individual
        sample.individual = ind;
        ind.save()
        sample.save();

        // create new Sample 
        Sample newSample = new Sample(
            type = sample.type,
            subType = sample.subType
        )

        newSample.individual = oldIndividual
        newSample.save()

        // move Sample identifiers
        SampleIdentifier.findAllBySample(sample).each {SampleIdentifier identifier ->
            identifier.sample = newSample
            identifier.save()
        }

        // invalidate meta-data entries
        

        // deal with the files 
    }
}
