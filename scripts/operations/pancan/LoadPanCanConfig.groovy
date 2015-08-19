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
//For example: QualityControlWorkflows:1.0.177
String pluginVersionToUse = ''


/**
 * the complete path to the config file.
 * The file should be located in: $OTP_ROOT_PATH/$PROJECT/configFiles/$Workflow/
 * The file should be named as: ${Workflow}_${WorkflowVersion}_v${fileVersion}.xml
 *
 * for example: $OTP_ROOT_PATH/$PROJECT/configFiles/PANCAN_ALIGNMENT/PANCAN_ALIGNMENT_1.0.177_v1.0.xml
 */
String configFilePath = ''


/**
 * name of reference genome. Currently the following values possible:
 * - hg19: human reference genome hg19
 * - hs37d5: human reference genome hs37d5
 * - hs37d5+mouse:  human-mouse reference genome from CO
 * - GRCm38mm10: mouse reference genome
 * - hs37d5_GRCm38mm:  human (hs37d5) - mouse (GRCm38mm) reference genome
 *
 * For a full list, execute "de.dkfz.tbi.otp.ngsdata.ReferenceGenome.list()*.name" on a groovy web console
 */
String refGenName = ''

//for example: hs37d5.fa.chrLenOnlyACGT_realChromosomes.tab
String statSizeFileName = ''


String seqTypeName = "WHOLE_GENOME"

//-----------------------

String panCanAlignmentDeciderBeanName = 'panCanAlignmentDecider'

Project.withTransaction {
    assert projectName
    assert pluginVersionToUse
    assert configFilePath
    assert refGenName
    assert statSizeFileName

    assert configFilePath.endsWith('xml')
    assert new File(configFilePath).exists()
    assert configFilePath ==~ '$OTP_ROOT_PATH/.*/configFiles/PANCAN_ALIGNMENT/PANCAN_ALIGNMENT_\\d+.\\d+.\\d+_v\\d+.\\d+.xml'

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
    )

    assert ctx[panCanAlignmentDeciderBeanName]
    project.alignmentDeciderBeanName = panCanAlignmentDeciderBeanName
    assert project.save(flush: true, failOnError: true)

    ReferenceGenome referenceGenome = CollectionUtils.exactlyOneElement(ReferenceGenome.findAllByName(refGenName))

    SeqType seqType = CollectionUtils.exactlyOneElement(SeqType.findAllByNameAndLibraryLayout(seqTypeName, SeqType.LIBRARYLAYOUT_PAIRED))

    ReferenceGenomeProjectSeqType oldReferenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.findByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(project, seqType)
    if (oldReferenceGenomeProjectSeqType) {
        oldReferenceGenomeProjectSeqType.deprecatedDate = new Date()
        assert oldReferenceGenomeProjectSeqType.save(flush: true, failOnError: true)
    }

    ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = new ReferenceGenomeProjectSeqType(
            project:  project,
            seqType: seqType,
            referenceGenome: referenceGenome,
            sampleType: null,
            statSizeFileName: statSizeFileName,
    )
    assert referenceGenomeProjectSeqType.save(flush: true, failOnError: true)
}
