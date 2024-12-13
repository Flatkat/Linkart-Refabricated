package com.github.vini2003.linkart.utility;

import com.github.vini2003.linkart.configuration.LinkartConfiguration;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;

public class CollisionUtils {
    public static boolean shouldCollide(Entity source, Entity target) {
        if (source instanceof AbstractMinecartEntity check) {
            int i = 0;

            do {
                if (check == target) {
                    return false;
                }

                check = check.linkart$getFollower();
                ++i;
            } while (check != null && i < LinkartConfiguration.collisionDepth);

            check = (AbstractMinecartEntity) source;
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
