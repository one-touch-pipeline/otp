package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import grails.converters.*
import org.springframework.validation.*


class MetaDataFieldsController {
    LibraryPreparationKitService libraryPreparationKitService
    SequencingKitLabelService sequencingKitLabelService
    SeqPlatformModelLabelService seqPlatformModelLabelService
    SeqTypeService seqTypeService
    SeqPlatformService seqPlatformService
    SeqCenterService seqCenterService
    LibraryPreparationKitSynonymService libraryPreparationKitSynonymService
    AntibodyTargetService antibodyTargetService


    def index() {

        List libraryPreparationKits = LibraryPreparationKit.list(sort: "name", order: "asc").collect {
            [
                    id: it.id,
                    name : it.name,
                    shortDisplayName : it.shortDisplayName,
                    adapterFile: it.adapterFile,
                    reverseComplementAdapterSequence: it.reverseComplementAdapterSequence,
                    alias: LibraryPreparationKitSynonym.findAllByLibraryPreparationKit(it, [sort: "name", order: "asc"])*.name.join(' | '),
                    referenceGenomesWithBedFiles: BedFile.findAllByLibraryPreparationKit(it, [sort: "referenceGenome.name", order: "asc"])*.referenceGenome*.name.join(' | '),
            ]
        }

        List antiBodyTargets = AntibodyTarget.list(sort: "name", order: "asc").collect {
            [
                    name: it.name
            ]
        }

        List seqCenters = SeqCenter.list(sort: "name", order: "asc").collect {
            [
                    name: it.name,
                    dirName: it.dirName
            ]
        }
        //TODO OTP-2607: adjust handling of seqPlatforms, put handling of seqPlatformGroups on a new page
        List seqPlatforms = SeqPlatform.list().collect {
            [
                    name       : it.name,
                    platformGroup: it.seqPlatformGroups?.name,
                    model      : it.seqPlatformModelLabel?.name,
                    modelAlias : it.seqPlatformModelLabel?.alias?.sort()?.join(' | '),
                    hasModel   : it.seqPlatformModelLabel ? true : false,
                    seqKit     : it.sequencingKitLabel?.name,
                    seqKitAlias: it.sequencingKitLabel?.alias?.sort()?.join(' | '),
                    hasSeqKit  : it.sequencingKitLabel?.name ? true : false
            ]
        }.sort{"${it.platformGroup}, ${it.name}, ${it.model}, ${it.seqKit}"}

        List seqTypes = SeqType.list(sort: "name", order: "asc").collect {
            [
                    name          : it.name,
                    dirName       : it.dirName,
                    libraryLayouts: SeqType.findAllByName(it.name)*.libraryLayout.sort().reverse().join(' | '),
                    layouts       :
                            [
                                    SINGLE   : SeqType.findByNameAndLibraryLayout(it.name, SeqType.LIBRARYLAYOUT_SINGLE) ? true : false,
                                    PAIRED   : SeqType.findByNameAndLibraryLayout(it.name, SeqType.LIBRARYLAYOUT_PAIRED) ? true : false,
                                    MATE_PAIR: SeqType.findByNameAndLibraryLayout(it.name, SeqType.LIBRARYLAYOUT_MATE_PAIR) ? true : false
                            ],
                    displayName   : it.displayName
            ]
        }.unique()

        return [
                antiBodyTargets: antiBodyTargets,
                libraryPreparationKits: libraryPreparationKits,
                seqCenters     : seqCenters,
                seqPlatforms   : seqPlatforms,
                seqTypes       : seqTypes,
        ]
    }

    JSON createLibraryPreparationKit(CreateLibraryPreparationKitCommand cmd) {
        checkErrorAndCallMethod(cmd, { libraryPreparationKitService.createLibraryPreparationKit(cmd.name, cmd.shortDisplayName, cmd.adapterFile, cmd.reverseComplementAdapterSequence) })
    }

