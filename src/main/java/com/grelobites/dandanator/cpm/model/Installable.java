package com.grelobites.dandanator.cpm.model;

import javafx.beans.Observable;
import javafx.beans.property.StringProperty;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class Installable {
    private StringProperty name;
    private byte[] data;

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public Observable[] getObservables() {
        return new Observable[] {name};
    }

    public void exportAsFile(File saveFile) throws IOException {

    }

    public static Optional<Installable> fromFile(String installableName, File file) {
        return Optional.empty();
    }
}
