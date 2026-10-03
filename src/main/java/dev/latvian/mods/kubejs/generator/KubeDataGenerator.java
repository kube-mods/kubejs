package dev.latvian.mods.kubejs.generator;

import dev.latvian.mods.kubejs.script.data.VirtualDataMapFile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.builtin.BiomeVillagerType;
import net.neoforged.neoforge.registries.datamaps.builtin.MonsterRoomMob;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.registries.datamaps.builtin.Oxidizable;
import net.neoforged.neoforge.registries.datamaps.builtin.ParrotImitation;
import net.neoforged.neoforge.registries.datamaps.builtin.RaidHeroGift;
import net.neoforged.neoforge.registries.datamaps.builtin.VibrationFrequency;
import net.neoforged.neoforge.registries.datamaps.builtin.Waxable;

import java.util.function.Consumer;

public interface KubeDataGenerator extends KubeResourceGenerator {
	<R, T> void dataMap(DataMapType<R, T> type, Consumer<VirtualDataMapFile<R, T>> consumer);

	default void setMonsterRoomMobs(EntityType<?> entityType, int weight) {
		dataMap(NeoForgeDataMaps.MONSTER_ROOM_MOBS, callback -> callback.accept(entityType.kjs$getIdLocation(), new MonsterRoomMob(weight)));
	}

	default void setOxidizable(Block from, Block to) {
		if (from != to) {
			dataMap(NeoForgeDataMaps.OXIDIZABLES, callback -> callback.accept(from.kjs$getIdLocation(), new Oxidizable(to)));
		}
	}

	default void setParrotImitation(EntityType<?> type, SoundEvent sound) {
		dataMap(NeoForgeDataMaps.PARROT_IMITATIONS, callback -> callback.accept(type.kjs$getIdLocation(), new ParrotImitation(sound)));
	}

	default void setRaidHeroGifts(VillagerProfession profession, ResourceKey<LootTable> lootTable) {
		dataMap(NeoForgeDataMaps.RAID_HERO_GIFTS, callback -> callback.accept(BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession), new RaidHeroGift(lootTable)));
	}

	default void setVibrationFrequency(GameEvent gameEvent, int frequency) {
		dataMap(NeoForgeDataMaps.VIBRATION_FREQUENCIES, callback -> callback.accept(BuiltInRegistries.GAME_EVENT.getKey(gameEvent), new VibrationFrequency(frequency)));
	}

	default void setVillagerType(ResourceKey<Biome> biome, VillagerType villagerType) {
		ResourceKey<VillagerType> resourceKey = ResourceKey.create(Registries.VILLAGER_TYPE, BuiltInRegistries.VILLAGER_TYPE.getKey(villagerType));
		dataMap(NeoForgeDataMaps.VILLAGER_TYPES, callback -> callback.accept(biome.identifier(), new BiomeVillagerType(resourceKey)));
	}

	default void setWaxable(Block from, Block to) {
		if (from != to) {
			dataMap(NeoForgeDataMaps.WAXABLES, callback -> callback.accept(from.kjs$getIdLocation(), new Waxable(to)));
		}
	}
}
