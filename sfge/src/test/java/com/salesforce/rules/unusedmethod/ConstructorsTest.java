package com.salesforce.rules.unusedmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.rules.Violation;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * These tests verify that constructors are properly handled by {@link
 * com.salesforce.rules.UnusedMethodRule}.
 */
public class ConstructorsTest extends BaseUnusedMethodTest {

    /**
     * Simple tests verifying that an obviously unused constructor is flagged as unused.
     *
     * @param declaration - The declaration of the tested constructor
     * @param arity - The constructor's arity
     */
    // TODO: ENABLE MORE TESTS AS WE ADD MORE FUNCTIONALITY
    @CsvSource({
        // One test per constructor, per visibility scope.
        // EXCEPTION: No test for private 0-arity, since such methods are ineligible.
        // "public MyClass(), 0",
        "protected MyClass(), 0",
        // "public MyClass(boolean b), 1",
        "protected MyClass(boolean b), 1",
        "private MyClass(boolean b), 1"
    })
    @ParameterizedTest(name = "{displayName}: {1}-arity constructor: {0}")
    public void constructorWithoutInvocation_expectViolation(String declaration, int arity) {
        // spotless:off
        String sourceCode =
            "global class MyClass {\n"
          + "    " + declaration + " {}\n"
          + "}";
        // spotless:on
        Consumer<Violation.RuleViolation> assertion =
                v -> {
                    assertEquals("<init>", v.getSourceVertexName());
                    assertEquals(arity, ((MethodVertex) v.getSourceVertex()).getArity());
                };
        assertViolations(sourceCode, assertion);
    }

