package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.dataprocessing.*
import grails.converters.*
import org.springframework.validation.*


class MetaDataFieldsController implements CheckAndCall {
    LibraryPreparationKitService libraryPreparationKitService
    SeqTypeService seqTypeService
    SeqPlatformService seqPlatformService
    SeqCenterService seqCenterService
    AntibodyTargetService antibodyTargetService


    def index() {

        List libraryPreparationKits = LibraryPreparationKit.list(sort: "name", order: "asc").collect {
            [
                    id                              : it.id,
                    name                            : it.name,
                    shortDisplayName                : it.shortDisplayName,
                    adapterFile                     : it.adapterFile,
                    reverseComplementAdapterSequence: it.reverseComplementAdapterSequence,
                    importAliases                   : it.importAlias?.sort()?.join(' | '),
                    referenceGenomesWithBedFiles    : BedFile.findAllByLibraryPreparationKit(it, [sort: "referenceGenome.name", order: "asc"])*.referenceGenome*.name.join(' | '),
            ]
        }

        List antibodyTargets = AntibodyTarget.list(sort: "name", order: "asc").collect {
            [
                    name         : it.name,
                    importAliases: it.importAlias?.sort()?.join(' | '),
            ]
        }

        List seqCenters = SeqCenter.list(sort: "name", order: "asc").collect {
            [
                    name   : it.name,
                    dirName: it.dirName,
            ]
        }

        List seqPlatforms = SeqPlatform.list().collect {
            [
                    name               : it.name,
                    model              : it.seqPlatformModelLabel?.name,
                    modelImportAliases : it.seqPlatformModelLabel?.importAlias?.sort()?.join(' | '),
                    hasModel           : it.seqPlatformModelLabel ? true : false,
                    seqKit             : it.sequencingKitLabel?.name,
                    seqKitImportAliases: it.sequencingKitLabel?.importAlias?.sort()?.join(' | '),
                    hasSeqKit          : it.sequencingKitLabel?.name ? true : false,
            ]
        }.sort { "${it.name}, ${it.model}, ${it.seqKit}" }

        List seqTypes = SeqType.list(sort: "name", order: "asc").collect {
            [
                    name          : it.name,
                    dirName       : it.dirName,
                    singleCell    : it.singleCell,
                    libraryLayouts: SeqType.findAllByNameAndSingleCell(it.name, it.singleCell)*.libraryLayout.sort().reverse().join(' | '),
                    layouts       :
                            [
                                    SINGLE   : SeqType.findByNameAndLibraryLayoutAndSingleCell(it.name, SeqType.LIBRARYLAYOUT_SINGLE, it.singleCell) ? true : false,
                                    PAIRED   : SeqType.findByNameAndLibraryLayoutAndSingleCell(it.name, SeqType.LIBRARYLAYOUT_PAIRED, it.singleCell) ? true : false,
                                    MATE_PAIR: SeqType.findByNameAndLibraryLayoutAndSingleCell(it.name, SeqType.LIBRARYLAYOUT_MATE_PAIR, it.singleCell) ? true : false,
                            ],
                    displayName   : it.displayName,
                    importAliases : SeqType.findAllByName(it.name)*.importAlias?.flatten()?.unique()?.sort()?.join(' | '),
            ]
        }.unique()

        return [
                antibodyTargets       : antibodyTargets,
                libraryPreparationKits: libraryPreparationKits,
                seqCenters            : seqCenters,
                seqPlatforms          : seqPlatforms,
                seqTypes              : seqTypes,
        ]
    }

