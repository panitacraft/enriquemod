package com.panita.enriquecraft;

import com.panita.enriquecraft.core.CoreModule;
import com.panita.enriquecraft.core.framework.module.ModuleManager;
import com.panita.enriquecraft.staff.StaffModule;
import net.fabricmc.api.ModInitializer;

public class Enriquecraft implements ModInitializer {

    public static final String MOD_ID = "enriquecraft";

    @Override
    public void onInitialize() {
        ModuleManager modules = new ModuleManager(MOD_ID);
        modules.register(new CoreModule());
        modules.register(new StaffModule());
    }
}
