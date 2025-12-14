package com.ravenherz.rhzwe.util;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.controller.objects.FormDescription;
import com.ravenherz.rhzwe.util.io.ServletFile;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

@Component("settings")
public class Settings implements Serializable {

    private List<FormDescription> formDescriptions;

    private static Settings settings;
    private ServletContext servletContext;
    private static final Logger LOGGER = LoggerFactory.getLogger(Settings.class);

    @Autowired
    public void setServletContext(ServletContext servletContextImpl) {
        servletContext = servletContextImpl;
    }

    // VARIABLES NEEDED IN CASE OF MISCONFIGURATION
    public final static int DEFAULT_NUMBER_OF_COLUMS_NAV_FOOTER = 3;

    private static String path = null;
    /*
     A collection of 'context's
     */
    private Map<String, Map<String, String>> storage;

    /**
     * Private constructor which loads all json-datasources from configuration directory from filesystem
     */

    @PostConstruct
    private void postConstruct() {
        settings = this;
        path = servletContext.getRealPath("");
        storage = new HashMap<>();
        Gson gson = new Gson();


        formDescriptions = new ArrayList<>();
        File directoryWithFormDescriptions = ServletFile
                .classPathFile( "/static/content-private/forms/");
        if (directoryWithFormDescriptions.listFiles() != null) {
            JsonReader reader = null;
            File[] requireNonNull = Objects
                    .requireNonNull(directoryWithFormDescriptions.listFiles());
            int i = 0;
            if (i < requireNonNull.length) {
                do {
                    File file = requireNonNull[i];
                    try {
                        reader = new JsonReader(new FileReader(file));
                        FormDescription formDescription = gson
                                .fromJson(reader, FormDescription.class);
                        if (formDescription != null) {
                            formDescriptions.add(formDescription);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    i++;
                } while (i < requireNonNull.length);
            }
        }

        // localy defined path, being checked each time app runs at first time
        // path to lookup for configuration files
        String pathToConf = Strings.PATH_CONFIGURATION;
        File directory = ServletFile.classPathFile(pathToConf);

        Arrays.stream(Objects.requireNonNull(directory.listFiles())).forEach(file -> {
            if (!file.isDirectory() && file.canRead()) {
                JsonReader reader = null;
                try {
                    reader = new JsonReader(new FileReader(file));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if (reader != null) {
                    HashMap<String, String> contents = gson.fromJson(reader, HashMap.class);
                    storage.put(file.getName()
                            .replace(Strings.EXTENSION_JSON, Strings.STR_EMPTY), contents);
                } else {
                    LOGGER.error("File not found");
                }
            }
        });
    }

    /**
     * Get filesystem context path
     *
     * @return context path as a String
     */
    public String getPath() {
        return path;
    }

	/*
		So, first of all, we are trying to get a 'context' from 'storage'
		Then we are trying to get 'value' by 'key' from 'context'
		If the reference is not null then we are trimming it to find out
		if value is a string with whitespaces, and if it actually contains
		some date we return the 'value' to user.
	 */

    /**
     * Returns 'value' defined by 'key' from some 'context' dal source
     *
     * @param context context, dal source (usually json in configuration folder)
     * @param key a key to find value by
     * @return value defined by key
     */
    public String getValue(String context, String key) {
        String result = storage
                .getOrDefault(context, new HashMap<>())
                .getOrDefault(key, null);
        if (result == null) return null;
        else return result.trim();
    }


	/*
		If context is null we create a new hashmap,
		anyway we put key:value pair in existed or newly created context
	 */

    /**
     * Put 'value' defined by 'key' into some 'context' dal source
     *
     * @param context context, dal source (usually json in configuration folder; a new one, or already existing)
     * @param key key (a new one, or already existing)
     * @param value new value
     */
    public void putValue(String context, String key, String value) {
        storage.computeIfAbsent(context, k -> new HashMap<>());
        storage.get(context).put(key, value);
    }

	/*
		If locally defined path is null the application is not ready;
		however if it isn't null we still check if a special key:value is set
	 */

    /**
     * Is application is ready to go in it's normal state?
     *
     * @return is ready
     */
    public boolean isConfigured() {
        return getValue(Strings.CONTEXT_DATASOURCE_SETUP, Strings.KEY_IS_CONFIGURED) != null;
    }

    public static Settings getInstance() { return settings;}

    public List<FormDescription> getFormDescriptions() {
        return formDescriptions;
    }
}
