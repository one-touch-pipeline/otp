/**
 * Shows all not passed threshold for bam files set to blocked for a selected project.
 *
 * The project is called for a given project (input area).
 *
 * The script search for bam files of following criteria:
 * - asociated to the given project
 * - QcTrafficLightStatus is set to BLOCKED
 * - bam file is not withdrawn
 * - bam file is the latest for the mergingWorkPackage
 *
 * Fo these bam files all not passed thressholds are listed
 */

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.*
import de.dkfz.tbi.otp.utils.*

//----------------------------------------------------
//input area

String projectName = ""

//----------------------------------------------------
//processing

assert projectName: 'No Project name given'

Project project = Project.findByName(projectName)
assert project: "Project ${projectName} not found"

//K20K-JRJ7D2 und K20K-ZFA224

List<AbstractMergedBamFile> bamFiles = AbstractMergedBamFile.createCriteria().list {
    workPackage {
        sample {
            individual {
                eq('project', project)
            }
        }
    }
    eq('qcTrafficLightStatus', AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED)
    eq('withdrawn', false)
}.findAll { AbstractMergedBamFile bamFile ->
    bamFile.isMostRecentBamFile()
}

println "Found ${bamFiles.size()} blocked bam files"

bamFiles.each { AbstractBamFile bamFile ->
    println "Bamfile ${bamFile}"

    AbstractQualityAssessment qc = CollectionUtils.exactlyOneElement(
            bamFile.seqType.name == 'RNA' ?
                    RnaQualityAssessment.createCriteria().list {
                        qualityAssessmentMergedPass {
                            eq('abstractMergedBamFile', bamFile)
                        }
                        eq('chromosome', RoddyQualityAssessment.ALL)
                    } :
                    RoddyMergedBamQa.createCriteria().list {
                        qualityAssessmentMergedPass {
                            eq('abstractMergedBamFile', bamFile)
                        }
                        eq('chromosome', RoddyQualityAssessment.ALL)
                    }
    )

    List<QcThreshold> qcThresholds = QcThreshold.getValidQcPropertyForQcClass(qc.class.name).collect { String property ->
        QcThreshold.findByQcClassAndSeqTypeAndQcProperty1AndProject(qc.class.name, bamFile.seqType, property, bamFile.project) ?:
                QcThreshold.findByQcClassAndSeqTypeAndQcProperty1AndProjectIsNull(qc.class.name, bamFile.seqType, property)
    }.findAll { QcThreshold qcThreshold ->
        return qcThreshold?.qcPassed(qc) == QcThreshold.ThresholdLevel.ERROR
    }
    println "    Found thressholds (${qcThresholds.size()}):"
    qcThresholds.each {
        println "        ${it.qcProperty1}: ${qc[it.qcProperty1]}"
    }
}

''
