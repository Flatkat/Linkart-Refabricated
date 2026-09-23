package com.github.vini2003.linkart.utility;

import com.github.vini2003.linkart.Linkart;
import com.github.vini2003.linkart.configuration.LinkartConfiguration;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
//? if >=1.21.5
//import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

public class LoadingCarts extends SavedData {

    /*? if <1.21.5*/
    private static final Factory<LoadingCarts> TYPE = new Factory<>(LoadingCarts::new, (compound, lookup) -> new LoadingCarts().readNbt(compound), null);//thanks, FAPI
    /*? if >=1.21.5 {*/
    /*private static final Codec<LoadingCarts> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            BlockPos.CODEC.listOf().fieldOf("chunksToSave").forGetter(carts -> carts.chunksToReload.stream().toList())
        ).apply(instance, LoadingCarts::new)
    );

    //? if <26.1 {
    private static final SavedDataType<LoadingCarts> TYPE = new SavedDataType<>(
        "linkart_loading_carts", LoadingCarts::new, CODEC, null
    );
    //? } else {
    /^private static final SavedDataType<LoadingCarts> TYPE = new SavedDataType<>(
            ResourceLocation.fromNamespaceAndPath(Linkart.ID, "loading_carts"),
            LoadingCarts::new, CODEC, null
    );
    ^///? }
    *//*?}*/

    public static LoadingCarts getOrCreate(ServerLevel serverLevel) {
        return serverLevel.getDataStorage().computeIfAbsent(TYPE/*? if <1.21.5 {*/, "linkart_loading_carts"/*?}*/);
    }

    private final Set<BlockPos> chunksToReload = new HashSet<>();
    private final Set<AbstractMinecart> cartsToBlockPos = new HashSet<>();

    public LoadingCarts() { this(List.of()); }

    public LoadingCarts(Collection<BlockPos> chunksToReload) {
        this.chunksToReload.addAll(chunksToReload);
    }

    /*? if <1.21.5 {*/
    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        ListTag list = new ListTag();
        for (AbstractMinecart minecart : cartsToBlockPos) {
            if (!minecart.isRemoved()) list.add(LongTag.valueOf(minecart.blockPosition().asLong()));
        }
        nbt.put("chunksToSave", list);
        cartsToBlockPos.clear();
        return nbt;
    }

    public LoadingCarts readNbt(CompoundTag nbt) {
        ListTag list = nbt.getList("chunksToSave", Tag.TAG_LONG);
        for (Tag element : list) {
            chunksToReload.add(BlockPos.of(((LongTag) element).getAsLong()));
        }
        return this;
    }
    /*?}*/

    public void tick(ServerLevel level) {
        if (!chunksToReload.isEmpty()) {
            for (BlockPos pos : chunksToReload) {
                //? if <26.1 {
                ChunkPos chunkPos = new ChunkPos(pos);
                //? } else {
                /*ChunkPos chunkPos = ChunkPos.containing(pos);
                *///? }
                //? if <1.21.5 {
                level.getChunkSource().addRegionTicket(TicketType.PORTAL, chunkPos, LinkartConfiguration.chunkloadingRadius, pos);
                //?} else
                //level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, chunkPos, LinkartConfiguration.chunkloadingRadius);
            }
            chunksToReload.clear();
            setDirty();
        }
    }

    public void addCart(AbstractMinecart cart) {
        cartsToBlockPos.add(cart);
        setDirty();
    }

    public void removeCart(AbstractMinecart cart) {
        cartsToBlockPos.remove(cart);
        setDirty();
    }
}
