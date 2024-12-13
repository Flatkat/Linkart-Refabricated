package com.github.vini2003.linkart.utility;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.function.Supplier;

public class LinkartCommand {

    private static final Supplier<Text> RELOADED = () -> TextUtil.literal("reloaded linkart config");

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("linkart")
                .then(CommandManager.literal("config")
                        .then(CommandManager.literal("reload")
                                .executes(context -> {
//                                    Linkart.loadConfig();
                                    context.getSource().sendFeedback(RELOADED, true);
                                    return 1;
                                }))));
    }
}
