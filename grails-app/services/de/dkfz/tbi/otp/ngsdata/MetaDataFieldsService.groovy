package de.dkfz.tbi.otp.ngsdata
import de.dkfz.tbi.otp.ngsdata.*


class MetaDataFieldsService {

    public List listExomeEnrichmentKitWithAliases(){
        return ExomeEnrichmentKit.list(sort: "name", order: "asc").collect {
            [it, ExomeEnrichmentKitSynonym.findAllByExomeEnrichmentKit(it,[sort: "name", order: "asc"])]
        }
    }

    public List listAntibodyTarget(){
        return AntibodyTarget.list(sort: "name", order: "asc")
    }

    public List listSeqCenter(){
        return SeqCenter.list(sort: "name", order: "asc")
    }
}