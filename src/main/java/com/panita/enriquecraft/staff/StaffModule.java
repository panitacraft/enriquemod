package com.panita.enriquecraft.staff;

import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.framework.inject.ServiceRegistry;
import com.panita.enriquecraft.core.framework.module.EnriquecraftModule;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.staff.config.StaffConfig;
import com.panita.enriquecraft.staff.gui.StaffMenus;
import com.panita.enriquecraft.staff.message.CoordinateView;
import com.panita.enriquecraft.staff.message.CustomItemView;
import com.panita.enriquecraft.staff.message.DeathInventoryView;
import com.panita.enriquecraft.staff.service.CoordinateService;
import com.panita.enriquecraft.staff.service.CustomItemService;
import com.panita.enriquecraft.staff.service.DeathInventoryService;

/**
 * Tools for the server staff: saved coordinates, saved custom items and death inventory
 * recovery, all under the {@code /staff} command.
 */
public final class StaffModule implements EnriquecraftModule {

    @Override
    public void registerServices(ServiceRegistry services) {
        CoordinateService coordinates = new CoordinateService(services.get(WorldData.class));
        services.register(CoordinateService.class, coordinates);
        services.register(CoordinateView.class, new CoordinateView(services.get(Messenger.class), coordinates));

        CustomItemService customItems = new CustomItemService(services.get(WorldData.class));
        services.register(CustomItemService.class, customItems);
        services.register(CustomItemView.class, new CustomItemView(services.get(Messenger.class)));

        services.register(DeathInventoryService.class,
                new DeathInventoryService(services.get(WorldData.class), services.get(StaffConfig.class)));
        services.register(DeathInventoryView.class, new DeathInventoryView(services.get(Messenger.class)));

        services.register(StaffMenus.class, new StaffMenus(services.get(UiService.class),
                services.get(CoordinateService.class), services.get(CoordinateView.class),
                services.get(CustomItemService.class), services.get(CustomItemView.class),
                services.get(DeathInventoryService.class), services.get(DeathInventoryView.class)));
    }
}
