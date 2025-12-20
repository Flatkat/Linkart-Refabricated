package com.github.vini2003.linkart.mixin;

import com.github.vini2003.linkart.api.LinkableMinecart;
import com.github.vini2003.linkart.configuration.LinkartConfiguration;
import com.github.vini2003.linkart.utility.CartUtils;
import com.github.vini2003.linkart.utility.CollisionUtils;
import com.github.vini2003.linkart.utility.LoadingCarts;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.6 {
/*import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
*///?}
//? if =1.21.1 {
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//?}
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin({AbstractMinecart.class})
public abstract class AbstractMinecartEntityMixin extends Entity implements LinkableMinecart {

    // Used to smooth out acceleration
    @Unique private static final double SAFE_SPEEDUP_THRESHOLD = 0.4;
    @Unique private static final double SMOOTH_SPEEDUP_AMOUNT = 0.2;
    @Unique private static final double SAFE_SPEEDUP_DIFFERENCE = 0.02;
    @Unique private double lastMovementLength = 0.0D;  // Movement length on previous tick

    @Unique private AbstractMinecart linkart$following;
    @Unique private AbstractMinecart linkart$follower;
    @Unique private UUID linkart$followingUUID;
    @Unique private UUID linkart$followerUUID;
    @Unique private ItemStack linkart$itemStack = ItemStack.EMPTY;

    public AbstractMinecartEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Unique private double limitMovementLength(double targetMovementLength) {
        double cartLastMovementLength = this.lastMovementLength;

        boolean isLeading = (this.linkart$getFollowing() == null && this.linkart$getFollower() != null);
        // Don't limit if we are not the leading minecart
        if (!isLeading) return targetMovementLength;
        // Don't limit if we are below the safe speedup threshold
        if (targetMovementLength <= SAFE_SPEEDUP_THRESHOLD) return targetMovementLength;

        AbstractMinecart follower = this.linkart$getFollower();
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
    @ModifyArg(method = "moveAlongTrack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", ordinal = 0))
    //? if >=1.21.4
    //@ModifyArg(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/VehicleEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", ordinal = 0))
    private Vec3 modifiedMovement(Vec3 movement) {
        if (this.lastMovementLength < movement.length()) {
            final double targetMovementLength = movement.length();

            // Limit the movement length
            movement = movement.scale(limitMovementLength(targetMovementLength) / targetMovementLength);
        }

        this.lastMovementLength = movement.length();
        return movement;
    }

    // CHECK THIS AGAIN
    //? if =1.21.1 {
    @WrapOperation(method = "moveAlongTrack", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D"))
    private double linkart$skipVelocityClamping(double value, double min, double max, Operation<Double> original) {
        if (this.linkart$getFollowing() != null) {
            AbstractMinecart following = this.linkart$getFollowing();
            while (following.linkart$getFollowing() != null) {
                following = following.linkart$getFollowing();
            }
            double parent = ((MinecartAccessor) following).linkart$getMaxSpeed();
            return Mth.clamp(value, -parent, parent);
        }
        return original.call(value, min, max);
    }
    //?}

    @Inject(at = @At("HEAD"), method = "tick")
    private void linkart$tick(CallbackInfo ci) {
        if (level().isClientSide()) return;
        AbstractMinecart cast = (AbstractMinecart) (Object) this;
        if (linkart$getFollowing() == null) return;

        Vec3 pos = position();
        Vec3 pos2 = linkart$getFollowing().position();
        double dist = Math.max(Math.abs(pos.distanceTo(pos2)) - LinkartConfiguration.distance, 0);
        Vec3 vec3d = pos.vectorTo(pos2);
        vec3d = vec3d.scale(LinkartConfiguration.velocityMultiplier);

        // Check if we are on a sharp curve
        Vec3 vel = getDeltaMovement();
        Vec3 vel2 = linkart$getFollowing().getDeltaMovement();
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
        vec3d = vec3d.normalize().scale(dist);

        if (dist <= 1) {
            // Go slower (1.0->0.8) the closer (1->0) we are
            setDeltaMovement(vec3d.scale(0.8 + 0.2 * Math.abs(dist)));
        } else if (dist <= LinkartConfiguration.pathfindingDistance) {
            setDeltaMovement(vec3d);
        } else {
            CartUtils.unlinkFromParent(cast);
        }

        if (LinkartConfiguration.chunkloading) {
            if (linkart$getFollower() != null && !CartUtils.approximatelyZero(this.getDeltaMovement().length())) {
                //? if <1.21.5 {
                ((ServerLevel) this.level()).getChunkSource().addRegionTicket(TicketType.PORTAL, this.chunkPosition(), LinkartConfiguration.chunkloadingRadius, this.blockPosition());
                //?} else
                //((ServerLevel) this.level()).getChunkSource().addTicketWithRadius(TicketType.PORTAL, this.chunkPosition(), LinkartConfiguration.chunkloadingRadius);
                LoadingCarts.getOrCreate((ServerLevel) this.level()).addCart(cast);
            } else {
                LoadingCarts.getOrCreate((ServerLevel) this.level()).removeCart(cast);
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "push(Lnet/minecraft/world/entity/Entity;)V", cancellable = true)
    void onPushAway(Entity entity, CallbackInfo ci) {
        if (!CollisionUtils.shouldCollide(this, entity)) ci.cancel();
    }

    @Inject(at = @At("RETURN"), method = "addAdditionalSaveData")
    //? if >=1.21.6 {
    /*private void linkart$addAdditionalSaveData(ValueOutput compoundTag, CallbackInfo ci) {
    *///? } else {
    private void linkart$addAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
    //? }
        //? if >=1.21.5 {
        /*compoundTag.storeNullable("LK-Following", UUIDUtil.CODEC, linkart$followingUUID);
        compoundTag.storeNullable("LK-Follower", UUIDUtil.CODEC, linkart$followerUUID);
        if(!this.linkart$getLinkItem().isEmpty()) {
            //? if =1.21.5
            //RegistryOps<Tag> registryOps = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            compoundTag.store("LK-ItemStack", ItemStack.CODEC, /^? if =1.21.5 >>^//^registryOps,^/ this.linkart$getLinkItem());
        }
        *///?} else {
        if (linkart$followingUUID != null) compoundTag.putUUID("LK-Following", linkart$followingUUID);
        if (linkart$followerUUID != null) compoundTag.putUUID("LK-Follower", linkart$followerUUID);
        compoundTag.put("LK-ItemStack", this.linkart$getLinkItem().saveOptional(this.registryAccess()));
        //?}
    }

    @Inject(at = @At("RETURN"), method = "readAdditionalSaveData")
    //? if >=1.21.6 {
    /*private void linkart$readAdditionalSaveData(ValueInput compoundTag, CallbackInfo ci) {
    *///? } else {
    private void linkart$readAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
    //? }
        /*? if >=1.21.5 {*/
        /*linkart$followingUUID = compoundTag.read("LK-Following", UUIDUtil.CODEC).orElse(null);
        linkart$followerUUID = compoundTag.read("LK-Follower", UUIDUtil.CODEC).orElse(null);
        //? if =1.21.5
        //RegistryOps<Tag> registryOps = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        this.linkart$setLinkItem(compoundTag.read("LK-ItemStack", ItemStack.OPTIONAL_CODEC/^? if =1.21.5 >> ').'^//^,registryOps^/).orElse(ItemStack.EMPTY));
        *//*?} else {*/
        if (compoundTag.contains("LK-Following")) linkart$followingUUID = compoundTag.getUUID("LK-Following");
        if (compoundTag.contains("LK-Follower")) linkart$followerUUID = compoundTag.getUUID("LK-Follower");
        if (compoundTag.contains("LK-ItemStack", Tag.TAG_COMPOUND)) {
            this.linkart$setLinkItem(ItemStack.parseOptional(this.registryAccess(), compoundTag.getCompound("LK-ItemStack")));
        }
        /*?}*/
    }

    @Override
    public AbstractMinecart linkart$getFollowing() {
        if (linkart$following == null && linkart$followingUUID != null) {
            linkart$following = (AbstractMinecart) ((ServerLevel) this.level()).getEntity(linkart$followingUUID);
        }
        return linkart$following;
    }

    @Override
    public void linkart$setFollowing(AbstractMinecart following) {
        this.linkart$following = following;
        this.linkart$followingUUID = following != null ? following.getUUID() : null;
    }

    @Override
    public AbstractMinecart linkart$getFollower() {
        if (linkart$follower == null && linkart$followerUUID != null) {
            linkart$follower = (AbstractMinecart) ((ServerLevel) this.level()).getEntity(linkart$followerUUID);
        }
        return linkart$follower;
    }

    @Override
    public void linkart$setFollower(AbstractMinecart follower) {
        this.linkart$follower = follower;
        this.linkart$followerUUID = follower != null ? follower.getUUID() : null;
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
