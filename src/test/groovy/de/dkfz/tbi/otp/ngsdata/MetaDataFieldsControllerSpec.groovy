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

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.CollectionUtils

import static javax.servlet.http.HttpServletResponse.*

class MetaDataFieldsControllerSpec extends Specification implements ControllerUnitTest<MetaDataFieldsController>, DataTest, UserAndRoles {

    @Override
    Class[] getDomainClassesToMock() {
        [
                AntibodyTarget,
                LibraryPreparationKit,
                Project,
                ProjectRole,
                Realm,
                Role,
                SeqCenter,
                SeqPlatform,
                SeqPlatformModelLabel,
                SeqType,
                SequencingKitLabel,
                User,
                UserProjectRole,
                UserRole,
        ]
    }

    void setupData() {
        createUserAndRoles()
        controller.seqTypeService = new SeqTypeService()
        controller.seqCenterService = new SeqCenterService()
        controller.seqPlatformService = new SeqPlatformService()
        controller.seqPlatformService.sequencingKitLabelService = new SequencingKitLabelService()
        controller.seqPlatformService.seqPlatformModelLabelService = new SeqPlatformModelLabelService()
        controller.antibodyTargetService = new AntibodyTargetService()
        controller.libraryPreparationKitService = new LibraryPreparationKitService()
    }

    void "test JSON createLibraryPreparationKit valid input"() {
        given:
        setupData()

        when:
        request.method = 'POST'
        controller.params.name = 'LibraryPreparationKit'
        controller.params.adapterFile = '/asdf'
        controller.params.reverseComplementAdapterSequence = 'GATC'
        controller.params.libraryPreparationKitService = controller.libraryPreparationKitService
        controller.createLibraryPreparationKit()

        then:
        controller.response.status == SC_OK
        controller.response.json.success
        CollectionUtils.atMostOneElement(LibraryPreparationKit.findAllByName('LibraryPreparationKit'))
    }

    @Unroll
    void "test JSON createLibraryPreparationKit invalid input"() {
        given:
        setupData()

        DomainFactory.createLibraryPreparationKit(name: 'LibraryPreparationKit', importAlias: ['LibraryPreparationKitImportAlias'])

        when:
        request.method = 'POST'
        controller.params.name = name
        controller.params.libraryPreparationKitService = controller.libraryPreparationKitService
        controller.createLibraryPreparationKit()

        then:
        controller.response.status == SC_OK
        !controller.response.json.success

        where:
        name << [
                '',
                'LibraryPreparationKit',
                'LibraryPreparationKitImportAlias',
        ]
    }

    void "test JSON createLibraryPreparationKitImportAlias valid input"() {
        given:
        setupData()

        LibraryPreparationKit libraryPreparationKit = DomainFactory.createLibraryPreparationKit(name: 'LibraryPreparationKit')

        when:
        request.method = 'POST'
        controller.params.id = libraryPreparationKit.id
        controller.params.importAlias = 'LibraryPreparationKitImportAlias'
        controller.params.libraryPreparationKitService = controller.libraryPreparationKitService
        controller.createLibraryPreparationKitImportAlias()

        then:
        controller.response.status == SC_OK
        controller.response.json.success
        libraryPreparationKit.importAlias.contains('LibraryPreparationKitImportAlias')
    }

    @Unroll
    void "test JSON createLibraryPreparationKitImportAlias invalid input"() {
        given:
        setupData()

        LibraryPreparationKit libraryPreparationKit = DomainFactory.createLibraryPreparationKit(
                name: 'LibraryPreparationKit', importAlias: ['LibraryPreparationKitImportAlias'])

        when:
        request.method = 'POST'
        controller.params.id = libraryPreparationKit.id
        controller.params.importAlias = importAlias
        controller.params.libraryPreparationKitService = controller.libraryPreparationKitService
        controller.createLibraryPreparationKitImportAlias()

        then:
        controller.response.status == SC_OK
        !controller.response.json.success

        where:
        importAlias << ['', 'LibraryPreparationKit', 'LibraryPreparationKitImportAlias']
    }

    void "test JSON createAntibodyTarget valid input"() {
        given:
        setupData()

        when:
        request.method = 'POST'
        controller.params.name = 'AntibodyTarget'
        controller.params.antibodyTargetService = controller.antibodyTargetService
        controller.createAntibodyTarget()

        then:
        controller.response.status == SC_OK
        controller.response.json.success
        CollectionUtils.atMostOneElement(AntibodyTarget.findAllByName('AntibodyTarget'))
    }

