import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.qcTrafficLight.*
import de.dkfz.tbi.otp.utils.*

/**
 * Script show for one individual, sampleType and seqType, which thresholds are failing
 */


String pid = ""
String sampleTypeName = ""
String seqTypeName = ""
String libraryLayout = "PAIRED"

//-----------------


def bamFile = CollectionUtils.exactlyOneElement(AbstractMergedBamFile.createCriteria().list {
    workPackage {
        sample {
            individual {
                eq('pid', pid)
            }
            sampleType {
                eq('name', sampleTypeName)
            }
        }
        seqType {
            eq('name', seqTypeName)
            eq('libraryLayout', libraryLayout)
        }
    }
}.findAll {
    it.isMostRecentBamFile()
})
println bamFile

def qa = CollectionUtils.exactlyOneElement(RoddyQualityAssessment.createCriteria().list {
    qualityAssessmentMergedPass {
        eq('abstractMergedBamFile', bamFile)
    }
    eq('chromosome', 'all')
}.findAll {
    (it instanceof RoddyMergedBamQa) || it.roddyBamFile.seqType.isRna()
})
println qa


println "\n\nfailed thresholds"
QcThreshold.getValidQcPropertyForQcClass(qa.class.name).findAll { String property ->
    QcThreshold qcThreshold =
            QcThreshold.findByQcClassAndSeqTypeAndQcProperty1AndProject(qa.class.name, bamFile.seqType, property, bamFile.project) ?:
                    QcThreshold.findByQcClassAndSeqTypeAndQcProperty1AndProjectIsNull(qa.class.name, bamFile.seqType, property)
    return qcThreshold?.qcPassed(qa) == QcThreshold.ThresholdLevel.ERROR
}.each {
    println "    ${it} = ${qa[it]}"
}

''
