package com.github.vini2003.linkart.api;

import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface LinkableMinecart {

    default @Nullable AbstractMinecartEntity linkart$getFollowing() {
        throw new IllegalStateException("Implemented via mixin");
    }

    default void linkart$setFollowing(AbstractMinecartEntity following) {
        throw new IllegalStateException("Implemented via mixin");
    }

    default @Nullable AbstractMinecartEntity linkart$getFollower() {
        throw new IllegalStateException("Implemented via mixin");
    }

    default void linkart$setFollower(AbstractMinecartEntity follower) {
        throw new IllegalStateException("Implemented via mixin");
    }

    default ItemStack linkart$getLinkItem() {
        throw new IllegalStateException("Implemented via mixin");
    }

    default void linkart$setLinkItem(ItemStack linkItem) {
        throw new IllegalStateException("Implemented via mixin");
    }
}
