package org.pato.duckc.services;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;

public class FileProcessorService {

    private final ObservableList<File> filesToProcess = FXCollections.observableArrayList();

    public ObservableList<File> getFilesToProcess() {
        return filesToProcess;
    }

    public void addFiles(java.util.List<File> newFiles) {
        for (File file : newFiles) {
            if (!filesToProcess.contains(file)) {
                filesToProcess.add(file);
            }
        }
    }

    public void removeFile(File file) {
        filesToProcess.remove(file);
    }

    public void clear() {
        filesToProcess.clear();
    }
}
