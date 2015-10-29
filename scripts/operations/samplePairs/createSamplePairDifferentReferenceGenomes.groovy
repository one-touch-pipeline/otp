package operations.samplePairs

import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.SampleType

//TODO: integrate into OTP (OTP-1677)

static Collection<SamplePair> findMissingXenograftDiseaseControlSamplePairs() {
    final Collection queryResults = SamplePair.executeQuery("""
SELECT DISTINCT
mwp1,
mwp2
FROM
MergingWorkPackage mwp1
join mwp1.sample.individual.project project_1
join mwp1.sample.sampleType sampleType_1,
MergingWorkPackage mwp2
join mwp2.sample.sampleType sampleType_2,
SampleTypePerProject stpp1,
SampleTypePerProject stpp2
WHERE
${(SamplePair.mergingWorkPackageEqualProperties + ['sample.individual'] - ['referenceGenome']).collect{
        "(mwp1.${it} = mwp2.${it} OR mwp1.${it} IS NULL AND mwp2.${it} IS NULL)"
    }.join(' AND\n')} AND
mwp1.referenceGenome.name in ('hs37d5+mouse', 'hs37d5_GRCm38mm') AND
mwp2.referenceGenome.name = 'hs37d5' AND
stpp1.project = project_1 AND
stpp2.project = project_1 AND
stpp1.sampleType = sampleType_1 AND
stpp2.sampleType = sampleType_2 AND
stpp1.category = :disease AND
stpp2.category = :control AND
NOT EXISTS (
FROM
SamplePair
WHERE
mergingWorkPackage1 = mwp1 AND
mergingWorkPackage2 = mwp2)
""", [
            disease: SampleType.Category.DISEASE,
            control: SampleType.Category.CONTROL,
    ], [readOnly: true])
    return queryResults.collect {
        SamplePair.createInstance(
                mergingWorkPackage1: it[0],
                mergingWorkPackage2: it[1],
        )
    }
}


SamplePair.withTransaction {
    def samplePairs = findMissingXenograftDiseaseControlSamplePairs()
    samplePairs*.save(flush: true, failOnError: true)
    println samplePairs.join('\n')
}
''