    @Unroll
    void "test JSON createAntibodyTarget invalid input"() {
        given:
        setupData()

        DomainFactory.createAntibodyTarget(name: 'AntibodyTarget')

        when:
        request.method = 'POST'
        controller.params.name = name
        controller.params.antibodyTargetService = controller.antibodyTargetService
        controller.createAntibodyTarget()

        then:
        controller.response.status == SC_OK
        !controller.response.json.success

        where:
        name << ['', 'Antibody Target', 'AntibodyTarget', 'antibodytarget', 'ANTIBODYTARGET']
    }

    void "test JSON createSeqCenter valid input"() {
        given:
        setupData()

        when:
        request.method = 'POST'
        controller.params.name = 'SEQCENTER'
        controller.params.dirName = 'seqcenter'
        controller.params.seqPlatformService = controller.seqPlatformService
        controller.createSeqCenter()

        then:
        controller.response.status == SC_OK
        controller.response.json.success
        CollectionUtils.atMostOneElement(SeqCenter.findAllByNameAndDirName('SEQCENTER', 'seqcenter'))
    }

    @Unroll
    void "test JSON createSeqCenter invalid input"() {
        given:
        setupData()

        DomainFactory.createSeqCenter(name: 'SEQCENTER', dirName: 'seqcenter')

        when:
        request.method = 'POST'
        controller.params.name = name
        controller.params.dirName = dirName
        controller.params.seqPlatformService = controller.seqPlatformService
        controller.createSeqCenter()

        then:
        controller.response.status == SC_OK
        !controller.response.json.success

        where:
        name         | dirName
        ''           | ''
        'SEQCENTER'  | ''
        'SEQCENTER2' | ''
        'SEQCENTER2' | 'seqcenter'
        ''           | 'seqcenter2'
        'SEQCENTER'  | 'seqcenter2'
    }

    @Unroll
    void "test JSON createSeqPlatform valid input"() {
        given:
        setupData()

        SeqPlatformModelLabel seqPlatformModelLabel = DomainFactory.createSeqPlatformModelLabel(name: "SeqPlatformModelLabel")
        SequencingKitLabel sequencingKitLabel = DomainFactory.createSequencingKitLabel(name: "SequencingKitLabel")

        when:
        request.method = 'POST'
        controller.params.platform = platform
        controller.params.model = model
        controller.params.kit = kit
        controller.params.seqPlatformService = controller.seqPlatformService
        controller.createSeqPlatform()

        then:
        controller.response.status == SC_OK
        controller.response.json.success
        CollectionUtils.atMostOneElement(SeqPlatform.findAllByNameAndSeqPlatformModelLabelAndSequencingKitLabel(
                platform,
                controller.params.model ? seqPlatformModelLabel : null,
                controller.params.kit ? sequencingKitLabel : null,
        ))

        where:
        platform      | model                   | kit
        'SeqPlatform' | 'SeqPlatformModelLabel' | ''
        'SeqPlatform' | 'SeqPlatformModelLabel' | 'SequencingKitLabel'
    }

    @Unroll
    void "test JSON createSeqPlatform valid input with preexisting SeqPlatform"() {
        given:
        setupData()

        SeqPlatformModelLabel seqPlatformModelLabel = DomainFactory.createSeqPlatformModelLabel(name: "SeqPlatformModelLabel")
        DomainFactory.createSeqPlatformModelLabel(name: "SeqPlatformModelLabel2")
        SequencingKitLabel sequencingKitLabel = DomainFactory.createSequencingKitLabel(name: "SequencingKitLabel")
        DomainFactory.createSequencingKitLabel(name: "SequencingKitLabel2")
        DomainFactory.createSeqPlatform(
                name: "SeqPlatform",
                seqPlatformModelLabel: seqPlatformModelLabel,
                sequencingKitLabel: sequencingKitLabel,
        )

        when:
        request.method = 'POST'
        controller.params.platform = platform
        controller.params.model = model
        controller.params.kit = kit
        controller.params.seqPlatformService = controller.seqPlatformService
        controller.params.seqPlatformModelLabelService = new SeqPlatformModelLabelService()
        controller.createSeqPlatform()

        then:
        controller.response.status == SC_OK
        controller.response.json.success
        CollectionUtils.atMostOneElement(SeqPlatform.findAllByNameAndSeqPlatformModelLabelAndSequencingKitLabel(
                platform,
                controller.params.model ? CollectionUtils.atMostOneElement(SeqPlatformModelLabel.findAllByName(model)) : null,
                controller.params.kit ? CollectionUtils.atMostOneElement(SequencingKitLabel.findAllByName(kit)) : null,
        ))

        where:
        platform       | model                    | kit
        'SeqPlatform2' | 'SeqPlatformModelLabel'  | 'SequencingKitLabel'
        'SeqPlatform'  | 'SeqPlatformModelLabel2' | 'SequencingKitLabel'
        'SeqPlatform'  | 'SeqPlatformModelLabel'  | 'SequencingKitLabel2'
    }

