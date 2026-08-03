package io.quarkiverse.httpproblem.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class HttpValidationProblemTest {

    @Test
    void violationsShouldBeUnmodifiable() {
        List<Violation> violations = new ArrayList<>();
        violations.add(Violation.In.body.field("email").message("required"));

        HttpValidationProblem problem = new HttpValidationProblem(400, "Bad Request", violations);

        assertThatThrownBy(() -> problem.getViolations().add(Violation.In.body.field("name").message("required")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mutatingOriginalListShouldNotAffectProblem() {
        List<Violation> violations = new ArrayList<>();
        violations.add(Violation.In.body.field("email").message("required"));

        HttpValidationProblem problem = new HttpValidationProblem(400, "Bad Request", violations);

        violations.add(Violation.In.body.field("name").message("required"));

        assertThat(problem.getViolations()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void violationsInParametersMapShouldBeUnmodifiable() {
        List<Violation> violations = List.of(Violation.In.body.field("email").message("required"));

        HttpValidationProblem problem = new HttpValidationProblem(400, "Bad Request", violations);
        List<Violation> fromParams = (List<Violation>) problem.getParameters().get("violations");

        assertThatThrownBy(() -> fromParams.add(Violation.In.body.field("name").message("required")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getViolationsShouldReturnAllViolations() {
        List<Violation> violations = List.of(
                Violation.In.body.field("email").message("required"),
                Violation.In.query.field("page").message("must be positive"));

        HttpValidationProblem problem = new HttpValidationProblem(400, "Bad Request", violations);

        assertThat(problem.getViolations()).hasSize(2);
        assertThat(problem.getViolations().get(0).field).isEqualTo("email");
        assertThat(problem.getViolations().get(1).field).isEqualTo("page");
    }

}
