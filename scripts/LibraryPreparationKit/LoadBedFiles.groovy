import de.dkfz.tbi.otp.ngsdata.*
import static org.springframework.util.Assert.*
import de.dkfz.tbi.ngstools.bedUtils.*

BedFileService bedFileService = ctx.bedFileService

/**
 * This script loads bed files and useful meta information of the
 * bed files into OTP's database. Integration tests for this script
 * exists: de.dkfz.tbi.otp.ngsdata.LoadBedFileScriptTests
 * When updating bedFilesToLoad the test must be updated as well.
 *
 */
class Input {
    String bedName
    String refGenName
    String kitName
}

// bedFileToLoad must be kept synchronous to bedFilesToLoad in de.dkfz.tbi.otp.ngsdata.LoadBedFileScriptTests
List<Input> bedFilesToLoad = [
        new Input(bedName: "Agilent3withoutUTRs_chr.bed", refGenName: "hg19", kitName: "Agilent SureSelect V3"),
        new Input(bedName: "Agilent3withoutUTRs_plain.bed", refGenName: "hs37d5", kitName: "Agilent SureSelect V3"),
        new Input(bedName: "Agilent3withoutUTRs_plain_xenograft_GRCm38mm.bed", refGenName: "hs37d5_GRCm38mm", kitName: "Agilent SureSelect V3"),
        new Input(bedName: "Agilent3withoutUTRs_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "Agilent SureSelect V3"),
        new Input(bedName: "Agilent4withoutUTRs_chr.bed", refGenName: "hg19", kitName: "Agilent SureSelect V4"),
        new Input(bedName: "Agilent4withoutUTRs_plain.bed", refGenName: "hs37d5", kitName: "Agilent SureSelect V4"),
        new Input(bedName: "Agilent4withoutUTRs_plain_xenograft_GRCm38mm.bed", refGenName: "hs37d5_GRCm38mm", kitName: "Agilent SureSelect V4"),
        new Input(bedName: "Agilent4withoutUTRs_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "Agilent SureSelect V4"),
        new Input(bedName: "Agilent4withUTRs_chr.bed", refGenName: "hg19", kitName: "Agilent SureSelect V4+UTRs"),
        new Input(bedName: "Agilent4withUTRs_plain.bed", refGenName: "hs37d5", kitName: "Agilent SureSelect V4+UTRs"),
        new Input(bedName: "Agilent4withUTRs_plain_xenograft_GRCm38mm.bed", refGenName: "hs37d5_GRCm38mm", kitName: "Agilent SureSelect V4+UTRs"),
        new Input(bedName: "Agilent4withUTRs_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "Agilent SureSelect V4+UTRs"),
        new Input(bedName: "Agilent5withoutUTRs_chr.bed", refGenName: "hg19", kitName: "Agilent SureSelect V5"),
        new Input(bedName: "Agilent5withoutUTRs_plain.bed", refGenName: "hs37d5", kitName: "Agilent SureSelect V5"),
        new Input(bedName: "Agilent5withoutUTRs_plain_xenograft_GRCm38mm.bed", refGenName: "hs37d5_GRCm38mm", kitName: "Agilent SureSelect V5"),
        new Input(bedName: "Agilent5withoutUTRs_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "Agilent SureSelect V5"),
        new Input(bedName: "Agilent5withUTRs_chr.bed", refGenName: "hg19", kitName: "Agilent SureSelect V5+UTRs"),
        new Input(bedName: "Agilent5withUTRs_plain.bed", refGenName: "hs37d5", kitName: "Agilent SureSelect V5+UTRs"),
        new Input(bedName: "Agilent5withUTRs_plain_xenograft_GRCm38mm.bed", refGenName: "hs37d5_GRCm38mm", kitName: "Agilent SureSelect V5+UTRs"),
        new Input(bedName: "Agilent5withUTRs_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "Agilent SureSelect V5+UTRs"),
        new Input(bedName: "nexterarapidcapture_exome_targetedregions.bed", refGenName: "hs37d5", kitName: "Illumina Nextera Rapid"),
        new Input(bedName: "nexterarapidcapture_exome_targetedregions_xenograft_GRCm38mm.bed", refGenName: "hs37d5_GRCm38mm", kitName: "Illumina Nextera Rapid"),
        new Input(bedName: "Nimblegen_SeqCap_EZ_Exome_v2_chr.bed", refGenName: "hg19", kitName: "Roche NimbleGen SeqCap EZ Human Exome library v2"),
        new Input(bedName: "Nimblegen_SeqCap_EZ_Exome_v2_plain.bed", refGenName: "hs37d5", kitName: "Roche NimbleGen SeqCap EZ Human Exome library v2"),
        new Input(bedName: "Nimblegen_SeqCap_EZ_Exome_v2_plain_xenograft_GRCm38mm.bed", refGenName: "hs37d5_GRCm38mm", kitName: "Roche NimbleGen SeqCap EZ Human Exome library v2"),
        new Input(bedName: "Nimblegen_SeqCap_EZ_Exome_v2_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "Roche NimbleGen SeqCap EZ Human Exome library v2"),
        new Input(bedName: "Nimblegen_SeqCap_EZ_Exome_v3_chr.bed", refGenName: "hg19", kitName: "Roche NimbleGen SeqCap EZ Human Exome library v3"),
        new Input(bedName: "Nimblegen_SeqCap_EZ_Exome_v3_plain.bed", refGenName: "hs37d5", kitName: "Roche NimbleGen SeqCap EZ Human Exome library v3"),
        new Input(bedName: "Nimblegen_SeqCap_EZ_Exome_v3_plain_xenograft_GRCm38mm.bed", refGenName: "hs37d5_GRCm38mm", kitName: "Roche NimbleGen SeqCap EZ Human Exome library v3"),
        new Input(bedName: "Nimblegen_SeqCap_EZ_Exome_v3_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "Roche NimbleGen SeqCap EZ Human Exome library v3"),
        new Input(bedName: "TruSeqExomeTargetedRegions_chr.bed", refGenName: "hg19", kitName: "Illumina TruSeq Exome Enrichment Kit"),
        new Input(bedName: "TruSeqExomeTargetedRegions_plain.bed", refGenName: "hs37d5", kitName: "Illumina TruSeq Exome Enrichment Kit"),
        new Input(bedName: "TruSeqExomeTargetedRegions_plain_xenograft_GRCm38mm.bed", refGenName: "hs37d5_GRCm38mm", kitName: "Illumina TruSeq Exome Enrichment Kit"),
        new Input(bedName: "TruSeqExomeTargetedRegions_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "Illumina TruSeq Exome Enrichment Kit"),
        new Input(bedName: "Agilent_S0447132_Covered.bed", refGenName: "hs37d5", kitName: "Agilent S0447132"),
        new Input(bedName: "Agilent_S0447132_Covered_xenograft_GRCm38mm.bed", refGenName: "hs37d5_GRCm38mm", kitName: "Agilent S0447132"),
        new Input(bedName: "Agilent_S0447132_Covered_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "Agilent S0447132"),

        new Input(bedName: "SureSelect50MbV4_chr.bed", refGenName: "hg19", kitName: "SureSelect50MbV4"),
        new Input(bedName: "SureSelect50MbV4_plain.bed", refGenName: "hs37d5", kitName: "SureSelect50MbV4"),
        new Input(bedName: "SureSelect50MbV4_plain_xenograft_GRCm38mm.bed", refGenName: "hs37d5_GRCm38mm", kitName: "SureSelect50MbV4"),
        new Input(bedName: "SureSelect50MbV4_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "SureSelect50MbV4"),

        new Input(bedName: "SureSelect50MbV5_chr.bed", refGenName: "hg19", kitName: "SureSelect50MbV5"),
        new Input(bedName: "SureSelect50MbV5_plain.bed", refGenName: "hs37d5", kitName: "SureSelect50MbV5"),
        new Input(bedName: "SureSelect50MbV5_plain.bed", refGenName: "hs37d5_GRCm38mm", kitName: "SureSelect50MbV5"),
        new Input(bedName: "SureSelect50MbV5_plain_xenograft.bed", refGenName: "hs37d5+mouse", kitName: "SureSelect50MbV5"),

]


BedFile.withTransaction {
    bedFilesToLoad.each { Input input ->
        BedFile existingBedFile = BedFile.findByFileName(input.bedName)
        if (existingBedFile) {
            println "bedFile with name ${input.bedName} already exists in the db and will be skipped."
            return
        }
        ReferenceGenome referenceGenome = ReferenceGenome.findByName(input.refGenName)
        notNull referenceGenome
        LibraryPreparationKit kit = LibraryPreparationKit.findByName(input.kitName)
        notNull kit
        BedFile bedFileDom = new BedFile(
                fileName: input.bedName,
                referenceGenome: referenceGenome,
                libraryPreparationKit: kit)
        notNull bedFilePath = bedFileService.filePath(bedFileDom)
        // retrieve referenceGenomeEntryNames for a specific referenceGenome
        List<ReferenceGenomeEntry> referenceGenomeEntries = ReferenceGenomeEntry.findAllByReferenceGenome(referenceGenome)
        notEmpty referenceGenomeEntries
        TargetIntervals targetIntervals = TargetIntervalsFactory.create(bedFilePath, referenceGenomeEntries*.name)
        bedFileDom.targetSize = targetIntervals.getBaseCount()
        bedFileDom.mergedTargetSize = targetIntervals.getUniqueBaseCount()
        notNull bedFileDom.validate()
        bedFileDom.save(flush: true)
        println "Added ${input.bedName} to OTP database."
    }
}
