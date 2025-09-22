package com.github.vini2003.linkart.mixin;

import com.github.vini2003.linkart.api.LinkableMinecart;
import com.github.vini2003.linkart.configuration.LinkartConfiguration;
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
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.6 {
/*import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
*///?}
//? if =1.21.1 {
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.math.MathHelper;
//?}

import java.util.UUID;

@Mixin({AbstractMinecartEntity.class})
public abstract class AbstractMinecartEntityMixin extends Entity implements LinkableMinecart {

    // Used to smooth out acceleration
    @Unique private static final double SAFE_SPEEDUP_THRESHOLD = 0.4;
    @Unique private static final double SMOOTH_SPEEDUP_AMOUNT = 0.2;
    @Unique private static final double SAFE_SPEEDUP_DIFFERENCE = 0.02;
    @Unique private double lastMovementLength = 0.0D;  // Movement length on previous tick

    @Unique private AbstractMinecartEntity linkart$following;
    @Unique private AbstractMinecartEntity linkart$follower;
    @Unique private UUID linkart$followingUUID;
    @Unique private UUID linkart$followerUUID;
    @Unique private ItemStack linkart$itemStack = ItemStack.EMPTY;

    public AbstractMinecartEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Unique private double limitMovementLength(double targetMovementLength) {
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
    //? if =1.21.1
    @ModifyArg(method = "moveOnRail", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V", ordinal = 0))
    //? if >=1.21.4
    /*@ModifyArg(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/VehicleEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V", ordinal = 0))*/
    private Vec3d modifiedMovement(Vec3d movement) {
        if (this.lastMovementLength < movement.length()) {
            final double targetMovementLength = movement.length();

            // Limit the movement length
            movement = movement.multiply(limitMovementLength(targetMovementLength) / targetMovementLength);
        }

        this.lastMovementLength = movement.length();
        return movement;
    }

    // CHECK THIS AGAIN
    //? if =1.21.1 {
    @WrapOperation(method = "moveOnRail", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(DDD)D"))
    private double linkart$skipVelocityClamping(double value, double min, double max, Operation<Double> original) {
        if (this.linkart$getFollowing() != null) {
            AbstractMinecartEntity following = this.linkart$getFollowing();
            while (following.linkart$getFollowing() != null) {
                following = following.linkart$getFollowing();
            }
            double parent = ((MinecartAccessor) following).linkart$getMaxSpeed();
            return MathHelper.clamp(value, -parent, parent);
        }
        return original.call(value, min, max);
    }
    //?}

    @Inject(at = @At("HEAD"), method = "tick")
    private void linkart$tick(CallbackInfo ci) {
        if (getWorld().isClient()) return;
        AbstractMinecartEntity cast = (AbstractMinecartEntity) (Object) this;
        if (linkart$getFollowing() == null) return;

        Vec3d pos = getPos();
        Vec3d pos2 = linkart$getFollowing().getPos();
        double dist = Math.max(Math.abs(pos.distanceTo(pos2)) - LinkartConfiguration.distance, 0);
        Vec3d vec3d = pos.relativize(pos2);
        vec3d = vec3d.multiply(LinkartConfiguration.velocityMultiplier);

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
            dist += LinkartConfiguration.distance;
            vec3d = vel;
        }

        // Calculate new velocity
        vec3d = vec3d.normalize().multiply(dist);

        if (dist <= 1) {
            // Go slower (1.0->0.8) the closer (1->0) we are
            setVelocity(vec3d.multiply(0.8 + 0.2 * Math.abs(dist)));
        } else if (dist <= LinkartConfiguration.pathfindingDistance) {
            setVelocity(vec3d);
        } else {
            CartUtils.unlinkFromParent(cast);
        }

        if (LinkartConfiguration.chunkloading) {
            if (linkart$getFollower() != null && !CartUtils.approximatelyZero(this.getVelocity().length())) {
                ((ServerWorld) this.getWorld()).getChunkManager().addTicket(ChunkTicketType.PORTAL, this.getChunkPos(), LinkartConfiguration.chunkloadingRadius/*? if <1.21.5 {*/, this.getBlockPos()/*?}*/);
                LoadingCarts.getOrCreate((ServerWorld) getWorld()).addCart(cast);
            } else {
                LoadingCarts.getOrCreate((ServerWorld) getWorld()).removeCart(cast);
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "pushAwayFrom", cancellable = true)
    void onPushAway(Entity entity, CallbackInfo ci) {
        if (!CollisionUtils.shouldCollide(this, entity)) ci.cancel();
    }

    //? if <1.21.6 {
    @Inject(at = @At("RETURN"), method = "writeCustomDataToNbt")
    private void linkart$write(NbtCompound nbt, CallbackInfo ci) {
        if (linkart$followingUUID != null) nbt./*? if >=1.21.5 {*//*put*//*?} else {*/putUuid/*?}*/("LK-Following"/*? if >=1.21.5 {*//*, Uuids.INT_STREAM_CODEC*//*?}*/, linkart$followingUUID);
        if (linkart$followerUUID != null) nbt./*? if >=1.21.5 {*//*put*//*?} else {*/putUuid/*?}*/("LK-Follower"/*? if >=1.21.5 {*//*, Uuids.INT_STREAM_CODEC*//*?}*/, linkart$followerUUID);
        if (linkart$itemStack != null && !linkart$itemStack.isEmpty()) nbt.put("LK-ItemStack", linkart$itemStack./*? if =1.21.1 {*/encodeAllowEmpty/*?}*//*? if =1.21.4 {*//*toNbtAllowEmpty*//*?}*//*? if >=1.21.5 {*//*toNbt*//*?}*/(this.getRegistryManager()));
    }

    @Inject(at = @At("RETURN"), method = "readCustomDataFromNbt")
    private void linkart$read(NbtCompound nbt, CallbackInfo ci) {
        /*? if >=1.21.5 {*/
        /*linkart$followingUUID = nbt.get("LK-Following", Uuids.INT_STREAM_CODEC).orElse(null);
        linkart$followerUUID = nbt.get("LK-Follower", Uuids.INT_STREAM_CODEC).orElse(null);
        if (nbt.contains("LK-ItemStack")) linkart$itemStack = ItemStack.fromNbt(this.getRegistryManager(), nbt.getCompound("LK-ItemStack").orElseThrow()).orElse(null);
        *//*?} else {*/
        if (nbt.contains("LK-Following")) linkart$followingUUID = nbt.getUuid("LK-Following");
        if (nbt.contains("LK-Follower")) linkart$followerUUID = nbt.getUuid("LK-Follower");
        if (nbt.contains("LK-ItemStack")) linkart$itemStack = ItemStack.fromNbtOrEmpty(this.getRegistryManager(), nbt.getCompound("LK-ItemStack"));
        /*?}*/
    }
    //?} else {
    /*@Inject(at = @At("RETURN"), method = "writeCustomData")
    private void linkart$write(WriteView view, CallbackInfo ci) {
        view.putNullable("LK-Following", Uuids.INT_STREAM_CODEC, linkart$followingUUID);
        view.putNullable("LK-Follower", Uuids.INT_STREAM_CODEC, linkart$followerUUID);

        if(!linkart$itemStack.isEmpty()) {
            view.put("LK-ItemStack", ItemStack.CODEC, linkart$itemStack);
        }
    }

    @Inject(at = @At("RETURN"), method = "readCustomData")
    private void linkart$read(ReadView view, CallbackInfo ci) {
        view.read("LK-Following", Uuids.INT_STREAM_CODEC).ifPresent(uuid -> linkart$followingUUID = uuid);
        view.read("LK-Follower", Uuids.INT_STREAM_CODEC).ifPresent(uuid -> linkart$followerUUID = uuid);
        linkart$itemStack = view.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }
    *///?}

    @Override
    public AbstractMinecartEntity linkart$getFollowing() {
        if (linkart$following == null && linkart$followingUUID != null) {
            linkart$following = (AbstractMinecartEntity) ((ServerWorld) this.getWorld()).getEntity(linkart$followingUUID);
        }
        return linkart$following;
    }

    @Override
    public void linkart$setFollowing(AbstractMinecartEntity following) {
        this.linkart$following = following;
        this.linkart$followingUUID = following != null ? following.getUuid() : null;
    }

    @Override
    public AbstractMinecartEntity linkart$getFollower() {
        if (linkart$follower == null && linkart$followerUUID != null) {
            linkart$follower = (AbstractMinecartEntity) ((ServerWorld) this.getWorld()).getEntity(linkart$followerUUID);
        }
        return linkart$follower;
    }

    @Override
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
