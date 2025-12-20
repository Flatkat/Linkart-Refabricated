package com.github.vini2003.linkart;

import com.github.vini2003.linkart.configuration.LinkartConfiguration;
import com.github.vini2003.linkart.mixin.PersistentStateAccessor;
import com.github.vini2003.linkart.utility.LinkartCommand;
import com.github.vini2003.linkart.utility.LoadingCarts;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Linkart implements ModInitializer {

    public static final String ID = "linkart";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final TagKey<Item> LINKERS = TagKey.create(itemKey(), ResourceLocation.fromNamespaceAndPath(ID, "linkers"));

    public void onInitialize() {
        MidnightConfig.init("linkart", LinkartConfiguration.class);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LinkartCommand.register(dispatcher);
        });

        ServerWorldEvents.LOAD.register((server, level) -> {
            if (LinkartConfiguration.chunkloading) LoadingCarts.getOrCreate(level);
        });

        ServerTickEvents.START_WORLD_TICK.register(level -> {
            if (LinkartConfiguration.chunkloading && ((PersistentStateAccessor) level.getDataStorage()).linkart$loadedStates().containsKey("linkart_loading_carts")) {
                LoadingCarts.getOrCreate(level).tick(level);
            }
        });
    }

    private static ResourceKey<? extends Registry<Item>> itemKey() {
        return ResourceKey.createRegistryKey(ResourceLocation.tryParse("item"));
    }
}
