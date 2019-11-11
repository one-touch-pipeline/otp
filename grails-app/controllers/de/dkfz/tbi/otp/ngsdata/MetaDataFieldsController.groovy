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
package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON
import grails.validation.Validateable

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.dataprocessing.OtpPath

class MetaDataFieldsController implements CheckAndCall {
    LibraryPreparationKitService libraryPreparationKitService
    SeqTypeService seqTypeService
    SeqPlatformService seqPlatformService
    SeqCenterService seqCenterService
    AntibodyTargetService antibodyTargetService


    def index() {
        List libraryPreparationKits = LibraryPreparationKit.list(sort: "name", order: "asc").collect { LibraryPreparationKit it ->
            [
                    id                              : it.id,
                    name                            : it.name,
                    shortDisplayName                : it.shortDisplayName,
                    adapterFile                     : it.adapterFile,
                    reverseComplementAdapterSequence: it.reverseComplementAdapterSequence,
                    importAliases                   : it.importAlias?.sort()?.join(' | '),
                    referenceGenomesWithBedFiles    : BedFile.findAllByLibraryPreparationKit(
                            it, [sort: "referenceGenome.name", order: "asc"])*.referenceGenome*.name.join(' | '),
            ]
        }

        List antibodyTargets = AntibodyTarget.list(sort: "name", order: "asc").collect {
            [
                    id           : it.id,
                    name         : it.name,
                    importAliases: it.importAlias?.sort()?.join(' | '),
            ]
        }

        List seqCenters = SeqCenter.list(sort: "name", order: "asc").collect {
            [
                    id     : it.id,
                    name   : it.name,
                    dirName: it.dirName,
            ]
        }

        List seqPlatforms = SeqPlatform.list().collect {
            [
                    name               : it.name,
                    modelId            : it.seqPlatformModelLabel?.id,
                    model              : it.seqPlatformModelLabel?.name,
                    modelImportAliases : it.seqPlatformModelLabel?.importAlias?.sort()?.join(' | '),
                    hasModel           : it.seqPlatformModelLabel ? true : false,
                    seqKitId           : it.sequencingKitLabel?.id,
                    seqKit             : it.sequencingKitLabel?.name,
                    seqKitImportAliases: it.sequencingKitLabel?.importAlias?.sort()?.join(' | '),
                    hasSeqKit          : it.sequencingKitLabel?.name ? true : false,
            ]
        }.sort { "${it.name}, ${it.model}, ${it.seqKit}" }

        List seqTypes = SeqType.list(sort: "name", order: "asc").collect {
            [
                    id               : SeqType.findAllByName(it.name)*.id?.sort()?.first(),
                    name             : it.name,
                    dirName          : it.dirName,
                    singleCell       : it.singleCell,
                    hasAntibodyTarget: it.hasAntibodyTarget,
                    libraryLayouts   : SeqType.findAllByNameAndSingleCell(it.name, it.singleCell)*.libraryLayout.sort().reverse().join(' | '),
                    layouts          :
                            [
                                    SINGLE   : SeqType.findByNameAndLibraryLayoutAndSingleCell(it.name, LibraryLayout.SINGLE, it.singleCell) ? true : false,
                                    PAIRED   : SeqType.findByNameAndLibraryLayoutAndSingleCell(it.name, LibraryLayout.PAIRED, it.singleCell) ? true : false,
                                    MATE_PAIR: SeqType.findByNameAndLibraryLayoutAndSingleCell(it.name, LibraryLayout.MATE_PAIR, it.singleCell) ? true : false,
                            ],
                    displayName      : it.displayName,
                    importAliases    : SeqType.findAllByName(it.name)*.importAlias?.flatten()?.unique()?.sort()?.join(' | '),
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
        checkErrorAndCallMethod(cmd) {
            libraryPreparationKitService.create(cmd.name, [
                    shortDisplayName                : cmd.shortDisplayName,
                    adapterFile                     : cmd.adapterFile,
                    reverseComplementAdapterSequence: cmd.reverseComplementAdapterSequence,
            ])
        }
    }

    JSON addAdapterFileToLibraryPreparationKit(AddAdapterFileToLibraryPreparationKitCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            libraryPreparationKitService.addAdapterFileToLibraryPreparationKit(cmd.libraryPreparationKit, cmd.adapterFile)
        }
    }

    JSON addAdapterSequenceToLibraryPreparationKit(AddAdapterSequenceToLibraryPreparationKitCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            libraryPreparationKitService.addAdapterSequenceToLibraryPreparationKit(cmd.libraryPreparationKit, cmd.reverseComplementAdapterSequence)
        }
    }

