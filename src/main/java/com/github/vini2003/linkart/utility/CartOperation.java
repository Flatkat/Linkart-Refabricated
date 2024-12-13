package com.github.vini2003.linkart.utility;


import com.github.vini2003.linkart.configuration.LinkartConfiguration;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;

public record CartOperation(Type type, AbstractMinecartEntity minecart) {

    public enum Type {
        LINKING {
            @Override
            public ActionResult perform(AbstractMinecartEntity minecart, CartOperation operation, ItemStack stack) {
                if (minecart.linkart$getFollower() == operation.minecart()) return ActionResult.FAIL; //Linking a parent cart to its follower.
                if (minecart.linkart$getFollowing() != null) return ActionResult.FAIL; //Linking to an already linked cart.
                if (Math.abs(minecart.distanceTo(operation.minecart()) - 1) > LinkartConfiguration.pathfindingDistance)
                    return ActionResult.FAIL; //Linking beyond pathfindingDistance, will just break on first tick.

                //Leading minecarts must never be linked to a follower. This creates an immovable object or an Ouroboros, if you will.
                if (minecart.linkart$getFollower() != null && minecart.linkart$getFollowing() == null) {
                    var temp = minecart;
                    while (temp != null) {
                        if (temp == operation.minecart()) return ActionResult.FAIL;
                        temp = temp.linkart$getFollower();
                    }
                }

                CartUtils.linkTo(minecart, operation.minecart(), stack);
                return ActionResult.SUCCESS;
            }
        },
        UNLINKING {
            @Override
            public ActionResult perform(AbstractMinecartEntity minecart, CartOperation operation, ItemStack stack) {
                if (operation.minecart().linkart$getFollower() != minecart) return ActionResult.FAIL;

                CartUtils.unlinkFromParent(minecart);
                return ActionResult.SUCCESS;
            }
        };

        public abstract ActionResult perform(AbstractMinecartEntity minecart, CartOperation operation, ItemStack stack);
    }
}