    @Unroll
    void "test JSON createSeqPlatform invalid input"() {
        given:
        setupData()

        SeqPlatformModelLabel seqPlatformModelLabel = DomainFactory.createSeqPlatformModelLabel(name: "SeqPlatformModelLabel")
        SequencingKitLabel sequencingKitLabel = DomainFactory.createSequencingKitLabel(name: "SequencingKitLabel")
        DomainFactory.createSeqPlatform(
                name: "SeqPlatform",
                seqPlatformModelLabel: seqPlatformModelLabel,
                sequencingKitLabel: sequencingKitLabel,
        )

        when:
        request.method = 'POST'
        controller.params.platform = platform
        controller.params.model = model
        controller.params.kit = kit
        controller.params.seqPlatformService = controller.seqPlatformService
        controller.createSeqPlatform()

        then:
        controller.response.status == SC_OK
        !controller.response.json.success

        where:
        platform      | model                   | kit
        ''            | ''                      | ''
        ''            | 'SeqPlatformModelLabel' | ''
        ''            | ''                      | 'SequencingKitLabel'
        ''            | 'SeqPlatformModelLabel' | 'SequencingKitLabel'
        'SeqPlatform' | 'SeqPlatformModelLabel' | 'SequencingKitLabel'
    }

    void "test JSON createSeqPlatformModelLabelImportAlias valid input"() {
        given:
        setupData()

        SeqPlatformModelLabel seqPlatformModelLabel = DomainFactory.createSeqPlatformModelLabel(name: "SeqPlatformModelLabel")
        controller.params.id = seqPlatformModelLabel.id

        when:
        request.method = 'POST'
        controller.params.importAlias = 'SeqPlatformModelLabelAlias'
        controller.params.seqPlatformModelLabelService = controller.seqPlatformService.seqPlatformModelLabelService
        controller.createSeqPlatformModelLabelImportAlias()

        then:
        controller.response.status == SC_OK
        controller.response.json.success
        controller.seqPlatformService.seqPlatformModelLabelService.findByNameOrImportAlias('SeqPlatformModelLabelAlias')
    }

    @Unroll
    void "test JSON createSeqPlatformModelLabelImportAlias invalid input"() {
        given:
        setupData()

        DomainFactory.createSeqPlatformModelLabel(name: "SeqPlatformModelLabel", importAlias: ["SeqPlatformModelLabelAlias"])
        controller.params.name = "SeqPlatformModelLabel"

        when:
        request.method = 'POST'
        controller.params.importAlias = importAlias
        controller.params.seqPlatformModelLabelService = controller.seqPlatformService.seqPlatformModelLabelService
        controller.createSeqPlatformModelLabelImportAlias()

        then:
        controller.response.status == SC_OK
        !controller.response.json.success

        where:
        importAlias << ['', 'SeqPlatformModelLabel', 'SeqPlatformModelLabelAlias']
    }

    void "test JSON createSequencingKitLabelImportAlias valid input"() {
        given:
        setupData()

        SequencingKitLabel sequencingKitLabel = DomainFactory.createSequencingKitLabel(name: "SequencingKitLabel")
        controller.params.id = sequencingKitLabel.id

        when:
        request.method = 'POST'
        controller.params.importAlias = 'SequencingKitLabelAlias'
        controller.params.sequencingKitLabelService = controller.seqPlatformService.sequencingKitLabelService
        controller.createSequencingKitLabelImportAlias()

        then:
        controller.response.status == SC_OK
        controller.response.json.success
        controller.seqPlatformService.sequencingKitLabelService.findByNameOrImportAlias('SequencingKitLabelAlias')
    }

