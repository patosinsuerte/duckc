package org.pato.duckc.services;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.File;
import java.util.prefs.Preferences;

public class UserPreferencesService {

    private final Preferences preferences;
    private static final String KEY_DESTINATION_PATH = "destination_path";


    private final StringProperty destinationPath = new SimpleStringProperty();

    public UserPreferencesService() {

        String defaultFolder = this.getDefaultFolderByOS();


        this.preferences = Preferences.userNodeForPackage(UserPreferencesService.class);


        String savedPath = preferences.get(KEY_DESTINATION_PATH, defaultFolder);
        this.destinationPath.set(savedPath);
    }

    public String getDefaultFolderByOS() {
        String home = System.getProperty("user.home");


        File downloads = new File(home, "Downloads");
        if (downloads.exists() && downloads.isDirectory()) {
            return downloads.getAbsolutePath();
        }
        File descargas = new File(home, "Descargas");
        if (descargas.exists() && descargas.isDirectory()) {
            return descargas.getAbsolutePath();
        }
        return home;
    }

    public String getDestinationPath() {
        return destinationPath.get();
    }

    public StringProperty destinationPathProperty() {
        return destinationPath;
    }


    public void setDestinationPath(String path) {
        if (path != null && !path.isEmpty()) {
            this.destinationPath.set(path);

            this.preferences.put(KEY_DESTINATION_PATH, path);
        }
    }


}
