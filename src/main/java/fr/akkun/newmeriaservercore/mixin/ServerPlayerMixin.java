package fr.akkun.newmeriaservercore.mixin;

import fr.akkun.newmeriaservercore.admin.Vanish;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A vanished player (see {@link Vanish}) is never sent to the other players' clients: the server's entity
 * tracking asks this method whether a player may see an entity before telling their client about it.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "broadcastToPlayer", at = @At("HEAD"), cancellable = true)
    private void newmeriaservercore$hideWhenVanished(ServerPlayer viewer, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (viewer != self && Vanish.isVanished(self)) {
            cir.setReturnValue(false);
        }
    }
}
