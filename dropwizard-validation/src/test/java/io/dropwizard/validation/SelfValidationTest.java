package io.dropwizard.validation;

import io.dropwizard.util.Maps;
import io.dropwizard.validation.selfvalidating.SelfValidating;
import io.dropwizard.validation.selfvalidating.SelfValidation;
import io.dropwizard.validation.selfvalidating.ViolationCollector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.Validator;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@NotThreadSafe
class SelfValidationTest {

    private static final String FAILED = "failed";
    private static final String FAILED_RESULT = " " + FAILED;

    @AfterEach
    @BeforeEach
    public void clearAllLoggers() {
        //this must be a clear all because the validation runs in other threads
        TestLoggerFactory.clearAll();
    }

    @SelfValidating
    public static class FailingExample {
        @SuppressWarnings("unused")
        @SelfValidation
        public void validateFail(ViolationCollector col) {
            col.addViolation(FAILED);
        }
    }

    public static class SubclassExample extends FailingExample {
        @SuppressWarnings("unused")
        @SelfValidation
        public void subValidateFail(ViolationCollector col) {
            col.addViolation(FAILED + "subclass");
        }
    }

    @SelfValidating
    public static class AnnotatedSubclassExample extends FailingExample {
        @SuppressWarnings("unused")
        @SelfValidation
        public void subValidateFail(ViolationCollector col) {
            col.addViolation(FAILED + "subclass");
        }
    }

    public static class OverridingExample extends FailingExample {
        @Override
        public void validateFail(ViolationCollector col) {
        }
    }

    @SelfValidating
    public static class DirectContextExample {
        @SuppressWarnings("unused")
        @SelfValidation
        public void validateFail(ViolationCollector col) {
            col.getContext().buildConstraintViolationWithTemplate(FAILED).addConstraintViolation();
            col.setViolationOccurred(true);
        }
    }

    @SelfValidating
    public static class CorrectExample {
        @SuppressWarnings("unused")
        @SelfValidation
        public void validateCorrect(ViolationCollector col) {
        }
    }

    @SelfValidating
    public static class InvalidExample {
        @SuppressWarnings("unused")
        @SelfValidation
        public void validateCorrect(ViolationCollector col) {
        }

        @SuppressWarnings("unused")
        @SelfValidation
        public void validateFailAdditionalParameters(ViolationCollector col, int a) {
            col.addViolation(FAILED);
        }

        @SelfValidation
        public boolean validateFailReturn(ViolationCollector col) {
            col.addViolation(FAILED);
            return true;
        }

        @SelfValidation
        private void validateFailPrivate(ViolationCollector col) {
            col.addViolation(FAILED);
        }
    }


    @SelfValidating
    public static class ComplexExample {
        @SuppressWarnings("unused")
        @SelfValidation
        public void validateFail1(ViolationCollector col) {
            col.addViolation(FAILED + "1");
        }

        @SuppressWarnings("unused")
        @SelfValidation
        public void validateFail2(ViolationCollector col) {
            col.addViolation("p2", FAILED);
        }

        @SuppressWarnings("unused")
        @SelfValidation
        public void validateFail3(ViolationCollector col) {
            col.addViolation("p", 3, FAILED);
        }

        @SuppressWarnings("unused")
        @SelfValidation
        public void validateFail4(ViolationCollector col) {
            col.addViolation("p", "four", FAILED);
        }

        @SuppressWarnings("unused")
        @SelfValidation
        public void validateCorrect(ViolationCollector col) {
        }
    }

    @SelfValidating
    public static class NoValidations {
    }

    @SelfValidating
    public static class InjectionExample {
        @SuppressWarnings("unused")
        @SelfValidation
        public void validateFail(ViolationCollector col) {
            col.addViolation("${'value'}");
            col.addViolation("$\\A{1+1}");
            col.addViolation("{value}", Collections.singletonMap("value", "TEST"));
            col.addViolation("${'property'}", "${'value'}");
            col.addViolation("${'property'}", 1, "${'value'}");
            col.addViolation("${'property'}", "${'key'}", "${'value'}");
        }
    }

    @SelfValidating
    public static class MessageParametersExample {
        @SuppressWarnings("unused")
        @SelfValidation
        public void validateFail(ViolationCollector col) {
            col.addViolation("{1+1}");
            col.addViolation("{value}", Collections.singletonMap("value", "VALUE"));
            col.addViolation("No parameter", Collections.singletonMap("value", "VALUE"));
            col.addViolation("{value} {unsetParameter}", Collections.singletonMap("value", "VALUE"));
            col.addViolation("{value", Collections.singletonMap("value", "VALUE"));
            col.addViolation("value}", Collections.singletonMap("value", "VALUE"));
            col.addViolation("{  value  }", Collections.singletonMap("value", "VALUE"));
            col.addViolation("Mixed ${'value'} {value}", Collections.singletonMap("value", "VALUE"));
            col.addViolation("Nested {value}", Collections.singletonMap("value", "${'nested'}"));
            col.addViolation("{property}", "{value}", Maps.of("property", "PROPERTY", "value", "VALUE"));
            col.addViolation("{property}", 1, "{value}", Maps.of("property", "PROPERTY", "value", "VALUE"));
            col.addViolation("{property}", "{key}", "{value}", Maps.of("property", "PROPERTY", "key", "KEY", "value", "VALUE"));
        }
    }

