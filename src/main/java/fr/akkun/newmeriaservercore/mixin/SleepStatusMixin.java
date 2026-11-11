package fr.akkun.newmeriaservercore.mixin;

import fr.akkun.newmeriaservercore.admin.Vanish;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

/** Vanished players (see {@link Vanish}) are left out of the count of players needed to skip the night. */
@Mixin(SleepStatus.class)
public abstract class SleepStatusMixin {
    @ModifyVariable(method = {"update", "areEnoughDeepSleeping"}, at = @At("HEAD"), argsOnly = true)
    private List<ServerPlayer> newmeriaservercore$ignoreVanished(List<ServerPlayer> players) {
        return players.stream().filter(player -> !Vanish.isVanished(player)).toList();
    }
}
