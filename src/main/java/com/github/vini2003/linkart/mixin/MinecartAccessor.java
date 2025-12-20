package com.github.vini2003.linkart.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
//? if >=1.21.2
//import net.minecraft.server.level.ServerLevel;

@Mixin(AbstractMinecart.class)
public interface MinecartAccessor {

    @Invoker("getMaxSpeed")
    //? if >=1.21.2 {
    /*double linkart$getMaxSpeed(ServerLevel level);
    *///? } else
    double linkart$getMaxSpeed();
}