    JSON createLibraryPreparationKit(CreateLibraryPreparationKitCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            libraryPreparationKitService.create(cmd.name,
                    [
                            shortDisplayName                : cmd.shortDisplayName,
                            adapterFile                     : cmd.adapterFile,
                            reverseComplementAdapterSequence: cmd.reverseComplementAdapterSequence,
                    ])
        })
    }

    JSON addAdapterFileToLibraryPreparationKit(AddAdapterFileToLibraryPreparationKitCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            libraryPreparationKitService.addAdapterFileToLibraryPreparationKit(cmd.libraryPreparationKit, cmd.adapterFile)
        })
    }

    JSON addAdapterSequenceToLibraryPreparationKit(AddAdapterSequenceToLibraryPreparationKitCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            libraryPreparationKitService.addAdapterSequenceToLibraryPreparationKit(cmd.libraryPreparationKit, cmd.reverseComplementAdapterSequence)
        })
    }

    JSON createAntibodyTarget(CreateAntibodyTargetCommand cmd) {
        checkErrorAndCallMethod(cmd, { antibodyTargetService.create(cmd.name) })
    }

    JSON createSeqCenter(CreateSeqCenterCommand cmd) {
        checkErrorAndCallMethod(cmd, { seqCenterService.createSeqCenter(cmd.name, cmd.dirName) })
    }

    JSON createSeqPlatform(CreateSeqPlatformCommand cmd) {
        checkErrorAndCallMethod(cmd, { seqPlatformService.createNewSeqPlatform(cmd.platform, cmd.model, cmd.kit) })
    }

    JSON createModelImportAlias(CreateModelImportAliasCommand cmd) {
        createImportAlias(cmd);
    }

    JSON createSequencingKitImportAlias(CreateSequencingKitImportAliasCommand cmd) {
        createImportAlias(cmd);
    }

    JSON createSeqTypeImportAlias(CreateSeqTypeImportAliasCommand cmd) {
        createImportAlias(cmd);
    }

    JSON createAntibodyTargetImportAlias(CreateAntibodyTargetImportAliasCommand cmd) {
        createImportAlias(cmd);
    }

    JSON createLibraryPreparationKitImportAlias(CreateLibraryPreparationKitImportAliasCommand cmd) {
        createImportAlias(cmd);
    }

    JSON createSeqType(CreateSeqTypeCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            seqTypeService.createMultiple(cmd.type, cmd.getLibraryLayouts(), [dirName: cmd.dirName, displayName: cmd.displayName, singleCell: cmd.singleCell])
        })
    }

    JSON createLayout(CreateLayoutCommand cmd) {
        SeqType seqType = seqTypeService.findByNameOrImportAlias(cmd.name, [singleCell: cmd.singleCell])
        checkErrorAndCallMethod(cmd, {
            seqTypeService.createMultiple(seqType.name, cmd.getLibraryLayouts(), [dirName: seqType.dirName, displayName: seqType.displayName, singleCell: cmd.singleCell], seqType.importAlias.toList())
        })
    }

    void createImportAlias(CreateImportAliasCommand cmd){
        checkErrorAndCallMethod(cmd, { cmd.service.addNewAlias(cmd.name, cmd.importAlias) })
    }
}

class CreateLibraryPreparationKitCommand implements Serializable {
    String name
    String shortDisplayName
    String adapterFile
    String reverseComplementAdapterSequence
    LibraryPreparationKitService libraryPreparationKitService
    static constraints = {
        name(blank: false, validator: { val, obj ->
            if (obj.libraryPreparationKitService.findByNameOrImportAlias(val)) {
                return 'Duplicate'
            }
        })
        shortDisplayName(blank: false, validator: { val, obj ->
            if (LibraryPreparationKit.findByShortDisplayName(val)) {
                return 'Duplicate'
            }
        })
        adapterFile (nullable: true, blank:false, validator: { val, obj ->
            if (val && !OtpPath.isValidAbsolutePath(val)) {
                return 'Not a valid file name'
            }
        })
        reverseComplementAdapterSequence (nullable: true, blank:false)
    }

    void setName(String name) {
        this.name = name?.trim()?.replaceAll(" +", " ") //trims and removes additional white spaces
    }

    void setShortDisplayName(String shortDisplayName) {
        this.shortDisplayName = shortDisplayName?.trim()?.replaceAll(" +", " ")
    }

    void setAdapterFile(String adapterFile) {
        this.adapterFile = adapterFile ?: null
    }

