package io.quarkiverse.httpproblem.deployment;

import static io.quarkiverse.httpproblem.deployment.ExceptionMapperDefinition.mapper;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Priorities;

import org.eclipse.microprofile.openapi.OASFilter;

import io.quarkus.runtime.configuration.ConfigurationException;

import io.quarkiverse.httpproblem.DetailSanitizer;
import io.quarkiverse.httpproblem.ProblemRuntimeFixedConfig;
import io.quarkiverse.httpproblem.postprocessing.MdcPropertiesInjector;
import io.quarkiverse.httpproblem.postprocessing.PostProcessorsRegistry;
import io.quarkiverse.httpproblem.postprocessing.ProblemDefaultsProvider;
import io.quarkiverse.httpproblem.postprocessing.ProblemLogLevel;
import io.quarkiverse.httpproblem.postprocessing.ProblemLogger;
import io.quarkiverse.httpproblem.postprocessing.ProblemLoggingConfig;
import io.quarkiverse.httpproblem.postprocessing.ProblemPostProcessor;
import io.quarkiverse.httpproblem.postprocessing.ProblemRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.jsonb.spi.JsonbDeserializerBuildItem;
import io.quarkus.jsonb.spi.JsonbSerializerBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.reactive.spi.CustomExceptionMapperBuildItem;
import io.quarkus.resteasy.reactive.spi.ExceptionMapperBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.smallrye.openapi.deployment.spi.AddToOpenAPIDefinitionBuildItem;

public class ProblemProcessor {

    private static final String FEATURE_NAME = "http-problem";
    private static final String EXTENSION_MAIN_PACKAGE = "io.quarkiverse.httpproblem.";

    /**
     * Don't change this to constants from Capability for the sake of older Quarkus versions
     */
    private static final List<String> REST_JSON_CAPABILITIES = Arrays.asList(
            "io.quarkus.jsonb",
            "io.quarkus.jackson",
            "io.quarkus.resteasy.json",
            "io.quarkus.resteasy-json");

    private static List<ExceptionMapperDefinition> neededExceptionMappers(ProblemBuildConfig config) {
        Map<String, ProblemBuildConfig.MapperConfig> mapperConfig = config.mapper();

        Stream<ExceptionMapperDefinition> allMappers = Stream.of(
                mapper(EXTENSION_MAIN_PACKAGE + "HttpProblemMapper")
                        .thatHandles(EXTENSION_MAIN_PACKAGE + "HttpProblem"),

                mapper(EXTENSION_MAIN_PACKAGE + "jaxrs.WebApplicationExceptionMapper")
                        .thatHandles("jakarta.ws.rs.WebApplicationException"),
                mapper(EXTENSION_MAIN_PACKAGE + "jaxrs.JaxRsForbiddenExceptionMapper")
                        .thatHandles("jakarta.ws.rs.ForbiddenException"),
                mapper(EXTENSION_MAIN_PACKAGE + "jaxrs.NotFoundExceptionMapper")
                        .thatHandles("jakarta.ws.rs.NotFoundException"),

                mapper(EXTENSION_MAIN_PACKAGE + "security.UnauthorizedExceptionMapper")
                        .thatHandles("io.quarkus.security.UnauthorizedException").onlyIf(new RestEasyClassicDetector()),
                mapper(EXTENSION_MAIN_PACKAGE + "security.AuthenticationFailedExceptionMapper")
                        .thatHandles("io.quarkus.security.AuthenticationFailedException").onlyIf(new RestEasyClassicDetector()),
                mapper(EXTENSION_MAIN_PACKAGE + "security.AuthenticationRedirectExceptionMapper")
                        .thatHandles("io.quarkus.security.AuthenticationRedirectException"),
                mapper(EXTENSION_MAIN_PACKAGE + "security.AuthenticationCompletionExceptionMapper")
                        .thatHandles("io.quarkus.security.AuthenticationCompletionException"),
                mapper(EXTENSION_MAIN_PACKAGE + "security.ForbiddenExceptionMapper")
                        .thatHandles("io.quarkus.security.ForbiddenException"),

                mapper(EXTENSION_MAIN_PACKAGE + "validation.ValidationExceptionMapper")
                        .thatHandles("jakarta.validation.ValidationException"),

                mapper(EXTENSION_MAIN_PACKAGE + "validation.ConstraintViolationExceptionMapper")
                        .thatHandles("jakarta.validation.ConstraintViolationException"),

                mapper(EXTENSION_MAIN_PACKAGE + "jackson.JsonProcessingExceptionMapper")
                        .thatHandles("com.fasterxml.jackson.core.JsonProcessingException")
                        .onlyIf(new JacksonDetector()),
                mapper(EXTENSION_MAIN_PACKAGE + "jackson.UnrecognizedPropertyExceptionMapper")
                        .thatHandles("com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException")
                        .onlyIf(new JacksonDetector()),
                mapper(EXTENSION_MAIN_PACKAGE + "jackson.InvalidFormatExceptionMapper")
                        .thatHandles("com.fasterxml.jackson.databind.exc.InvalidFormatException")
                        .onlyIf(new JacksonDetector()),
                mapper(EXTENSION_MAIN_PACKAGE + "jackson.MismatchedInputExceptionMapper")
                        .thatHandles("com.fasterxml.jackson.databind.exc.MismatchedInputException")
                        .onlyIf(new JacksonDetector()),
                mapper(EXTENSION_MAIN_PACKAGE + "jackson.InvalidDefinitionExceptionMapper")
                        .thatHandles("com.fasterxml.jackson.databind.exc.InvalidDefinitionException")
                        .onlyIf(new JacksonDetector()),

                mapper(EXTENSION_MAIN_PACKAGE + "jsonb.RestEasyClassicJsonbExceptionMapper")
                        .thatHandles("jakarta.ws.rs.ProcessingException")
                        .onlyIf(new JsonBDetector()),

                mapper(EXTENSION_MAIN_PACKAGE + "jsonb.JsonbExceptionMapper")
                        .thatHandles("jakarta.json.bind.JsonbException")
                        .onlyIf(new JsonBDetector()),

                mapper(EXTENSION_MAIN_PACKAGE + "ZalandoProblemMapper")
                        .thatHandles("org.zalando.problem.ThrowableProblem"),

                mapper(EXTENSION_MAIN_PACKAGE + "DefaultExceptionMapper")
                        .thatHandles("java.lang.Exception"));

        return allMappers
                .filter(ExceptionMapperDefinition::isNeeded)
                .filter(mapper -> isMapperEnabled(mapper.exceptionClassName, mapperConfig))
                .collect(Collectors.toList());
    }

