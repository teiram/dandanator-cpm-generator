package com.grelobites.dandanator.cpm.model;

import com.grelobites.dandanator.cpm.ApplicationContext;
import com.grelobites.dandanator.cpm.Constants;
import com.grelobites.dandanator.cpm.handlers.CpcCpm22RomSetHandler;
import com.grelobites.dandanator.cpm.handlers.CpcCpmPlusRomSetHandler;
import com.grelobites.dandanator.cpm.handlers.SpectrumCpmRomSetHandler;
import javafx.scene.image.Image;

public enum HandlerType {
    SPECTRUM(SpectrumCpmRomSetHandler.class, Constants.SPECTRUM_ICON, "Spectrum"),
    CPC_464(CpcCpm22RomSetHandler.class, Constants.CPC464_ICON, "CPC 464"),
    CPC_6128(CpcCpmPlusRomSetHandler.class, Constants.CPC6128_ICON, "CPC 6128");


    private Class<? extends RomSetHandler> handlerClass;
    private Image icon;
    private String caption;

    HandlerType(Class<? extends RomSetHandler> handlerClass, Image icon, String caption) {
        this.handlerClass = handlerClass;
        this.icon = icon;
        this.caption = caption;
    }

    public RomSetHandler handler(ApplicationContext context) {
        try {
            return handlerClass.getDeclaredConstructor(ApplicationContext.class).newInstance(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Image icon() {
        return icon;
    }

    public String toString() {
        return caption;
    }

}
