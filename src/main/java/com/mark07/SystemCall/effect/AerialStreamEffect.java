package com.mark07.SystemCall.effect;

import com.mark07.SystemCall.mixin.EntityInvoker;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class AerialStreamEffect extends MobEffect {

    public AerialStreamEffect() { super(MobEffectCategory.BENEFICIAL, 0xFFE16A); }

    private int t = 20;

    @Override public boolean isDurationEffectTick(int i, int j) { return true; }

    @Override
    public void applyEffectTick(LivingEntity livingEntity, int i) {
        Vec3 toward = new Vec3(
                -Mth.sin(livingEntity.getYRot() * 0.017453292F),
                -Mth.sin(livingEntity.getXRot() * 0.017453292F),
                Mth.cos(livingEntity.getYRot() * 0.017453292F)
        );
        Vec3 vec3 = livingEntity.getDeltaMovement().add(toward);
        double proj = vec3.dot(toward) / toward.length();
        vec3 = vec3.scale(1.3 / proj);

        if (livingEntity.isFallFlying()) {
            livingEntity.setDeltaMovement(vec3);
        }

    }
}
