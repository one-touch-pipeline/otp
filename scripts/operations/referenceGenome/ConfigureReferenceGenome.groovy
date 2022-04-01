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

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

String projectName = ''
assert projectName: "no project given"

Collection<SeqType> seqTypes = [
        //SeqTypeService.wholeGenomePairedSeqType,
        //SeqTypeService.exomePairedSeqType,
        //SeqTypeService.wholeGenomeBisulfitePairedSeqType,
        //SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType,
]

Collection<SampleType> sampleTypes = [
        //null,
        //exactlyOneElement(SampleType.findAllByName('')),
        //exactlyOneElement(SampleType.findAllByName('XENOGRAFT')),
]
//sampleTypes.addAll(SampleType.createCriteria().list { like 'name', 'XENOGRAFT%' })

/**
 * name of reference genome. Currently the following values are possible:
 * - hg19: human reference genome hg19
 * - hs37d5: human reference genome hs37d5
 * - hs37d5+mouse: human-mouse reference genome from CO
 * - GRCm38mm10: mouse reference genome
 * - hs37d5_GRCm38mm: human (hs37d5) - mouse (GRCm38mm) reference genome
 *
 * with phix:
 * - 1KGRef_PhiX: human reference genome hs37d5 with phix
 * - GRCm38mm10_PhiX: mouse reference genome with Phix
 * - hs37d5_GRCm38mm_PhiX:  human (hs37d5) - mouse (GRCm38mm) reference genome
 *
 * WGBS reference genomes:
 * - methylCtools_GRCm38mm10_PhiX_Lambda: wgbs reference genome for GRCm38mm10
 * - methylCtools_hs37d5_PhiX_Lambda: wgbs reference genome for hs37d5
 * - methylCtools_mm10_UCSC_PhiX_Lambda: wgbs reference genome for ?
 * - methylCtools_hs37d5_GRCm38mm10_PhiX_Lambda: wgbs reference genome for human (hs37d5) - mouse (GRCm38mm)
 *
 * For a full list, execute "de.dkfz.tbi.otp.ngsdata.ReferenceGenome.list()*.name" on a groovy web console
 */
String refGenName = ''

/**
 * Must be set for projects which are aligned with the PanCan alignment workflow, otherwise must be null.
 * possible Values, depends on reference genome:
 *
 * - hg19:
 *   - hg19_1-22_X_Y_M.fa.chrLenOnlyACGT.tab
 *
 * - hs37d5:
 *   - hs37d5.fa.chrLenOnlyACGT_realChromosomes.tab
 *   - hs37d5.fa.chrLenOnlyACGT.tab
 *
 * - hs37d5+mouse:
 *   - hg19_GRCh37_mm10.fa.chrLenOnlyACGT_realChromosomes.tab
 *   - hg19_GRCh37_mm10.fa.chrLenOnlyACGT.tab
 *
 * - GRCm38mm10:
 *   - GRCm38mm10.fa.chrLenOnlyACGT_realChromosomes.tab
 *   - GRCm38mm10.fa.chrLenOnlyACGT.tab
 *
 * - hs37d5_GRCm38mm:
 *   - hs37d5_GRCm38mm.fa.chrLenOnlyACGT_realChromosomes.tab
 *   - hs37d5_GRCm38mm.fa.chrLenOnlyACGT.tab
 *
 * - 1KGRef_PhiX:
 *   - hs37d5_PhiX.fa.chrLenOnlyACGT_realChromosomes.tab
 *   - hs37d5_PhiX.fa.chrLenOnlyACGT.tab
 *
 * - methylCtools_GRCm38mm10_PhiX_Lambda:
 *   - GRCm38mm10_PhiX_Lambda.fa.chrLenOnlyACGT.tab
 *
 * - methylCtools_hs37d5_PhiX_Lambda:
 *   - hs37d5_PhiX_Lambda.fa.chrLenOnlyACGT.tab
 *
 * - methylCtools_mm10_UCSC_PhiX_Lambda:
 *   - mm10_PhiX_Lambda.fa.chrLenOnlyACGT.tab
 *
 * - GRCm38mm10_PhiX:
 *   - GRCm38mm10.fa.chrLenOnlyACGT.tab
 *   - GRCm38mm10.fa.chrLenOnlyACGT_realChromosomes.tab
 *
 * - hs37d5_GRCm38mm_PhiX:
 *   - hs37d5_GRCm38mm.fa.chrLenOnlyACGT_realChromosomes.tab
 *   - hs37d5_GRCm38mm.fa.chrLenOnlyACGT.tab
 *
 * - methylCtools_hs37d5_GRCm38mm10_PhiX_Lambda:
 *   - hs37d5_GRCm38mm10_PhiX.chrLenOnlyACGT.tab
 *
 * Usually the realChromosome files are to prefer.
 * The list could be create with the script "statSizeFileList"
 */
String statSizeFileName = ''

//RNA should not be configured over this script. Use the GUI instead!
List<SeqType> blackListedSeqTypes = [
        SeqTypeService.rnaPairedSeqType,
        SeqTypeService.rnaSingleSeqType,
]

Project project = atMostOneElement(Project.findAllByName(projectName))

assert !(seqTypes.any { it in blackListedSeqTypes }): "Blacklisted seqTypes selected, cant continue!"

String rgpstToStringHelper(ReferenceGenomeProjectSeqType rgpst) {
    return "${rgpst.referenceGenome.name} | ${rgpst.statSizeFileName}"
}

ReferenceGenome.withTransaction {
    assert refGenName
    assert statSizeFileName == null || !statSizeFileName.empty
    ReferenceGenome referenceGenome = exactlyOneElement(ReferenceGenome.findAllByName(refGenName))

    println "Changes for '${project.name}'"
    seqTypes.each { seqType ->
        sampleTypes.each { sampleType ->
            println String.format("${seqType.toString()},  ${sampleType?.name}")
            Map keyProperties = [
                    project: project,
                    seqType: seqType,
                    sampleType: sampleType,
            ]

            ReferenceGenomeProjectSeqType oldRGPST = atMostOneElement(ReferenceGenomeProjectSeqType.findAllWhere(keyProperties + [deprecatedDate: null]))
            if (oldRGPST) {
                oldRGPST.deprecatedDate = new Date()
                assert oldRGPST.save(flush: true)
                println "  - deprecate: ${rgpstToStringHelper(oldRGPST)} -> on: ${oldRGPST.deprecatedDate}"
            }
            ReferenceGenomeProjectSeqType newRGPST = new ReferenceGenomeProjectSeqType(keyProperties + [
                    referenceGenome: referenceGenome,
                    statSizeFileName: statSizeFileName,
            ])
            assert newRGPST.save(flush: true)
            println "  - create   : ${rgpstToStringHelper(newRGPST)}"
        }
    }
    assert false: "rollback for debug"
}

''