    JSON addAdapterFileToLibraryPreparationKit(AddAdapterFileToLibraryPreparationKitCommand cmd) {
        checkErrorAndCallMethod(cmd, { libraryPreparationKitService.addAdapterFileToLibraryPreparationKit(cmd.libraryPreparationKit, cmd.adapterFile) })
    }

    JSON addAdapterSequenceToLibraryPreparationKit(AddAdapterSequenceToLibraryPreparationKitCommand cmd) {
        checkErrorAndCallMethod(cmd, { libraryPreparationKitService.addAdapterSequenceToLibraryPreparationKit(cmd.libraryPreparationKit, cmd.reverseComplementAdapterSequence) })
    }

    JSON createLibraryPreparationKitAlias(CreateLibraryPreparationKitAliasCommand cmd) {
        checkErrorAndCallMethod(cmd, { libraryPreparationKitSynonymService.createLibraryPreparationKitSynonym(cmd.alias, cmd.id) })
    }

    JSON createAntibodyTarget(CreateAntibodyTargetCommand cmd) {
        checkErrorAndCallMethod(cmd, { antibodyTargetService.createAntibodyTarget(cmd.name) })
    }

    JSON createSeqCenter(CreateSeqCenterCommand cmd) {
        checkErrorAndCallMethod(cmd, { seqCenterService.createSeqCenter(cmd.name, cmd.dirName) })
    }

    JSON createSeqPlatform(CreateSeqPlatformCommand cmd) {
        checkErrorAndCallMethod(cmd, { seqPlatformService.createNewSeqPlatform(cmd.platform, cmd.group, cmd.model, cmd.kit) })
    }

    JSON createModelAlias(CreateModelAliasCommand cmd) {
        checkErrorAndCallMethod(cmd, { seqPlatformModelLabelService.addNewAliasToSeqPlatformModelLabel(cmd.id, cmd.alias) })
    }

    JSON createSequencingKitAlias(CreateSequencingKitAliasCommand cmd) {
        checkErrorAndCallMethod(cmd, { sequencingKitLabelService.addNewAliasToSequencingKitLabel(cmd.id, cmd.alias) })
    }

    JSON createSeqType(CreateSeqTypeCommand cmd) {
        if (cmd.hasErrors()) {
            Map data = getErrorData(cmd.errors.getFieldError())
            render data as JSON
            return
        }
        SeqType.withTransaction{
            createSeqTyp(cmd, cmd.type, cmd.dirName, cmd.displayName)
        }
    }

    JSON createLayout(CreateLayoutCommand cmd) {
        if (cmd.hasErrors()) {
            Map data = getErrorData(cmd.errors.getFieldError())
            render data as JSON
            return
        }
        SeqType seqType = SeqType.findByName(cmd.id)
        SeqType.withTransaction{
            createSeqTyp(cmd, seqType.name, seqType.dirName, seqType.displayName)
        }
    }

    private void createSeqTyp(Serializable cmd, String name, String dirName, String displayName) {
        if (cmd.single) {
            checkErrorAndCallMethod(cmd, { seqTypeService.createSeqType(name, dirName, displayName, SeqType.LIBRARYLAYOUT_SINGLE) })
        }
        if (cmd.paired) {
            checkErrorAndCallMethod(cmd, { seqTypeService.createSeqType(name, dirName, displayName, SeqType.LIBRARYLAYOUT_PAIRED) })
        }
        if (cmd.mate_pair) {
            checkErrorAndCallMethod(cmd, { seqTypeService.createSeqType(name, dirName, displayName, SeqType.LIBRARYLAYOUT_MATE_PAIR) })
        }
    }

    private void checkErrorAndCallMethod (Serializable cmd, Closure method) {
        Map data
        if (cmd.hasErrors()) {
            data = getErrorData(cmd.errors.getFieldError())
        } else {
            method()
            data = [success: true]
        }
        render data as JSON
    }

