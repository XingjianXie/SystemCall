package com.mark07.SystemCall.entity.decoration;


import com.mark07.SystemCall.mixin.ArmorStandInvoker;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class SystemCallHologramEntity extends ArmorStand {

    public SystemCallHologramEntity(Level level, double x, double y, double z) {
        super(level, x, y, z);
        super.setInvisible(true);
        super.setCustomNameVisible(true);
        super.setNoGravity(true);
        super.setInvulnerable(true);
        ((ArmorStandInvoker)this).invokeSetSmall(true);
        ((ArmorStandInvoker)this).invokeSetShowArms(false);
        ((ArmorStandInvoker)this).invokeSetNoBasePlate(true);
        ((ArmorStandInvoker)this).invokeSetMarker(true);
        setBoundingBox(new AABB(0, 0, 0, 0, 0, 0));
    }

    @Override
    public void kill() {

    }
}