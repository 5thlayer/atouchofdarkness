// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.lampad;

import java.util.List;

import io.github._5thlayer.atouchofdarkness.light.LightConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Lampad (GLOSSARY.md): she carries fuel from her Votive into the placed Lamps it tends, one whole
 * item at a time, while her binding holds. The binding is counted in ticks while she is loaded, so she
 * does nothing while her chunk is unloaded and catches nothing up after. Once it has faded she takes
 * the next Nightbrand from her Votive, which renews it to the light config's binding. Each binding she
 * takes a gold nugget from her Votive for each so many Lamps in range, rounded up, at its start and again
 * whenever more Lamps come into range than it has paid for, and tends only as many Lamps as its gold paid
 * for, nearest the Votive first. With no Nightbrand there, or no Votive, she stands dormant, a statue,
 * until one comes. She holds the Nightbrand of her binding and the Lamp she was summoned from, and drops
 * both, with the fuel she carries, when she dies; the gold is lost. Dormant, she is a statue that a player's
 * blow breaks, as if she had died. Mobs treat her as a passive mob. Hurt, she heals in the Dark by her
 * {@link HealGoal} and tends nothing until she is whole, her binding fading all the while. She gives no
 * light, and the Babau strikes only players.
 */
public final class LampadEntity extends PathfinderMob {

    private static final EntityDataAccessor<Boolean> DORMANT = SynchedEntityData.defineId(LampadEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HEALING = SynchedEntityData.defineId(LampadEntity.class, EntityDataSerializers.BOOLEAN);

    private static final String VOTIVE = "votive";
    private static final String BINDING = "binding";
    private static final String NUGGETS = "nuggets";

    /** Ticks between her looks for Lamps come into range that her gold hasn't paid for yet. */
    private static final int CHARGE_INTERVAL = 20;
    private static final String LAMP = "lamp";
    private static final String NIGHTBRAND = "nightbrand";

    private @Nullable BlockPos votive;

    /** Ticks of her binding left; none while dormant. */
    private int binding;

    /** The gold nuggets this binding has taken. */
    private int nuggets;

    private ItemStack lamp = ItemStack.EMPTY;
    private ItemStack nightbrand = ItemStack.EMPTY;

    public LampadEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        getNavigation().setRequiredPathLength(48F);
        setGuaranteedDrop(EquipmentSlot.MAINHAND);
        // Hurt, she would stop there to heal, so she never steps into a fire, such as a lit Campfire.
        setPathfindingMalus(PathType.FIRE, -1F);
    }

