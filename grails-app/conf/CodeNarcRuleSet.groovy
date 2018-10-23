final String TEST = "*/*test*/*"
final String SPEC = "*Spec.groovy"
final String INTEGRATION_SPEC = "*IntegrationSpec.groovy"
final String CONTROLLER = "*Controller.groovy"
final String SERVICE = "*Service.groovy"
final int DEFAULT = 1 //Value for rules that we have no explicitly discussed yet

ruleset {

    description '''
        A Sample Groovy RuleSet containing all CodeNarc Rules, grouped by category.
        You can use this as a template for your own custom RuleSet.
        Just delete the rules that you don't want to include.
        '''

    // rulesets/basic.xml
    AssertWithinFinallyBlock {
        priority = 1
    }
    AssignmentInConditional {
        priority = DEFAULT
    }
    BigDecimalInstantiation {
        priority = DEFAULT
    }
    BitwiseOperatorInConditional {
        priority = DEFAULT
    }
    BooleanGetBoolean {
        priority = DEFAULT
    }
    BrokenNullCheck {
        priority = DEFAULT
    }
    BrokenOddnessCheck {
        priority = DEFAULT
    }
    ClassForName {
        priority = 1
    }
    ComparisonOfTwoConstants {
        priority = DEFAULT
    }
    ComparisonWithSelf {
        priority = 1
    }
    ConstantAssertExpression {
        priority = 3
    }
    ConstantIfExpression {
        priority = DEFAULT
    }
    ConstantTernaryExpression {
        priority = DEFAULT
    }
    DeadCode {
        priority = 1
    }
    DoubleNegative {
        priority = DEFAULT
    }
    DuplicateCaseStatement {
        priority = DEFAULT
    }
    DuplicateMapKey {
        priority = DEFAULT
    }
    DuplicateSetValue {
        priority = 2
    }
    EmptyCatchBlock {
        priority = 2
    }
    EmptyClass {
        priority = DEFAULT
    }
    EmptyElseBlock {
        priority = DEFAULT
    }
    EmptyFinallyBlock {
        priority = DEFAULT
    }
    EmptyForStatement {
        priority = DEFAULT
    }
    EmptyIfStatement {
        priority = 1
    }
    EmptyInstanceInitializer {
        priority = DEFAULT
    }
    EmptyMethod {
        priority = 2
        doNotApplyToFileNames = CONTROLLER
    }
    EmptyStaticInitializer {
        priority = DEFAULT
    }
    EmptySwitchStatement {
        priority = DEFAULT
    }
    EmptySynchronizedStatement {
        priority = DEFAULT
    }
    EmptyTryBlock {
        priority = DEFAULT
    }
    EmptyWhileStatement {
        priority = DEFAULT
    }
    EqualsAndHashCode {
        priority = DEFAULT
    }
    EqualsOverloaded {
        priority = DEFAULT
    }
    ExplicitGarbageCollection {
        priority = DEFAULT
    }
    ForLoopShouldBeWhileLoop {
        priority = DEFAULT
    }
    HardCodedWindowsFileSeparator {
        priority = DEFAULT
    }
    HardCodedWindowsRootDirectory {
        priority = DEFAULT
    }
    IntegerGetInteger {
        priority = DEFAULT
    }
    MultipleUnaryOperators {
        priority = DEFAULT
    }
    RandomDoubleCoercedToZero {
        priority = DEFAULT
    }
    RemoveAllOnSelf {
        priority = DEFAULT
    }
    ReturnFromFinallyBlock {
        priority = DEFAULT
    }
    ThrowExceptionFromFinallyBlock {
        priority = DEFAULT
    }

    // rulesets/braces.xml
    ElseBlockBraces {
        priority = 1
    }
    ForStatementBraces {
        priority = 1
    }
    IfStatementBraces {
        priority = 1
    }
    WhileStatementBraces {
        priority = 1
    }

    // rulesets/concurrency.xml
    BusyWait {
        priority = 1
    }
    DoubleCheckedLocking {
        priority = 1
    }
    InconsistentPropertyLocking {
        priority = DEFAULT
    }
    InconsistentPropertySynchronization {
        priority = DEFAULT
    }
    NestedSynchronization {
        priority = DEFAULT
    }
    StaticCalendarField {
        priority = DEFAULT
    }
    StaticConnection {
        priority = DEFAULT
    }
    StaticDateFormatField {
        priority = DEFAULT
    }
    StaticMatcherField {
        priority = DEFAULT
    }
    StaticSimpleDateFormatField {
        priority = DEFAULT
    }
    SynchronizedMethod {
        priority = 1
    }
    SynchronizedOnBoxedPrimitive {
        priority = DEFAULT
    }
    SynchronizedOnGetClass {
        priority = DEFAULT
    }
    SynchronizedOnReentrantLock {
        priority = DEFAULT
    }
    SynchronizedOnString {
        priority = DEFAULT
    }
    SynchronizedOnThis {
        priority = 1
    }
    SynchronizedReadObjectMethod {
        priority = DEFAULT
    }
    SystemRunFinalizersOnExit {
        priority = DEFAULT
    }
    ThisReferenceEscapesConstructor {
        priority = 2
    }
    ThreadGroup {
        priority = DEFAULT
    }
    ThreadLocalNotStaticFinal {
        priority = 1
    }
    ThreadYield {
        priority = DEFAULT
    }
    UseOfNotifyMethod {
        priority = DEFAULT
    }
    VolatileArrayField {
        priority = DEFAULT
    }
    VolatileLongOrDoubleField {
        priority = DEFAULT
    }
    WaitOutsideOfWhileLoop {
        priority = DEFAULT
    }

    // rulesets/convention.xml
    ConfusingTernary {
        priority = 1
    }
    CouldBeElvis {
        priority = 1
    }
    CouldBeSwitchStatement {
        priority = 1
    }
    //FieldTypeRequired //does not work well with Grails
    HashtableIsObsolete {
        priority = 1
    }
    IfStatementCouldBeTernary {
        priority = 2
    }
    InvertedCondition {
        priority = 3
    }
    InvertedIfElse {
        priority = 2
    }
    LongLiteralWithLowerCaseL {
        priority = 1
    }
    MethodParameterTypeRequired {
        priority = 1
    }
    MethodReturnTypeRequired {
        priority = 2
        doNotApplyToFileNames = CONTROLLER
    }
    NoDef {
        priority = 2
        doNotApplyToFileNames = CONTROLLER
    }
    NoJavaUtilDate {
        priority = 2
    }
    NoTabCharacter {
        priority = 1
    }
    ParameterReassignment {
        priority = 1
    }
    //PublicMethodsBeforeNonPublicMethods //does not fit with OTP-Convention of grouping related methods by topic
    //StaticFieldsBeforeInstanceFields //does not fit with OTP-Convention of Services before class fields including statics
    //StaticMethodsBeforeInstanceMethods //does not fit with OTP-Convention of grouping related methods by topic
    TernaryCouldBeElvis {
        priority = 1
    }
    TrailingComma {
        priority = 1
    }
    VariableTypeRequired {
        priority = 2
    }
    VectorIsObsolete {
        priority = 1
    }

    // rulesets/design.xml
    AbstractClassWithPublicConstructor {
        priority = DEFAULT
    }
    AbstractClassWithoutAbstractMethod {
        applyToFileNames = SERVICE
        priority = 2
    }
    AssignmentToStaticFieldFromInstanceMethod {
        priority = 2
    }
    BooleanMethodReturnsNull {
        priority = 2
    }
    BuilderMethodWithSideEffects {
        priority = 2
    }
    CloneableWithoutClone {
        priority = DEFAULT
    }
    CloseWithoutCloseable {
        priority = 2
    }
    CompareToWithoutComparable {
        priority = DEFAULT
    }
    ConstantsOnlyInterface {
        priority = 2
    }
    EmptyMethodInAbstractClass {
        priority = 3
    }
    FinalClassWithProtectedMember {
        priority = DEFAULT
    }
    ImplementationAsType {
        priority = 1
    }
    Instanceof {
        priority = 2
    }
    LocaleSetDefault {
        priority = DEFAULT
    }
    NestedForLoop {
        priority = 1
    }
    PrivateFieldCouldBeFinal {
        priority = 2
    }
    PublicInstanceField {
        priority = 2
    }
    ReturnsNullInsteadOfEmptyArray {
        priority = 1
    }
    ReturnsNullInsteadOfEmptyCollection {
        priority = 1
    }
    SimpleDateFormatMissingLocale {
        priority = 1
    }
    StatelessSingleton {
        priority = DEFAULT
    }
    ToStringReturnsNull {
        priority = DEFAULT
    }

    // rulesets/dry.xml
    DuplicateListLiteral {
        priority = 2
    }
    //DuplicateMapLiteral //has problems with stuff like .save(flush: true)
    DuplicateNumberLiteral {
        priority = 2
    }
    //DuplicateStringLiteral //has problems with stuff like .split(",")

    // rulesets/enhanced.xml
    CloneWithoutCloneable {
        priority = DEFAULT
    }
    JUnitAssertEqualsConstantActualValue {
        priority = DEFAULT
    }
    MissingOverrideAnnotation {
        priority = 1
        doNotApplyToFileNames = INTEGRATION_SPEC
    }
    UnsafeImplementationAsMap {
        priority = DEFAULT
    }

    // rulesets/exceptions.xml
    CatchArrayIndexOutOfBoundsException {
        priority = 1
    }
    CatchError {
        priority = 1
    }
    CatchException {
        priority = 1
    }
    CatchIllegalMonitorStateException {
        priority = 1
    }
    CatchIndexOutOfBoundsException {
        priority = 1
    }
    CatchNullPointerException {
        priority = 1
    }
    CatchRuntimeException {
        priority = 1
    }
    CatchThrowable {
        priority = 1
    }
    ConfusingClassNamedException {
        priority = DEFAULT
    }
    ExceptionExtendsError {
        priority = DEFAULT
    }
    ExceptionExtendsThrowable {
        priority = DEFAULT
    }
    ExceptionNotThrown {
        priority = DEFAULT
    }
    MissingNewInThrowStatement {
        priority = 1
    }
    ReturnNullFromCatchBlock {
        priority = 1
    }
    SwallowThreadDeath {
        priority = DEFAULT
    }
    ThrowError {
        priority = 1
    }
    ThrowException {
        priority = 1
    }
    ThrowNullPointerException {
        priority = 1
    }
    ThrowRuntimeException {
        priority = 1
    }
    ThrowThrowable {
        priority = 1
    }

    // rulesets/formatting.xml
    BlankLineBeforePackage {
        priority = 3
    }
    BlockEndsWithBlankLine {
        priority = 3
    }
    BlockStartsWithBlankLine {
        priority = 2
    }
    BracesForClass {
        priority = DEFAULT
    }
    BracesForForLoop {
        priority = DEFAULT
    }
    BracesForIfElse {
        priority = DEFAULT
    }
    BracesForMethod {
        priority = DEFAULT
    }
    BracesForTryCatchFinally {
        priority = DEFAULT
    }
    //ClassJavadoc
    ClosureStatementOnOpeningLineOfMultipleLineClosure {
        priority = 1
    }
    ConsecutiveBlankLines {
        priority = 3
    }
    FileEndsWithoutNewline {
        priority = 2
    }
    Indentation {
        priority = 2
    }
    LineLength {
        length = 160
    }
    MissingBlankLineAfterImports {
        priority = 1
    }
    MissingBlankLineAfterPackage {
        priority = 1
    }
    SpaceAfterCatch {
        priority = 1
    }
    SpaceAfterClosingBrace {
        priority = 3
    }
    SpaceAfterComma {
        priority = 1
    }
    SpaceAfterFor {
        priority = 1
    }
    SpaceAfterIf {
        priority = 1
    }
    SpaceAfterOpeningBrace {
        priority = 1
    }
    SpaceAfterSemicolon {
        priority = 1
    }
    SpaceAfterSwitch {
        priority = 1
    }
    SpaceAfterWhile {
        priority = 1
    }
    SpaceAroundClosureArrow {
        priority = 1
    }
    SpaceAroundMapEntryColon {
        characterBeforeColonRegex = /.*/
        characterAfterColonRegex = /\s+/
        priority = 1
    }
    SpaceAroundOperator {
        priority = 1
    }
    SpaceBeforeClosingBrace {
        priority = 1
    }
    SpaceBeforeOpeningBrace {
        priority = 1
    }
    TrailingWhitespace {
        priority = 1
    }

    // rulesets/generic.xml
    IllegalClassMember {
        priority = DEFAULT
    }
    IllegalClassReference {
        priority = DEFAULT
    }
    IllegalPackageReference {
        priority = DEFAULT
    }
    IllegalRegex {
        priority = DEFAULT
    }
    IllegalString {
        priority = DEFAULT
    }
    IllegalSubclass {
        priority = DEFAULT
    }
    RequiredRegex {
        priority = DEFAULT
    }
    RequiredString {
        priority = DEFAULT
    }
    StatelessClass {
        priority = DEFAULT
    }

    // rulesets/grails.xml
    //GrailsDomainHasEquals //Entity provides equals()
    //GrailsDomainHasToString //we don't do this in OTP
    GrailsDomainReservedSqlKeywordName {
        priority = 1
    }
    //GrailsDomainStringPropertyMaxSize //done by hibernate
    GrailsDomainWithServiceReference {
        priority = DEFAULT
    }
    GrailsDuplicateConstraint {
        priority = 1
    }
    GrailsDuplicateMapping {
        priority = 1
    }
    GrailsMassAssignment {
        priority = DEFAULT
    }
    GrailsPublicControllerMethod {
        priority = DEFAULT
    }
    GrailsServletContextReference {
        priority = DEFAULT
    }
    //GrailsStatelessService //seems to have problems with Spring

    // rulesets/groovyism.xml
    AssignCollectionSort {
        priority = 1
    }
    AssignCollectionUnique {
        priority = 2
    }
    ClosureAsLastMethodParameter {
        priority = 2
    }
    CollectAllIsDeprecated {
        priority = DEFAULT
    }
    ConfusingMultipleReturns {
        priority = DEFAULT
    }
    ExplicitArrayListInstantiation {
        priority = 1
    }
    ExplicitCallToAndMethod {
        priority = 1
    }
    ExplicitCallToCompareToMethod {
        priority = 1
    }
    ExplicitCallToDivMethod {
        priority = 1
    }
    ExplicitCallToEqualsMethod {
        priority = 1
    }
    ExplicitCallToGetAtMethod {
        priority = 1
    }
    ExplicitCallToLeftShiftMethod {
        priority = 1
    }
    ExplicitCallToMinusMethod {
        priority = 1
    }
    ExplicitCallToModMethod {
        priority = 1
    }
    ExplicitCallToMultiplyMethod {
        priority = 1
    }
    ExplicitCallToOrMethod {
        priority = 1
    }
    ExplicitCallToPlusMethod {
        priority = 1
    }
    ExplicitCallToPowerMethod {
        priority = 1
    }
    ExplicitCallToRightShiftMethod {
        priority = 1
    }
    ExplicitCallToXorMethod {
        priority = 1
    }
    ExplicitHashMapInstantiation {
        priority = 1
    }
    ExplicitHashSetInstantiation {
        priority = 1
    }
    ExplicitLinkedHashMapInstantiation {
        priority = 1
    }
    ExplicitLinkedListInstantiation {
        priority = 1
    }
    ExplicitStackInstantiation {
        priority = 1
    }
    ExplicitTreeSetInstantiation {
        priority = 1
    }
    GStringAsMapKey {
        priority = 1
    }
    GStringExpressionWithinString {
        priority = 1
    }
    GetterMethodCouldBeProperty {
        priority = 3
    }
    GroovyLangImmutable {
        priority = 1
    }
    UseCollectMany {
        priority = 2
    }
    UseCollectNested {
        priority = DEFAULT
    }

    // rulesets/imports.xml
    DuplicateImport {
        priority = 1
    }
    ImportFromSamePackage {
        priority = 1
    }
    ImportFromSunPackages {
        priority = 2
    }
    //MisorderedStaticImports //we do the opposite
    //NoWildcardImports //does not fit with OTP-Convention of using only WildcardImports
    UnnecessaryGroovyImport {
        priority = 2
    }
    UnusedImport {
        priority = 1
    }

    // rulesets/jdbc.xml
    DirectConnectionManagement {
        priority = DEFAULT
    }
    JdbcConnectionReference {
        priority = DEFAULT
    }
    JdbcResultSetReference {
        priority = DEFAULT
    }
    JdbcStatementReference {
        priority = DEFAULT
    }

    // rulesets/junit.xml
    ChainedTest {
        priority = 2
    }
    CoupledTestCase {
        priority = DEFAULT
    }
    JUnitAssertAlwaysFails {
        priority = DEFAULT
    }
    JUnitAssertAlwaysSucceeds {
        priority = DEFAULT
    }
    JUnitFailWithoutMessage {
        priority = DEFAULT
    }
    JUnitLostTest {
        doNotApplyToFileNames = SPEC
    }
    //JUnitPublicField
    //JUnitPublicNonTestMethod
    JUnitPublicProperty {
        priority = 3
    }
    JUnitSetUpCallsSuper {
        priority = 1
    }
    //JUnitStyleAssertions //will be fixed by converting to spock
    JUnitTearDownCallsSuper {
        priority = DEFAULT
    }
    JUnitTestMethodWithoutAssert {
        doNotApplyToFileNames = SPEC
    }
    JUnitUnnecessarySetUp {
        priority = DEFAULT
    }
    JUnitUnnecessaryTearDown {
        priority = DEFAULT
    }
    //JUnitUnnecessaryThrowsException
    SpockIgnoreRestUsed {
        priority = DEFAULT
    }
    UnnecessaryFail {
        priority = DEFAULT
    }
    //UseAssertEqualsInsteadOfAssertTrue
    //UseAssertFalseInsteadOfNegation
    //UseAssertNullInsteadOfAssertEquals
    //UseAssertSameInsteadOfAssertTrue
    //UseAssertTrueInsteadOfAssertEquals
    //UseAssertTrueInsteadOfNegation

    // rulesets/logging.xml
    LoggerForDifferentClass {
        priority = DEFAULT
    }
    LoggerWithWrongModifiers {
        priority = DEFAULT
    }
    LoggingSwallowsStacktrace {
        priority = 1
    }
    MultipleLoggers {
        priority = DEFAULT
    }
    PrintStackTrace {
        priority = 1
    }
    Println {
        priority = 1
    }
    SystemErrPrint {
        priority = DEFAULT
    }
    SystemOutPrint {
        priority = 1
    }

    // rulesets/naming.xml
    AbstractClassName {
        priority = DEFAULT
    }
    ClassName {
        priority = DEFAULT
    }
    ClassNameSameAsFilename {
        priority = DEFAULT
    }
    ClassNameSameAsSuperclass {
        priority = 2
    }
    ConfusingMethodName {
        priority = DEFAULT
    }
    //FactoryMethodName //we don't do this
    FieldName {
        priority = DEFAULT
    }
    InterfaceName {
        priority = DEFAULT
    }
    InterfaceNameSameAsSuperInterface {
        priority = DEFAULT
    }
    MethodName {
        priority = 2
        doNotApplyToFileNames = TEST
    }
    ObjectOverrideMisspelledMethodName {
        priority = DEFAULT
    }
    //PackageName //we use camelCase
    PackageNameMatchesFilePath {
        priority = DEFAULT
    }
    ParameterName {
        priority = 2
    }
    PropertyName {
        priority = 2
        doNotApplyToFileNames = TEST
    }
    VariableName {
        priority = 2
        doNotApplyToFileNames = TEST
    }

    // rulesets/security.xml
    FileCreateTempFile {
        priority = DEFAULT
    }
    InsecureRandom {
        priority = 1
    }
    JavaIoPackageAccess {
        priority = 2
    }
    NonFinalPublicField {
        priority = 2
        doNotApplyToFileNames = TEST
    }
    NonFinalSubclassOfSensitiveInterface {
        priority = DEFAULT
    }
    ObjectFinalize {
        priority = DEFAULT
    }
    PublicFinalizeMethod {
        priority = DEFAULT
    }
    SystemExit {
        priority = DEFAULT
    }
    UnsafeArrayDeclaration {
        priority = DEFAULT
    }

    // rulesets/serialization.xml
    EnumCustomSerializationIgnored {
        priority = DEFAULT
    }
    SerialPersistentFields {
        priority = DEFAULT
    }
    SerialVersionUID {
        priority = DEFAULT
    }
    SerializableClassMustDefineSerialVersionUID {
        priority = 3
    }

    // rulesets/size.xml
    AbcMetric {
        priority = 1
    }
    ClassSize {
        priority = 2
    }
    CrapMetric {
        priority = 2
    }
    CyclomaticComplexity {
        priority = 1
    }
    MethodCount {
        priority = 2
    }
    MethodSize {
        priority = 2
    }
    NestedBlockDepth {
        priority = 2
    }
    ParameterCount {
        priority = 2
    }

    // rulesets/unnecessary.xml
    AddEmptyString {
        priority = DEFAULT
    }
    ConsecutiveLiteralAppends {
        priority = DEFAULT
    }
    ConsecutiveStringConcatenation {
        priority = DEFAULT
    }
    UnnecessaryBigDecimalInstantiation {
        priority = DEFAULT
    }
    UnnecessaryBigIntegerInstantiation {
        priority = DEFAULT
    }
    UnnecessaryBooleanExpression {
        doNotApplyToFileNames = TEST
    }
    UnnecessaryBooleanInstantiation {
        priority = DEFAULT
    }
    UnnecessaryCallForLastElement {
        priority = DEFAULT
    }
    UnnecessaryCallToSubstring {
        priority = DEFAULT
    }
    UnnecessaryCast {
        priority = DEFAULT
    }
    UnnecessaryCatchBlock {
        priority = DEFAULT
    }
    UnnecessaryCollectCall {
        priority = 3
    }
    UnnecessaryCollectionCall {
        priority = 1
    }
    UnnecessaryConstructor {
        priority = DEFAULT
    }
    //UnnecessaryDefInFieldDeclaration
    //UnnecessaryDefInMethodDeclaration
    //UnnecessaryDefInVariableDeclaration
    UnnecessaryDotClass {
        priority = DEFAULT
    }
    UnnecessaryDoubleInstantiation {
        priority = DEFAULT
    }
    UnnecessaryElseStatement {
        priority = 3
    }
    UnnecessaryFinalOnPrivateMethod {
        priority = DEFAULT
    }
    UnnecessaryFloatInstantiation {
        priority = DEFAULT
    }
    //UnnecessaryGString //We would prefer a rule to only use GStrings
    UnnecessaryGetter {
        priority = 3
    }
    UnnecessaryIfStatement {
        priority = 1
    }
    UnnecessaryInstanceOfCheck {
        priority = 1
    }
    UnnecessaryInstantiationToGetClass {
        priority = DEFAULT
    }
    UnnecessaryIntegerInstantiation {
        priority = DEFAULT
    }
    UnnecessaryLongInstantiation {
        priority = DEFAULT
    }
    UnnecessaryModOne {
        priority = DEFAULT
    }
    UnnecessaryNullCheck {
        priority = DEFAULT
    }
    UnnecessaryNullCheckBeforeInstanceOf {
        priority = 1
    }
    UnnecessaryObjectReferences {
        priority = 3
    }
    UnnecessaryOverridingMethod {
        priority = DEFAULT
    }
    UnnecessaryPackageReference {
        priority = 2
    }
    UnnecessaryParenthesesForMethodCallWithClosure {
        priority = 1
    }
    UnnecessaryPublicModifier {
        priority = 1
    }
    //UnnecessaryReturnKeyword //no
    UnnecessarySafeNavigationOperator {
        priority = DEFAULT
    }
    UnnecessarySelfAssignment {
        priority = 1
    }
    UnnecessarySemicolon {
        priority = 1
    }
    UnnecessarySetter {
        priority = 3
    }
    UnnecessaryStringInstantiation {
        priority = DEFAULT
    }
    //UnnecessarySubstring
    UnnecessaryTernaryExpression {
        priority = DEFAULT
    }
    UnnecessaryToString {
        priority = 1
    }
    UnnecessaryTransientModifier {
        priority = DEFAULT
    }

    // rulesets/unused.xml
    UnusedArray {
        priority = 1
    }
    UnusedMethodParameter {
        priority = 1
    }
    UnusedObject {
        priority = 1
    }
    UnusedPrivateField {
        priority = 1
    }
    UnusedPrivateMethod {
        priority = 1
    }
    UnusedPrivateMethodParameter {
        priority = 1
    }
    UnusedVariable {
        priority = 1
    }
}