    @Unroll
    void "test JSON createSequencingKitLabelImportAlias invalid input"() {
        given:
        setupData()

        DomainFactory.createSequencingKitLabel(name: "SequencingKitLabel", importAlias: ["SequencingKitLabelAlias"])
        controller.params.name = "SequencingKitLabel"

        when:
        request.method = 'POST'
        controller.params.importAlias = importAlias
        controller.params.sequencingKitLabelService = controller.seqPlatformService.sequencingKitLabelService
        controller.createSequencingKitLabelImportAlias()

        then:
        controller.response.status == SC_OK
        !controller.response.json.success

        where:
        importAlias << ['', 'SequencingKitLabel', 'SequencingKitLabelAlias']
    }

    @Unroll
    void "createSeqType, valid input"() {
        given:
        setupData()

        when:
        controller.params.seqTypeName = 'SEQTYPE'
        controller.params.dirName = 'seqtype'
        controller.params.displayName = 'SEQ TYPE'
        controller.params.single = single
        controller.params.paired = paired
        controller.params.mate_pair = mate_pair
        controller.params.singleCell = singleCell
        controller.params.anyLayout = single || paired || mate_pair
        controller.params.seqTypeService = controller.seqTypeService

        SynchronizerTokensHolder tokenHolder = SynchronizerTokensHolder.store(controller.session)
        controller.params[SynchronizerTokensHolder.TOKEN_URI] = "/metaDataFields/createSeqType"
        controller.params[SynchronizerTokensHolder.TOKEN_KEY] = tokenHolder.generateToken("/metaDataFields/createSeqType")

        request.method = "POST"

        controller.createSeqType()

        then:
        controller.response.status == SC_MOVED_TEMPORARILY
        controller.response.redirectedUrl == "/metaDataFields/seqTypes"
        !single || CollectionUtils.atMostOneElement(SeqType.findAllByNameAndDirNameAndLibraryLayoutAndSingleCell('SEQTYPE', 'seqtype', SequencingReadType.SINGLE, singleCell))
        !paired || CollectionUtils.atMostOneElement(SeqType.findAllByNameAndDirNameAndLibraryLayoutAndSingleCell('SEQTYPE', 'seqtype', SequencingReadType.PAIRED, singleCell))
        !mate_pair || CollectionUtils.atMostOneElement(SeqType.findAllByNameAndDirNameAndLibraryLayoutAndSingleCell('SEQTYPE', 'seqtype', SequencingReadType.MATE_PAIR, singleCell))

        where:
        single | paired | mate_pair | singleCell
        true   | false  | false     | false
        false  | true   | false     | false
        true   | true   | false     | false
        false  | false  | true      | false
        true   | false  | true      | false
        false  | true   | true      | false
        true   | true   | true      | false
        true   | false  | false     | true
        false  | true   | false     | true
        true   | true   | false     | true
        false  | false  | true      | true
        true   | false  | true      | true
        false  | true   | true      | true
        true   | true   | true      | true
    }

    @Unroll
    void "createSeqType, invalid input"() {
        given:
        setupData()

        DomainFactory.createSeqType(
                name: 'SEQTYPE',
                dirName: 'seqtype',
                displayName: 'SEQ TYPE',
                importAlias: ['importAlias'],
                libraryLayout: SequencingReadType.SINGLE
        )

        when:
        controller.params.seqTypeName = type
        controller.params.dirName = dirName
        controller.params.displayName = displayName
        controller.params.single = single
        controller.params.paired = paired
        controller.params.mate_pair = mate_pair
        controller.params.singleCell = singleCell
        controller.params.anyLayout = single || paired || mate_pair
        controller.params.seqTypeService = controller.seqTypeService

        SynchronizerTokensHolder tokenHolder = SynchronizerTokensHolder.store(controller.session)
        controller.params[SynchronizerTokensHolder.TOKEN_URI] = "/metaDataFields/createSeqType"
        controller.params[SynchronizerTokensHolder.TOKEN_KEY] = tokenHolder.generateToken("/metaDataFields/createSeqType")

        request.method = "POST"

        controller.createSeqType()

        then:
        controller.response.status == SC_MOVED_TEMPORARILY
        controller.response.redirectedUrl == "/metaDataFields/seqTypes"
        controller.flash.message.message == "dataFields.seqType.create.failed"

        where:
        type       | dirName    | displayName | single | paired | mate_pair | singleCell
        ''         | ''         | ''          | false  | false  | false     | false
        ''         | ''         | ''          | true   | false  | false     | false
        'SEQTYPE2' | ''         | ''          | true   | false  | false     | false
        'SEQTYPE'  | 'seqtype2' | ''          | true   | false  | false     | false
        'SEQ TYPE' | 'seqtype2' | ''          | true   | false  | false     | false
        'SEQTYPE2' | 'seqtype'  | ''          | true   | false  | false     | false
        'SEQTYPE2' | 'seqtype2' | 'SEQTYPE'   | true   | false  | false     | false
        'SEQTYPE2' | 'seqtype2' | 'SEQ TYPE'  | true   | false  | false     | false
        'SEQTYPE2' | 'seqtype2' | 'SEQTYPE'   | true   | false  | false     | false
    }

