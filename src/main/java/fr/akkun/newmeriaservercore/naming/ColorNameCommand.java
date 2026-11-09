package fr.akkun.newmeriaservercore.naming;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.akkun.newmeriaservercore.NewmeriaServerCore;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/** {@code /colorname <player> <color>}: sets (or overwrites) the persistent chat/tab name color
 *  for a player, by name - works even if that player has never joined before. See {@link
 *  PlayerColors} for storage and {@link PlayerNameColorEvents} for where it's actually applied. */
@EventBusSubscriber(modid = NewmeriaServerCore.MOD_ID)
public class ColorNameCommand {
    private static final PermissionCheck ADMIN_PERMISSION = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    private static final List<ChatFormatting> COLORS = List.of(
            ChatFormatting.BLACK, ChatFormatting.DARK_BLUE, ChatFormatting.DARK_GREEN, ChatFormatting.DARK_AQUA,
            ChatFormatting.DARK_RED, ChatFormatting.DARK_PURPLE, ChatFormatting.GOLD, ChatFormatting.GRAY,
            ChatFormatting.DARK_GRAY, ChatFormatting.BLUE, ChatFormatting.GREEN, ChatFormatting.AQUA,
            ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE, ChatFormatting.YELLOW, ChatFormatting.WHITE
    );

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("colorname")
                .requires(Commands.hasPermission(ADMIN_PERMISSION))
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("color", StringArgumentType.word())
                                .suggests(ColorNameCommand::suggestColors)
                                .executes(ctx -> setColor(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"),
                                        StringArgumentType.getString(ctx, "color"))))));
    }

    private static CompletableFuture<Suggestions> suggestColors(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        for (ChatFormatting color : COLORS) {
            builder.suggest(color.name().toLowerCase(Locale.ROOT));
        }
        return builder.buildFuture();
    }

    private static int setColor(CommandSourceStack source, String playerName, String colorName) {
        ChatFormatting color = COLORS.stream()
                .filter(c -> c.name().equalsIgnoreCase(colorName))
                .findFirst()
                .orElse(null);
        if (color == null) {
            source.sendFailure(Component.translatable("commands.newmeriacore.colorname.invalid_color", colorName));
            return 0;
        }

        PlayerColors.set(source.getServer(), playerName, color);

        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(playerName);
        if (online != null) {
            online.refreshDisplayName();
            online.refreshTabListName();
        }

        source.sendSuccess(() -> Component.translatable("commands.newmeriacore.colorname.success", playerName, colorName), true);
        return 1;
    }
}
