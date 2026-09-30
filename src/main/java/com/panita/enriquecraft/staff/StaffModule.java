package com.panita.enriquecraft.staff;

import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.framework.inject.ServiceRegistry;
import com.panita.enriquecraft.core.framework.module.EnriquecraftModule;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.staff.message.CoordinateView;
import com.panita.enriquecraft.staff.service.CoordinateService;

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
    }
}
