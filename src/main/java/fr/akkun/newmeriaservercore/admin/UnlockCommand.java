package fr.akkun.newmeriaservercore.admin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.akkun.newmeriaservercore.NewmeriaServerCore;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /unlock} (admins): unlocks every advancement of the server - vanilla, mods and datapacks - for the
 * player who runs it. Same as vanilla's {@code /advancement grant @s everything}.
 */
@EventBusSubscriber(modid = NewmeriaServerCore.MOD_ID)
public class UnlockCommand {
    private static final PermissionCheck ADMIN_PERMISSION = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("unlock")
                .requires(Commands.hasPermission(ADMIN_PERMISSION))
                .executes(ctx -> unlock(ctx.getSource())));
    }

    private static int unlock(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerAdvancements advancements = player.getAdvancements();
        int unlocked = 0;
        for (AdvancementHolder advancement : source.getServer().getAdvancements().getAllAdvancements()) {
            AdvancementProgress progress = advancements.getOrStartProgress(advancement);
            if (progress.isDone()) {
                continue;
            }
            for (String criterion : progress.getRemainingCriteria()) {
                advancements.award(advancement, criterion);
            }
            unlocked++;
        }
        int count = unlocked;
        // Vanilla's message for /advancement grant, so that the client already has its translation.
        source.sendSuccess(() -> Component.translatable("commands.advancement.grant.many.to.one.success", count, player.getDisplayName()), true);
        return count;
    }
}
