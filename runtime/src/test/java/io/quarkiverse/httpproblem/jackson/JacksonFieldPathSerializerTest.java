package io.quarkiverse.httpproblem.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonMappingException;

class JacksonFieldPathSerializerTest {

    @Test
    void singleFieldName() {
        List<JsonMappingException.Reference> path = List.of(
                new JsonMappingException.Reference(this, "userName"));

        assertThat(JacksonFieldPathSerializer.serializePath(path)).isEqualTo("userName");
    }

    @Test
    void nestedFieldPath() {
        List<JsonMappingException.Reference> path = List.of(
                new JsonMappingException.Reference(this, "address"),
                new JsonMappingException.Reference(this, "city"));

        assertThat(JacksonFieldPathSerializer.serializePath(path)).isEqualTo("address.city");
    }

    @Test
    void arrayIndexInPath() {
        List<JsonMappingException.Reference> path = List.of(
                new JsonMappingException.Reference(this, "items"),
                new JsonMappingException.Reference(this, 2),
                new JsonMappingException.Reference(this, "name"));

        assertThat(JacksonFieldPathSerializer.serializePath(path)).isEqualTo("items[2].name");
    }

    @Test
    void emptyPathReturnsQuestionMark() {
        assertThat(JacksonFieldPathSerializer.serializePath(Collections.emptyList())).isEqualTo("?");
    }

    @Test
    void pathStartingWithArrayIndex() {
        List<JsonMappingException.Reference> path = List.of(
                new JsonMappingException.Reference(this, 0),
                new JsonMappingException.Reference(this, "name"));

        assertThat(JacksonFieldPathSerializer.serializePath(path)).isEqualTo("[0].name");
    }

    @Test
    void onlyArrayIndex() {
        List<JsonMappingException.Reference> path = List.of(
                new JsonMappingException.Reference(this, 5));

        assertThat(JacksonFieldPathSerializer.serializePath(path)).isEqualTo("[5]");
    }

    @Test
    void consecutiveArrayIndices() {
        List<JsonMappingException.Reference> path = List.of(
                new JsonMappingException.Reference(this, "matrix"),
                new JsonMappingException.Reference(this, 1),
                new JsonMappingException.Reference(this, 3));

        assertThat(JacksonFieldPathSerializer.serializePath(path)).isEqualTo("matrix[1][3]");
    }
}
