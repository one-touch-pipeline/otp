final String TEST = "*/*test*/*"
final String SPEC = "*Spec.groovy"
final String INTEGRATION_SPEC = "*IntegrationSpec.groovy"
final String CONTROLLER = "*Controller.groovy"
final String SERVICE = "*Service.groovy"
final int DEFAULT = 1 //Value for rules that we have no explicitly discussed yet
final int CRITICAL = 1
final int HIGH = 2
final int MIDDLE = 3
final int LOW = 4


ruleset {

    description '''
        A Sample Groovy RuleSet containing all CodeNarc Rules, grouped by category.
        You can use this as a template for your own custom RuleSet.
        Just delete the rules that you don't want to include.
        '''

    // rulesets/basic.xml
    AssertWithinFinallyBlock {
        priority = HIGH
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
        priority = HIGH
    }
    ComparisonOfTwoConstants {
        priority = DEFAULT
    }
    ComparisonWithSelf {
        priority = CRITICAL
    }
    ConstantAssertExpression {
        priority = LOW
    }
    ConstantIfExpression {
        priority = DEFAULT
    }
    ConstantTernaryExpression {
        priority = DEFAULT
    }
    DeadCode {
        priority = HIGH
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
        priority = MIDDLE
    }
    EmptyCatchBlock {
        priority = MIDDLE
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
        priority = HIGH
    }
    EmptyInstanceInitializer {
        priority = DEFAULT
    }
    EmptyMethod {
        priority = MIDDLE
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
        priority = HIGH
    }
    ForStatementBraces {
        priority = CRITICAL
    }
    IfStatementBraces {
        priority = HIGH
    }
    WhileStatementBraces {
        priority = CRITICAL
    }

    // rulesets/concurrency.xml
    BusyWait {
        priority = HIGH
    }
    DoubleCheckedLocking {
        priority = HIGH
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
        priority = HIGH
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
        priority = HIGH
    }
    SynchronizedReadObjectMethod {
        priority = DEFAULT
    }
    SystemRunFinalizersOnExit {
        priority = DEFAULT
    }
    ThisReferenceEscapesConstructor {
        priority = MIDDLE
    }
    ThreadGroup {
        priority = DEFAULT
    }
    ThreadLocalNotStaticFinal {
        priority = HIGH
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
        priority = HIGH
    }
    CouldBeElvis {
        priority = HIGH
    }
    CouldBeSwitchStatement {
        priority = HIGH
    }
    //FieldTypeRequired //does not work well with Grails
    HashtableIsObsolete {
        priority = CRITICAL
    }
    IfStatementCouldBeTernary {
        priority = MIDDLE
    }
    InvertedCondition {
        priority = LOW
    }
    InvertedIfElse {
        priority = MIDDLE
    }
    LongLiteralWithLowerCaseL {
        priority = HIGH
    }
    MethodParameterTypeRequired {
        priority = HIGH
    }
    MethodReturnTypeRequired {
        priority = MIDDLE
        doNotApplyToFileNames = CONTROLLER
    }
    NoDef {
        priority = MIDDLE
        doNotApplyToFileNames = CONTROLLER
    }
    NoJavaUtilDate {
        priority = MIDDLE
    }
    NoTabCharacter {
        priority = HIGH
    }
    ParameterReassignment {
        priority = HIGH
    }
    //PublicMethodsBeforeNonPublicMethods //does not fit with OTP-Convention of grouping related methods by topic
    //StaticFieldsBeforeInstanceFields //does not fit with OTP-Convention of Services before class fields including statics
    //StaticMethodsBeforeInstanceMethods //does not fit with OTP-Convention of grouping related methods by topic
    TernaryCouldBeElvis {
        priority = CRITICAL
    }
    TrailingComma {
        priority = HIGH
    }
    VariableTypeRequired {
        priority = MIDDLE
    }
    VectorIsObsolete {
        priority = CRITICAL
    }

    // rulesets/design.xml
    AbstractClassWithPublicConstructor {
        priority = DEFAULT
    }
    AbstractClassWithoutAbstractMethod {
        applyToFileNames = SERVICE
        priority = MIDDLE
    }
    AssignmentToStaticFieldFromInstanceMethod {
        priority = MIDDLE
    }
    BooleanMethodReturnsNull {
        priority = MIDDLE
    }
    BuilderMethodWithSideEffects {
        priority = MIDDLE
    }
    CloneableWithoutClone {
        priority = DEFAULT
    }
    CloseWithoutCloseable {
        priority = MIDDLE
    }
    CompareToWithoutComparable {
        priority = DEFAULT
    }
    ConstantsOnlyInterface {
        priority = MIDDLE
    }
    EmptyMethodInAbstractClass {
        priority = LOW
    }
    FinalClassWithProtectedMember {
        priority = DEFAULT
    }
    ImplementationAsType {
        priority = HIGH
    }
    Instanceof {
        priority = MIDDLE
    }
    LocaleSetDefault {
        priority = DEFAULT
    }
    NestedForLoop {
        priority = HIGH
    }
    PrivateFieldCouldBeFinal {
        priority = MIDDLE
    }
    PublicInstanceField {
        priority = MIDDLE
    }
    ReturnsNullInsteadOfEmptyArray {
        priority = CRITICAL
    }
    ReturnsNullInsteadOfEmptyCollection {
        priority = HIGH
    }
    SimpleDateFormatMissingLocale {
        priority = HIGH
    }
    StatelessSingleton {
        priority = DEFAULT
    }
    ToStringReturnsNull {
        priority = DEFAULT
    }

    // rulesets/dry.xml
    DuplicateListLiteral {
        priority = MIDDLE
    }
    //DuplicateMapLiteral //has problems with stuff like .save(flush: true)
    DuplicateNumberLiteral {
        priority = MIDDLE
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
        priority = CRITICAL
        doNotApplyToFileNames = INTEGRATION_SPEC
    }
    UnsafeImplementationAsMap {
        priority = DEFAULT
    }

    // rulesets/exceptions.xml
    CatchArrayIndexOutOfBoundsException {
        priority = CRITICAL
    }
    CatchError {
        priority = CRITICAL
    }
    CatchException {
        priority = HIGH
    }
    CatchIllegalMonitorStateException {
        priority = CRITICAL
    }
    CatchIndexOutOfBoundsException {
        priority = CRITICAL
    }
    CatchNullPointerException {
        priority = CRITICAL
    }
    CatchRuntimeException {
        priority = HIGH
    }
    CatchThrowable {
        priority = HIGH
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
        priority = CRITICAL
    }
    ReturnNullFromCatchBlock {
        priority = HIGH
    }
    SwallowThreadDeath {
        priority = DEFAULT
    }
    ThrowError {
        priority = HIGH
    }
    ThrowException {
        priority = HIGH
    }
    ThrowNullPointerException {
        priority = CRITICAL
    }
    ThrowRuntimeException {
        priority = HIGH
    }
    ThrowThrowable {
        priority = CRITICAL
    }

    // rulesets/formatting.xml
    BlankLineBeforePackage {
        priority = LOW
    }
    BlockEndsWithBlankLine {
        priority = LOW
    }
    BlockStartsWithBlankLine {
        priority = MIDDLE
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
        priority = CRITICAL
    }
    ConsecutiveBlankLines {
        priority = LOW
    }
    FileEndsWithoutNewline {
        priority = MIDDLE
    }
    Indentation {
        priority = MIDDLE
    }
    LineLength {
        priority = MIDDLE
        length = 160
    }
    MissingBlankLineAfterImports {
        priority = CRITICAL
    }
    MissingBlankLineAfterPackage {
        priority = CRITICAL
    }
    SpaceAfterCatch {
        priority = CRITICAL
    }
    SpaceAfterClosingBrace {
        priority = LOW
    }
    SpaceAfterComma {
        priority = HIGH
    }
    SpaceAfterFor {
        priority = CRITICAL
    }
    SpaceAfterIf {
        priority = HIGH
    }
    SpaceAfterOpeningBrace {
        priority = HIGH
    }
    SpaceAfterSemicolon {
        priority = CRITICAL
    }
    SpaceAfterSwitch {
        priority = CRITICAL
    }
    SpaceAfterWhile {
        priority = CRITICAL
    }
    SpaceAroundClosureArrow {
        priority = HIGH
    }
    SpaceAroundMapEntryColon {
        characterBeforeColonRegex = /.*/
        characterAfterColonRegex = /\s+/
        priority = HIGH
    }
    SpaceAroundOperator {
        priority = HIGH
    }
    SpaceBeforeClosingBrace {
        priority = HIGH
    }
    SpaceBeforeOpeningBrace {
        priority = HIGH
    }
    TrailingWhitespace {
        priority = HIGH
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
        priority = HIGH
    }
    //GrailsDomainStringPropertyMaxSize //done by hibernate
    GrailsDomainWithServiceReference {
        priority = DEFAULT
    }
    GrailsDuplicateConstraint {
        priority = CRITICAL
    }
    GrailsDuplicateMapping {
        priority = CRITICAL
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
        priority = CRITICAL
    }
    AssignCollectionUnique {
        priority = MIDDLE
    }
    ClosureAsLastMethodParameter {
        priority = MIDDLE
    }
    CollectAllIsDeprecated {
        priority = DEFAULT
    }
    ConfusingMultipleReturns {
        priority = DEFAULT
    }
    ExplicitArrayListInstantiation {
        priority = CRITICAL
    }
    ExplicitCallToAndMethod {
        priority = CRITICAL
    }
    ExplicitCallToCompareToMethod {
        priority = CRITICAL
    }
    ExplicitCallToDivMethod {
        priority = CRITICAL
    }
    ExplicitCallToEqualsMethod {
        priority = HIGH
    }
    ExplicitCallToGetAtMethod {
        priority = CRITICAL
    }
    ExplicitCallToLeftShiftMethod {
        priority = CRITICAL
    }
    ExplicitCallToMinusMethod {
        priority = HIGH
    }
    ExplicitCallToModMethod {
        priority = CRITICAL
    }
    ExplicitCallToMultiplyMethod {
        priority = CRITICAL
    }
    ExplicitCallToOrMethod {
        priority = CRITICAL
    }
    ExplicitCallToPlusMethod {
        priority = HIGH
    }
    ExplicitCallToPowerMethod {
        priority = CRITICAL
    }
    ExplicitCallToRightShiftMethod {
        priority = CRITICAL
    }
    ExplicitCallToXorMethod {
        priority = CRITICAL
    }
    ExplicitHashMapInstantiation {
        priority = HIGH
    }
    ExplicitHashSetInstantiation {
        priority = HIGH
    }
    ExplicitLinkedHashMapInstantiation {
        priority = CRITICAL
    }
    ExplicitLinkedListInstantiation {
        priority = CRITICAL
    }
    ExplicitStackInstantiation {
        priority = CRITICAL
    }
    ExplicitTreeSetInstantiation {
        priority = CRITICAL
    }
    GStringAsMapKey {
        priority = HIGH
    }
    GStringExpressionWithinString {
        priority = HIGH
    }
    GetterMethodCouldBeProperty {
        priority = LOW
    }
    GroovyLangImmutable {
        priority = CRITICAL
    }
    UseCollectMany {
        priority = MIDDLE
    }
    UseCollectNested {
        priority = DEFAULT
    }

    // rulesets/imports.xml
    DuplicateImport {
        priority = HIGH
    }
    ImportFromSamePackage {
        priority = HIGH
    }
    ImportFromSunPackages {
        priority = MIDDLE
    }
    //MisorderedStaticImports //we do the opposite
    //NoWildcardImports //does not fit with OTP-Convention of using only WildcardImports
    UnnecessaryGroovyImport {
        priority = MIDDLE
    }
    UnusedImport {
        priority = HIGH
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
        priority = MIDDLE
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
        priority = MIDDLE
        doNotApplyToFileNames = SPEC
    }
    //JUnitPublicField
    //JUnitPublicNonTestMethod
    JUnitPublicProperty {
        priority = LOW
    }
    JUnitSetUpCallsSuper {
        priority = HIGH
    }
    //JUnitStyleAssertions //will be fixed by converting to spock
    JUnitTearDownCallsSuper {
        priority = DEFAULT
    }
    JUnitTestMethodWithoutAssert {
        priority = MIDDLE
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
        priority = CRITICAL
    }
    MultipleLoggers {
        priority = DEFAULT
    }
    PrintStackTrace {
        priority = HIGH
    }
    Println {
        priority = HIGH
    }
    SystemErrPrint {
        priority = DEFAULT
    }
    SystemOutPrint {
        priority = HIGH
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
        priority = MIDDLE
    }
    ConfusingMethodName {
        priority = DEFAULT
    }
    //FactoryMethodName //we don't do this
    FieldName {
        priority = HIGH
    }
    InterfaceName {
        priority = DEFAULT
    }
    InterfaceNameSameAsSuperInterface {
        priority = DEFAULT
    }
    MethodName {
        priority = MIDDLE
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
        priority = MIDDLE
    }
    PropertyName {
        priority = MIDDLE
        doNotApplyToFileNames = TEST
    }
    VariableName {
        priority = MIDDLE
        doNotApplyToFileNames = TEST
    }

    // rulesets/security.xml
    FileCreateTempFile {
        priority = DEFAULT
    }
    InsecureRandom {
        priority = HIGH
    }
    JavaIoPackageAccess {
        priority = MIDDLE
    }
    NonFinalPublicField {
        priority = MIDDLE
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
        priority = LOW
    }

    // rulesets/size.xml
    AbcMetric {
        priority = HIGH
    }
    ClassSize {
        priority = MIDDLE
    }
    CrapMetric {
        priority = MIDDLE
    }
    CyclomaticComplexity {
        priority = HIGH
    }
    MethodCount {
        priority = MIDDLE
    }
    MethodSize {
        priority = MIDDLE
    }
    NestedBlockDepth {
        priority = MIDDLE
    }
    ParameterCount {
        priority = MIDDLE
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
        priority = LOW
    }
    UnnecessaryCollectionCall {
        priority = CRITICAL
    }
    UnnecessaryConstructor {
        priority = HIGH
    }
    //UnnecessaryDefInFieldDeclaration
    //UnnecessaryDefInMethodDeclaration
    //UnnecessaryDefInVariableDeclaration
    UnnecessaryDotClass {
        priority = LOW
    }
    UnnecessaryDoubleInstantiation {
        priority = DEFAULT
    }
    UnnecessaryElseStatement {
        priority = LOW
    }
    UnnecessaryFinalOnPrivateMethod {
        priority = DEFAULT
    }
    UnnecessaryFloatInstantiation {
        priority = DEFAULT
    }
    //UnnecessaryGString //We would prefer a rule to only use GStrings
    UnnecessaryGetter {
        priority = LOW
    }
    UnnecessaryIfStatement {
        priority = HIGH
    }
    UnnecessaryInstanceOfCheck {
        priority = CRITICAL
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
        priority = HIGH
    }
    UnnecessaryObjectReferences {
        priority = LOW
    }
    UnnecessaryOverridingMethod {
        priority = DEFAULT
    }
    UnnecessaryPackageReference {
        priority = MIDDLE
    }
    UnnecessaryParenthesesForMethodCallWithClosure {
        priority = HIGH
    }
    UnnecessaryPublicModifier {
        priority = CRITICAL
    }
    //UnnecessaryReturnKeyword //no
    UnnecessarySafeNavigationOperator {
        priority = DEFAULT
    }
    UnnecessarySelfAssignment {
        priority = HIGH
    }
    UnnecessarySemicolon {
        priority = HIGH
    }
    UnnecessarySetter {
        priority = LOW
    }
    UnnecessaryStringInstantiation {
        priority = DEFAULT
    }
    //UnnecessarySubstring
    UnnecessaryTernaryExpression {
        priority = DEFAULT
    }
    UnnecessaryToString {
        priority = HIGH
    }
    UnnecessaryTransientModifier {
        priority = DEFAULT
    }

    // rulesets/unused.xml
    UnusedArray {
        priority = CRITICAL
    }
    UnusedMethodParameter {
        priority = HIGH
    }
    UnusedObject {
        priority = CRITICAL
    }
    UnusedPrivateField {
        priority = HIGH
    }
    UnusedPrivateMethod {
        priority = HIGH
    }
    UnusedPrivateMethodParameter {
        priority = CRITICAL
    }
    UnusedVariable {
        priority = CRITICAL
    }
}
