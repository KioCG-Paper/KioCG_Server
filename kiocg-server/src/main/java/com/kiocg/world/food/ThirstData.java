package com.kiocg.world.food;

import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ThirstData {
    public static final int MAX_THIRST = 20;
    public static final int HEAL_LEVEL = 19; // 19和20都显示完整的气泡
    public static final float DEHYDRATION_DROP = 4.0F;
    public static final int AIR_OFFSET_TICK = 5;

    private int thirstLevel = MAX_THIRST;
    private float dehydrationLevel;
    // 在水下偏移氧气条数据包
    private boolean underWater;

    public int getThirstLevel() {
        return thirstLevel;
    }

    public boolean hasEnoughWater() {
        return this.getThirstLevel() > 6.0F;
    }

    public boolean needsWater() {
        return thirstLevel < MAX_THIRST;
    }

    public void setThirstLevel(int value) {
        thirstLevel = value;
    }

    public void addThirstLevel(int add) {
        int level = thirstLevel + add;
        if (level > MAX_THIRST) {
            level = MAX_THIRST;
            dehydrationLevel = 0;
        }
        thirstLevel = Math.max(level, 0);
    }

    public float getDehydrationLevel() {
        return dehydrationLevel;
    }

    public void setDehydrationLevel(float value) {
        dehydrationLevel = value;
    }

    public void addDehydrationLevel(float add) {
        dehydrationLevel = Math.min(dehydrationLevel + add, 40.0F);
    }

    // 10: 273-299
    // 9: 243-270
    // 8: 213-240
    // ...
    // 2: 33-60
    // 1: 3-30
    // 0: <=0
    private int getAirSupplyDisplay() {
        int clientLevel = Mth.ceil(thirstLevel / 2.0);
        if (!underWater) {
            return 30 * (clientLevel - 1) + (3 + AIR_OFFSET_TICK);
        } else {
            return 30 * clientLevel - (1 + AIR_OFFSET_TICK);
        }
    }

    public boolean isThirsty(ServerPlayer player) {
        return thirstLevel <= 0 && !player.getAbilities().invulnerable;
    }

    public void setUnderWater(boolean underWater) {
        this.underWater = underWater;
    }

    public void tick(ServerPlayer player) {
        if (!player.getAbilities().invulnerable) {
            this.causeThirstDehydration(player, (float) player.level().paperConfig().kiocg.thirst.naturalDehydration);

            if (dehydrationLevel > DEHYDRATION_DROP) {
                dehydrationLevel -= DEHYDRATION_DROP;
                if (player.level().getDifficulty() != Difficulty.PEACEFUL) {
                    addThirstLevel(-1);
                }
            }

            if (!this.hasEnoughWater() && player.tickCount % 20 == 0) {
                if (!player.hasEffect(MobEffects.WEAKNESS) || player.getEffect(MobEffects.WEAKNESS).getAmplifier() == 0 && player.getEffect(MobEffects.WEAKNESS).endsWithin(11 * 20 - 1)) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 11 * 20, 0, true, true));
                }
            }

            player.getEntityData().set(Entity.DATA_AIR_SUPPLY_ID, getAirSupplyDisplay(), true);
        }
    }

    public void readAdditionalSaveData(ValueInput input) {
        thirstLevel = input.getIntOr("KioCG.ThirstLevel", MAX_THIRST);
        dehydrationLevel = input.getFloatOr("KioCG.ThirstDehydrationLevel", 0.0F);
    }

    public void addAdditionalSaveData(ValueOutput output) {
        output.putInt("KioCG.ThirstLevel", thirstLevel);
        output.putFloat("KioCG.ThirstDehydrationLevel", dehydrationLevel);
    }

    // 基准温度=温度-0.5
    // 基准湿度=湿度-0.5
    // 口渴倍率=1+ABS(基准温度)*0.7+(基准温度+基准湿度)*0.3
    public void causeThirstDehydration(final ServerPlayer player, final float amount) {
        Level level = player.level();
        Biome biome = level.getNoiseBiome(QuartPos.fromBlock(player.getBlockX()), QuartPos.fromBlock(player.getBlockY()), QuartPos.fromBlock(player.getBlockZ())).value();
        float temperature = biome.getTemperature(player.blockPosition(), level.getSeaLevel()) - 0.5F;
        float humidity = biome.climateSettings.downfall() - 0.5F;
        float multiplier = 1.0F + Math.abs(temperature) * 0.7F + (temperature + humidity) * 0.3F;
        this.addDehydrationLevel(amount * multiplier);
    }
}
