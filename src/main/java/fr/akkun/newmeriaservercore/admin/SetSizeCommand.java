package fr.akkun.newmeriaservercore.admin;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.akkun.newmeriaservercore.NewmeriaServerCore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

/**
 * {@code /setsize <player> <size>} (admins): the size of a player, as a multiplier of the normal one - 1 is
 * normal (about 2 blocks tall), 0.5 half (about 1 block), 2 double... Same as vanilla's
 * {@code /attribute <player> minecraft:scale base set <size>}, so it is saved with the player and the
 * accepted sizes are the attribute's: from 0.0625 to 16. {@code <player>} can be a selector ({@code @a}...).
 */
@EventBusSubscriber(modid = NewmeriaServerCore.MOD_ID)
public class SetSizeCommand {
    private static final PermissionCheck ADMIN_PERMISSION = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);
    private static final double MIN_SIZE = 0.0625;
    private static final double MAX_SIZE = 16.0;

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("setsize")
                .requires(Commands.hasPermission(ADMIN_PERMISSION))
                .then(Commands.argument("player", EntityArgument.players())
                        .then(Commands.argument("size", DoubleArgumentType.doubleArg(MIN_SIZE, MAX_SIZE))
                                .executes(ctx -> setSize(ctx.getSource(),
                                        EntityArgument.getPlayers(ctx, "player"),
                                        DoubleArgumentType.getDouble(ctx, "size"))))));
    }

    private static int setSize(CommandSourceStack source, Collection<ServerPlayer> players, double size) throws CommandSyntaxException {
        int changed = 0;
        for (ServerPlayer player : players) {
            AttributeInstance scale = player.getAttribute(Attributes.SCALE);
            if (scale != null) {
                scale.setBaseValue(size);
                changed++;
            }
        }
        if (changed == 1) {
            Component name = players.iterator().next().getDisplayName();
            source.sendSuccess(() -> Component.translatableWithFallback("commands.newmeriaservercore.setsize.one",
                    "Taille de %s définie sur %s", name, size), true);
        } else {
            int count = changed;
            source.sendSuccess(() -> Component.translatableWithFallback("commands.newmeriaservercore.setsize.many",
                    "Taille de %s joueurs définie sur %s", count, size), true);
        }
        return changed;
    }
}
