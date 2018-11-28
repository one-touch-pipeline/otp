import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
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
            MergedAlignmentDataFile.findAllByMergingLogInList(mergingLogs).each { MergedAlignmentDataFile mergedAlignmentDataFile ->
                output << "\t\t\t${mergedAlignmentDataFile}"
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
output << ProcessingOption.findAllByProjectAndNameInList(project, ProcessingOption.OptionName.values()).join("\n")

println(output.join("\n"))
''
