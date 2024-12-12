package com.github.vini2003.linkart.mixin;

import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractMinecartEntity.class)
public interface MinecartAccessor {

    @Invoker("getMaxSpeed")
    double linkart$getMaxSpeed(ServerWorld world);


}
