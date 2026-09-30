package com.panita.enriquecraft.staff.config;

import com.panita.enriquecraft.core.framework.config.ConfigSectionBuilder;
import com.panita.enriquecraft.core.framework.config.ConfigValue;
import com.panita.enriquecraft.core.framework.config.ModConfig;

/**
 * Settings of the staff module, stored in the {@code staff} section of the config file.
 */
public final class StaffConfig implements ModConfig {

    public final ConfigValue<Integer> maxDeathRecordsPerPlayer;

    public StaffConfig(ConfigSectionBuilder builder) {
        maxDeathRecordsPerPlayer = builder.intRange("deathRecords.maxPerPlayer", 20, 1, 200,
                "How many death inventories are kept for each player. When there are more, the oldest are deleted.");
    }
}
