package de.dkfz.tbi.otp.ngsdata
import de.dkfz.tbi.otp.ngsdata.*


class MetaDataFieldsService {

    public List listExomeEnrichmentKitWithAliases(){
        return ExomeEnrichmentKit.list(sort: "name", order: "asc").collect {
            [it, ExomeEnrichmentKitSynonym.findAllByExomeEnrichmentKit(it,[sort: "name", order: "asc"])]
        }
    }
}