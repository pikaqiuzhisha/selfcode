package com.chargedot.charge.util;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * @Author：caoj
 * @Description： json工具类
 * @Data：Created in 2018/3/6
 */
public class JacksonUtil {
    private static ObjectMapper mapper;

    public static synchronized ObjectMapper getMapperInstance(boolean createNew) {
        if (createNew) {
            return new ObjectMapper();
        } else if (mapper == null) {
            mapper = new ObjectMapper();
        }
        return mapper;
    }

    /**
     * @param json
     * @param class1
     * @param <T>
     * @return bean
     */
    public static <T> T json2Bean(String json, Class<T> class1) {
        try {
            mapper = getMapperInstance(false);
            return mapper.readValue(json, class1);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param bean
     * @return json
     */
    public static String bean2Json(Object bean) {
        try {
            mapper = getMapperInstance(false);
            return mapper.writeValueAsString(bean);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param json
     * @param class1
     * @param <T>
     * @return List
     */
    public static <T> List<T> json2List(String json, Class<T> class1) {
        try {
            mapper = getMapperInstance(false);
            JavaType javaType = mapper.getTypeFactory().constructParametricType(List.class, class1);
            //如果是Map类型  mapper.getTypeFactory().constructParametricType(HashMap.class,String.class, Bean.class);
            List<T> list = (List<T>) mapper.readValue(json, javaType);
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param list
     * @return json
     */
    public static String list2Json(List<?> list) {
        try {
            mapper = getMapperInstance(false);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.writeValueAsString(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param json
     * @param type
     * @return Map
     */
    public static Map<?, ?> json2Map(String json, TypeReference<?> type) {
        try {
            mapper = getMapperInstance(false);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return (Map<?, ?>) mapper.readValue(json, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param map
     * @return json
     */
    public static String map2Json(Map<?, ?> map) {
        try {
            mapper = getMapperInstance(false);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