    JSON createAntibodyTarget(CreateAntibodyTargetCommand cmd) {
        checkErrorAndCallMethod(cmd) { antibodyTargetService.create(cmd.name) }
    }

    JSON createSeqCenter(CreateSeqCenterCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            seqCenterService.createSeqCenter(cmd.name, cmd.dirName)
        }
    }

    JSON createSeqPlatform(CreateSeqPlatformCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            seqPlatformService.createNewSeqPlatform(cmd.platform, cmd.model, cmd.kit)
        }
    }

    JSON createSeqPlatformModelLabelImportAlias(CreateSeqPlatformModelLabelImportAliasCommand cmd) {
        createImportAlias(cmd)
    }

    JSON createSequencingKitLabelImportAlias(CreateSequencingKitLabelImportAliasCommand cmd) {
        createImportAlias(cmd)
    }

    JSON createSeqTypeImportAlias(CreateSeqTypeImportAliasCommand cmd) {
        createImportAlias(cmd)
    }

    JSON createAntibodyTargetImportAlias(CreateAntibodyTargetImportAliasCommand cmd) {
        createImportAlias(cmd)
    }

    JSON createLibraryPreparationKitImportAlias(CreateLibraryPreparationKitImportAliasCommand cmd) {
        createImportAlias(cmd)
    }

    JSON createSeqType(CreateSeqTypeCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            seqTypeService.createMultiple(cmd.type, cmd.getLibraryLayouts(), [
                    dirName: cmd.dirName,
                    displayName: cmd.displayName,
                    singleCell: cmd.singleCell,
                    hasAntibodyTarget: cmd.hasAntibodyTarget,
            ])
        }
    }

    JSON createLayout(CreateLayoutCommand cmd) {
        SeqType seqType = seqTypeService.findByNameOrImportAlias(cmd.name, [singleCell: cmd.singleCell])
        checkErrorAndCallMethod(cmd) {
            seqTypeService.createMultiple(seqType.name, cmd.getLibraryLayouts(), [
                    dirName: seqType.dirName,
                    displayName: seqType.displayName,
                    singleCell: cmd.singleCell,
                    hasAntibodyTarget: seqType.hasAntibodyTarget,
            ], seqType.importAlias.toList())
        }
    }

    void createImportAlias(CreateImportAliasCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            cmd.service.addNewAlias(cmd.id, cmd.importAlias)
        }
    }
}