    /**
     * Test for cases where a class calls its own constructor internally via the `this()` syntax.
     *
     * @param visibility - The visibility of the target constructor.
     */
    @ValueSource(
            strings = {
                // "public",
                "protected",
                "private"
            })
    @ParameterizedTest(name = "{displayName}: {0} constructor")
    public void constructorCalledViaThis_expectNoViolation(String visibility) {
        // spotless:off
        String sourceCode =
            "global class MyClass {\n"
            // Declare the tested constructor with the specified visibility.
          + "    " +  visibility + " MyClass(boolean b, boolean b2) {}\n"
          + "    \n"
            // Use the engine directive to prevent this constructor from tripping the rule.
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public MyClass(boolean b) {\n"
            // Invocation of the tested constructor.
          + "        this(b, true);\n"
          + "    }\n"
          + "}";
        // spotless:on
        assertNoViolations(sourceCode, 1);
    }

    /**
     * Tests for cases where a subclass calls its parent's constructor via {@code super()}.
     *
     * @param visibility - Visibility of the parent constructor
     * @param arity - Arity of the parent constructor
     */
    @CsvSource({
        // "public, 0",
        // "public, 1",
        "protected, 0",
        "protected, 1"
    })
    @ParameterizedTest(name = "{displayName}: {0} constructor with arity {1}")
    public void constructorCalledViaSuper_expectNoViolation(String visibility, Integer arity) {
        // spotless:off
        String[] sourceCodes = new String[]{
            "global virtual class ParentClass {\n"
            // Declare a constructor with the expected arity that does nothing in particular.
          + "    " + visibility + " ParentClass(" + StringUtils.repeat("boolean b", arity) + ") {}\n"
          + "}",
            "global class ChildClass extends ParentClass {\n"
            // Declare a constructor annotated with the engine directive so it doesn't trip the rule.
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public ChildClass() {\n"
            // Invoke the parent constructor via super
          + "        super(" + StringUtils.repeat("true", arity) + ");\n"
          + "    }\n"
          + "}"
        };
        // spotless:on
        assertNoViolations(sourceCodes, 1);
    }

    /**
     * Tests for cases where a class's grandchild class calls its {@code super()} constructor.
     * Constructors only inherit one level, so a violation is expected.
     *
     * @param arity - The arity of the tested constructor.
     */
    @ValueSource(ints = {0, 1})
    @ParameterizedTest(name = "{displayName}: arity {0}")
    public void constructorUncalledByGrandchildSuper_expectViolation(Integer arity) {
        // spotless:off
        String[] sourceCodes = new String[]{
            "global virtual class ParentClass {\n"
            // Declare a constructor with the expected arity that does nothing in particular.
          + "    protected ParentClass(" + StringUtils.repeat("boolean b", arity) + ") {}\n"
          + "}",
            "global virtual class ChildClass extends ParentClass {\n"
            // Declare a constructor with the expected arity that does nothing in particular.
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public ChildClass(" + StringUtils.repeat("boolean b", arity) + ") {}\n"
          + "}",
            "global class GrandchildClass extends ChildClass {\n"
            // Declare a constructor annotated to not trip the rule.
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public GrandchildClass() {\n"
          + "        super(" + StringUtils.repeat("true", arity) + ");\n"
          + "    }\n"
          + "}"
        };
        // spotless:on
        Consumer<Violation.RuleViolation> assertion =
                v -> {
                    assertEquals("<init>", v.getSourceVertexName());
                    assertEquals("ParentClass", v.getSourceDefiningType());
                };
        assertViolations(sourceCodes, assertion);
    }

    /**
     * Tests for cases where an inner class's constructor is called in the outer class.
     *
     * @param variable - The way the variable is declared.
     * @param constructor - The way the constructor is invoked.
     */
    @CsvSource({
        "OuterClass.InnerClass, OuterClass.InnerClass",
        "OuterClass.InnerClass, InnerClass",
        "InnerClass, OuterClass.InnerClass",
        "InnerClass, InnerClass"
    })
    @ParameterizedTest(name = "{displayName}: Declared as {0}, constructed as new {1}()")
    @Disabled
    public void innerConstructorUsedByOuter_expectNoViolation(String variable, String constructor) {
        // spotless:off
        String sourceCode =
            "global class OuterClass {\n"
            // Declare a method in the outer class to call the inner constructor, annotated to not trip the rule.
          + "    /* sfge-disable-stack UnusedMethodRule */\n"
          + "    public void invoker() {\n"
          + "        " + variable + " obj = new " + constructor + "(false);\n"
          + "    }\n"
          + "    global class InnerClass {\n"
            // Declare a constructor for the inner class
          + "        public InnerClass(boolean b) {}\n"
          + "    }\n"
          + "}";
        // spotless:on
        assertNoViolations(sourceCode, 1);
    }

    /**
     * Tests for cases where an inner class's constructor is called by a sibling inner class.
     *
     * @param variable - The way the variable is declared.
     * @param constructor - The way the constructor is invoked.
     */
    @CsvSource({
        "OuterClass.InnerClass, OuterClass.InnerClass",
        "OuterClass.InnerClass, InnerClass",
        "InnerClass, OuterClass.InnerClass",
        "InnerClass, InnerClass"
    })
    @ParameterizedTest(name = "{displayName}: Declared as {0}, constructed as new {1}()")
    @Disabled
    public void innerConstructorUsedBySiblingInner_expectNoViolation(
            String variable, String constructor) {
        // spotless:off
        String sourceCode =
            "global class OuterClass {\n"
          + "    global class InnerClass {\n"
            // Declare a constructor for the inner class
          + "        public InnerClass(boolean b) {}\n"
          + "    }\n"
          + "    \n"
          + "    global class InnerClass2 {\n"
            // Declare a method on the sibling inner class that instantiates the tested one.
          + "        /* sfge-disable-stack UnusedMethodRule */\n"
          + "        public void invoker() {\n"
          + "            " + variable + " obj = new " + constructor + "(false);\n"
          + "        }\n"
          + "    }\n"
          + "}";
        // spotless:on
        assertNoViolations(sourceCode, 1);
    }
}