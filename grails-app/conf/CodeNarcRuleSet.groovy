final String TEST = "*/*test*/*"
final String SPEC = "*Spec.groovy"
final String CONTROLLER = "*Controller.groovy"

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
    AssignmentInConditional
    BigDecimalInstantiation
    BitwiseOperatorInConditional
    BooleanGetBoolean
    BrokenNullCheck
    BrokenOddnessCheck
    ClassForName {
        priority = 1
    }
    ComparisonOfTwoConstants
    ComparisonWithSelf
    ConstantAssertExpression
    ConstantIfExpression
    ConstantTernaryExpression
    DeadCode {
        priority = 1
    }
    DoubleNegative
    DuplicateCaseStatement
    DuplicateMapKey
    DuplicateSetValue
    EmptyCatchBlock
    EmptyClass
    EmptyElseBlock
    EmptyFinallyBlock
    EmptyForStatement
    EmptyIfStatement {
        priority = 1
    }
    EmptyInstanceInitializer
    EmptyMethod {
        doNotApplyToFileNames = CONTROLLER
    }
    EmptyStaticInitializer
    EmptySwitchStatement
    EmptySynchronizedStatement
    EmptyTryBlock
    EmptyWhileStatement
    EqualsAndHashCode
    EqualsOverloaded
    ExplicitGarbageCollection
    ForLoopShouldBeWhileLoop
    HardCodedWindowsFileSeparator
    HardCodedWindowsRootDirectory
    IntegerGetInteger
    MultipleUnaryOperators
    RandomDoubleCoercedToZero
    RemoveAllOnSelf
    ReturnFromFinallyBlock
    ThrowExceptionFromFinallyBlock

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
    BusyWait
    DoubleCheckedLocking
    InconsistentPropertyLocking
    InconsistentPropertySynchronization
    NestedSynchronization
    StaticCalendarField
    StaticConnection
    StaticDateFormatField
    StaticMatcherField
    StaticSimpleDateFormatField
    SynchronizedMethod
    SynchronizedOnBoxedPrimitive
    SynchronizedOnGetClass
    SynchronizedOnReentrantLock
    SynchronizedOnString
    SynchronizedOnThis
    SynchronizedReadObjectMethod
    SystemRunFinalizersOnExit
    ThisReferenceEscapesConstructor
    ThreadGroup
    ThreadLocalNotStaticFinal {
        priority = 1
    }
    ThreadYield
    UseOfNotifyMethod
    VolatileArrayField
    VolatileLongOrDoubleField
    WaitOutsideOfWhileLoop

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
    HashtableIsObsolete
    IfStatementCouldBeTernary
    InvertedCondition
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
    }
    NoDef {
        priority = 2
    }
    NoJavaUtilDate
    NoTabCharacter
    ParameterReassignment {
        priority = 1
    }
    //PublicMethodsBeforeNonPublicMethods //does not fit with OTP-Convention of grouping related methods by topic
    //StaticFieldsBeforeInstanceFields //does not fit with OTP-Convention of Services before class fields including statics
    //StaticMethodsBeforeInstanceMethods //does not fit with OTP-Convention of grouping related methods by topic
    TernaryCouldBeElvis
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
    AbstractClassWithPublicConstructor
    AbstractClassWithoutAbstractMethod
    AssignmentToStaticFieldFromInstanceMethod
    BooleanMethodReturnsNull
    BuilderMethodWithSideEffects
    CloneableWithoutClone
    CloseWithoutCloseable
    CompareToWithoutComparable
    ConstantsOnlyInterface
    EmptyMethodInAbstractClass
    FinalClassWithProtectedMember
    ImplementationAsType {
        priority = 1
    }
    Instanceof
    LocaleSetDefault
    NestedForLoop {
        priority = 1
    }
    PrivateFieldCouldBeFinal {
        priority = 2
    }
    PublicInstanceField
    ReturnsNullInsteadOfEmptyArray {
        priority = 1
    }
    ReturnsNullInsteadOfEmptyCollection {
        priority = 1
    }
    SimpleDateFormatMissingLocale {
        priority = 1
    }
    StatelessSingleton
    ToStringReturnsNull

    // rulesets/dry.xml
    DuplicateListLiteral {
        priority = 2
    }
    DuplicateMapLiteral
    DuplicateNumberLiteral {
        priority = 2
    }
    DuplicateStringLiteral

    // rulesets/enhanced.xml
    CloneWithoutCloneable
    JUnitAssertEqualsConstantActualValue
    MissingOverrideAnnotation {
        priority = 1
    }
    UnsafeImplementationAsMap

    // rulesets/exceptions.xml
    CatchArrayIndexOutOfBoundsException
    CatchError
    CatchException {
        priority = 1
    }
    CatchIllegalMonitorStateException
    CatchIndexOutOfBoundsException
    CatchNullPointerException
    CatchRuntimeException
    CatchThrowable {
        priority = 1
    }
    ConfusingClassNamedException
    ExceptionExtendsError
    ExceptionExtendsThrowable
    ExceptionNotThrown
    MissingNewInThrowStatement
    ReturnNullFromCatchBlock
    SwallowThreadDeath
    ThrowError {
        priority = 1
    }
    ThrowException {
        priority = 1
    }
    ThrowNullPointerException
    ThrowRuntimeException
    ThrowThrowable

    // rulesets/formatting.xml
    BlankLineBeforePackage
    BlockEndsWithBlankLine
    BlockStartsWithBlankLine
    BracesForClass
    BracesForForLoop
    BracesForIfElse
    BracesForMethod
    BracesForTryCatchFinally
    ClassJavadoc
    ClosureStatementOnOpeningLineOfMultipleLineClosure {
        priority = 1
    }
    ConsecutiveBlankLines
    FileEndsWithoutNewline
    Indentation
    LineLength
    MissingBlankLineAfterImports {
        priority = 1
    }
    MissingBlankLineAfterPackage {
        priority = 1
    }
    SpaceAfterCatch {
        priority = 1
    }
    SpaceAfterClosingBrace
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
    IllegalClassMember
    IllegalClassReference
    IllegalPackageReference
    IllegalRegex
    IllegalString
    IllegalSubclass
    RequiredRegex
    RequiredString
    StatelessClass

    // rulesets/grails.xml
    //GrailsDomainHasEquals //Entity provides equals()
    //GrailsDomainHasToString //we don't do this in OTP
    GrailsDomainReservedSqlKeywordName {
        priority = 1
    }
    //GrailsDomainStringPropertyMaxSize //done by hibernate
    GrailsDomainWithServiceReference
    GrailsDuplicateConstraint {
        priority = 1
    }
    GrailsDuplicateMapping {
        priority = 1
    }
    GrailsMassAssignment
    GrailsPublicControllerMethod
    GrailsServletContextReference
    GrailsStatelessService

    // rulesets/groovyism.xml
    AssignCollectionSort {
        priority = 1
    }
    AssignCollectionUnique
    ClosureAsLastMethodParameter {
        priority = 2
    }
    CollectAllIsDeprecated
    ConfusingMultipleReturns
    ExplicitArrayListInstantiation {
        priority = 1
    }
    ExplicitCallToAndMethod
    ExplicitCallToCompareToMethod {
        priority = 1
    }
    ExplicitCallToDivMethod {
        priority = 1
    }
    ExplicitCallToEqualsMethod {
        priority = 1
    }
    ExplicitCallToGetAtMethod
    ExplicitCallToLeftShiftMethod
    ExplicitCallToMinusMethod
    ExplicitCallToModMethod
    ExplicitCallToMultiplyMethod
    ExplicitCallToOrMethod
    ExplicitCallToPlusMethod
    ExplicitCallToPowerMethod
    ExplicitCallToRightShiftMethod
    ExplicitCallToXorMethod
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
    ExplicitStackInstantiation
    ExplicitTreeSetInstantiation
    GStringAsMapKey {
        priority = 1
    }
    GStringExpressionWithinString {
        priority = 1
    }
    GetterMethodCouldBeProperty
    GroovyLangImmutable {
        priority = 1
    }
    UseCollectMany
    UseCollectNested

    // rulesets/imports.xml
    DuplicateImport {
        priority = 1
    }
    ImportFromSamePackage {
        priority = 1
    }
    ImportFromSunPackages
    //MisorderedStaticImports //we do the opposite
    //NoWildcardImports //does not fit with OTP-Convention of using only WildcardImports
    UnnecessaryGroovyImport {
        priority = 2
    }
    UnusedImport {
        priority = 1
    }

    // rulesets/jdbc.xml
    DirectConnectionManagement
    JdbcConnectionReference
    JdbcResultSetReference
    JdbcStatementReference

    // rulesets/junit.xml
    ChainedTest
    CoupledTestCase
    JUnitAssertAlwaysFails
    JUnitAssertAlwaysSucceeds
    JUnitFailWithoutMessage
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
    JUnitTearDownCallsSuper
    JUnitTestMethodWithoutAssert {
        doNotApplyToFileNames = SPEC
    }
    JUnitUnnecessarySetUp
    JUnitUnnecessaryTearDown
    JUnitUnnecessaryThrowsException
    SpockIgnoreRestUsed
    UnnecessaryFail
    //UseAssertEqualsInsteadOfAssertTrue
    //UseAssertFalseInsteadOfNegation
    UseAssertNullInsteadOfAssertEquals
    //UseAssertSameInsteadOfAssertTrue
    UseAssertTrueInsteadOfAssertEquals
    UseAssertTrueInsteadOfNegation

    // rulesets/logging.xml
    LoggerForDifferentClass
    LoggerWithWrongModifiers
    LoggingSwallowsStacktrace {
        priority = 1
    }
    MultipleLoggers
    PrintStackTrace {
        priority = 1
    }
    Println {
        priority = 1
    }
    SystemErrPrint
    SystemOutPrint {
        priority = 1
    }

    // rulesets/naming.xml
    AbstractClassName
    ClassName
    ClassNameSameAsFilename
    ClassNameSameAsSuperclass
    ConfusingMethodName
    //FactoryMethodName //we don't do this
    FieldName
    InterfaceName
    InterfaceNameSameAsSuperInterface
    MethodName {
        doNotApplyToFileNames = TEST
    }
    ObjectOverrideMisspelledMethodName
    PackageName
    PackageNameMatchesFilePath
    ParameterName
    PropertyName
    VariableName

    // rulesets/security.xml
    FileCreateTempFile
    InsecureRandom {
        priority = 1
    }
    JavaIoPackageAccess
    NonFinalPublicField {
        doNotApplyToFileNames = TEST
    }
    NonFinalSubclassOfSensitiveInterface
    ObjectFinalize
    PublicFinalizeMethod
    SystemExit
    UnsafeArrayDeclaration

    // rulesets/serialization.xml
    EnumCustomSerializationIgnored
    SerialPersistentFields
    SerialVersionUID
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
    CrapMetric   // Requires the GMetrics jar and a Cobertura coverage file
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
    ParameterCount

    // rulesets/unnecessary.xml
    AddEmptyString
    ConsecutiveLiteralAppends
    ConsecutiveStringConcatenation
    UnnecessaryBigDecimalInstantiation
    UnnecessaryBigIntegerInstantiation
    UnnecessaryBooleanExpression {
        doNotApplyToFileNames = TEST
    }
    UnnecessaryBooleanInstantiation
    UnnecessaryCallForLastElement
    UnnecessaryCallToSubstring
    UnnecessaryCast
    UnnecessaryCatchBlock
    UnnecessaryCollectCall
    UnnecessaryCollectionCall
    UnnecessaryConstructor
    UnnecessaryDefInFieldDeclaration
    UnnecessaryDefInMethodDeclaration
    UnnecessaryDefInVariableDeclaration
    UnnecessaryDotClass
    UnnecessaryDoubleInstantiation
    UnnecessaryElseStatement
    UnnecessaryFinalOnPrivateMethod
    UnnecessaryFloatInstantiation
    //UnnecessaryGString //We would prefer a rule to only use GStrings
    UnnecessaryGetter
    UnnecessaryIfStatement
    UnnecessaryInstanceOfCheck
    UnnecessaryInstantiationToGetClass
    UnnecessaryIntegerInstantiation
    UnnecessaryLongInstantiation
    UnnecessaryModOne
    UnnecessaryNullCheck
    UnnecessaryNullCheckBeforeInstanceOf
    UnnecessaryObjectReferences
    UnnecessaryOverridingMethod
    UnnecessaryPackageReference
    UnnecessaryParenthesesForMethodCallWithClosure
    UnnecessaryPublicModifier
    //UnnecessaryReturnKeyword //no
    UnnecessarySafeNavigationOperator
    UnnecessarySelfAssignment
    UnnecessarySemicolon
    UnnecessarySetter
    UnnecessaryStringInstantiation
    UnnecessarySubstring
    UnnecessaryTernaryExpression
    UnnecessaryToString
    UnnecessaryTransientModifier

    // rulesets/unused.xml
    UnusedArray
    UnusedMethodParameter
    UnusedObject
    UnusedPrivateField
    UnusedPrivateMethod
    UnusedPrivateMethodParameter
    UnusedVariable
}
