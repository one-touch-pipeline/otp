package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.testing.UserAndRoles
import grails.plugin.springsecurity.SpringSecurityUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

class MetaDataFieldsControllerIntegrationSpec extends Specification implements UserAndRoles {

    MetaDataFieldsController controller = new MetaDataFieldsController()

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
        SpringSecurityUtils.doWithAuth("operator"){
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
        LibraryPreparationKit libraryPreparationKit = new LibraryPreparationKit(name: 'LibraryPreparationKit', shortDisplayName: 'LPK')
        libraryPreparationKit.save(flush: true)
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = new LibraryPreparationKitSynonym(name: 'LibraryPreparationKitSynonym', libraryPreparationKit: libraryPreparationKit)
        libraryPreparationKitSynonym.save(flush: true)

        when:
        controller.params.name = name
        controller.params.shortDisplayName = shortDisplayName
        SpringSecurityUtils.doWithAuth("operator"){
            controller.createLibraryPreparationKit()
        }

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        name                            | shortDisplayName
        ''                              | 'LPK2'
        'LibraryPreparationKit'         | 'LPK2'
        'LibraryPreparationKitSynonym'  | 'LPK2'
        'LibraryPreparationKit2'        | ''
        'LibraryPreparationKit2'        | 'LPK'
    }

    void "test JSON createLibraryPreparationKitSynonym valid input"() {
        given:
        LibraryPreparationKit libraryPreparationKit = new LibraryPreparationKit(name: 'LibraryPreparationKit', shortDisplayName: 'LibraryPreparationKit')
        libraryPreparationKit.save(flush: true)
        controller.params.id = 'LibraryPreparationKit'

        when:
        controller.params.alias = 'LibraryPreparationKitSynonym'
        SpringSecurityUtils.doWithAuth("operator"){
            controller.createLibraryPreparationKitAlias()
        }

        then:
        controller.response.status == 200
        controller.response.json.success
        LibraryPreparationKitSynonym.findByName('LibraryPreparationKitSynonym')
    }

    @Unroll
    void "test JSON createLibraryPreparationKitSynonym invalid input"() {
        given:
        LibraryPreparationKit libraryPreparationKit = new LibraryPreparationKit(name: 'LibraryPreparationKit', shortDisplayName: 'LibraryPreparationKit')
        libraryPreparationKit.save(flush: true)
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = new LibraryPreparationKitSynonym(name: 'LibraryPreparationKitSynonym', libraryPreparationKit: libraryPreparationKit)
        libraryPreparationKitSynonym.save(flush: true)
        controller.params.id = 'LibraryPreparationKit'

        when:
        controller.params.alias = alias
        SpringSecurityUtils.doWithAuth("operator"){
            controller.createLibraryPreparationKitAlias()
        }

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        alias <<  ['', 'LibraryPreparationKit', 'LibraryPreparationKitSynonym']
    }

