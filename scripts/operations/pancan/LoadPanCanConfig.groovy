/**
 * A script to load a the roddy pancan project config into the database.
 */

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*

String projectName = ''

// The PID is optional. It must only be set if the config is individual specific.
// If the PID is set, alignment will automatically triggered for that PID.
String individualPid = ''


//The roddy plugin version. It needs to be a valid value for Roddy.
//For example: AlignmentAndQCWorkflows:1.1.39
String pluginVersionToUse = ''


//The config version used. Has to be match the expression: '^v\d+_\d+$'
//The first version of a file has 'v1_0
String configVersion = 'v1_0'

/**
 * The seq type for which roddy should be configured, could be:
 * * WGS
 * * WES
 * * WGBS
 * * WGBSTAG
 * * RNA
 */
String seqTypeRoddyName = ''

String libraryLayout = 'PAIRED'

/**
 * the complete path to the config file.
 * The file should be located in: $OTP_ROOT_PATH/$PROJECT/configFiles/$Workflow/
 * The file should be named as: ${Pipeline}_${seqType.roddyName}_${seqType.libraryLayout}_${WorkflowVersion}_v${fileVersion}.xml
 *
 * for example: $OTP_ROOT_PATH/$PROJECT/configFiles/PANCAN_ALIGNMENT/PANCAN_ALIGNMENT_WES_1.1.39_v1_0.xml
 */
String configFilePath = ''


boolean adapterTrimmingNeeded = false

//-----------------------
RoddyWorkflowConfigService roddyWorkflowConfigService = ctx.roddyWorkflowConfigService
String panCanAlignmentDeciderBeanName = AlignmentDeciderBeanNames.PAN_CAN_ALIGNMENT.bean

LogThreadLocal.withThreadLog(System.out, { Project.withTransaction {
    assert projectName
    assert pluginVersionToUse
    assert configFilePath
    assert seqTypeRoddyName
    assert libraryLayout

    assert configFilePath.endsWith('xml')
    assert new File(configFilePath).exists()

    SeqType seqType = CollectionUtils.exactlyOneElement(SeqType.findAllByRoddyNameAndLibraryLayout(seqTypeRoddyName, libraryLayout))

    Pipeline pipeline = Pipeline.Name.forSeqType(seqType).pipeline

    String individualPidPath = individualPid ? "${individualPid}/" : ''
    assert configFilePath ==~ ".*/configFiles/${pipeline.name.name()}/${individualPidPath}${pipeline.name.name()}_${seqType.roddyName}_${seqType.libraryLayout}_\\d+.\\d+.\\d+_v\\d+_\\d+.xml"

    Project project = CollectionUtils.exactlyOneElement(Project.findAllByName(projectName))

    if (!individualPid) {
        roddyWorkflowConfigService.importProjectConfigFile(
                project,
                seqType,
                pluginVersionToUse,
                pipeline,
                configFilePath,
                configVersion,
                adapterTrimmingNeeded,
        )
    } else {
        Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(individualPid))
        roddyWorkflowConfigService.loadPanCanConfigAndTriggerAlignment(
                project,
                seqType,
                pluginVersionToUse,
                pipeline,
                configFilePath,
                configVersion,
                adapterTrimmingNeeded,
                individual,
        )
    }

    assert ctx[panCanAlignmentDeciderBeanName]
    project.alignmentDeciderBeanName = panCanAlignmentDeciderBeanName
    assert project.save(flush: true, failOnError: true)

    println "Config file loaded."
    println "Don't forget to also configure the reference genome (ConfigureReferenceGenome.groovy)."
}})
