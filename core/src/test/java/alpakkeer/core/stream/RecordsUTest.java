package alpakkeer.core.stream;

import alpakkeer.config.ObjectMapperConfiguration;
import alpakkeer.core.util.ObjectMapperFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class RecordsUTest {

    @Value
    @NoArgsConstructor(force = true)
    @AllArgsConstructor(staticName = "apply")
    private static class Bla {

        String foo;

        int bar;

        String nullableString;

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSerialization() throws JsonProcessingException {
        var record = Record.apply(Bla.apply("foo", 123, null), "123");
        var om = ObjectMapperFactory.apply().create(ObjectMapperConfiguration.apply(Collections.emptyList(), List.of("NON_NULL")));
        var json = om.writeValueAsString(record);

        System.out.println(json);

        var recordRead = om.readValue(json, Record.class);
        assert (record.equals(recordRead));
    }

}
