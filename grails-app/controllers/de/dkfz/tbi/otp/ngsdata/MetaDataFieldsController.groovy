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
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.Validateable
import grails.validation.ValidationException

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.utils.StringUtils
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator

@Secured("hasRole('ROLE_OPERATOR')")
class MetaDataFieldsController implements CheckAndCall {

    static allowedMethods = [
            index                          : "GET",
            libraryPreparationKits         : "GET",
            antibodyTargets                : "GET",
            seqCenters                     : "GET",
            seqPlatforms                   : "GET",
            seqTypes                       : "GET",
            createSeqType                  : "POST",
            changeLibPrepKitLegacyState    : "POST",
            changeSeqTypeLegacyState       : "POST",
            changeAntibodyTargetLegacyState: "POST",
            changeSeqPlatformLegacyState   : "POST",
    ]

    LibraryPreparationKitService libraryPreparationKitService
    AntibodyTargetService antibodyTargetService
    SeqCenterService seqCenterService
    SeqPlatformService seqPlatformService
    SeqTypeService seqTypeService

    def index() {
        redirect action: 'libraryPreparationKits'
    }

    def libraryPreparationKits() {
        return [libraryPreparationKits: libraryPreparationKitService.getDisplayableMetadata()]
    }

    def antibodyTargets() {
        return [antibodyTargets: antibodyTargetService.getDisplayableMetadata()]
    }

    def seqCenters() {
        return [seqCenters: seqCenterService.getDisplayableMetadata()]
    }

    def seqPlatforms() {
        return [seqPlatforms: seqPlatformService.getDisplayableMetadata()]
    }

    def seqTypes() {
        return [
                seqTypes: seqTypeService.getDisplayableMetadata(),
                cmd     : flash.cmd as CreateSeqTypeCommand,
        ]
    }

    def showAdapterFile(SelectLibraryPreparationKitCommand cmd) {
        String content = ""
        if (cmd.libraryPreparationKit) {
            try {
                content = libraryPreparationKitService.getAdapterFileContentToRender(cmd.libraryPreparationKit)
            } catch (AssertionError e) {
                flash.message = new FlashMessage(g.message(code: "dataFields.adapterFile.error") as String, e.message)
                redirect(action: "libraryPreparationKits")
            }
        } else {
            flash.message = new FlashMessage(g.message(code: "dataFields.adapterFile.error") as String, ["Library preparation kit does not exist"])
            redirect(action: "libraryPreparationKits")
        }

        return [
                "libraryPreparationKit": cmd.libraryPreparationKit,
                "adapterFileContent"   : content,
        ]
    }

