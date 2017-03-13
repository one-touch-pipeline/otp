package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

class RoddyWorkflowConfigService {

    static void loadPanCanConfigAndTriggerAlignment(Project project, SeqType seqType, String pluginVersionToUse, Pipeline pipeline, String configFilePath, String configVersion, boolean adapterTrimmingNeeded, Individual individual) {
        assert individual : "The individual is not allowed to be null"

        RoddyBamFile.withTransaction {
            RoddyWorkflowConfig.importProjectConfigFile(project, seqType, pluginVersionToUse, pipeline, configFilePath, configVersion, adapterTrimmingNeeded, individual)

            List<MergingWorkPackage> mergingWorkPackages = MergingWorkPackage.createCriteria().list {
                eq('seqType', seqType)
                eq('pipeline', pipeline)
                sample {
                    eq('individual', individual)
                }
            }

            assert mergingWorkPackages : "no MWP found"

            println "Old roddyBamFiles are marked as withdrawn"
            RoddyBamFile.findAllByWorkPackageInList(mergingWorkPackages)*.withdraw()

            println "Realignment will be triggered for bam files of ${individual}"
            mergingWorkPackages*.needsProcessing = true
            mergingWorkPackages*.save(flush: true)
        }
    }
}
