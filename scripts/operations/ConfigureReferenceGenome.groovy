import static de.dkfz.tbi.otp.utils.CollectionUtils.*
import de.dkfz.tbi.otp.ngsdata.*


Project project = exactlyOneElement(Project.findAllByName(''))
Collection<SeqType> seqTypes = [
        //exactlyOneElement(SeqType.findAllByNameAndLibraryLayout('WHOLE_GENOME', 'PAIRED')),
        //exactlyOneElement(SeqType.findAllByNameAndLibraryLayout('EXON', 'PAIRED')),
]
Collection<SampleType> sampleTypes = [
        //null,
        //exactlyOneElement(SampleType.findAllByName('')),
]
//sampleTypes.addAll(SampleType.createCriteria().list { like 'name', 'XENOGRAFT%' })
/**
 * name of reference genome. Currently the following values are possible:
 * - hg19: human reference genome hg19
 * - hs37d5: human reference genome hs37d5
 * - hs37d5+mouse:  human-mouse reference genome from CO
 * - GRCm38mm10: mouse reference genome
 * - hs37d5_GRCm38mm:  human (hs37d5) - mouse (GRCm38mm) reference genome
 *
 * For a full list, execute "de.dkfz.tbi.otp.ngsdata.ReferenceGenome.list()*.name" on a groovy web console
 */
String refGenName = ''

/**
 * Must be set for projects which are aligned with the PanCan alignment workflow, otherwise must be null.
 * possible Values, depends on reference genome:
 * - hg19:
 *   - hg19_1-22_X_Y_M.fa.chrLenOnlyACGT.tab
 * - hs37d5:
 *   - hs37d5.fa.chrLenOnlyACGT_realChromosomes.tab
 *   - hs37d5.fa.chrLenOnlyACGT.tab
 * - hs37d5+mouse:
 *   - hg19_GRCh37_mm10.fa.chrLenOnlyACGT_realChromosomes.tab
 *   - hg19_GRCh37_mm10.fa.chrLenOnlyACGT.tab
 * - GRCm38mm10:
 *   - GRCm38mm10.fa.chrLenOnlyACGT_realChromosomes.tab
 *   - GRCm38mm10.fa.chrLenOnlyACGT.tab
 * - hs37d5_GRCm38mm:
 *   - hs37d5_GRCm38mm.fa.chrLenOnlyACGT_realChromosomes.tab
 *   - hs37d5_GRCm38mm.fa.chrLenOnlyACGT.tab
 *
 * Usually the realChromosome files are to prefer.
 * The list could be create with the script "statSizeFileList"
 */
String statSizeFileName = ''


ReferenceGenome.withTransaction {
    assert refGenName
    assert statSizeFileName == null || !statSizeFileName.empty
    ReferenceGenome referenceGenome = exactlyOneElement(ReferenceGenome.findAllByName(refGenName))
    seqTypes.each { seqType ->
        sampleTypes.each { sampleType ->
            Map keyProperties = [
                    project: project,
                    seqType: seqType,
                    sampleType: sampleType,
            ]
            ReferenceGenomeProjectSeqType oldReferenceGenomeProjectSeqType = atMostOneElement(
                    ReferenceGenomeProjectSeqType.findAllWhere(keyProperties + [deprecatedDate: null]))
            if (oldReferenceGenomeProjectSeqType) {
                oldReferenceGenomeProjectSeqType.deprecatedDate = new Date()
                assert oldReferenceGenomeProjectSeqType.save(flush: true, failOnError: true)
                println "Deprecated ${oldReferenceGenomeProjectSeqType}"
            }
            ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = new ReferenceGenomeProjectSeqType(keyProperties + [
                    referenceGenome: referenceGenome,
                    statSizeFileName: statSizeFileName,
            ])
            assert referenceGenomeProjectSeqType.save(flush: true, failOnError: true)
            println "Created ${referenceGenomeProjectSeqType}"
        }
    }
}
println ''
