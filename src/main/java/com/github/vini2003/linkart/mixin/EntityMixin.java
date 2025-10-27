package com.github.vini2003.linkart.mixin;

import com.github.vini2003.linkart.utility.CartUtils;
import com.github.vini2003.linkart.utility.CollisionUtils;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(at = @At("HEAD"), method = "remove")
    void linkart$removeLink(CallbackInfo callbackInformation, @Local(argsOnly = true) Entity.RemovalReason reason) {
        if ((Entity) (Object) this instanceof AbstractMinecartEntity minecart && !minecart./*? if >=1.21.9 {*//*getEntityWorld()*//*?} else {*/getWorld()/*?}*/.isClient() && reason.shouldDestroy()) {
            CartUtils.unlinkFromParent(minecart);
            CartUtils.unlinkFromParent(minecart.linkart$getFollower());
        }
    }

    @Inject(at = @At("HEAD"), method = "adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;", cancellable = true)
    void linkart$onRecalculateVelocity(Vec3d movement, CallbackInfoReturnable<Vec3d> cir) {
        if ((Object) this instanceof AbstractMinecartEntity minecart) {
            List<Entity> collisions = minecart./*? if >=1.21.9 {*//*getEntityWorld()*//*?} else {*/getWorld()/*?}*/.getOtherEntities((Entity) (Object) this, minecart.getBoundingBox().stretch(movement));

            for (Entity entity : collisions) {
                if (!CollisionUtils.shouldCollide(minecart, entity) && minecart./*? if >=1.21.9 {*//*getEntityWorld()*//*?} else {*/getWorld()/*?}*/.getBlockState(minecart.getBlockPos()).getBlock() instanceof AbstractRailBlock) {
                    cir.setReturnValue(movement);
                    return;
                }
            }
        }
    }
}
