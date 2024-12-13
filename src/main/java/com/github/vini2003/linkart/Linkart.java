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
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Linkart implements ModInitializer {

    public static final String ID = "linkart";
    public static final Logger LOGGER = LogManager.getLogger(ID);
    public static final TagKey<Item> LINKERS = TagKey.of(itemKey(), Identifier.tryParse(ID, "linkers"));

    public void onInitialize() {
        MidnightConfig.init("linkart", LinkartConfiguration.class);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LinkartCommand.register(dispatcher);
        });

        ServerWorldEvents.LOAD.register((server, world) -> {
            if (LinkartConfiguration.chunkloading) LoadingCarts.getOrCreate(world);
        });

        ServerTickEvents.START_WORLD_TICK.register(world -> {
            if (LinkartConfiguration.chunkloading && ((PersistentStateAccessor) world.getPersistentStateManager()).linkart$loadedStates().containsKey("linkart_loading_carts")) {
                LoadingCarts.getOrCreate(world).tick(world);
            }
        });
    }

    private static RegistryKey<? extends Registry<Item>> itemKey() {
        return RegistryKey.ofRegistry(Identifier.tryParse("item"));
    }
}