    void setReverseComplementAdapterSequence(String reverseComplementAdapterSequence) {
        this.reverseComplementAdapterSequence = reverseComplementAdapterSequence ?: null
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

class CreateAntibodyTargetCommand implements Serializable {
    String name
    AntibodyTargetService antibodyTargetService

    static constraints = {
        name(blank: false, validator: { val, obj ->
            if (obj.antibodyTargetService.findByNameOrImportAlias(val)) {
                return 'Duplicate'
            }
            if (!OtpPath.isValidPathComponent(val)) {
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
        name(blank: false, validator: { val, obj ->
            if (SeqCenter.findByName(val)) {
                return 'Duplicate'
            }
        })
        dirName(blank: false, validator: { val, obj ->
            if (SeqCenter.findByDirName(val)) {
                return 'Duplicate'
            }
        })
    }

    void setName(String name) {
        this.name = name?.trim()?.replaceAll(" +", " ")
    }

    void setDirName(String dirName) {
        this.dirName = dirName?.trim()?.replaceAll(" +", " ")
    }
}

class CreateSeqPlatformCommand implements Serializable {
    SeqPlatformService seqPlatformService
    String platform
    String model
    String kit
    static constraints = {
        platform(blank: false,
                validator: { val, obj ->
                    if (obj.seqPlatformService.findSeqPlatform(obj.platform, obj.model, obj.kit)) {
                        return 'Duplicate'
                    }
                })
        model(blank: false, nullable: false)
        kit(blank: false, nullable: true)
    }

    void setPlatform(String platform) {
        this.platform = platform?.trim()?.replaceAll(" +", " ")
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

abstract class CreateImportAliasCommand implements Serializable {
    abstract MetadataFieldsService getService()
    String name
    String importAlias

    static constraints = {
        name(blank: false)
        importAlias(blank: false, validator: { val, obj ->
            if (obj.service.findByNameOrImportAlias(val)) {
                return 'Duplicate'
            }
        })
    }

    void setImportAlias(String importAlias) {
        this.importAlias = importAlias?.trim()?.replaceAll(" +", " ")
    }

    void setId(String id) {
        this.name = id
    }
}

class CreateModelImportAliasCommand extends CreateImportAliasCommand {
    SeqPlatformModelLabelService seqPlatformModelLabelService

    @Override
    MetadataFieldsService getService() {
        return seqPlatformModelLabelService
    }
}

class CreateSequencingKitImportAliasCommand extends CreateImportAliasCommand {
    SequencingKitLabelService sequencingKitLabelService

    @Override
    MetadataFieldsService getService() {
        return sequencingKitLabelService
    }
}

class CreateSeqTypeImportAliasCommand extends CreateImportAliasCommand {
    SeqTypeService seqTypeService
    boolean singleCell

    @Override
    MetadataFieldsService getService() {
        return seqTypeService
    }
}

class CreateAntibodyTargetImportAliasCommand extends CreateImportAliasCommand {
    AntibodyTargetService antibodyTargetService

    @Override
    MetadataFieldsService getService() {
        return antibodyTargetService
    }
}

class CreateLibraryPreparationKitImportAliasCommand extends CreateImportAliasCommand {
    LibraryPreparationKitService libraryPreparationKitService

    @Override
    MetadataFieldsService getService() {
        libraryPreparationKitService
    }
}

abstract class CreateWithLayoutCommand implements Serializable {
    SeqTypeService seqTypeService
    boolean single
    boolean paired
    boolean mate_pair
    boolean anyLayout = true
    boolean singleCell

    static constraints = {
        anyLayout(blank: false, validator: { val, obj ->
            if (!(obj.single || obj.paired || obj.mate_pair)) {
                return 'Empty'
            }
        })
    }

    List<String> getLibraryLayouts() {
        List<String> libraryLayouts = []
        if (this.single) {
            libraryLayouts.add(SeqType.LIBRARYLAYOUT_SINGLE)
        }
        if (this.paired) {
            libraryLayouts.add(SeqType.LIBRARYLAYOUT_PAIRED)
        }
        if (this.mate_pair) {
            libraryLayouts.add(SeqType.LIBRARYLAYOUT_MATE_PAIR)
        }
        return libraryLayouts
    }
}

class CreateSeqTypeCommand extends CreateWithLayoutCommand {
    String type
    String dirName
    String displayName

    static constraints = {
        type(blank: false, validator: { val, obj ->
            if (obj.getLibraryLayouts().find {
                obj.seqTypeService.findByNameOrImportAlias(val, [libraryLayout: it, singleCell: obj.singleCell])
            }) {
                return 'Duplicate'
            }
        })
        dirName(blank: false, validator: { val, obj ->
            if (SeqType.findByDirName(val)) {
                return 'Duplicate'
            }
        })
        displayName(blank: false, validator: { val, obj ->
            if (obj.getLibraryLayouts().find {
                obj.seqTypeService.findByNameOrImportAlias(val, [libraryLayout: it, singleCell: obj.singleCell])
            }) {
                return 'Duplicate'
            }
        })
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

class CreateLayoutCommand extends CreateWithLayoutCommand {
    String name

    static constraints = {
        single(validator: { val, obj ->
            if (val && obj.seqTypeService.findByNameOrImportAlias(obj.name, [libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE, singleCell: obj.singleCell])) {
                return 'Duplicate'
            }
        })
        paired(validator: { val, obj ->
            if (val && obj.seqTypeService.findByNameOrImportAlias(obj.name, [libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED, singleCell: obj.singleCell])) {
                return 'Duplicate'
            }
        })
        mate_pair(validator: { val, obj ->
            if (val && obj.seqTypeService.findByNameOrImportAlias(obj.name, [libraryLayout: SeqType.LIBRARYLAYOUT_MATE_PAIR, singleCell: obj.singleCell])) {
                return 'Duplicate'
            }
        })
        name(blank: false)
    }

    void setId(String id) {
        this.name = id
    }
}