    /** The copper golem's health and pace. */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DORMANT, true);
        builder.define(HEALING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new HealGoal(this));
        goalSelector.addGoal(1, new TendLampsGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** Summoned at this Votive, from this Lamp, with no binding yet. */
    void summonedAt(BlockPos votive, ItemStack lamp) {
        this.votive = votive.immutable();
        this.lamp = lamp;
        setHomeTo(votive, TendedLamps.ACROSS);
    }

    /** Standing as a statue, her binding faded or with no Votive, until a Nightbrand renews it. */
    public boolean isDormant() {
        return entityData.get(DORMANT);
    }

    private void setDormant(boolean dormant) {
        if (dormant != isDormant()) {
            entityData.set(DORMANT, dormant);
            if (dormant) {
                getNavigation().stop();
                playSound(SoundEvents.COPPER_GOLEM_BECOME_STATUE);
            } else {
                playSound(SoundEvents.COPPER_GOLEM_SPAWN);
            }
        }
    }

    /** Short of her full health, so healing and tending nothing. */
    public boolean isHurt() {
        return getHealth() < getMaxHealth();
    }

    /** Healing in the Dark, as she is hurt. */
    public boolean isHealing() {
        return entityData.get(HEALING);
    }

    void setHealing(boolean healing) {
        entityData.set(HEALING, healing);
    }

    @Nullable BlockPos votive() {
        return votive;
    }

    /** Her Votive, loaded and still hers, or null. */
    @Nullable VotiveBlockEntity votiveEntity() {
        return votive != null && level().isLoaded(votive) && level().getBlockEntity(votive) instanceof VotiveBlockEntity entity
                && entity.isBoundTo(getUUID()) ? entity : null;
    }

    /** The Lamps she tends: those her Votive tends that this binding's gold paid for, nearest it first. */
    List<BlockPos> tended() {
        if (votive == null || !(level() instanceof ServerLevel server)) {
            return List.of();
        }
        var lamps = TendedLamps.of(server, votive);
        return lamps.subList(0, Math.min(Gold.paidFor(nuggets, LightConfig.LAMPAD_LAMPS_PER_NUGGET.getAsInt()), lamps.size()));
    }

    /** Takes from her Votive the nuggets owed for more Lamps in range than this binding has paid for, as far as it has them. */
    private void charge(ServerLevel level, VotiveBlockEntity votiveEntity) {
        int owed = Gold.owed(TendedLamps.of(level, votive).size(), LightConfig.LAMPAD_LAMPS_PER_NUGGET.getAsInt()) - nuggets;
        int taken = owed > 0 ? votiveEntity.takeGold(owed) : 0;
        if (taken > 0) {
            nuggets += taken;
            goldTakenCues(level);
        }
    }

    /** A clink of gold, and golden sparks over her Votive. */
    private void goldTakenCues(ServerLevel level) {
        level.playSound(null, votive, SoundEvents.ARMOR_EQUIP_GOLD.value(), getSoundSource(), 1F, 1.2F);
        level.sendParticles(ParticleTypes.WAX_ON, votive.getX() + 0.5, votive.getY() + 1.1, votive.getZ() + 0.5, 10, 0.3, 0.15, 0.3, 0.5);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server && isAlive()) {
            bind(server);
        }
    }

    /** Counts her binding down, and renews it from her Votive once it has faded. */
    private void bind(ServerLevel level) {
        if (votive == null) {
            setDormant(true);
            return;
        }
        // Her Votive's chunk unloaded: she waits as she is.
        if (!level.isLoaded(votive)) {
            return;
        }
        var votiveEntity = votiveEntity();
        if (votiveEntity == null) {
            // Broken: she stands dormant for good.
            votive = null;
            binding = 0;
            clearHome();
            setDormant(true);
            return;
        }
        if (binding > 0) {
            binding--;
        }
        if (binding == 0) {
            renew(level, votiveEntity);
        } else if (tickCount % CHARGE_INTERVAL == 0) {
            charge(level, votiveEntity);
        }
    }

    /** The next Nightbrand, and the gold its Lamps owe. */
    private void renew(ServerLevel level, VotiveBlockEntity votiveEntity) {
        nuggets = 0;
        var next = votiveEntity.takeNightbrand();
        if (next.isEmpty()) {
            setDormant(true);
            return;
        }
        nightbrand = next;
        binding = LightConfig.LAMPAD_BINDING.getAsInt();
        charge(level, votiveEntity);
        setDormant(false);
    }

    /** A statue stands still, and isn't pushed. */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isDormant();
    }

    @Override
    public boolean isPushable() {
        return super.isPushable() && !isDormant();
    }

    /** A statue breaks at a player's blow. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return super.hurtServer(level, source, isDormant() && source.getEntity() instanceof Player ? Float.MAX_VALUE : damage);
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return isDormant() ? SoundEvents.COPPER_GOLEM_STATUE_BREAK : super.getDeathSound();
    }

    /** The Lamp she was summoned from and the Nightbrand of her binding, besides the fuel she carries. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        for (var held : List.of(lamp, nightbrand)) {
            if (!held.isEmpty()) {
                spawnAtLocation(level, held);
            }
        }
        lamp = ItemStack.EMPTY;
        nightbrand = ItemStack.EMPTY;
    }

    /** Dead, her Votive has no Lampad, and summons one again. */
    @Override
    public void die(DamageSource source) {
        super.die(source);
        releaseVotive();
    }

    /** The same once she is gone for good some other way, such as {@code /kill}. */
    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy()) {
            releaseVotive();
        }
        super.remove(reason);
    }

    private void releaseVotive() {
        var votiveEntity = level().isClientSide() ? null : votiveEntity();
        if (votiveEntity != null) {
            votiveEntity.release(getUUID());
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable(VOTIVE, BlockPos.CODEC, votive);
        output.putInt(BINDING, binding);
        output.putInt(NUGGETS, nuggets);
        if (!lamp.isEmpty()) {
            output.store(LAMP, ItemStack.CODEC, lamp);
        }
        if (!nightbrand.isEmpty()) {
            output.store(NIGHTBRAND, ItemStack.CODEC, nightbrand);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        votive = input.read(VOTIVE, BlockPos.CODEC).orElse(null);
        binding = input.getIntOr(BINDING, 0);
        nuggets = input.getIntOr(NUGGETS, 0);
        lamp = input.read(LAMP, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        nightbrand = input.read(NIGHTBRAND, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        entityData.set(DORMANT, binding == 0);
        if (votive != null) {
            setHomeTo(votive, TendedLamps.ACROSS);
        }
    }
}
