package alpakkeer.config;

import alpakkeer.core.config.annotations.ConfigurationProperties;
import alpakkeer.core.config.annotations.Value;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@ConfigurationProperties
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(staticName = "apply")
public class ObjectMapperConfiguration {

    @Value("serialization-features")
    List<String> serializationFeatures;

    @Value("serialization-inclusions")
    List<String> serializationInclusions;

    public static ObjectMapperConfiguration apply() {
        return apply(Collections.emptyList(), Collections.emptyList());
    }
}
