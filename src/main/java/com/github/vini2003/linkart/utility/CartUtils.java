package com.github.vini2003.linkart.utility;

import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

public class CartUtils {

    public static void spawnChainParticles(AbstractMinecartEntity entity) {
        if (!entity.getWorld().isClient()) {
            ((ServerWorld) entity.getWorld()).spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, entity.linkart$getLinkItem()), entity.getX(), entity.getY() + 0.3, entity.getZ(), 15, 0.2, 0.2, 0.2, 0.2);
        }
    }

    public static boolean approximatelyZero(double a) {
        return Math.abs(0 - a) < 0.00029146489604938;
    }

    public static void unlinkFromParent(AbstractMinecartEntity entity) {
        if (entity == null) return;
        var following = entity.linkart$getFollowing();
        if (following == null) return;

        following.linkart$setFollower(null);
        entity.linkart$setFollowing(null);

        entity.setVelocity(0, 0, 0);

        if (!entity.linkart$getLinkItem().isEmpty()) {
            entity.dropStack((ServerWorld) entity.getWorld(), entity.linkart$getLinkItem());
            spawnChainParticles(entity);
        }

        entity.linkart$setLinkItem(ItemStack.EMPTY);
    }

    public static void linkTo(AbstractMinecartEntity minecart, AbstractMinecartEntity to, ItemStack linkingItem) {
        minecart.linkart$setFollowing(to);
        to.linkart$setFollower(minecart);

        if (!linkingItem.isEmpty()) {
            ItemStack linkStack = linkingItem.copy();
            linkStack.setCount(1);
            minecart.linkart$setLinkItem(linkStack);
        }

        CartUtils.spawnChainParticles(minecart);
    }
}
