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
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.qcTrafficLight.*
import de.dkfz.tbi.otp.utils.*

//----------------------------------------------------
//input area

String projectName = ""

//----------------------------------------------------
//processing

assert projectName: 'No Project name given'

Project project = CollectionUtils.atMostOneElement(Project.findAllByName(projectName))
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
        CollectionUtils.atMostOneElement(QcThreshold.findAllByQcClassAndSeqTypeAndQcProperty1AndProject(qc.class.name, bamFile.seqType, property, bamFile.project)) ?:
                CollectionUtils.atMostOneElement(QcThreshold.findAllByQcClassAndSeqTypeAndQcProperty1AndProjectIsNull(qc.class.name, bamFile.seqType, property))
    }.findAll { QcThreshold qcThreshold ->
        return qcThreshold?.qcPassed(qc) == QcThreshold.ThresholdLevel.ERROR
    }
    println "    Found thressholds (${qcThresholds.size()}):"
    qcThresholds.each {
        println "        ${it.qcProperty1}: ${qc[it.qcProperty1]}"
    }
}

''