    JSON updateAutoImportDirectory(UpdateSeqCenterAbsolutePathCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            seqCenterService.updateAutoImportDirectory(cmd.seqCenter, cmd.absolutePath)
        }
    }

    JSON updateAutoImportable(UpdateSeqCenterFlagCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            seqCenterService.updateAutoImportable(cmd.seqCenter, cmd.flag)
        }
    }

    JSON updateCopyMetadataFile(UpdateSeqCenterFlagCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            seqCenterService.updateCopyMetadateFile(cmd.seqCenter, cmd.flag)
        }
    }

    JSON updateImportDirsAllowLinking(ReplaceSeqCenterAbsolutePathCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            seqCenterService.updateImportDirsAllowLinking(cmd.seqCenter, cmd.oldAbsolutePath, cmd.absolutePath)
        }
    }

    JSON createImportDirsAllowLinking(UpdateSeqCenterAbsolutePathCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            seqCenterService.createImportDirsAllowLinking(cmd.seqCenter, cmd.absolutePath)
        }
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

    def changeLibPrepKitLegacyState(LibPrepKitLegacyCommand cmd) {
        checkErrorAndCallMethodWithFlashMessage(cmd, "dataFields.legacy") {
            libraryPreparationKitService.changeLegacyState(cmd.libraryPreparationKit, cmd.legacy)
        }
        redirect action: 'libraryPreparationKits'
    }

    def changeSeqTypeLegacyState(SeqTypeLegacyCommand cmd) {
        checkErrorAndCallMethodWithFlashMessage(cmd, "dataFields.legacy") {
            seqTypeService.changeLegacyState(cmd.seqType, cmd.legacy)
        }
        redirect action: 'seqTypes'
    }

    def changeAntibodyTargetLegacyState(AntibodyTargetLegacyCommand cmd) {
        checkErrorAndCallMethodWithFlashMessage(cmd, "dataFields.legacy") {
            antibodyTargetService.changeLegacyState(cmd.antibodyTarget, cmd.legacy)
        }
        redirect action: 'antibodyTargets'
    }

    def changeSeqPlatformLegacyState(SeqPlatformLegacyCommand cmd) {
        checkErrorAndCallMethodWithFlashMessage(cmd, "dataFields.legacy") {
            seqPlatformService.changeLegacyState(cmd.seqPlatform, cmd.legacy)
        }
        redirect action: 'seqPlatforms'
    }

    def createSeqType(CreateSeqTypeCommand cmd) {
        withForm {
            flash.cmd = cmd
            try {
                assert !cmd.hasErrors()
                seqTypeService.createMultiple(cmd.seqTypeName, cmd.getLibraryLayouts(), [
                        dirName          : cmd.dirName,
                        displayName      : cmd.displayName,
                        singleCell       : cmd.singleCell,
                        hasAntibodyTarget: cmd.hasAntibodyTarget,
                ], cmd.aliases)
                flash.cmd = null
                flash.message = new FlashMessage(g.message(code: "dataFields.seqType.create.success") as String)
            } catch (ValidationException e) {
                flash.message = new FlashMessage(g.message(code: "dataFields.seqType.create.failed") as String, e.errors)
            } catch (AssertionError e) {
                flash.message = new FlashMessage(g.message(code: "dataFields.seqType.create.failed") as String, [e.message])
            }
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "dataFields.seqType.create.failed.invalidToken") as String, ["Token is invalid"])
        }
        redirect(action: "seqTypes")
    }

    JSON createLayout(CreateLayoutCommand cmd) {
        SeqType seqType = seqTypeService.findByNameOrImportAlias(cmd.name, [singleCell: cmd.singleCell])
        checkErrorAndCallMethod(cmd) {
            seqTypeService.createMultiple(seqType.name, cmd.getLibraryLayouts(), [
                    dirName          : seqType.dirName,
                    displayName      : seqType.displayName,
                    singleCell       : cmd.singleCell,
                    hasAntibodyTarget: seqType.hasAntibodyTarget,
            ], seqType.importAlias.toList())
        }
    }

    private void createImportAlias(CreateImportAliasCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            cmd.service.addNewAlias(cmd.id, cmd.importAlias)
        }
    }
}

class SelectLibraryPreparationKitCommand {
    LibraryPreparationKit libraryPreparationKit
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
        this.name = StringUtils.trimAndShortenWhitespace(name)
    }

    void setShortDisplayName(String shortDisplayName) {
        this.shortDisplayName = StringUtils.trimAndShortenWhitespace(shortDisplayName)
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
        this.adapterFile = StringUtils.trimAndShortenWhitespace(adapterFile)
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
            if (!OtpPathValidator.isValidPathComponent(val)) {
                return 'validator.path.component'
            }
        })
    }

    void setName(String name) {
        this.name = StringUtils.trimAndShortenWhitespace(name)
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
        this.name = StringUtils.trimAndShortenWhitespace(name)
    }

    void setDirName(String dirName) {
        this.dirName = StringUtils.trimAndShortenWhitespace(dirName)
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
        this.platform = StringUtils.trimAndShortenWhitespace(platform)
    }

    void setModel(String model) {
        this.model = StringUtils.blankToNull(StringUtils.trimAndShortenWhitespace(model))
    }

    void setKit(String kit) {
        this.kit = StringUtils.blankToNull(StringUtils.trimAndShortenWhitespace(kit))
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
        this.importAlias = StringUtils.trimAndShortenWhitespace(importAlias)
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

// SuppressWarnings PropertyName: The variable mate_pair should be refactored to be matePair. Too risky to do during bulk violation removal.
@SuppressWarnings("PropertyName")
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

    List<SequencingReadType> getLibraryLayouts() {
        List<SequencingReadType> libraryLayouts = []
        if (this.single) {
            libraryLayouts.add(SequencingReadType.SINGLE)
        }
        if (this.paired) {
            libraryLayouts.add(SequencingReadType.PAIRED)
        }
        if (this.mate_pair) {
            libraryLayouts.add(SequencingReadType.MATE_PAIR)
        }
        return libraryLayouts
    }
}

