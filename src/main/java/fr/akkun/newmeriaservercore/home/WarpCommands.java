package fr.akkun.newmeriaservercore.home;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.akkun.newmeriaservercore.NewmeriaServerCore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Warps: named teleport points shared by the whole server.
 * <ul>
 *   <li>{@code /warp <name>} (everyone): teleports to that warp. /back then leads to where the player left from.</li>
 *   <li>{@code /warp_add <name>} (admins): creates a warp where the admin stands. Refused if the name is taken.</li>
 *   <li>{@code /warp_del <name>} (admins): deletes a warp.</li>
 * </ul>
 */
@EventBusSubscriber(modid = NewmeriaServerCore.MOD_ID)
public class WarpCommands {
    private static final PermissionCheck ADMIN_PERMISSION = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);
    private static final String NAME = "name";

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("warp")
                .then(Commands.argument(NAME, StringArgumentType.word())
                        .suggests(WarpCommands::suggestWarps)
                        .executes(ctx -> warp(ctx.getSource(), StringArgumentType.getString(ctx, NAME)))));
        dispatcher.register(Commands.literal("warp_add")
                .requires(Commands.hasPermission(ADMIN_PERMISSION))
                .then(Commands.argument(NAME, StringArgumentType.word())
                        .executes(ctx -> add(ctx.getSource(), StringArgumentType.getString(ctx, NAME)))));
        dispatcher.register(Commands.literal("warp_del")
                .requires(Commands.hasPermission(ADMIN_PERMISSION))
                .then(Commands.argument(NAME, StringArgumentType.word())
                        .suggests(WarpCommands::suggestWarps)
                        .executes(ctx -> delete(ctx.getSource(), StringArgumentType.getString(ctx, NAME)))));
    }

    private static Map<String, TeleportPoint> warps(MinecraftServer server) {
        return server.overworld().getData(HomeAttachments.WARPS);
    }

    // The stored map is never modified in place: every change replaces it, which is what marks it for saving.
    private static void setWarps(MinecraftServer server, Map<String, TeleportPoint> warps) {
        server.overworld().setData(HomeAttachments.WARPS, Map.copyOf(warps));
    }

    private static CompletableFuture<Suggestions> suggestWarps(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(warps(ctx.getSource().getServer()).keySet(), builder);
    }

    private static int warp(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TeleportPoint point = warps(source.getServer()).get(name);
        if (point == null) {
            source.sendFailure(Component.translatable("commands.newmeriacore.warp.unknown", name));
            return 0;
        }
        if (!HomeCommands.teleport(source, player, point)) {
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.newmeriacore.warp.success", name), false);
        return 1;
    }

    private static int add(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Map<String, TeleportPoint> warps = new HashMap<>(warps(source.getServer()));
        if (warps.containsKey(name)) {
            source.sendFailure(Component.translatable("commands.newmeriacore.warp_add.already_exists", name));
            return 0;
        }
        warps.put(name, TeleportPoint.of(player));
        setWarps(source.getServer(), warps);
        source.sendSuccess(() -> Component.translatable("commands.newmeriacore.warp_add.success", name), true);
        return 1;
    }

    private static int delete(CommandSourceStack source, String name) {
        Map<String, TeleportPoint> warps = new HashMap<>(warps(source.getServer()));
        if (warps.remove(name) == null) {
            source.sendFailure(Component.translatable("commands.newmeriacore.warp.unknown", name));
            return 0;
        }
        setWarps(source.getServer(), warps);
        source.sendSuccess(() -> Component.translatable("commands.newmeriacore.warp_del.success", name), true);
        return 1;
    }
}
