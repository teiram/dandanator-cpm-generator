package com.grelobites.dandanator.cpm.model;

import com.grelobites.dandanator.cpm.util.ArchiveFlags;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

public class Archive {
    private static AtomicLong ID_GENERATOR = new AtomicLong(0);
    private final long id;
    private StringProperty name;
    private StringProperty extension;
    private IntegerProperty userArea;
    private IntegerProperty size;
    private byte[] data;
    private EnumSet<ArchiveFlags> flags = EnumSet.noneOf(ArchiveFlags.class);

    public Archive(String name, String extension, int userArea, byte[] data) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.name = new SimpleStringProperty(name);
        this.extension = new SimpleStringProperty(extension);
        this.userArea = new SimpleIntegerProperty(userArea);
        this.size = new SimpleIntegerProperty(data.length);
        this.data = data;
    }

    public Archive(String name, String extension, int userArea, byte[] data, EnumSet<ArchiveFlags> flags) {
        this(name, extension, userArea, data);
        this.flags = flags;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getExtension() {
        return extension.get();
    }

    public StringProperty extensionProperty() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension.set(extension);
    }

    public int getUserArea() {
        return userArea.get();
    }

    public IntegerProperty userAreaProperty() {
        return userArea;
    }

    public void setUserArea(int userArea) {
        this.userArea.set(userArea);
    }

    public EnumSet<ArchiveFlags> getFlags() {
        return flags;
    }

    public void setFlags(EnumSet<ArchiveFlags> flags) {
        this.flags = flags;
    }

    public byte[] getData() {
        return data;
    }

    public int getSize() {
        return size.get();
    }

    public IntegerProperty sizeProperty() {
        return size;
    }

    public void setSize(int size) {
        this.size.set(size);
    }

    public Observable[] getObservables() {
        return new Observable[] {name, extension, userArea};
    }


    @Override
    public String toString() {
        return "Archive{" +
                "name=" + name +
                ", extension=" + extension +
                ", userArea=" + userArea +
                ", flags=" + flags +
                ", size=" + data.length +
                '}';
    }
}
