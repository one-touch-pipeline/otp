/**
 * A script to load a the roddy pancan project config into the database.
 */

import de.dkfz.tbi.otp.dataprocessing.Workflow
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.CollectionUtils



String projectName = ''


//The roddy plugin version. It needs to be a valid value for Roddy.
//For example: QualityControlWorkflows:1.0.178
String pluginVersionToUse = ''


//The config version used. Has to be match the expression: '^v\d+_\d+$'
//The first version of a file has 'v1_0
String configVersion = 'v1_0'


/**
 * the complete path to the config file.
 * The file should be located in: $OTP_ROOT_PATH/$PROJECT/configFiles/$Workflow/
 * The file should be named as: ${Workflow}_${WorkflowVersion}_v${fileVersion}.xml
 *
 * for example: $OTP_ROOT_PATH/$PROJECT/configFiles/PANCAN_ALIGNMENT/PANCAN_ALIGNMENT_1.0.178_v1_0.xml
 */
String configFilePath = ''


//-----------------------

String panCanAlignmentDeciderBeanName = 'panCanAlignmentDecider'

Project.withTransaction {
    assert projectName
    assert pluginVersionToUse
    assert configFilePath

    assert configFilePath.endsWith('xml')
    assert new File(configFilePath).exists()
    assert configFilePath ==~ '$OTP_ROOT_PATH/.*/configFiles/PANCAN_ALIGNMENT/PANCAN_ALIGNMENT_\\d+.\\d+.\\d+_v\\d+_\\d+.xml'

    Project project = CollectionUtils.exactlyOneElement(Project.findAllByName(projectName))

    Workflow workflow = CollectionUtils.exactlyOneElement(Workflow.findAllByTypeAndName(
            Workflow.Type.ALIGNMENT,
            Workflow.Name.PANCAN_ALIGNMENT,
    ))

    RoddyWorkflowConfig.importProjectConfigFile(
            project,
            pluginVersionToUse,
            workflow,
            configFilePath,
            configVersion,
    )

    assert ctx[panCanAlignmentDeciderBeanName]
    project.alignmentDeciderBeanName = panCanAlignmentDeciderBeanName
    assert project.save(flush: true, failOnError: true)

    println "Config file loaded."
    println "Don't forget to also configure the reference genome (ConfigureReferenceGenome.groovy)."
}