    private Map getErrorData(FieldError errors) {
        return [success: false, error: "'" + errors.getRejectedValue() + "' is not a valid value for '" + errors.getField() + "'. Error code: '" + errors.code + "'"]
    }
}

class CreateLibraryPreparationKitCommand implements Serializable {
        String name
        String shortDisplayName
        String adapterFile
        String reverseComplementAdapterSequence
        static constraints = {
            name(blank: false, validator: {val, obj ->
                if (LibraryPreparationKit.findByName(val) || LibraryPreparationKitSynonym.findByName(val)) {
                    return 'Duplicate'
                }
            })
            shortDisplayName(blank: false, validator: {val, obj ->
                if (LibraryPreparationKit.findByShortDisplayName(val)) {
                    return 'Duplicate'
                }
            })
            adapterFile validator: {val, obj ->
                if (val && !OtpPath.isValidAbsolutePath(val)) {
                    return 'Not a valid file name'
                }
            }
        }
        void setName(String name) {
            this.name = name?.trim()?.replaceAll(" +", " ") //trims and removes additional white spaces
        }
        void setShortDisplayName(String shortDisplayName) {
            this.shortDisplayName = shortDisplayName?.trim()?.replaceAll(" +", " ")
        }
}

class AddAdapterFileToLibraryPreparationKitCommand implements Serializable {
    String adapterFile
    LibraryPreparationKit libraryPreparationKit
    static constraints = {
        adapterFile blank: false, nullable: false, validator: { val, obj ->
            if (!OtpPath.isValidAbsolutePath(val)) {
                return 'Not a valid file name'
            }
        }
        libraryPreparationKit nullable: false
    }
    void setAdapterFile(String adapterFile) {
        this.adapterFile = adapterFile?.trim()?.replaceAll(" +", " ")
    }
}

class AddAdapterSequenceToLibraryPreparationKitCommand implements Serializable {
    String reverseComplementAdapterSequence
    LibraryPreparationKit libraryPreparationKit
    static constraints = {
        reverseComplementAdapterSequence blank: false, nullable: false
        libraryPreparationKit nullable: false
    }
}

class CreateLibraryPreparationKitAliasCommand implements Serializable {
    String alias
    String id
    static constraints = {
        alias(blank: false,validator: {val, obj ->
            if(LibraryPreparationKit.findByName(val) || LibraryPreparationKitSynonym.findByName(val)) {
                return 'Duplicate'
            }})
        id(blank: false)
    }
    void setAlias(String alias) {
        this.alias = alias?.trim()?.replaceAll(" +", " ")
    }
}

class CreateAntibodyTargetCommand implements Serializable {
        String name
        static constraints = {
            name(blank: false, validator: {val, obj ->
                if(AntibodyTarget.findByNameIlike(val)) {
                    return 'Duplicate'
                }
                if(!OtpPath.isValidPathComponent(val)) {
                    return 'Invalid Pattern'
                }
            })
        }
        void setName(String name) {
            this.name = name?.trim()?.replaceAll(" +", " ")
        }
}

class CreateSeqCenterCommand implements Serializable {
        String name
        String dirName
        static constraints = {
            name(blank: false, validator: {val, obj ->
                if (SeqCenter.findByName(val)) {
                    return 'Duplicate'
                }})
            dirName(blank: false, validator: {val, obj ->
                if (SeqCenter.findByDirName(val)) {
                    return 'Duplicate'
                }})
        }
        void setName(String name) {
            this.name = name?.trim()?.replaceAll(" +", " ")
        }
        void setDirName(String dirName) {
            this.dirName = dirName?.trim()?.replaceAll(" +", " ")
        }
}

