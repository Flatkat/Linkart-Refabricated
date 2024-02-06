package com.github.vini2003.linkart.mixin;

import com.github.vini2003.linkart.Linkart;
import com.github.vini2003.linkart.api.LinkableMinecart;
import com.github.vini2003.linkart.utility.CartUtils;
import com.github.vini2003.linkart.utility.CollisionUtils;
import com.github.vini2003.linkart.utility.LoadingCarts;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin({AbstractMinecartEntity.class})
public abstract class AbstractMinecartEntityMixin extends Entity implements LinkableMinecart {

    // Used to smooth out acceleration
    @Unique
    private static final double SAFE_SPEEDUP_THRESHOLD = 0.4;
    @Unique
    private static final double SMOOTH_SPEEDUP_AMOUNT = 0.2;
    @Unique
    private static final double SAFE_SPEEDUP_DIFFERENCE = 0.02;
    @Unique
    private double lastMovementLength = 0.0D;  // Movement length on previous tick

    @Unique
    private AbstractMinecartEntity linkart$following;
    @Unique
    private AbstractMinecartEntity linkart$follower;
    @Unique
    private UUID linkart$followingUUID;
    @Unique
    private UUID linkart$followerUUID;
    @Unique
    private ItemStack linkart$itemStack = ItemStack.EMPTY;

    public AbstractMinecartEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Unique
    private double limitMovementLength(double targetMovementLength) {
        double cartLastMovementLength = this.lastMovementLength;

        boolean isLeading = (this.linkart$getFollowing() == null && this.linkart$getFollower() != null);
        // Don't limit if we are not the leading minecart
        if (!isLeading) return targetMovementLength;
        // Don't limit if we are below the safe speedup threshold
        if (targetMovementLength <= SAFE_SPEEDUP_THRESHOLD) return targetMovementLength;

        AbstractMinecartEntity follower = this.linkart$getFollower();
        // Check if there are follower minecarts not at our speed
        while (follower != null) {
            double followerLastMovementLength = ((AbstractMinecartEntityMixin) (Object) follower).lastMovementLength;
            if (Math.abs(followerLastMovementLength - cartLastMovementLength) > SAFE_SPEEDUP_DIFFERENCE)
                // If so, maintain same speed
                return cartLastMovementLength;
            follower = follower.linkart$getFollower();
        }

        // Otherwise increase our speed slowly
        return Math.min(Math.max(
                        cartLastMovementLength + SMOOTH_SPEEDUP_AMOUNT,
                        SAFE_SPEEDUP_THRESHOLD),  // min
                targetMovementLength);  // max
    }