    void "createSeqType, has invalid token"() {
        given:
        setupData()

        when:
        controller.params.type = 'SEQTYPE'
        controller.params.dirName = 'seqtype'
        controller.params.displayName = 'SEQ TYPE'
        controller.params.single = true
        controller.params.paired = true
        controller.params.mate_pair = true
        controller.params.singleCell = true
        controller.params.anyLayout = true
        controller.params.seqTypeService = controller.seqTypeService

        request.method = "POST"

        controller.createSeqType()

        then:
        controller.response.status == SC_MOVED_TEMPORARILY
        controller.response.redirectedUrl == "/metaDataFields/seqTypes"
        controller.flash.message.message == "dataFields.seqType.create.failed.invalidToken"
    }

    @Unroll
    void "test JSON createLayout valid input"() {
        given:
        setupData()

        DomainFactory.createSeqType(name: 'SEQTYPE', dirName: 'SEQTYPE', libraryLayout: SequencingReadType.SINGLE, singleCell: false)
        DomainFactory.createSeqType(name: 'SEQTYPE2', dirName: 'SEQTYPE2', libraryLayout: SequencingReadType.PAIRED, singleCell: false)
        DomainFactory.createSeqType(name: 'SEQTYPE3', dirName: 'SEQTYPE3', libraryLayout: SequencingReadType.MATE_PAIR, singleCell: false)
        DomainFactory.createSeqType(name: 'SEQTYPE', dirName: 'SEQTYPE_SC', libraryLayout: SequencingReadType.PAIRED, singleCell: true)

        when:
        request.method = 'POST'
        controller.params.name = name
        controller.params.single = single
        controller.params.paired = paired
        controller.params.mate_pair = mate_pair
        controller.params.singleCell = singleCell
        controller.params.anyLayout = single || paired || mate_pair
        controller.params.seqTypeService = controller.seqTypeService
        controller.createLayout()

        then:
        controller.response.status == SC_OK
        controller.response.json.success
        !single || CollectionUtils.atMostOneElement(SeqType.findAllByNameAndLibraryLayoutAndSingleCell(name, SequencingReadType.SINGLE, singleCell))
        !paired || CollectionUtils.atMostOneElement(SeqType.findAllByNameAndLibraryLayoutAndSingleCell(name, SequencingReadType.PAIRED, singleCell))
        !mate_pair || CollectionUtils.atMostOneElement(SeqType.findAllByNameAndLibraryLayoutAndSingleCell(name, SequencingReadType.MATE_PAIR, singleCell))

        where:
        name       | single | paired | mate_pair | singleCell
        'SEQTYPE'  | false  | true   | false     | false
        'SEQTYPE'  | false  | false  | true      | false
        'SEQTYPE'  | false  | true   | true      | false
        'SEQTYPE'  | false  | false  | true      | true
        'SEQTYPE'  | true   | false  | false     | true
        'SEQTYPE'  | true   | false  | true      | true
        'SEQTYPE2' | true   | false  | false     | false
        'SEQTYPE2' | false  | false  | true      | false
        'SEQTYPE2' | true   | false  | true      | false
        'SEQTYPE3' | false  | true   | false     | false
        'SEQTYPE3' | true   | false  | false     | false
        'SEQTYPE3' | true   | true   | false     | false
    }