    static boolean isMapperEnabled(String exceptionClassName, Map<String, ProblemBuildConfig.MapperConfig> mapperConfig) {
        String key = toKebabCase(exceptionClassName.substring(exceptionClassName.lastIndexOf('.') + 1));
        ProblemBuildConfig.MapperConfig cfg = mapperConfig.get(key);
        return cfg == null || cfg.enabled();
    }

    static String toKebabCase(String simpleName) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < simpleName.length(); i++) {
            char c = simpleName.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append('-');
            }
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
    }

    @BuildStep
    FeatureBuildItem createFeature(Capabilities capabilities) {
        if (REST_JSON_CAPABILITIES.stream().noneMatch(capabilities::isPresent)) {
            throw new ConfigurationException(
                    "The `quarkus-http-problem` extension requires a JSON provider. Please add "
                            + "`quarkus-rest-jackson` or `quarkus-rest-jsonb` (or classic `resteasy` equivalent) extension to your project.");
        }
        return new FeatureBuildItem(FEATURE_NAME);
    }

    @BuildStep(onlyIf = RestEasyClassicDetector.class)
    void registerMappersForClassic(BuildProducer<ResteasyJaxrsProviderBuildItem> providers, ProblemBuildConfig config,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        neededExceptionMappers(config).forEach(mapper -> {
            providers.produce(new ResteasyJaxrsProviderBuildItem(mapper.mapperClassName));
            additionalBeans.produce(new AdditionalBeanBuildItem(mapper.mapperClassName));
        });

    }

    @BuildStep(onlyIf = RestEasyReactiveDetector.class)
    void registerMappersForReactive(BuildProducer<ExceptionMapperBuildItem> providers, ProblemBuildConfig config,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        neededExceptionMappers(config).forEach(mapper -> {
            providers.produce(
                    new ExceptionMapperBuildItem(mapper.mapperClassName,
                            mapper.exceptionClassName, Priorities.AUTHENTICATION - 1, true));
            additionalBeans.produce(new AdditionalBeanBuildItem(mapper.mapperClassName));
        });
    }

    @BuildStep(onlyIf = RestEasyReactiveDetector.class)
    void registerCustomExceptionMappers(BuildProducer<CustomExceptionMapperBuildItem> customExceptionMapper,
            ProblemBuildConfig config, BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        String unauthorized = EXTENSION_MAIN_PACKAGE + "security.UnauthorizedExceptionReactiveMapper";
        String authentication = EXTENSION_MAIN_PACKAGE + "security.AuthenticationFailedExceptionReactiveMapper";

        Map<String, ProblemBuildConfig.MapperConfig> mapperConfig = config.mapper();
        if (isMapperEnabled(unauthorized, mapperConfig)) {
            customExceptionMapper.produce(new CustomExceptionMapperBuildItem(unauthorized));
            additionalBeans.produce(new AdditionalBeanBuildItem(unauthorized));
        }

        if (isMapperEnabled(authentication, mapperConfig)) {
            customExceptionMapper.produce(new CustomExceptionMapperBuildItem(authentication));
            additionalBeans.produce(new AdditionalBeanBuildItem(authentication));
        }
    }

    @BuildStep(onlyIf = JacksonDetector.class)
    void registerJacksonItems(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(
                EXTENSION_MAIN_PACKAGE + "jackson.JacksonProblemModuleRegistrar"));
    }

    @BuildStep(onlyIf = JsonBDetector.class)
    void registerJsonbItems(BuildProducer<JsonbSerializerBuildItem> serializers,
            BuildProducer<JsonbDeserializerBuildItem> deserializers) {
        serializers.produce(
                new JsonbSerializerBuildItem(EXTENSION_MAIN_PACKAGE + "jsonb.JsonbProblemSerializer"));
        deserializers.produce(
                new JsonbDeserializerBuildItem(EXTENSION_MAIN_PACKAGE + "jsonb.JsonbProblemDeserializer"));
    }

    /**
     * Force jandex indexing for runtime module classes so that @Schema annotated classes can be picked up by OpenApi.
     * It's an equivalent to adding beans.xml to runtime's module resources, but this has advantage of being enabled
     * conditionally: only if openapi is in the classpath.
     */
    @BuildStep(onlyIf = OpenApiDetector.class)
    void indexOpenApiClasses(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("io.quarkiverse.httpproblem", "quarkus-http-problem"));
    }

    @BuildStep(onlyIf = OpenApiDetector.class)
    void registerOpenApiFilter(BuildProducer<AddToOpenAPIDefinitionBuildItem> openAPIProducer,
            ProblemBuildConfig config, ProblemRuntimeFixedConfig runtimeConfig) {
        OASFilter filter = new OpenApiProblemFilter(config, runtimeConfig);
        openAPIProducer.produce(new AddToOpenAPIDefinitionBuildItem(filter));
    }

    @BuildStep
    ReflectiveClassBuildItem registerPojosForReflection() {
        return ReflectiveClassBuildItem
                .builder(EXTENSION_MAIN_PACKAGE + "validation.Violation")
                .methods()
                .fields()
                .build();
    }

    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(PostProcessorsRegistry.class));
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(ProblemDefaultsProvider.class));
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(MdcPropertiesInjector.class));
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(DetailSanitizer.class));
        // ProblemLogger is registered as a synthetic bean via setupLogging() to receive configuration
    }

    @Record(STATIC_INIT)
    @BuildStep
    SyntheticBeanBuildItem setupLogging(ProblemRecorder recorder, ProblemBuildConfig config) {
        ProblemBuildConfig.LoggingConfig logging = config.logging();
        if (!logging.enabled()) {
            return null;
        }

        Map<String, ProblemLogLevel> levels = new LinkedHashMap<>(ProblemLoggingConfig.DEFAULT_LEVELS);
        levels.putAll(logging.level());

        RuntimeValue<ProblemLogger> runtimeValue = recorder.createProblemLogger(levels, logging.includeStackTrace());
        return SyntheticBeanBuildItem.configure(ProblemLogger.class)
                .scope(Singleton.class)
                .addType(ProblemPostProcessor.class)
                .runtimeValue(runtimeValue)
                .unremovable()
                .done();
    }

    @BuildStep
    UnremovableBeanBuildItem markPostProcessorsUnremovable() {
        return UnremovableBeanBuildItem.beanTypes(ProblemPostProcessor.class);
    }
}