    private final Validator validator = BaseValidator.newValidator();

    @Test
    void failingExample() {
        assertThat(ConstraintViolations.format(validator.validate(new FailingExample())))
                .containsExactlyInAnyOrder(FAILED_RESULT);
        assertThat(TestLoggerFactory.getAllLoggingEvents())
                .isEmpty();
    }

    @Test
    void subClassExample() {
        assertThat(ConstraintViolations.format(validator.validate(new SubclassExample())))
                .containsExactlyInAnyOrder(
                        FAILED_RESULT,
                        FAILED_RESULT + "subclass"
                );
        assertThat(TestLoggerFactory.getAllLoggingEvents())
                .isEmpty();
    }

    @Test
    void annotatedSubClassExample() {
        assertThat(ConstraintViolations.format(validator.validate(new AnnotatedSubclassExample())))
                .containsExactlyInAnyOrder(
                        FAILED_RESULT,
                        FAILED_RESULT + "subclass"
                );
        assertThat(TestLoggerFactory.getAllLoggingEvents())
                .isEmpty();
    }

    @Test
    void overridingSubClassExample() {
        assertThat(ConstraintViolations.format(validator.validate(new OverridingExample())))
                .isEmpty();
        assertThat(TestLoggerFactory.getAllLoggingEvents())
                .isEmpty();
    }

    @Test
    void correctExample() {
        assertThat(ConstraintViolations.format(validator.validate(new CorrectExample())))
                .isEmpty();
        assertThat(TestLoggerFactory.getAllLoggingEvents())
                .isEmpty();
    }

    @Test
    void multipleTestingOfSameClass() {
        assertThat(ConstraintViolations.format(validator.validate(new CorrectExample())))
                .isEmpty();
        assertThat(ConstraintViolations.format(validator.validate(new CorrectExample())))
                .isEmpty();
        assertThat(TestLoggerFactory.getAllLoggingEvents())
                .isEmpty();
    }

    @Test
    void testDirectContextUsage() {
        assertThat(ConstraintViolations.format(validator.validate(new DirectContextExample())))
                .containsExactlyInAnyOrder(FAILED_RESULT);
        assertThat(TestLoggerFactory.getAllLoggingEvents())
                .isEmpty();
    }

    @Test
    void complexExample() {
        assertThat(ConstraintViolations.format(validator.validate(new ComplexExample())))
                .containsExactly(
                        " failed1",
                        "p2 failed",
                        "p[3] failed",
                        "p[four] failed");
        assertThat(TestLoggerFactory.getAllLoggingEvents())
                .isEmpty();
    }

    @Test
    void invalidExample() throws Exception {
        assertThat(ConstraintViolations.format(validator.validate(new InvalidExample())))
                .isEmpty();
        assertThat(TestLoggerFactory.getAllLoggingEvents())
                .containsExactlyInAnyOrder(
                        new LoggingEvent(
                                Level.ERROR,
                                "The method {} is annotated with @SelfValidation but does not have a single parameter of type {}",
                                InvalidExample.class.getMethod("validateFailAdditionalParameters", ViolationCollector.class, int.class),
                                ViolationCollector.class
                        ),
                        new LoggingEvent(
                                Level.ERROR,
                                "The method {} is annotated with @SelfValidation but does not return void. It is ignored",
                                InvalidExample.class.getMethod("validateFailReturn", ViolationCollector.class)
                        ),
                        new LoggingEvent(
                                Level.ERROR,
                                "The method {} is annotated with @SelfValidation but is not public",
                                InvalidExample.class.getDeclaredMethod("validateFailPrivate", ViolationCollector.class)
                        )
                );
    }

    @Test
    void giveWarningIfNoValidationMethods() {
        assertThat(ConstraintViolations.format(validator.validate(new NoValidations())))
                .isEmpty();
        assertThat(TestLoggerFactory.getAllLoggingEvents())
                .containsExactlyInAnyOrder(
                        new LoggingEvent(
                                Level.WARN,
                                "The class {} is annotated with @SelfValidating but contains no valid methods that are annotated with @SelfValidation",
                                NoValidations.class
                        )

                );
    }

    @Test
    void violationMessagesAreEscapedByDefault() {
        assertThat(ConstraintViolations.format(validator.validate(new InjectionExample()))).containsExactly(
                " $\\A{1+1}",
                " ${'value'}",
                " TEST",
                "${'property'} ${'value'}",
                "${'property'}[${'key'}] ${'value'}",
                "${'property'}[1] ${'value'}"
        );
        assertThat(TestLoggerFactory.getAllLoggingEvents()).isEmpty();
    }

    @Test
    void messageParametersExample() {
        assertThat(ConstraintViolations.format(validator.validate(new MessageParametersExample()))).containsExactly(
                " Mixed ${'value'} VALUE",
                " Nested ${'nested'}",
                " No parameter",
                " VALUE",
                " VALUE {unsetParameter}",
                " value}",
                " {  value  }",
                " {1+1}",
                " {value",
                "{property} VALUE",
                "{property}[1] VALUE",
                "{property}[{key}] VALUE"
        );
        assertThat(TestLoggerFactory.getAllLoggingEvents()).isEmpty();
    }
}
