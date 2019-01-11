package org.discordlist.spotifymicroservices.configuration;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Configuration extends JSONObject {

    private final Logger log = LoggerFactory.getLogger(Configuration.class);
    private final File configFile;
    private final Map<String, Object> defaults;

    public Configuration(String fileName) {
        this(new File(fileName));
    }

    private Configuration(File file) {
        this.configFile = file;
        this.defaults = new HashMap<>();
    }

    public Configuration init() {
        String content = null;
        try {
            if (configFile.exists())
                content = new BufferedReader(new FileReader(configFile)).lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            log.error("[Configuration] Error while loading config!", e);
        }
        if (content == null || content.equals("")) {
            log.warn("[Configuration] The config does not exists, started loading defaults!");
            defaults.forEach(this::put);
        } else {
            JSONObject object = checkObject(new JSONObject(content));
            object.toMap().forEach((key, value) -> put(key, (value instanceof Map ? (new JSONObject(((Map) value))) : (new JSONArray(listToString(((List<?>) value)))))));
        }
        save();
        return this;
    }

    public void addDefault(String key, Object value) {
        if (!(value instanceof JSONObject) && !(value instanceof JSONArray))
            throw new IllegalArgumentException("Object value needs to be a JSONObject or JSONArray!");
        defaults.put(key, value);
    }

    private void save() {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(configFile))) {
            bufferedWriter.write(toString(2));
        } catch (IOException e) {
            log.error("[Configuration] Error while saving Configuration!", e);
        }
    }

    private String listToString(List<?> list) {
        StringBuilder builder = new StringBuilder()
                .append("[");
        list.forEach(element -> builder.append("\"").append(element.toString()).append("\"").append(","));
        builder.append("]");
        if (builder.toString().contains(","))
            builder.replace(builder.lastIndexOf(","), builder.lastIndexOf(",") + 1, "");
        return builder.toString();
    }

    private JSONObject checkObject(JSONObject jsonObject) {
        defaults.forEach((key, value) -> {
            if (!jsonObject.has(key))
                jsonObject.put(key, value);
            else {
                Object defaultObject = defaults.get(key);
                //Ignore JSONArrays
                if (!(defaultObject instanceof JSONObject))
                    return;
                JSONObject existingObject = jsonObject.getJSONObject(key);
                ((JSONObject) defaultObject).toMap().forEach((defaultKey, defaultValue) -> {
                    if (!existingObject.has(defaultKey))
                        existingObject.put(defaultKey, defaultValue);
                });
            }
        });
        return jsonObject;
    }
}
