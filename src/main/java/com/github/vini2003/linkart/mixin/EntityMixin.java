package com.github.vini2003.linkart.mixin;

import com.github.vini2003.linkart.utility.CartUtils;
import com.github.vini2003.linkart.utility.CollisionUtils;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.phys.Vec3;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(at = @At("HEAD"), method = "remove")
    void linkart$removeLink(CallbackInfo callbackInformation, @Local(argsOnly = true) Entity.RemovalReason reason) {
        if ((Entity) (Object) this instanceof AbstractMinecart minecart && !minecart.level().isClientSide() && reason.shouldDestroy()) {
            CartUtils.unlinkFromParent(minecart);
            CartUtils.unlinkFromParent(minecart.linkart$getFollower());
        }
    }

    @Inject(at = @At("HEAD"), method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", cancellable = true)
    void linkart$onRecalculateVelocity(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        if ((Object) this instanceof AbstractMinecart minecart) {
            List<Entity> collisions = minecart.level().getEntities((Entity) (Object) this, minecart.getBoundingBox().expandTowards(movement));

            for (Entity entity : collisions) {
                if (!CollisionUtils.shouldCollide(minecart, entity) && minecart.level().getBlockState(minecart.blockPosition()).getBlock() instanceof BaseRailBlock) {
                    cir.setReturnValue(movement);
                    return;
                }
            }
        }
    }
}
