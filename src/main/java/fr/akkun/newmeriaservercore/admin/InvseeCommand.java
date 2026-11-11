package fr.akkun.newmeriaservercore.admin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.akkun.newmeriaservercore.NewmeriaServerCore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /invsee <player>} (admins): opens the inventory of an online player, armor and off hand included,
 * to see and change it live. See {@link InvseeMenu}.
 */
@EventBusSubscriber(modid = NewmeriaServerCore.MOD_ID)
public class InvseeCommand {
    private static final PermissionCheck ADMIN_PERMISSION = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("invsee")
                .requires(Commands.hasPermission(ADMIN_PERMISSION))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> open(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));
    }

    private static int open(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer admin = source.getPlayerOrException();
        Component title = Component.translatableWithFallback("container.newmeriaservercore.invsee", "Inventaire de %s", target.getDisplayName());
        admin.openMenu(new SimpleMenuProvider((containerId, adminInventory, player) -> new InvseeMenu(containerId, adminInventory, target), title));
        return 1;
    }
}
