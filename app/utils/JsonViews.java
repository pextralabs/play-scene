package utils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;

import java.io.IOException;

public class JsonViews {

    public static class Default {}
    public static class Complete extends Default {}

    private static ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(Include.NON_NULL);

    public static JsonNode toJson(Object bean) {
        return toJson(bean, Default.class);
    }

    public static String toString(Object bean) {
        return toString(bean, Default.class);
    }

    public static JsonNode toJson(Object bean, Class<?> view) {
        TokenBuffer buf = new TokenBuffer(objectMapper, false);
        JsonNode result;
        try {
            objectMapper.writerWithView(view).writeValue(buf, bean);
            JsonParser jp = buf.asParser();
            result = objectMapper.readTree(jp);
            jp.close();
        } catch (IOException e) { // should not occur, no real i/o...
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        return  result;
    }

    public static String toString(Object bean, Class<?> view) {
        try {
            return objectMapper.writerWithView(view).writeValueAsString(bean);
        } catch (IOException e) { // should not occur, no real i/o...
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

}