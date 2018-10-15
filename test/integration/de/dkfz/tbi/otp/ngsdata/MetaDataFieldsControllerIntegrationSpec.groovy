package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.testing.UserAndRoles
import grails.plugin.springsecurity.SpringSecurityUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

class MetaDataFieldsControllerIntegrationSpec extends Specification implements UserAndRoles {

    MetaDataFieldsController controller = new MetaDataFieldsController()

    SeqPlatformModelLabelService seqPlatformModelLabelService = new SeqPlatformModelLabelService()
    SequencingKitLabelService sequencingKitLabelService = new SequencingKitLabelService()

    @Rule
    TemporaryFolder temporaryFolder

    def setup() {
        createUserAndRoles()
    }

    void "test JSON createLibraryPreparationKit valid input"() {
        when:
        controller.params.name = 'LibraryPreparationKit'
        controller.params.shortDisplayName = 'LPK'
        controller.params.adapterFile = '/asdf'
        controller.params.reverseComplementAdapterSequence = 'GATC'
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createLibraryPreparationKit()
        }

        then:
        controller.response.status == 200
        controller.response.json.success
        LibraryPreparationKit.findByName('LibraryPreparationKit')
    }

    @Unroll
    void "test JSON createLibraryPreparationKit invalid input"() {
        given:
        DomainFactory.createLibraryPreparationKit(name: 'LibraryPreparationKit', shortDisplayName: 'LPK', importAlias: ['LibraryPreparationKitImportAlias'])

        when:
        controller.params.name = name
        controller.params.shortDisplayName = shortDisplayName
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createLibraryPreparationKit()
        }

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
        LibraryPreparationKit libraryPreparationKit = DomainFactory.createLibraryPreparationKit(name: 'LibraryPreparationKit', shortDisplayName: 'LPK')
        controller.params.name = libraryPreparationKit.name

        when:
        controller.params.importAlias = 'LibraryPreparationKitImportAlias'
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createLibraryPreparationKitImportAlias()
        }

        then:
        controller.response.status == 200
        controller.response.json.success
        libraryPreparationKit.importAlias.contains('LibraryPreparationKitImportAlias')
    }

    @Unroll
    void "test JSON createLibraryPreparationKitImportAlias invalid input"() {
        given:
        LibraryPreparationKit libraryPreparationKit = DomainFactory.createLibraryPreparationKit(name: 'LibraryPreparationKit', shortDisplayName: 'LPK', importAlias: ['LibraryPreparationKitImportAlias'])
        controller.params.name = libraryPreparationKit.name

        when:
        controller.params.importAlias = importAlias
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createLibraryPreparationKitImportAlias()
        }

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        importAlias << ['', 'LibraryPreparationKit', 'LibraryPreparationKitImportAlias']
    }

    void "test JSON createAntibodyTarget valid input"() {
        when:
        controller.params.name = 'AntibodyTarget'
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createAntibodyTarget()
        }

        then:
        controller.response.status == 200
        controller.response.json.success
        AntibodyTarget.findByName('AntibodyTarget')
    }

    @Unroll
    void "test JSON createAntibodyTarget invalid input"() {
        given:
        DomainFactory.createAntibodyTarget(name: 'AntibodyTarget')

        when:
        controller.params.name = name
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createAntibodyTarget()
        }

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        name << ['', 'Antibody Target', 'AntibodyTarget', 'antibodytarget', 'ANTIBODYTARGET']
    }

    void "test JSON createSeqCenter valid input"() {
        when:
        controller.params.name = 'SEQCENTER'
        controller.params.dirName = 'seqcenter'
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createSeqCenter()
        }

        then:
        controller.response.status == 200
        controller.response.json.success
        SeqCenter.findByNameAndDirName('SEQCENTER', 'seqcenter')
    }

    @Unroll
    void "test JSON createSeqCenter invalid input"() {
        given:
        DomainFactory.createSeqCenter(name: 'SEQCENTER', dirName: 'seqcenter')

        when:
        controller.params.name = name
        controller.params.dirName = dirName
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createSeqCenter()
        }

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
        SeqPlatformModelLabel seqPlatformModelLabel = DomainFactory.createSeqPlatformModelLabel(name: "SeqPlatformModelLabel")
        SequencingKitLabel sequencingKitLabel = DomainFactory.createSequencingKitLabel(name: "SequencingKitLabel")

        when:
        controller.params.platform = platform
        controller.params.model = model
        controller.params.kit = kit
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createSeqPlatform()
        }

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
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createSeqPlatform()
        }

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
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createSeqPlatform()
        }

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

    void "test JSON createModelAlias valid input"() {
        given:
        DomainFactory.createSeqPlatformModelLabel(name: "SeqPlatformModelLabel")
        controller.params.name = "SeqPlatformModelLabel"

        when:
        controller.params.importAlias = 'SeqPlatformModelLabelAlias'
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createModelImportAlias()
        }

        then:
        controller.response.status == 200
        controller.response.json.success
        seqPlatformModelLabelService.findByNameOrImportAlias('SeqPlatformModelLabelAlias')
    }

    @Unroll
    void "test JSON createModelAlias invalid input"() {
        given:
        DomainFactory.createSeqPlatformModelLabel(name: "SeqPlatformModelLabel", importAlias: ["SeqPlatformModelLabelAlias"])
        controller.params.name = "SeqPlatformModelLabel"

        when:
        controller.params.importAlias = importAlias
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createModelImportAlias()
        }

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        importAlias << ['', 'SeqPlatformModelLabel', 'SeqPlatformModelLabelAlias']
    }

    void "test JSON createSequencingKitAlias valid input"() {
        given:
        DomainFactory.createSequencingKitLabel(name: "SequencingKitLabel")
        controller.params.name = "SequencingKitLabel"

        when:
        controller.params.importAlias = 'SequencingKitLabelAlias'
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createSequencingKitImportAlias()
        }

        then:
        controller.response.status == 200
        controller.response.json.success
        sequencingKitLabelService.findByNameOrImportAlias('SequencingKitLabelAlias')
    }

    @Unroll
    void "test JSON createSequencingKitAlias invalid input"() {
        given:
        DomainFactory.createSequencingKitLabel(name: "SequencingKitLabel", importAlias: ["SequencingKitLabelAlias"])
        controller.params.name = "SequencingKitLabel"

        when:
        controller.params.importAlias = importAlias
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createSequencingKitImportAlias()
        }

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        importAlias << ['', 'SequencingKitLabel', 'SequencingKitLabelAlias']
    }

    @Unroll
    void "test JSON createSeqType valid input"() {
        when:
        controller.params.type = 'SEQTYPE'
        controller.params.dirName = 'seqtype'
        controller.params.displayName = 'SEQ TYPE'
        controller.params.single = single
        controller.params.paired = paired
        controller.params.mate_pair = mate_pair
        controller.params.singleCell = singleCell
        controller.params.anyLayout = single || paired || mate_pair
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createSeqType()
        }

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
        DomainFactory.createSeqType(name: 'SEQTYPE', dirName: 'seqtype', displayName: 'SEQ TYPE', importAlias: ['importAlias'], libraryLayout: LibraryLayout.SINGLE)

        when:
        controller.params.type = type
        controller.params.dirName = dirName
        controller.params.displayName = displayName
        controller.params.single = single
        controller.params.paired = paired
        controller.params.mate_pair = mate_pair
        controller.params.singleCell = singleCell
        controller.params.anyLayout = single || paired || mate_pair
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createSeqType()
        }

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        type       | dirName    | displayName | single | paired | mate_pair| singleCell
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
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createLayout()
        }

        then:
        controller.response.status == 200
        controller.response.json.success
        !single || SeqType.findByNameAndLibraryLayoutAndSingleCell(name, LibraryLayout.SINGLE,singleCell)
        !paired || SeqType.findByNameAndLibraryLayoutAndSingleCell(name, LibraryLayout.PAIRED,singleCell)
        !mate_pair || SeqType.findByNameAndLibraryLayoutAndSingleCell(name, LibraryLayout.MATE_PAIR,singleCell)

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
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createLayout()
        }

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

    void "test JSON createSeqTypeAlias valid input"() {
        given:
        SeqType seqTypeSingle = DomainFactory.createSeqType(name: 'SEQTYPE', dirName: 'SEQTYPE', libraryLayout: LibraryLayout.SINGLE)
        SeqType seqTypePaired = DomainFactory.createSeqType(name: 'SEQTYPE', dirName: 'SEQTYPE', libraryLayout: LibraryLayout.PAIRED)

        when:
        controller.params.name = name
        controller.params.importAlias = importAlias
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createSeqTypeImportAlias()
        }

        then:
        controller.response.status == 200
        controller.response.json.success
        seqTypeSingle.importAlias.contains(importAlias)
        seqTypePaired.importAlias.contains(importAlias)

        where:
        name      | importAlias
        'SEQTYPE' | 'importAlias1'
        'SEQTYPE' | 'importAlias2'
    }

    @Unroll
    void "test JSON createSeqTypeAlias invalid input"() {
        given:
        DomainFactory.createSeqType(name: 'SEQTYPE1', dirName: 'SEQTYPE1', libraryLayout: LibraryLayout.SINGLE)
        DomainFactory.createSeqType(name: 'SEQTYPE2', dirName: 'SEQTYPE2', importAlias: ['importAlias2'], libraryLayout: LibraryLayout.SINGLE)

        when:
        controller.params.name = name
        controller.params.importAlias = importAlias
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createSeqTypeImportAlias()
        }

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        name       | importAlias
        'SEQTYPE1' | null
        'SEQTYPE1' | ''
        'SEQTYPE1' | 'importAlias2'
        'SEQTYPE2' | 'importAlias2'
    }

    void "test JSON createAntibodyTargetAlias valid input"() {
        given:
        DomainFactory.createAntibodyTarget(name: 'ANTIBODYTARGET')

        when:
        controller.params.name = name
        controller.params.importAlias = importAlias
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createAntibodyTargetImportAlias()
        }

        then:
        controller.response.status == 200
        controller.response.json.success

        where:
        name             | importAlias
        'ANTIBODYTARGET' | 'importAlias1'
        'ANTIBODYTARGET' | 'importAlias2'
    }

    @Unroll
    void "test JSON createAntibodyTargetAlias invalid input"() {
        given:
        DomainFactory.createAntibodyTarget(name: 'ANTIBODYTARGET1')
        DomainFactory.createAntibodyTarget(name: 'ANTIBODYTARGET2', importAlias: ['importAlias2'])

        when:
        controller.params.name = name
        controller.params.importAlias = importAlias
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.createAntibodyTargetImportAlias()
        }

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        name              | importAlias
        'ANTIBODYTARGET1' | null
        'ANTIBODYTARGET1' | ''
        'ANTIBODYTARGET1' | 'importAlias2'
        'ANTIBODYTARGET2' | 'importAlias2'
    }
}