class CreateLibraryPreparationKitCommand implements Validateable {
    String name
    String shortDisplayName
    String adapterFile
    String reverseComplementAdapterSequence
    LibraryPreparationKitService libraryPreparationKitService
    static constraints = {
        name(blank: false, validator: { val, obj ->
            if (obj.libraryPreparationKitService.findByNameOrImportAlias(val)) {
                return 'default.not.unique.message'
            }
        })
        shortDisplayName(blank: false, validator: { val, obj ->
            if (LibraryPreparationKit.findByShortDisplayName(val)) {
                return 'default.not.unique.message'
            }
        })
        adapterFile(nullable: true, blank: false, shared: "absolutePath")
        reverseComplementAdapterSequence(nullable: true, blank: false)
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

class AddAdapterFileToLibraryPreparationKitCommand implements Validateable {
    String adapterFile
    LibraryPreparationKit libraryPreparationKit
    static constraints = {
        adapterFile blank: false, nullable: false, shared: "absolutePath"
        libraryPreparationKit nullable: false
    }

    void setAdapterFile(String adapterFile) {
        this.adapterFile = adapterFile?.trim()?.replaceAll(" +", " ")
    }
}

class AddAdapterSequenceToLibraryPreparationKitCommand implements Validateable {
    String reverseComplementAdapterSequence
    LibraryPreparationKit libraryPreparationKit
    static constraints = {
        reverseComplementAdapterSequence blank: false, nullable: false
        libraryPreparationKit nullable: false
    }
}

class CreateAntibodyTargetCommand implements Validateable {
    String name
    AntibodyTargetService antibodyTargetService

    static constraints = {
        name(blank: false, validator: { val, obj ->
            if (obj.antibodyTargetService.findByNameOrImportAlias(val)) {
                return 'default.not.unique.message'
            }
            if (!OtpPath.isValidPathComponent(val)) {
                return 'validator.path.component'
            }
        })
    }

    void setName(String name) {
        this.name = name?.trim()?.replaceAll(" +", " ")
    }
}

class CreateSeqCenterCommand implements Validateable {
    String name
    String dirName
    static constraints = {
        name(blank: false, validator: { val, obj ->
            if (SeqCenter.findByName(val)) {
                return 'default.not.unique.message'
            }
        })
        dirName(blank: false, validator: { val, obj ->
            if (SeqCenter.findByDirName(val)) {
                return 'default.not.unique.message'
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

class CreateSeqPlatformCommand implements Validateable {
    SeqPlatformService seqPlatformService
    String platform
    String model
    String kit
    static constraints = {
        platform(blank: false,
                validator: { val, obj ->
                    if (obj.seqPlatformService.findSeqPlatform(obj.platform, obj.model, obj.kit)) {
                        return 'default.not.unique.message'
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

abstract class CreateImportAliasCommand implements Validateable {
    abstract MetadataFieldsService getService()
    Long id
    String importAlias

    static constraints = {
        id(blank: false)
        importAlias(blank: false, validator: { val, obj ->
            if (obj.service.findByNameOrImportAlias(val)) {
                return 'default.not.unique.message'
            }
        })
    }

    void setImportAlias(String importAlias) {
        this.importAlias = importAlias?.trim()?.replaceAll(" +", " ")
    }
}

class CreateSeqPlatformModelLabelImportAliasCommand extends CreateImportAliasCommand {
    SeqPlatformModelLabelService seqPlatformModelLabelService

    @Override
    MetadataFieldsService getService() {
        return seqPlatformModelLabelService
    }
}

class CreateSequencingKitLabelImportAliasCommand extends CreateImportAliasCommand {
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

abstract class CreateWithLayoutCommand implements Validateable {
    SeqTypeService seqTypeService
    boolean single
    boolean paired
    boolean mate_pair
    boolean anyLayout = true
    boolean singleCell

    static constraints = {
        anyLayout(blank: false, validator: { val, obj ->
            if (!(obj.single || obj.paired || obj.mate_pair)) {
                return 'none.selected'
            }
        })
    }

    List<LibraryLayout> getLibraryLayouts() {
        List<LibraryLayout> libraryLayouts = []
        if (this.single) {
            libraryLayouts.add(LibraryLayout.SINGLE)
        }
        if (this.paired) {
            libraryLayouts.add(LibraryLayout.PAIRED)
        }
        if (this.mate_pair) {
            libraryLayouts.add(LibraryLayout.MATE_PAIR)
        }
        return libraryLayouts
    }
}

class CreateSeqTypeCommand extends CreateWithLayoutCommand {
    String type
    String dirName
    String displayName
    boolean hasAntibodyTarget

    static constraints = {
        type(blank: false, validator: { val, obj ->
            if (obj.getLibraryLayouts().find {
                obj.seqTypeService.findByNameOrImportAlias(val, [libraryLayout: it, singleCell: obj.singleCell])
            }) {
                return 'default.not.unique.message'
            }
        })
        dirName(blank: false, validator: { val, obj ->
            if (SeqType.findByDirName(val)) {
                return 'default.not.unique.message'
            }
        })
        displayName(blank: false, validator: { val, obj ->
            if (obj.getLibraryLayouts().find {
                obj.seqTypeService.findByNameOrImportAlias(val, [libraryLayout: it, singleCell: obj.singleCell])
            }) {
                return 'default.not.unique.message'
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
            if (val && obj.seqTypeService.findByNameOrImportAlias(obj.name, [libraryLayout: LibraryLayout.SINGLE, singleCell: obj.singleCell])) {
                return 'default.not.unique.message'
            }
        })
        paired(validator: { val, obj ->
            if (val && obj.seqTypeService.findByNameOrImportAlias(obj.name, [libraryLayout: LibraryLayout.PAIRED, singleCell: obj.singleCell])) {
                return 'default.not.unique.message'
            }
        })
        mate_pair(validator: { val, obj ->
            if (val && obj.seqTypeService.findByNameOrImportAlias(obj.name, [libraryLayout: LibraryLayout.MATE_PAIR, singleCell: obj.singleCell])) {
                return 'default.not.unique.message'
            }
        })
        name(blank: false)
    }

    void setId(String id) {
        this.name = id
    }
}
