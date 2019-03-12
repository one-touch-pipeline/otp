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

import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

/**
 * Creates all not existing possible sample pairs for an project, including disease disease sample pairs.
 */

//------------------------------
//input area
String projectName = ''


//------------------------------
//work area

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
            analysableSeqTypes: SeqTypeService.getAllAnalysableSeqTypes(),
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
