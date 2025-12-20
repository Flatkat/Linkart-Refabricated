package com.github.vini2003.linkart.utility;

import com.github.vini2003.linkart.Linkart;
import com.github.vini2003.linkart.configuration.LinkartConfiguration;
import com.mojang.brigadier.CommandDispatcher;
import java.util.function.Supplier;

import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class LinkartCommand {

    private static final Supplier<Component> RELOADED = () -> Component.literal("reloaded linkart config");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        //? if midnightlib: >=1.9.0 {
        dispatcher.register(Commands.literal("linkart")
                .then(Commands.literal("config")
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    MidnightConfig.configInstances.get("linkart").loadValuesFromJson();
                                    context.getSource().sendSuccess(RELOADED, true);
                                    return 1;
                                }))));
        //?}
    }
}