class CreateSeqPlatformCommand implements Serializable {
        String platform
        String group
        String model
        String kit
        static constraints = {
            platform(blank: false,
                    validator: {val, obj ->
                        if (SeqPlatformService.findSeqPlatform(obj.platform, obj.model, obj.kit)) {
                            return 'Duplicate'
                        }
                    })
            group(blank: false, nullable: true)
            model(blank: false, nullable: false)
            kit(blank: false, nullable: true)
        }
        void setPlatform(String platform) {
            this.platform = platform?.trim()?.replaceAll(" +", " ")
        }
        void setGroup(String group) {
            this.group = group?.trim()?.replaceAll(" +", " ")
            if (this.group.equals("")) {
                this.group = null
            }
        }
        void setModel(String model) {
            this.model = model?.trim()?.replaceAll(" +", " ")
            if (this.model.equals("")) {
                this.model = null
            }
        }
        void setKit(String kit) {
            this.kit = kit?.trim()?.replaceAll(" +", " ")
            if (this.kit.equals("")) {
                this.kit = null
            }
        }
}

class CreateModelAliasCommand implements Serializable {
    String alias
    String id
    static constraints = {
        alias(blank: false,validator: {val, obj ->
            if(SeqPlatformModelLabelService.findSeqPlatformModelLabelByNameOrAlias(val)) {
                return 'Duplicate'
            }})
        id(blank: false)
    }
    void setAlias(String alias) {
        this.alias = alias?.trim()?.replaceAll(" +", " ")
    }

}

class CreateSequencingKitAliasCommand implements Serializable {
    String alias
    String id
    static constraints = {
        alias(blank: false, validator: {val, obj ->
            if(SequencingKitLabelService.findSequencingKitLabelByNameOrAlias(val)) {
                return 'Duplicate'
            }})
        id(blank: false)
    }
    void setAlias(String alias) {
        this.alias = alias?.trim()?.replaceAll(" +", " ")
    }
}

class CreateSeqTypeCommand implements Serializable {
        boolean single
        boolean paired
        boolean mate_pair
        boolean anyLayout = true
        String type
        String dirName
        String displayName
        static constraints = {
            anyLayout(blank: false,validator: {val, obj ->
                if (!(obj.single || obj.paired || obj.mate_pair)) {
                    return 'Empty'
                }})
            type(blank: false, validator: {val, obj ->
                if (SeqTypeService.hasSeqTypeByNameOrDisplayName(val)) {
                    return 'Duplicate'
                }})
            dirName(blank: false, validator: {val, obj ->
                if (SeqType.findByDirName(val)) {
                    return 'Duplicate'
                }})
            displayName(blank: false, validator: {val, obj ->
                if (SeqTypeService.hasSeqTypeByNameOrDisplayName(val)) {
                    return 'Duplicate'
                }})

        }
        void setType(String type) {
            this.type = type?.trim()?.replaceAll(" +", " ")
        }
        void setDirName(String dirName) {
            this.dirName = dirName?.trim()?.replaceAll(" +", " ")
        }
        void setDisplayName(String displayName) {
            this.displayName = displayName?.trim()?.replaceAll(" +", " ")
            if (this.displayName.equals("")) {
                this.displayName = null
            }
        }
}

class CreateLayoutCommand implements Serializable {
        boolean single
        boolean paired
        boolean mate_pair
        boolean anyLayout = true
        String id
        static constraints = {
            single(validator: {val, obj ->
                if (val && SeqType.findByNameAndLibraryLayout(obj.id, SeqType.LIBRARYLAYOUT_SINGLE)) {
                    return 'Duplicate'
                }
            })
            paired(validator: {val, obj ->
                if (val && SeqType.findByNameAndLibraryLayout(obj.id, SeqType.LIBRARYLAYOUT_PAIRED)) {
                    return 'Duplicate'
                }
            })
            mate_pair(validator: {val, obj ->
                if (val && SeqType.findByNameAndLibraryLayout(obj.id, SeqType.LIBRARYLAYOUT_MATE_PAIR)) {
                    return 'Duplicate'
                }
            })
            anyLayout(blank: false,validator: {val, obj -> if (!(obj.single || obj.paired || obj.mate_pair)) { return 'Empty' } })
            id(blank: false)
        }
}
