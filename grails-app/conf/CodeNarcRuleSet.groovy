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

final String TEST = "*/*test*/*"
final String SPEC = "*Spec.groovy"
final String MAIN = "*/*main*/*"
final String INTEGRATION_SPEC = "*IntegrationSpec.groovy"
final String CONTROLLER = "*Controller.groovy"
final String SERVICE = "*Service.groovy"
final String VALIDATOR = "*Validator*"
final String WORKFLOW_TEST = "*/*workflow-test*/*"
final String SPOCK_TEST_EXPRESSION = "(.*/workflow-test/.*)|(.*Spec.groovy)"
final int DEFAULT = 1 //Value for rules that we have no explicitly discussed yet
final int CRITICAL = 1
final int HIGH = 2
final int MIDDLE = 3
final int LOW = 3


ruleset {
    description '''
All the Rules that will be used for OTP
'''

    // OTP Rules
    rule("file:grails-app/codenarcRules/ScheduledServiceBugRule.groovy")
    rule("file:grails-app/codenarcRules/DoNotCreateServicesWithNewRule.groovy")
    rule("file:grails-app/codenarcRules/EnumForBeanNameRule.groovy")
    rule("file:grails-app/codenarcRules/AnnotationsForValidatorsRule.groovy")
    rule("file:grails-app/codenarcRules/AnnotationsForStartJobsRule.groovy")
    rule("file:grails-app/codenarcRules/AnnotationsForJobsRule.groovy")
    rule("file:grails-app/codenarcRules/ExplicitFlushForSaveRule.groovy")
    rule("file:grails-app/codenarcRules/UnusedImportWithoutAutowiredRule.groovy")
    rule("file:grails-app/codenarcRules/AvoidFindWithoutAllRule.groovy")
    rule("file:grails-app/codenarcRules/ControllerMethodNotInAllowedMethodsRule.groovy")

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
        priority = CRITICAL
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
        priority = CRITICAL
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
        priority = CRITICAL
    }
    WhileStatementBraces {
        priority = CRITICAL
    }

    // rulesets/comments.xml
    // ClassJavadoc we don't do this
    JavadocConsecutiveEmptyLines {
        priority = DEFAULT
    }
    JavadocEmptyAuthorTag {
        priority = DEFAULT
    }
    JavadocEmptyExceptionTag {
        priority = DEFAULT
    }
    JavadocEmptyFirstLine {
        priority = CRITICAL
    }
    JavadocEmptyLastLine {
        priority = CRITICAL
    }
    JavadocEmptyParamTag {
        priority = DEFAULT
    }
    JavadocEmptyReturnTag {
        priority = CRITICAL
    }
    JavadocEmptySeeTag {
        priority = DEFAULT
    }
    JavadocEmptySinceTag {
        priority = DEFAULT
    }
    JavadocEmptyThrowsTag {
        priority = DEFAULT
    }
    JavadocEmptyVersionTag {
        priority = DEFAULT
    }
    JavadocMissingExceptionDescription {
        priority = DEFAULT
    }
    JavadocMissingParamDescription {
        priority = LOW
    }
    JavadocMissingThrowsDescription {
        priority = LOW
    }

    // rulesets/concurrency.xml
    BusyWait {
        priority = CRITICAL
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
        priority = CRITICAL
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
    // CompileStatic //not currently needed since 99% of all Classes are Compile Dynamic
    ConfusingTernary {
        priority = CRITICAL
    }
    CouldBeElvis {
        priority = CRITICAL
    }
    CouldBeSwitchStatement {
        priority = CRITICAL
    }
    FieldTypeRequired {
        ignoreFieldNames = 'mapping, constraints, hasMany, belongsTo, mappedBy, allowedMethods'
    }
    HashtableIsObsolete {
        priority = CRITICAL
    }
    //ImplicitClosureParameter //There are more cases where this would deteriorate code quality instead of improving it
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
        priority = CRITICAL
    }
    MethodParameterTypeRequired {
        priority = CRITICAL
    }
    MethodReturnTypeRequired {
        priority = MIDDLE
        doNotApplyToFileNames = CONTROLLER
    }
    NoDef {
        priority = MIDDLE
        doNotApplyToFileNames = CONTROLLER
    }
    //NoFloat //Float is fine since we don't use the numbers for calculations
    //NoDouble //Double is fine since we don't use the numbers for calculations
    /* Since our code use the Date class a lot, it is not possible to avoid it in new code.
    NoJavaUtilDate {
        priority = MIDDLE
    }*/
    NoTabCharacter {
        priority = HIGH
    }
    ParameterReassignment {
        priority = CRITICAL
    }
    //PublicMethodsBeforeNonPublicMethods //does not fit with OTP-Convention of grouping related methods by topic
    //StaticFieldsBeforeInstanceFields //does not fit with OTP-Convention of Services before class fields including statics
    //StaticMethodsBeforeInstanceMethods //does not fit with OTP-Convention of grouping related methods by topic
    TernaryCouldBeElvis {
        priority = CRITICAL
    }
    TrailingComma {
        priority = CRITICAL
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
    //BuilderMethodWithSideEffects //also finds non builder methods start with create
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
        priority = CRITICAL
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
    MissingOverrideAnnotation {
        priority = CRITICAL
        doNotApplyToFileNames = INTEGRATION_SPEC
    }
    UnsafeImplementationAsMap {
        priority = HIGH
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
        priority = CRITICAL
    }
    SwallowThreadDeath {
        priority = DEFAULT
    }
    ThrowError {
        priority = CRITICAL
    }
    ThrowException {
        priority = CRITICAL
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
        priority = CRITICAL
    }
    BlockEndsWithBlankLine {
        priority = CRITICAL
    }
    BlockStartsWithBlankLine {
        priority = CRITICAL
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
    // ClassEndsWithBlankLine we dont do this
    // ClassStartsWithBlankLine we dont do this
    ClosureStatementOnOpeningLineOfMultipleLineClosure {
        priority = CRITICAL
    }
    ConsecutiveBlankLines {
        priority = LOW
    }
    FileEndsWithoutNewline {
        priority = CRITICAL
    }
    Indentation {
        priority = MIDDLE
    }
    LineLength {
        priority = HIGH
        length = 160
        doNotApplyToFilesMatching = "(.*/.*test.*/.*)|(.*Validator.*)"
    }
    LineLength {
        priority = LOW
        length = 242
        applyToFileNames = TEST
        doNotApplyToFileNames = VALIDATOR
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
        priority = CRITICAL
    }
    SpaceAfterComma {
        priority = CRITICAL
    }
    SpaceAfterFor {
        priority = CRITICAL
    }
    SpaceAfterIf {
        priority = CRITICAL
    }
    SpaceAfterOpeningBrace {
        priority = CRITICAL
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
        priority = CRITICAL
    }
    SpaceAroundMapEntryColon {
        characterBeforeColonRegex = /.*/
        characterAfterColonRegex = /\s+/
        priority = CRITICAL
    }
    SpaceAroundOperator {
        priority = CRITICAL
    }
    SpaceBeforeClosingBrace {
        priority = CRITICAL
    }
    SpaceBeforeOpeningBrace {
        priority = CRITICAL
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
    ExplicitCallToPutAtMethod {
        priority = CRITICAL
    }
    ExplicitCallToRightShiftMethod {
        priority = CRITICAL
    }
    ExplicitCallToXorMethod {
        priority = CRITICAL
    }
    ExplicitHashMapInstantiation {
        priority = CRITICAL
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
        priority = CRITICAL
    }
    GStringExpressionWithinString {
        priority = CRITICAL
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
        priority = CRITICAL
    }
    ImportFromSamePackage {
        priority = CRITICAL
    }
    ImportFromSunPackages {
        priority = MIDDLE
    }
    MisorderedStaticImports {
        priority = CRITICAL
        comesBefore = false //to change to: Normal imports should appear before static imports
    }
    //NoWildcardImports //does not fit with OTP-Convention of using WildcardImports for 3+ imports
    UnnecessaryGroovyImport {
        priority = MIDDLE
    }
    //UnusedImport //replaced by UnusedImportWithoutAutowiredRule

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
    JUnitAssertEqualsConstantActualValue {
        priority = DEFAULT
    }
    JUnitFailWithoutMessage {
        priority = DEFAULT
    }
    JUnitLostTest {
        priority = MIDDLE
        doNotApplyToFilesMatching = SPOCK_TEST_EXPRESSION
    }
    //JUnitPublicField
    //JUnitPublicNonTestMethod
    /* Disabled, since following cases require public properties:
     * - The @Rule annotation of test, used for TemporaryFolder
     * - Autowireing of services in integration tests
     * Since that are many cases, twe decided to disable JUnitPublicProperty
     */
    //JUnitPublicProperty
    JUnitSetUpCallsSuper {
        priority = HIGH
        doNotApplyToFilesMatching = SPOCK_TEST_EXPRESSION
    }
    //JUnitStyleAssertions //will be fixed by converting to spock
    JUnitTearDownCallsSuper {
        priority = DEFAULT
        doNotApplyToFilesMatching = SPOCK_TEST_EXPRESSION
    }
    JUnitTestMethodWithoutAssert {
        priority = MIDDLE
        doNotApplyToFilesMatching = SPOCK_TEST_EXPRESSION
    }
    JUnitUnnecessarySetUp {
        priority = DEFAULT
        doNotApplyToFilesMatching = SPOCK_TEST_EXPRESSION
    }
    JUnitUnnecessaryTearDown {
        priority = DEFAULT
        doNotApplyToFilesMatching = SPOCK_TEST_EXPRESSION
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
        priority = CRITICAL
    }
    Println {
        priority = CRITICAL
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
        priority = CRITICAL
        applyToFileNames = MAIN
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
        doNotApplyToFileNames = TEST
    }
    ClassSize {
        priority = MIDDLE
        doNotApplyToFileNames = TEST
    }
    CrapMetric {
        priority = MIDDLE
    }
    CyclomaticComplexity {
        priority = HIGH
        doNotApplyToFileNames = TEST
    }
    MethodCount {
        priority = MIDDLE
        doNotApplyToFileNames = TEST
    }
    MethodSize {
        priority = MIDDLE
        doNotApplyToFileNames = TEST
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
        priority = CRITICAL
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
        priority = CRITICAL
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
        priority = CRITICAL
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
        priority = CRITICAL
    }
    UnnecessaryPublicModifier {
        priority = CRITICAL
    }
    //UnnecessaryReturnKeyword //no
    UnnecessarySafeNavigationOperator {
        priority = DEFAULT
    }
    UnnecessarySelfAssignment {
        priority = CRITICAL
    }
    UnnecessarySemicolon {
        priority = CRITICAL
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
        priority = CRITICAL
    }
    UnnecessaryTransientModifier {
        priority = DEFAULT
    }

    // rulesets/unused.xml
    UnusedArray {
        priority = CRITICAL
    }
    UnusedMethodParameter {
        priority = CRITICAL
    }
    UnusedObject {
        priority = CRITICAL
    }
    UnusedPrivateField {
        priority = CRITICAL
    }
    UnusedPrivateMethod {
        priority = CRITICAL
    }
    UnusedPrivateMethodParameter {
        priority = CRITICAL
    }
    UnusedVariable {
        priority = CRITICAL
    }
}
