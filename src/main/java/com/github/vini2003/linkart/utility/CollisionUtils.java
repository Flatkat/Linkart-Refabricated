package com.github.vini2003.linkart.utility;

import com.github.vini2003.linkart.configuration.LinkartConfiguration;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

public class CollisionUtils {
    public static boolean shouldCollide(Entity source, Entity target) {
        if (source instanceof AbstractMinecart check) {
            int i = 0;

            do {
                if (check == target) {
                    return false;
                }

                check = check.linkart$getFollower();
                ++i;
            } while (check != null && i < LinkartConfiguration.collisionDepth);

            check = (AbstractMinecart) source;
            i = 0;

            while (check != target) {
                check = check.linkart$getFollowing();
                ++i;
                if (check == null || i >= LinkartConfiguration.collisionDepth) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }
}