    // Ensure the train doesn't break apart (especially if other minecart mods increase speed)
    @ModifyArg(method = "moveOnRail", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V", ordinal = 0))
    private Vec3d modifiedMovement(Vec3d movement) {
        return this.linkart$modifyMovement(movement);
    }

    @Override
    public Vec3d linkart$modifyMovement(Vec3d movement) {
        if (this.lastMovementLength < movement.length()) {
            final double targetMovementLength = movement.length();

            // Limit the movement length
            movement = movement.multiply(limitMovementLength(targetMovementLength) / targetMovementLength);
        }

        this.lastMovementLength = movement.length();
        return movement;
    }

    @Inject(at = @At("HEAD"), method = "tick")
    private void linkart$tick(CallbackInfo ci) {
        if (!getWorld().isClient()) {
            AbstractMinecartEntity cast = (AbstractMinecartEntity) (Object) this;
            if (linkart$getFollowing() != null) {
                Vec3d pos = getPos();
                Vec3d pos2 = linkart$getFollowing().getPos();
                double dist = Math.max(Math.abs(pos.distanceTo(pos2)) - Linkart.CONFIG.distance, 0);
                Vec3d vec3d = pos.relativize(pos2);
                vec3d = vec3d.multiply(Linkart.CONFIG.velocityMultiplier);

                // Check if we are on a sharp curve
                Vec3d vel = getVelocity();
                Vec3d vel2 = linkart$getFollowing().getVelocity();
                boolean differentDirection = (
                        vel.length() > 0.15
                                && vel2.length() > 0.005
                                && vel.normalize().distanceTo(vel2.normalize()) > 1.42
                                && pos.distanceTo(pos2) > 0.5
                );

                if (differentDirection) {
                    // Keep ourselves going at same speed if on curve
                    dist += Linkart.CONFIG.distance;
                    vec3d = vel;
                }

                // Calculate new velocity
                vec3d = vec3d.normalize().multiply(dist);

                if (dist <= 1) {
                    // Go slower (1.0->0.8) the closer (1->0) we are
                    setVelocity(vec3d.multiply(0.8 + 0.2 * Math.abs(dist)));
                } else {
                    if (dist <= Linkart.CONFIG.pathfindingDistance) {
                        setVelocity(vec3d);
                    } else {
                        CartUtils.unlink(cast);
                    }
                }
            }

            if (Linkart.CONFIG.chunkloading) {
                if (linkart$getFollower() != null && !CartUtils.approximatelyZero(this.getVelocity().length())) {
                    ((ServerWorld) this.getWorld()).getChunkManager().addTicket(ChunkTicketType.PORTAL, this.getChunkPos(), Linkart.CONFIG.chunkloadingRadius, this.getBlockPos());
                    LoadingCarts.getOrCreate((ServerWorld) getWorld()).addCart(cast);
                } else {
                    LoadingCarts.getOrCreate((ServerWorld) getWorld()).removeCart(cast);
                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "pushAwayFrom", cancellable = true)
    void onPushAway(Entity entity, CallbackInfo ci) {
        if (!CollisionUtils.shouldCollide(this, entity)) {
            ci.cancel();
        }
    }

    @Inject(at = @At("RETURN"), method = "writeCustomDataToNbt")
    private void linkart$write(NbtCompound nbt, CallbackInfo ci) {
        if (linkart$followingUUID != null) {
            nbt.putUuid("LK-Following", linkart$followingUUID);
        }

        if (linkart$followerUUID != null) {
            nbt.putUuid("LK-Follower", linkart$followerUUID);
        }

        if (linkart$itemStack != null) {
            nbt.put("LK-ItemStack", linkart$itemStack.writeNbt(new NbtCompound()));
        }
    }

    @Inject(at = @At("RETURN"), method = "readCustomDataFromNbt")
    private void linkart$read(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("LK-Following")) {
            linkart$followingUUID = nbt.getUuid("LK-Following");
        }

        if (nbt.contains("LK-Follower")) {
            linkart$followerUUID = nbt.getUuid("LK-Follower");
        }

        if (nbt.contains("LK-ItemStack")) {
            linkart$itemStack = ItemStack.fromNbt(nbt.getCompound("LK-ItemStack"));
        }
    }

    @Override
    public AbstractMinecartEntity linkart$getFollowing() {
        if (linkart$following == null) {
            linkart$following = (AbstractMinecartEntity) ((ServerWorld) this.getWorld()).getEntity(linkart$followingUUID);
        }
        return linkart$following;
    }

    public void linkart$setFollowing(AbstractMinecartEntity following) {
        this.linkart$following = following;
        this.linkart$followingUUID = following != null ? following.getUuid() : null;
    }

    @Override
    public AbstractMinecartEntity linkart$getFollower() {
        if (linkart$follower == null) {
            linkart$follower = (AbstractMinecartEntity) ((ServerWorld) this.getWorld()).getEntity(linkart$followerUUID);
        }
        return linkart$follower;
    }

    public void linkart$setFollower(AbstractMinecartEntity follower) {
        this.linkart$follower = follower;
        this.linkart$followerUUID = follower != null ? follower.getUuid() : null;
    }

    @Override
    public ItemStack linkart$getLinkItem() {
        return linkart$itemStack;
    }

    @Override
    public void linkart$setLinkItem(ItemStack linkItem) {
        this.linkart$itemStack = linkItem == null ? ItemStack.EMPTY : linkItem;
    }
}
