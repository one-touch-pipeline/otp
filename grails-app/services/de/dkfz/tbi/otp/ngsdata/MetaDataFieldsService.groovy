package de.dkfz.tbi.otp.ngsdata
import de.dkfz.tbi.otp.ngsdata.*


class MetaDataFieldsService {

    public List listLibraryPreparationKitWithAliases(){
        return LibraryPreparationKit.list(sort: "name", order: "asc").collect {
            [it, LibraryPreparationKitSynonym.findAllByLibraryPreparationKit(it,[sort: "name", order: "asc"])]
        }
    }

    public List listAntibodyTarget(){
        return AntibodyTarget.list(sort: "name", order: "asc")
    }

    public List listSeqCenter(){
        return SeqCenter.list(sort: "name", order: "asc")
    }

    public List listPlatformAndIdentifier(){
        return SeqPlatform.list(sort: "name", order: "asc").collect {
            [it, SeqPlatformModelIdentifier.findAllBySeqPlatform(it, [sort: "name", order: "asc"])]
        }
    }

    public List listSeqType(){
        return SeqType.list()
    }
}