    @Unroll
    void "test JSON createLayout invalid input"() {
        given:
        setupData()

        DomainFactory.createSeqType(name: 'SEQTYPE', dirName: 'SEQTYPE', libraryLayout: SequencingReadType.SINGLE, singleCell: false)
        DomainFactory.createSeqType(name: 'SEQTYPE2', dirName: 'SEQTYPE2', libraryLayout: SequencingReadType.PAIRED, singleCell: false)
        DomainFactory.createSeqType(name: 'SEQTYPE3', dirName: 'SEQTYPE3', libraryLayout: SequencingReadType.MATE_PAIR, singleCell: false)
        DomainFactory.createSeqType(name: 'SEQTYPE', dirName: 'SEQTYPE_SC', libraryLayout: SequencingReadType.PAIRED, singleCell: true)

        when:
        request.method = 'POST'
        controller.params.name = name
        controller.params.single = single
        controller.params.paired = paired
        controller.params.mate_pair = mate_pair
        controller.params.singleCell = singleCell
        controller.params.anyLayout = single || paired || mate_pair
        controller.params.seqTypeService = controller.seqTypeService
        controller.createLayout()

        then:
        controller.response.status == SC_OK
        !controller.response.json.success

        where:
        name       | single | paired | mate_pair | singleCell
        'SEQTYPE'  | false  | false  | false     | false
        'SEQTYPE'  | false  | false  | false     | true
        'SEQTYPE2' | false  | false  | false     | false
        'SEQTYPE3' | false  | false  | false     | false
        'SEQTYPE'  | true   | false  | false     | false
        'SEQTYPE'  | false  | true   | false     | true
        'SEQTYPE2' | false  | true   | false     | false
        'SEQTYPE3' | false  | false  | true      | false
        'SEQTYPE'  | true   | true   | true      | false
        'SEQTYPE'  | true   | true   | true      | true
        'SEQTYPE2' | true   | true   | true      | false
        'SEQTYPE3' | true   | true   | true      | false
    }

    void "test JSON createSeqTypeAlias adds the same alias to all SeqTypes of the given name"() {
        given:
        setupData()

        SeqType seqTypeSingle = DomainFactory.createSeqType(name: 'SEQTYPE', dirName: 'SEQTYPE', libraryLayout: SequencingReadType.SINGLE)
        SeqType seqTypePaired = DomainFactory.createSeqType(name: 'SEQTYPE', dirName: 'SEQTYPE', libraryLayout: SequencingReadType.PAIRED)

        when:
        request.method = 'POST'
        controller.params.id = seqTypeSingle.id
        controller.params.importAlias = importAlias
        controller.params.seqTypeService = controller.seqTypeService
        controller.createSeqTypeImportAlias()

        then:
        seqTypeSingle.importAlias.contains(importAlias)
        seqTypePaired.importAlias.contains(importAlias)

        where:
        importAlias << ['importAlias1', 'importAlias2']
    }

    @Unroll
    void "test JSON createSeqTypeAlias invalid input (importAlias=#importAlias)"() {
        given:
        setupData()

        List<SeqType> seqTypes = [
                DomainFactory.createSeqType(name: "SEQTYPE0", dirName: "SEQTYPE0", libraryLayout: SequencingReadType.SINGLE),
                DomainFactory.createSeqType(name: "SEQTYPE1", dirName: "SEQTYPE1", importAlias: ['importAlias1'], libraryLayout: SequencingReadType.SINGLE),
        ]

        SeqType seqType = seqTypes[suffix]

        when:
        request.method = 'POST'
        controller.params.id = seqType.id
        controller.params.importAlias = importAlias
        controller.params.seqTypeService = controller.seqTypeService
        controller.createSeqTypeImportAlias()

        then:
        controller.response.status == SC_OK
        !controller.response.json.success

        where:
        suffix | importAlias
        0      | null
        0      | ''
        0      | 'importAlias1'
        1      | 'importAlias1'
    }

    void "test JSON createAntibodyTargetAlias valid input"() {
        given:
        setupData()

        AntibodyTarget antibodyTarget = DomainFactory.createAntibodyTarget()

        when:
        request.method = 'POST'
        controller.params.id = antibodyTarget.id
        controller.params.importAlias = importAlias
        controller.params.antibodyTargetService = controller.antibodyTargetService
        controller.createAntibodyTargetImportAlias()

        then:
        controller.response.status == SC_OK
        controller.response.json.success

        where:
        importAlias << ['importAlias1', 'importAlias2']
    }

    @Unroll
    void "test JSON createAntibodyTargetAlias invalid input (newAlias=#newAlias)"() {
        given:
        setupData()

        AntibodyTarget antibodyTarget = DomainFactory.createAntibodyTarget(name: 'ANTIBODYTARGET', importAlias: importAliases)

        when:
        request.method = 'POST'
        controller.params.id = antibodyTarget.id
        controller.params.importAlias = newAlias
        controller.params.antibodyTargetService = controller.antibodyTargetService
        controller.createAntibodyTargetImportAlias()

        then:
        controller.response.status == SC_OK
        !controller.response.json.success

        where:
        importAliases                    | newAlias
        []                               | null
        ['']                             | ''
        ['importAlias']                  | 'importAlias'
        ['importAlias1', 'importAlias2'] | 'importAlias2'
    }
}
