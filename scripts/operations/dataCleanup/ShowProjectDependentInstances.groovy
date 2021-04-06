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


import de.dkfz.tbi.otp.project.ProjectInfo
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.qcTrafficLight.*
import de.dkfz.tbi.otp.infrastructure.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

/**
 * Prints out an overview of all domain instances connected to the Project
 */


String projectName = ""


List<String> output = []
Project project = exactlyOneElement(Project.findAllByName(projectName))

Individual.findAllByProject(project).each { Individual individual ->
    output << "Individual: ${individual}"

    output << "\tClusterJobs:"
    ClusterJob.findAllByIndividual(individual).each { ClusterJob clusterJob ->
        output << "\t\t${clusterJob}"
    }

    Sample.findAllByIndividual(individual).each { Sample sample ->
        output << "\tSamples: ${sample}"

        SampleIdentifier.findAllBySample(sample).each { SampleIdentifier sampleIdentifier ->
            output << "\t\t\t${sampleIdentifier}"
        }

        SeqTrack.findAllBySample(sample).each { SeqTrack seqTrack ->
            output << "\t\t\t${seqTrack}"
        }

        SeqScan.findAllBySample(sample).each { SeqScan seqScan ->
            output << "\t\tSeqScan ${seqScan}"

            output << "\t\t\tMergingLogs:"
            List<MergingLog> mergingLogs = MergingLog.findAllBySeqScan(seqScan)
            mergingLogs.each { MergingLog mergingLog ->
                output << "\t\t\t${mergingLog}"
            }

            output << "\t\t\tMergedAlignmentDataFile:"
            if (mergingLogs) {
                MergedAlignmentDataFile.findAllByMergingLogInList(mergingLogs).each { MergedAlignmentDataFile mergedAlignmentDataFile ->
                    output << "\t\t\t${mergedAlignmentDataFile}"
                }
            }
        }
    }
    output << "\n"
}

[
        DataFile,
        ReferenceGenomeProjectSeqType,
        MergingCriteria,
        ProcessingThresholds,
        SampleTypePerProject,
        ConfigPerProjectAndSeqType,
        UserProjectRole,
        ProjectInfo,
        QcThreshold,
].each {
    output << "${it.simpleName}"
    output << it.findAllByProject(project).join("\n")
    output << ""
}

// Processing Options are only checked on currently existing ones
output << "ProcessingOption"
output << ProcessingOption.findAllByProjectAndNameInList(project, ProcessingOption.OptionName.values() as List).join("\n")

println(output.join("\n"))
''
