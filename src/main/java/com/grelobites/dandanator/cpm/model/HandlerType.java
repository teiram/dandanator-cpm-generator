package com.grelobites.dandanator.cpm.model;

import com.grelobites.dandanator.cpm.ApplicationContext;
import com.grelobites.dandanator.cpm.Constants;
import com.grelobites.dandanator.cpm.handlers.CpcCpmRomSetHandler;
import com.grelobites.dandanator.cpm.handlers.SpectrumCpmRomSetHandler;
import javafx.scene.image.Image;

public enum HandlerType {
    SPECTRUM(SpectrumCpmRomSetHandler.class, Constants.SPECTRUM_ICON),
    CPC(CpcCpmRomSetHandler.class, Constants.CPC_ICON);


    private Class<? extends RomSetHandler> handlerClass;
    private Image icon;

    HandlerType(Class<? extends RomSetHandler> handlerClass, Image icon) {
        this.handlerClass = handlerClass;
        this.icon = icon;
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

}
