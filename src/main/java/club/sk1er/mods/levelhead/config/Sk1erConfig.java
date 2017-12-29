package club.sk1er.mods.levelhead.config;

import club.sk1er.mods.levelhead.utils.JsonHolder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mitchellkatz on 12/23/17. Designed for production use on Sk1er.club
 */
public class Sk1erConfig {
    private JsonHolder config;
    private File file;
    private List<Object> configObjects = new ArrayList<>();


    public Sk1erConfig(File configFile) {
        this.file = configFile;
        try {
            if (configFile.exists()) {
                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null)
                    builder.append(line);

                String done = builder.toString();
                config = new JsonHolder(done);
                br.close();
                fr.close();
            } else {
                config = new JsonHolder();
                saveFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::save));
    }

    public void saveFile() {
        try {
            file.createNewFile();
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(config.toString());
            bw.close();
            fw.close();
        } catch (Exception e) {
        }
    }

    public void save() {
        for (Object o : configObjects)
            saveToJsonFromRamObject(o);
        saveFile();
    }

    public void register(Object object) {
        configObjects.add(object);
        loadToClass(object);
    }

    public void loadToClass(Object o) {
        try {
            loadToClassObject(o);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void loadToClassObject(Object object) throws IllegalAccessException {
        if (config == null)
            config = new JsonHolder();
        Class<?> aClass = object.getClass();
        for (Field field : aClass.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(ConfigOpt.class)) {
                if (config.has(aClass.getName())) {
                    JsonObject tmp = config.optJsonObject(aClass.getName()).getObject();
                    if (tmp.has(field.getName())) {
                        JsonElement jsonElement = tmp.get(field.getName());
                        if (field.getType().isAssignableFrom(int.class)) {
                            field.set(object, jsonElement.getAsInt());
                        } else if (field.getType().isAssignableFrom(String.class)) {
                            field.set(object, jsonElement.getAsString());
                        } else if (field.getType().isAssignableFrom(boolean.class)) {
                            field.set(object, jsonElement.getAsBoolean());
                        } else if (field.getType().isAssignableFrom(double.class)) {
                            field.set(object, jsonElement.getAsDouble());
                        }

                    }
                }
            }
        }
    }

    public void saveToJsonFromRamObject(Object o) {
        try {
            loadToJson(o);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void loadToJson(Object object) throws IllegalAccessException {
        if (config == null)
            config = new JsonHolder();
        Class<?> aClass = object.getClass();
        for (Field field : aClass.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getAnnotation(ConfigOpt.class) != null) {
                if (!config.has(aClass.getName())) {
                    config.put(aClass.getName(), new JsonObject());
                }
                JsonObject classObject = config.optJsonObject(aClass.getName()).getObject();
                if (field.getType().isAssignableFrom(int.class)) {
                    classObject.addProperty(field.getName(), field.getInt(object));
                } else if (field.getType().isAssignableFrom(String.class)) {
                    classObject.addProperty(field.getName(), (String) field.get(object));
                } else if (field.getType().isAssignableFrom(boolean.class)) {
                    classObject.addProperty(field.getName(), field.getBoolean(object));
                } else if (field.getType().isAssignableFrom(double.class)) {
                    classObject.addProperty(field.getName(), field.getDouble(object));
                }
            }
        }
    }
}