class CreateSeqTypeCommand extends CreateWithLayoutCommand {
    String seqTypeName
    String dirName
    String displayName
    boolean hasAntibodyTarget
    List<String> aliases

    static constraints = {
        seqTypeName(blank: false, validator: { val, obj ->
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
        aliases(nullable: true)
    }

    void setSeqTypeName(String seqTypeName) {
        this.seqTypeName = StringUtils.trimAndShortenWhitespace(seqTypeName)
    }

    void setDirName(String dirName) {
        this.dirName = StringUtils.trimAndShortenWhitespace(dirName)
    }

    void setDisplayName(String displayName) {
        this.displayName = StringUtils.blankToNull(StringUtils.trimAndShortenWhitespace(displayName))
    }

    void setAliases(List<String> aliasNames) {
        aliases = aliasNames ?: []
    }
}

class CreateLayoutCommand extends CreateWithLayoutCommand {
    String name

    static constraints = {
        single(validator: { val, obj ->
            if (val && obj.seqTypeService.findByNameOrImportAlias(obj.name, [libraryLayout: SequencingReadType.SINGLE, singleCell: obj.singleCell])) {
                return 'default.not.unique.message'
            }
        })
        paired(validator: { val, obj ->
            if (val && obj.seqTypeService.findByNameOrImportAlias(obj.name, [libraryLayout: SequencingReadType.PAIRED, singleCell: obj.singleCell])) {
                return 'default.not.unique.message'
            }
        })
        mate_pair(validator: { val, obj ->
            if (val && obj.seqTypeService.findByNameOrImportAlias(obj.name, [libraryLayout: SequencingReadType.MATE_PAIR, singleCell: obj.singleCell])) {
                return 'default.not.unique.message'
            }
        })
        name(blank: false)
    }

    void setId(String id) {
        this.name = id
    }
}

abstract class SeqCenterCommand implements Validateable {
    SeqCenter seqCenter
}

class UpdateSeqCenterFlagCommand extends SeqCenterCommand {
    boolean flag

    void setValue(String value) {
        this.flag = Boolean.valueOf(value)
    }
}

class UpdateSeqCenterAbsolutePathCommand extends SeqCenterCommand {
    String absolutePath

    static constraints = {
        absolutePath nullable: true, blank: true, validator: { val, obj ->
            if (val != null && !OtpPathValidator.isValidAbsolutePath(val)) {
                return "validator.absolute.path"
            }
        }
    }

    void setValue(String value) {
        String trimmedValue = value.trim()
        this.absolutePath = trimmedValue == "" ? null : trimmedValue
    }
}

class ReplaceSeqCenterAbsolutePathCommand extends UpdateSeqCenterAbsolutePathCommand {
    String oldAbsolutePath
}

class LegacyCommand implements Validateable {
    boolean legacy
}

class LibPrepKitLegacyCommand extends LegacyCommand {
    LibraryPreparationKit libraryPreparationKit
}

class SeqTypeLegacyCommand extends LegacyCommand {
    SeqType seqType
}

class AntibodyTargetLegacyCommand extends LegacyCommand {
    AntibodyTarget antibodyTarget
}

class SeqPlatformLegacyCommand extends LegacyCommand {
    SeqPlatform seqPlatform
}
