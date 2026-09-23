package com.github.vini2003.linkart.utility;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.core.jmx.Server;

public class CartUtils {

    public static void spawnChainParticles(AbstractMinecart entity) {
        Level level = entity.level();
        if (!level.isClientSide()) {
            //? if <26.1 {
            ((ServerLevel) level).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, entity.linkart$getLinkItem()), entity.getX(), entity.getY() + 0.3, entity.getZ(), 15, 0.2, 0.2, 0.2, 0.2);
            //? } else
            //((ServerLevel) level).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, entity.linkart$getLinkItem().getItem()), entity.getX(), entity.getY() + 0.3, entity.getZ(), 15, 0.2, 0.2, 0.2, 0.2);
        }
    }

    public static boolean approximatelyZero(double a) {
        return Math.abs(0 - a) < 0.00029146489604938;
    }

    public static void unlinkFromParent(AbstractMinecart entity) {
        if (entity == null) return;
        var following = entity.linkart$getFollowing();
        if (following == null) return;

        following.linkart$setFollower(null);
        entity.linkart$setFollowing(null);

        entity.setDeltaMovement(0, 0, 0);

        if (!entity.linkart$getLinkItem().isEmpty()) {
            //? if <=1.21.1 {
            entity.spawnAtLocation(entity.linkart$getLinkItem());
            //?} else
            //entity.spawnAtLocation((ServerLevel) entity.level(), entity.linkart$getLinkItem());
        }

        entity.linkart$setLinkItem(ItemStack.EMPTY);
    }

    public static void linkTo(AbstractMinecart minecart, AbstractMinecart to, ItemStack linkingItem) {
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