    void "test JSON createAntibodyTarget valid input"() {
        when:
        controller.params.name = 'AntibodyTarget'
        SpringSecurityUtils.doWithAuth("operator"){
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
        AntibodyTarget antibodyTarget = new AntibodyTarget(name: 'AntibodyTarget')
        antibodyTarget.save(flush: true)

        when:
        controller.params.name = name
        SpringSecurityUtils.doWithAuth("operator"){
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
        SpringSecurityUtils.doWithAuth("operator"){
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
        SeqCenter seqCenter = new SeqCenter(name: 'SEQCENTER', dirName: 'seqcenter')
        seqCenter.save(flush: true)

        when:
        controller.params.name = name
        controller.params.dirName = dirName
        SpringSecurityUtils.doWithAuth("operator"){
            controller.createSeqCenter()
        }

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        name            | dirName
        ''              | ''
        'SEQCENTER'     | ''
        'SEQCENTER2'    | ''
        'SEQCENTER2'    | 'seqcenter'
        ''              | 'seqcenter2'
        'SEQCENTER'     | 'seqcenter2'
    }

    @Unroll
    void "test JSON createSeqPlatform valid input"() {
        given:
        SeqPlatformGroup seqPlatformGroup = new SeqPlatformGroup(name: "SeqPlatformGroup")
        seqPlatformGroup.save(flush: true)
        SeqPlatformModelLabel seqPlatformModelLabel = new SeqPlatformModelLabel(name: "SeqPlatformModelLabel")
        seqPlatformModelLabel.save(flush: true)
        SequencingKitLabel sequencingKitLabel = new SequencingKitLabel(name: "SequencingKitLabel")
        sequencingKitLabel.save(flush: true)

        when:
        controller.params.platform = platform
        controller.params.group = group
        controller.params.model = model
        controller.params.kit = kit
        SpringSecurityUtils.doWithAuth("operator"){
            controller.createSeqPlatform()
        }

        then:
        controller.response.status == 200
        controller.response.json.success
        SeqPlatform.findByNameAndSeqPlatformModelLabelAndSequencingKitLabelAndSeqPlatformGroup(
                platform,
                controller.params.model? seqPlatformModelLabel:null,
                controller.params.kit? sequencingKitLabel:null,
                controller.params.group? seqPlatformGroup:null
        )

        where:
        platform            | group                     | model                         | kit
        'SeqPlatform'       | ''                        | 'SeqPlatformModelLabel'       | ''
        'SeqPlatform'       | 'SeqPlatformGroup'        | 'SeqPlatformModelLabel'       | ''
        'SeqPlatform'       | ''                        | 'SeqPlatformModelLabel'       | 'SequencingKitLabel'
        'SeqPlatform'       | 'SeqPlatformGroup'        | 'SeqPlatformModelLabel'       | 'SequencingKitLabel'
    }

    @Unroll
    void "test JSON createSeqPlatform valid input with preexisting SeqPlatform"() {
        given:
        SeqPlatformGroup seqPlatformGroup = new SeqPlatformGroup(name: "SeqPlatformGroup")
        seqPlatformGroup.save(flush: true)
        SeqPlatformGroup seqPlatformGroup2 = new SeqPlatformGroup(name: "SeqPlatformGroup2")
        seqPlatformGroup2.save(flush: true)
        SeqPlatformModelLabel seqPlatformModelLabel = new SeqPlatformModelLabel(name: "SeqPlatformModelLabel")
        seqPlatformModelLabel.save(flush: true)
        SeqPlatformModelLabel seqPlatformModelLabel2 = new SeqPlatformModelLabel(name: "SeqPlatformModelLabel2")
        seqPlatformModelLabel2.save(flush: true)
        SequencingKitLabel sequencingKitLabel = new SequencingKitLabel(name: "SequencingKitLabel")
        sequencingKitLabel.save(flush: true)
        SequencingKitLabel sequencingKitLabel2 = new SequencingKitLabel(name: "SequencingKitLabel2")
        sequencingKitLabel2.save(flush: true)
        SeqPlatform seqPlatform = new SeqPlatform(
                name: "SeqPlatform",
                seqPlatformModelLabel: seqPlatformModelLabel,
                sequencingKitLabel: sequencingKitLabel,
                seqPlatformGroup: seqPlatformGroup
        )

        when:
        controller.params.platform = platform
        controller.params.group = group
        controller.params.model = model
        controller.params.kit = kit
        SpringSecurityUtils.doWithAuth("operator"){
            controller.createSeqPlatform()
        }

        then:
        controller.response.status == 200
        controller.response.json.success
        SeqPlatform.findByNameAndSeqPlatformModelLabelAndSequencingKitLabelAndSeqPlatformGroup(
                platform,
                controller.params.model? SeqPlatformModelLabel.findByName(model):null,
                controller.params.kit? SequencingKitLabel.findByName(kit):null,
                controller.params.group? SeqPlatformGroup.findByName(group):null
        )

        where:
        platform            | group                     | model                         | kit
        'SeqPlatform2'      | 'SeqPlatformGroup'        | 'SeqPlatformModelLabel'       | 'SequencingKitLabel'
        'SeqPlatform'       | 'SeqPlatformGroup'        | 'SeqPlatformModelLabel2'      | 'SequencingKitLabel'
        'SeqPlatform'       | 'SeqPlatformGroup'        | 'SeqPlatformModelLabel'       | 'SequencingKitLabel2'
    }

    @Unroll
    void "test JSON createSeqPlatform invalid input"() {
        given:
        SeqPlatformGroup seqPlatformGroup = new SeqPlatformGroup(name: "SeqPlatformGroup")
        seqPlatformGroup.save(flush: true)
        SeqPlatformModelLabel seqPlatformModelLabel = new SeqPlatformModelLabel(name: "SeqPlatformModelLabel")
        seqPlatformModelLabel.save(flush: true)
        SequencingKitLabel sequencingKitLabel = new SequencingKitLabel(name: "SequencingKitLabel")
        sequencingKitLabel.save(flush: true)
        SeqPlatform seqPlatform = new SeqPlatform(
                name: "SeqPlatform",
                seqPlatformModelLabel: seqPlatformModelLabel,
                sequencingKitLabel: sequencingKitLabel,
                seqPlatformGroup: seqPlatformGroup
        )
        seqPlatform.save(flush: true)

        when:
        controller.params.platform = platform
        controller.params.group = group
        controller.params.model = model
        controller.params.kit = kit
        SpringSecurityUtils.doWithAuth("operator"){
            controller.createSeqPlatform()
        }

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        platform            | group                     | model                         | kit
        ''                  | ''                        | ''                            | ''
        ''                  | 'SeqPlatformGroup'        | ''                            | ''
        ''                  | ''                        | 'SeqPlatformModelLabel'       | ''
        ''                  | 'SeqPlatformGroup'        | 'SeqPlatformModelLabel'       | ''
        ''                  | ''                        | ''                            | 'SequencingKitLabel'
        ''                  | 'SeqPlatformGroup'        | ''                            | 'SequencingKitLabel'
        ''                  | ''                        | 'SeqPlatformModelLabel'       | 'SequencingKitLabel'
        'SeqPlatform'       | ''                        | 'SeqPlatformModelLabel'       | 'SequencingKitLabel'
        ''                  | 'SeqPlatformGroup'        | 'SeqPlatformModelLabel'       | 'SequencingKitLabel'
        'SeqPlatform'       | 'SeqPlatformGroup'        | 'SeqPlatformModelLabel'       | 'SequencingKitLabel'
    }

    void "test JSON createModelAlias valid input"() {
        given:
        SeqPlatformModelLabel seqPlatformModelLabel = new SeqPlatformModelLabel(name: "SeqPlatformModelLabel", alias: [])
        seqPlatformModelLabel.save(flush: true)
        controller.params.id = "SeqPlatformModelLabel"

        when:
        controller.params.alias = 'SeqPlatformModelLabelAlias'
        SpringSecurityUtils.doWithAuth("operator"){
            controller.createModelAlias()
        }

        then:
        controller.response.status == 200
        controller.response.json.success
        SeqPlatformModelLabelService.findSeqPlatformModelLabelByNameOrAlias('SeqPlatformModelLabelAlias')
    }

    @Unroll
    void "test JSON createModelAlias invalid input"() {
        given:
        SeqPlatformModelLabel seqPlatformModelLabel = new SeqPlatformModelLabel(name: "SeqPlatformModelLabel", alias: "SeqPlatformModelLabelAlias")
        seqPlatformModelLabel.save(flush: true)
        controller.params.id = "SeqPlatformModelLabel"

        when:
        controller.params.alias = alias
        SpringSecurityUtils.doWithAuth("operator"){
            controller.createModelAlias()
        }

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        alias << ['', 'SeqPlatformModelLabel' , 'SeqPlatformModelLabelAlias']
    }

    void "test JSON createSequencingKitAlias valid input"() {
        given:
        SequencingKitLabel sequencingKitLabel = new SequencingKitLabel(name: "SequencingKitLabel", alias: [])
        sequencingKitLabel.save(flush: true)
        controller.params.id = "SequencingKitLabel"

        when:
        controller.params.alias = 'SequencingKitLabelAlias'
        SpringSecurityUtils.doWithAuth("operator"){
            controller.createSequencingKitAlias()
        }

        then:
        controller.response.status == 200
        controller.response.json.success
        SequencingKitLabelService.findSequencingKitLabelByNameOrAlias('SequencingKitLabelAlias')
    }

    @Unroll
    void "test JSON createSequencingKitAlias invalid input"() {
        given:
        SequencingKitLabel sequencingKitLabel = new SequencingKitLabel(name: "SequencingKitLabel", alias: "SequencingKitLabelAlias")
        sequencingKitLabel.save(flush: true)
        controller.params.id = "SequencingKitLabel"

        when:
        controller.params.alias = alias
        SpringSecurityUtils.doWithAuth("operator"){
            controller.createSequencingKitAlias()
        }

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        alias << ['', 'SequencingKitLabel', 'SequencingKitLabelAlias']
    }

    @Unroll
    void "test JSON createSeqType valid input"() {
        when:
        controller.params.type = type
        controller.params.dirName = dirName
        controller.params.alias = alias
        controller.params.single = single
        controller.params.paired = paired
        controller.params.mate_pair = mate_pair
        controller.params.anyLayout = single||paired||mate_pair
        SpringSecurityUtils.doWithAuth("operator"){
            controller.createSeqType()
        }

        then:
        controller.response.status == 200
        controller.response.json.success
        !single || SeqType.findByNameAndDirNameAndLibraryLayout(type, dirName, SeqType.LIBRARYLAYOUT_SINGLE)
        !paired || SeqType.findByNameAndDirNameAndLibraryLayout(type, dirName, SeqType.LIBRARYLAYOUT_PAIRED)
        !mate_pair || SeqType.findByNameAndDirNameAndLibraryLayout(type, dirName, SeqType.LIBRARYLAYOUT_MATE_PAIR)

        where:
        type        | dirName       | alias         | single    | paired    | mate_pair
        'SEQTYPE'   | 'seqtype'     | ''            | true      | false     | false
        'SEQTYPE'   | 'seqtype'     | ''            | false     | true      | false
        'SEQTYPE'   | 'seqtype'     | ''            | true      | true      | false
        'SEQTYPE'   | 'seqtype'     | ''            | false     | false     | true
        'SEQTYPE'   | 'seqtype'     | ''            | true      | false     | true
        'SEQTYPE'   | 'seqtype'     | ''            | false     | true      | true
        'SEQTYPE'   | 'seqtype'     | ''            | true      | true      | true
        'SEQTYPE'   | 'seqtype'     | 'SEQ TYPE'    | true      | false     | false
    }

    @Unroll
    void "test JSON createSeqType invalid input"() {
        given:
        SeqType seqType = new SeqType(name: 'SEQTYPE', dirName: 'seqtype', alias: 'SEQ TYPE',libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE)
        seqType.save(flush: true)

        when:
        controller.params.type = type
        controller.params.dirName = dirName
        controller.params.alias = alias
        controller.params.single = single
        controller.params.paired = paired
        controller.params.mate_pair = mate_pair
        controller.params.anyLayout = single||paired||mate_pair
        SpringSecurityUtils.doWithAuth("operator"){
            controller.createSeqType()
        }

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        type        | dirName       | alias         | single    | paired    | mate_pair
        ''          | ''            | ''            | false     | false     | false
        ''          | ''            | ''            | true      | false     | false
        'SEQTYPE2'  | ''            | ''            | true      | false     | false
        'SEQTYPE'   | 'seqtype2'    | ''            | true      | false     | false
        'SEQ TYPE'  | 'seqtype2'    | ''            | true      | false     | false
        'SEQTYPE2'  | 'seqtype'     | ''            | true      | false     | false
        'SEQTYPE2'  | 'seqtype2'    | 'SEQTYPE'     | true      | false     | false
        'SEQTYPE2'  | 'seqtype2'    | 'SEQ TYPE'    | true      | false     | false
    }

    @Unroll
    void "test JSON createLayout valid input"() {
        given:
        SeqType seqType = new SeqType(name: 'SEQTYPE', dirName: 'SEQTYPE',libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE)
        seqType.save(flush: true)
        SeqType seqType2 = new SeqType(name: 'SEQTYPE2', dirName: 'SEQTYPE2',libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)
        seqType2.save(flush: true)
        SeqType seqType3 = new SeqType(name: 'SEQTYPE3', dirName: 'SEQTYPE3',libraryLayout: SeqType.LIBRARYLAYOUT_MATE_PAIR)
        seqType3.save(flush: true)

        when:
        controller.params.id = id
        controller.params.single = single
        controller.params.paired = paired
        controller.params.mate_pair = mate_pair
        controller.params.anyLayout = single||paired||mate_pair
        SpringSecurityUtils.doWithAuth("operator"){
            controller.createLayout()
        }

        then:
        controller.response.status == 200
        controller.response.json.success
        !single || SeqType.findByNameAndDirNameAndLibraryLayout(id, id, SeqType.LIBRARYLAYOUT_SINGLE)
        !paired || SeqType.findByNameAndDirNameAndLibraryLayout(id, id, SeqType.LIBRARYLAYOUT_PAIRED)
        !mate_pair || SeqType.findByNameAndDirNameAndLibraryLayout(id, id, SeqType.LIBRARYLAYOUT_MATE_PAIR)

        where:
        id          | single    | paired    | mate_pair
        'SEQTYPE'   | false     | true      | false
        'SEQTYPE'   | false     | false     | true
        'SEQTYPE'   | false     | true      | true
        'SEQTYPE2'  | true      | false     | false
        'SEQTYPE2'  | false     | false     | true
        'SEQTYPE2'  | true      | false     | true
        'SEQTYPE3'  | false     | true      | false
        'SEQTYPE3'  | true      | false     | false
        'SEQTYPE3'  | true      | true      | false
    }

    @Unroll
    void "test JSON createLayout invalid input"() {
        given:
        SeqType seqType = new SeqType(name: 'SEQTYPE', dirName: 'SEQTYPE',libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE)
        seqType.save(flush: true)
        SeqType seqType2 = new SeqType(name: 'SEQTYPE2', dirName: 'SEQTYPE2',libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)
        seqType2.save(flush: true)
        SeqType seqType3 = new SeqType(name: 'SEQTYPE3', dirName: 'SEQTYPE3',libraryLayout: SeqType.LIBRARYLAYOUT_MATE_PAIR)
        seqType3.save(flush: true)

        when:
        controller.params.id = id
        controller.params.single = single
        controller.params.paired = paired
        controller.params.mate_pair = mate_pair
        controller.params.anyLayout = single||paired||mate_pair
        SpringSecurityUtils.doWithAuth("operator"){
            controller.createLayout()
        }

        then:
        controller.response.status == 200
        !controller.response.json.success

        where:
        id          | single    | paired    | mate_pair
        'SEQTYPE'   | false     | false     | false
        'SEQTYPE2'  | false     | false     | false
        'SEQTYPE3'  | false     | false     | false
        'SEQTYPE'   | true      | false     | false
        'SEQTYPE2'  | false     | true      | false
        'SEQTYPE3'  | false     | false     | true
        'SEQTYPE'   | true      | true      | true
        'SEQTYPE2'  | true      | true      | true
        'SEQTYPE3'  | true      | true      | true
    }
}
