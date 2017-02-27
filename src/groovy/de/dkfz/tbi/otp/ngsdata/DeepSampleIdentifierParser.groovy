package de.dkfz.tbi.otp.ngsdata

import java.util.regex.Matcher
import org.springframework.stereotype.Component

@Component
class DeepSampleIdentifierParser implements SampleIdentifierParser{

    public DefaultParsedSampleIdentifier tryParse(String sampleIdentifier) {
        Matcher matcher = sampleIdentifier =~ createRegex(false)
        if (matcher && (matcher.group('libraryStrategy') != 'NOMe')) {
            String replicateNumber = (matcher.group('libraryStrategy') == 'WGBS') ? '1' : matcher.group('replicateNumber')
            return new DefaultParsedSampleIdentifier(
                    'DEEP',
                    matcher.group('pid'),
                    "Replicate${replicateNumber}",
                    sampleIdentifier,
            )
        }
        return null

    }

    protected String createRegex(boolean hasOptChange) {
        String subproject = "(4[1-4]|5[1-3]|0[012])"
        String species = "(H|M)"
        String sex = "(f|m)"
        String donorNr = "([0-9]([0-9]|[ab]))"
        String cellLines = "(HepaRG|HepG2|MCF10A)"
        String cellLineBroadInst = "(3T3L1)"
        String organ = "(Bl|Co|Il|In|LP|Li|SF|WE|WS|WM|Br|Bm|NN)"
        String cellType = "(Ad|Al|As|CM|Ec|EM|Fi|He|HG|HR|Ku|Ma|Mc|Mo|Mu|NP|PM|TA|Th|Ti|TM4|TM8|TN|TN8|TR|T8)"
        String disease = "(CD|Ci|Ct|C[0-9]|CC|Db[0-9]|D[1-9]|Oa|OC|OS|PH|PS|RA|RD|SC|Si|SL|SO|SP[1-9]|St|TE|TO|T[1-9]|UC|Sh|CS)"
        String diseaseSuffix = "([0-9])"
        String libraryStrategy = "(ATAC|CTCF|DNase|H3K27ac|H3K27me3|H3K36me3|H3K4me1|H3K4me2|H3K4me3|H3K9me3|H3K122ac|Input|mRNA|NOMe|PpargAb[12]|reH3K4me3|reH3K27me3|RRBS|snRNA|tRNA|WGBS)"
        String sequenceCenter = "(B|E|F|K|M|S|I|R)"
        String replicateNr = "([1-9])"
        String optChange = ""
        if (hasOptChange){
            optChange = "(\\swas\\schanged\\son\\s\\d{4}-\\d{2}-\\d{2})?" // in case of sample/lane swap
        }

        /*
        - exception for cellLines HepaRG, HepG2 or MCF10A, an additional number (diseaseSuffix) after the disease is allowed also no species, sex or donorNr are given
        - exception for samples of cellLineBroadInst 3T3L1, no donorNr is given
        */

        return "^" +
                "(?<pid>${subproject}_" +
                "((${species}${sex}(${donorNr}|${cellLineBroadInst})_${organ}${cellType}_${disease})|(${cellLines}_${organ}${cellType}_${disease}${diseaseSuffix})))_" +
                "(?<libraryStrategy>${libraryStrategy})_" +
                "${sequenceCenter}_" +
                "(?<replicateNumber>${replicateNr})" +
                "${optChange}" +
                /$/
    }
}
