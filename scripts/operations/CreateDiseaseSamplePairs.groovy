import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

/**
 * Creates all not existing possible sample pairs for an project, including disease disease sample pairs.
 */

String projectName = ''

//------------------------------
assert projectName: 'No project Name is given'

Project project = CollectionUtils.exactlyOneElement(Project.findAllByName(projectName))

SamplePair.withTransaction {
    println SamplePair.executeQuery("""
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
              ${
                  SamplePair.mergingWorkPackageEqualProperties.collect {
                      "(mwp1.${it} = mwp2.${it} OR mwp1.${it} IS NULL AND mwp2.${it} IS NULL)"
                  }.join(' AND\n')
              } AND
              mwp1 != mwp2 AND
              mwp1.seqType IN :analysableSeqTypes AND
              project_1 = :project AND
              stpp1.project = project_1 AND
              stpp2.project = project_1 AND
              stpp1.sampleType = sampleType_1 AND
              stpp2.sampleType = sampleType_2 AND
              stpp1.category = :disease AND
              stpp2.category in (:control) AND
              NOT EXISTS (
                FROM
                  SamplePair
                WHERE
                  mergingWorkPackage1 = mwp1 AND
                  mergingWorkPackage2 = mwp2)
            """, [
            disease           : SampleType.Category.DISEASE,
            control           : [SampleType.Category.DISEASE, SampleType.Category.CONTROL],
            analysableSeqTypes: SeqType.getAllAnalysableSeqTypes(),
            project           : project,
    ], [readOnly: true]).collect {
        SamplePair samplePair = SamplePair.createInstance(
                mergingWorkPackage1: it[0],
                mergingWorkPackage2: it[1]
        )
        samplePair.save()
        println samplePair
        return samplePair
    }.size()
    assert false
}
''
