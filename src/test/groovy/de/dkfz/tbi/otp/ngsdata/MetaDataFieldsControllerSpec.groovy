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

import grails.plugin.springsecurity.acl.AclSid
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.security.*

class MetaDataFieldsControllerSpec extends Specification implements ControllerUnitTest<MetaDataFieldsController>, DataTest, UserAndRoles {

    @Override
    Class[] getDomainClassesToMock() {
        [
                AclSid,
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

    @Rule
    TemporaryFolder temporaryFolder

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
        controller.params.name = 'LibraryPreparationKit'
        controller.params.shortDisplayName = 'LPK'
        controller.params.adapterFile = '/asdf'
        controller.params.reverseComplementAdapterSequence = 'GATC'
        controller.params.libraryPreparationKitService = controller.libraryPreparationKitService
        controller.createLibraryPreparationKit()

        then:
        controller.response.status == 200
        controller.response.json.success
        LibraryPreparationKit.findByName('LibraryPreparationKit')
    }

    @Unroll
    void "test JSON createLibraryPreparationKit invalid input"() {
        given:
        setupData()

        DomainFactory.createLibraryPreparationKit(name: 'LibraryPreparationKit', shortDisplayName: 'LPK', importAlias: ['LibraryPreparationKitImportAlias'])

        when:
        controller.params.name = name
        controller.params.shortDisplayName = shortDisplayName
        controller.params.libraryPreparationKitService = controller.libraryPreparationKitService
        controller.createLibraryPreparationKit()

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        name                               | shortDisplayName
        ''                                 | 'LPK2'
        'LibraryPreparationKit'            | 'LPK2'
        'LibraryPreparationKitImportAlias' | 'LPK2'
        'LibraryPreparationKit2'           | ''
        'LibraryPreparationKit2'           | 'LPK'
    }

    void "test JSON createLibraryPreparationKitImportAlias valid input"() {
        given:
        setupData()

        LibraryPreparationKit libraryPreparationKit = DomainFactory.createLibraryPreparationKit(name: 'LibraryPreparationKit', shortDisplayName: 'LPK')

        when:
        controller.params.id = libraryPreparationKit.id
        controller.params.importAlias = 'LibraryPreparationKitImportAlias'
        controller.params.libraryPreparationKitService = controller.libraryPreparationKitService
        controller.createLibraryPreparationKitImportAlias()

        then:
        controller.response.status == 200
        controller.response.json.success
        libraryPreparationKit.importAlias.contains('LibraryPreparationKitImportAlias')
    }

    @Unroll
    void "test JSON createLibraryPreparationKitImportAlias invalid input"() {
        given:
        setupData()

        LibraryPreparationKit libraryPreparationKit = DomainFactory.createLibraryPreparationKit(
                name: 'LibraryPreparationKit', shortDisplayName: 'LPK', importAlias: ['LibraryPreparationKitImportAlias'])

        when:
        controller.params.id = libraryPreparationKit.id
        controller.params.importAlias = importAlias
        controller.params.libraryPreparationKitService = controller.libraryPreparationKitService
        controller.createLibraryPreparationKitImportAlias()

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        importAlias << ['', 'LibraryPreparationKit', 'LibraryPreparationKitImportAlias']
    }

    void "test JSON createAntibodyTarget valid input"() {
        given:
        setupData()

        when:
        controller.params.name = 'AntibodyTarget'
        controller.params.antibodyTargetService = controller.antibodyTargetService
        controller.createAntibodyTarget()

        then:
        controller.response.status == 200
        controller.response.json.success
        AntibodyTarget.findByName('AntibodyTarget')
    }

    @Unroll
    void "test JSON createAntibodyTarget invalid input"() {
        given:
        setupData()

        DomainFactory.createAntibodyTarget(name: 'AntibodyTarget')

        when:
        controller.params.name = name
        controller.params.antibodyTargetService = controller.antibodyTargetService
        controller.createAntibodyTarget()

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        name << ['', 'Antibody Target', 'AntibodyTarget', 'antibodytarget', 'ANTIBODYTARGET']
    }

    void "test JSON createSeqCenter valid input"() {
        given:
        setupData()

        when:
        controller.params.name = 'SEQCENTER'
        controller.params.dirName = 'seqcenter'
        controller.params.seqPlatformService = controller.seqPlatformService
        controller.createSeqCenter()

        then:
        controller.response.status == 200
        controller.response.json.success
        SeqCenter.findByNameAndDirName('SEQCENTER', 'seqcenter')
    }

    @Unroll
    void "test JSON createSeqCenter invalid input"() {
        given:
        setupData()

        DomainFactory.createSeqCenter(name: 'SEQCENTER', dirName: 'seqcenter')

        when:
        controller.params.name = name
        controller.params.dirName = dirName
        controller.params.seqPlatformService = controller.seqPlatformService
        controller.createSeqCenter()

        then:
        controller.response.status == 200
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
        controller.params.platform = platform
        controller.params.model = model
        controller.params.kit = kit
        controller.params.seqPlatformService = controller.seqPlatformService
        controller.createSeqPlatform()

        then:
        controller.response.status == 200
        controller.response.json.success
        SeqPlatform.findByNameAndSeqPlatformModelLabelAndSequencingKitLabel(
                platform,
                controller.params.model ? seqPlatformModelLabel : null,
                controller.params.kit ? sequencingKitLabel : null,
        )

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
        controller.params.platform = platform
        controller.params.model = model
        controller.params.kit = kit
        controller.params.seqPlatformService = controller.seqPlatformService
        controller.params.seqPlatformModelLabelService = new SeqPlatformModelLabelService()
        controller.createSeqPlatform()

        then:
        controller.response.status == 200
        controller.response.json.success
        SeqPlatform.findByNameAndSeqPlatformModelLabelAndSequencingKitLabel(
                platform,
                controller.params.model ? SeqPlatformModelLabel.findByName(model) : null,
                controller.params.kit ? SequencingKitLabel.findByName(kit) : null,
        )

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
        controller.params.platform = platform
        controller.params.model = model
        controller.params.kit = kit
        controller.params.seqPlatformService = controller.seqPlatformService
        controller.createSeqPlatform()

        then:
        controller.response.status == 200
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
        controller.params.importAlias = 'SeqPlatformModelLabelAlias'
        controller.params.seqPlatformModelLabelService = controller.seqPlatformService.seqPlatformModelLabelService
        controller.createSeqPlatformModelLabelImportAlias()

        then:
        controller.response.status == 200
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
        controller.params.importAlias = importAlias
        controller.params.seqPlatformModelLabelService = controller.seqPlatformService.seqPlatformModelLabelService
        controller.createSeqPlatformModelLabelImportAlias()

        then:
        controller.response.status == 200
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
        controller.params.importAlias = 'SequencingKitLabelAlias'
        controller.params.sequencingKitLabelService = controller.seqPlatformService.sequencingKitLabelService
        controller.createSequencingKitLabelImportAlias()

        then:
        controller.response.status == 200
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
        controller.params.importAlias = importAlias
        controller.params.sequencingKitLabelService = controller.seqPlatformService.sequencingKitLabelService
        controller.createSequencingKitLabelImportAlias()

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        importAlias << ['', 'SequencingKitLabel', 'SequencingKitLabelAlias']
    }

    @Unroll
    void "test JSON createSeqType valid input"() {
        given:
        setupData()

        when:
        controller.params.type = 'SEQTYPE'
        controller.params.dirName = 'seqtype'
        controller.params.displayName = 'SEQ TYPE'
        controller.params.single = single
        controller.params.paired = paired
        controller.params.mate_pair = mate_pair
        controller.params.singleCell = singleCell
        controller.params.anyLayout = single || paired || mate_pair
        controller.params.seqTypeService = controller.seqTypeService
        controller.createSeqType()

        then:
        controller.response.status == 200
        controller.response.json.success
        !single || SeqType.findByNameAndDirNameAndLibraryLayoutAndSingleCell('SEQTYPE', 'seqtype', LibraryLayout.SINGLE, singleCell)
        !paired || SeqType.findByNameAndDirNameAndLibraryLayoutAndSingleCell('SEQTYPE', 'seqtype', LibraryLayout.PAIRED, singleCell)
        !mate_pair || SeqType.findByNameAndDirNameAndLibraryLayoutAndSingleCell('SEQTYPE', 'seqtype', LibraryLayout.MATE_PAIR, singleCell)

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
    void "test JSON createSeqType invalid input"() {
        given:
        setupData()

        DomainFactory.createSeqType(
                name: 'SEQTYPE', dirName: 'seqtype', displayName: 'SEQ TYPE', importAlias: ['importAlias'], libraryLayout: LibraryLayout.SINGLE)

        when:
        controller.params.type = type
        controller.params.dirName = dirName
        controller.params.displayName = displayName
        controller.params.single = single
        controller.params.paired = paired
        controller.params.mate_pair = mate_pair
        controller.params.singleCell = singleCell
        controller.params.anyLayout = single || paired || mate_pair
        controller.params.seqTypeService = controller.seqTypeService
        controller.createSeqType()

        then:
        controller.response.status == 200
        !controller.response.json.success

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
        'SEQTYPE2' | 'seqtype2' | 'SEQTYPE2'  | true   | false  | false     | null
    }

    @Unroll
    void "test JSON createLayout valid input"() {
        given:
        setupData()

        DomainFactory.createSeqType(name: 'SEQTYPE', dirName: 'SEQTYPE', libraryLayout: LibraryLayout.SINGLE, singleCell: false)
        DomainFactory.createSeqType(name: 'SEQTYPE2', dirName: 'SEQTYPE2', libraryLayout: LibraryLayout.PAIRED, singleCell: false)
        DomainFactory.createSeqType(name: 'SEQTYPE3', dirName: 'SEQTYPE3', libraryLayout: LibraryLayout.MATE_PAIR, singleCell: false)
        DomainFactory.createSeqType(name: 'SEQTYPE', dirName: 'SEQTYPE_SC', libraryLayout: LibraryLayout.PAIRED, singleCell: true)

        when:
        controller.params.name = name
        controller.params.single = single
        controller.params.paired = paired
        controller.params.mate_pair = mate_pair
        controller.params.singleCell = singleCell
        controller.params.anyLayout = single || paired || mate_pair
        controller.params.seqTypeService = controller.seqTypeService
        controller.createLayout()

        then:
        controller.response.status == 200
        controller.response.json.success
        !single || SeqType.findByNameAndLibraryLayoutAndSingleCell(name, LibraryLayout.SINGLE, singleCell)
        !paired || SeqType.findByNameAndLibraryLayoutAndSingleCell(name, LibraryLayout.PAIRED, singleCell)
        !mate_pair || SeqType.findByNameAndLibraryLayoutAndSingleCell(name, LibraryLayout.MATE_PAIR, singleCell)

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

        DomainFactory.createSeqType(name: 'SEQTYPE', dirName: 'SEQTYPE', libraryLayout: LibraryLayout.SINGLE, singleCell: false)
        DomainFactory.createSeqType(name: 'SEQTYPE2', dirName: 'SEQTYPE2', libraryLayout: LibraryLayout.PAIRED, singleCell: false)
        DomainFactory.createSeqType(name: 'SEQTYPE3', dirName: 'SEQTYPE3', libraryLayout: LibraryLayout.MATE_PAIR, singleCell: false)
        DomainFactory.createSeqType(name: 'SEQTYPE', dirName: 'SEQTYPE_SC', libraryLayout: LibraryLayout.PAIRED, singleCell: true)

        when:
        controller.params.name = name
        controller.params.single = single
        controller.params.paired = paired
        controller.params.mate_pair = mate_pair
        controller.params.singleCell = singleCell
        controller.params.anyLayout = single || paired || mate_pair
        controller.params.seqTypeService = controller.seqTypeService
        controller.createLayout()

        then:
        controller.response.status == 200
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

        SeqType seqTypeSingle = DomainFactory.createSeqType(name: 'SEQTYPE', dirName: 'SEQTYPE', libraryLayout: LibraryLayout.SINGLE)
        SeqType seqTypePaired = DomainFactory.createSeqType(name: 'SEQTYPE', dirName: 'SEQTYPE', libraryLayout: LibraryLayout.PAIRED)

        when:
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
                DomainFactory.createSeqType(name: "SEQTYPE0", dirName: "SEQTYPE0", libraryLayout: LibraryLayout.SINGLE),
                DomainFactory.createSeqType(name: "SEQTYPE1", dirName: "SEQTYPE1", importAlias: ['importAlias1'], libraryLayout: LibraryLayout.SINGLE),
        ]

        SeqType seqType = seqTypes[suffix]

        when:
        controller.params.id = seqType.id
        controller.params.importAlias = importAlias
        controller.params.seqTypeService = controller.seqTypeService
        controller.createSeqTypeImportAlias()

        then:
        controller.response.status == 200
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
        controller.params.id = antibodyTarget.id
        controller.params.importAlias = importAlias
        controller.params.antibodyTargetService = controller.antibodyTargetService
        controller.createAntibodyTargetImportAlias()

        then:
        controller.response.status == 200
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
        controller.params.id = antibodyTarget.id
        controller.params.importAlias = newAlias
        controller.params.antibodyTargetService = controller.antibodyTargetService
        controller.createAntibodyTargetImportAlias()

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        importAliases                    | newAlias
        []                               | null
        ['']                             | ''
        ['importAlias']                  | 'importAlias'
        ['importAlias1', 'importAlias2'] | 'importAlias2'
    }
}
