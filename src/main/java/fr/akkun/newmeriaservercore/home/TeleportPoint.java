package fr.akkun.newmeriaservercore.home;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A place a player can be sent back to: dimension, exact position and where they were looking. */
public record TeleportPoint(ResourceKey<Level> dimension, Vec3 position, float yRot, float xRot) {
    public static final Codec<TeleportPoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(TeleportPoint::dimension),
            Vec3.CODEC.fieldOf("position").forGetter(TeleportPoint::position),
            Codec.FLOAT.fieldOf("y_rot").forGetter(TeleportPoint::yRot),
            Codec.FLOAT.fieldOf("x_rot").forGetter(TeleportPoint::xRot)
    ).apply(instance, TeleportPoint::new));

    public static TeleportPoint of(ServerPlayer player) {
        return new TeleportPoint(player.level().dimension(), player.position(), player.getYRot(), player.getXRot());
    }
}
