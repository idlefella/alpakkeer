package alpakkeer.core.util;

import alpakkeer.config.ObjectMapperConfiguration;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.scala.DefaultScalaModule;

import java.util.Map;

abstract class ExceptionMixIn {

    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class, property = "$id")
    private Throwable cause;

}

public final class ObjectMapperFactory {

    private ObjectMapperFactory() {

    }

    public static ObjectMapperFactory apply() {
        return new ObjectMapperFactory();
    }

    public ObjectMapper create() {
        return create(ObjectMapperConfiguration.apply());
    }

    public ObjectMapper create(ObjectMapperConfiguration configuration) {
        ObjectMapper om = new ObjectMapper();
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.registerModule(new JavaTimeModule());
        om.registerModule(new Jdk8Module());
        om.registerModule(new DefaultScalaModule());
        om.setMixIns(Map.of(Throwable.class, ExceptionMixIn.class));

        om.getSerializationConfig()
                .getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.ANY);

        om.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        om.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        om.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
        om.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
        om.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);

        SimpleModule module = new SimpleModule();
        om.registerModule(module);

        // Apply configuration
        configuration.getSerializationFeatures().forEach(serializationFeature -> {
            om.enable(SerializationFeature.valueOf(serializationFeature));
        });
        configuration.getSerializationInclusions().forEach(serializationInclusion ->
                om.setSerializationInclusion(JsonInclude.Include.valueOf(serializationInclusion))
        );

        om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        return om;
    }

}
