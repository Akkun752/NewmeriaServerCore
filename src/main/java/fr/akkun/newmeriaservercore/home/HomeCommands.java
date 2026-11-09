package fr.akkun.newmeriaservercore.home;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.akkun.newmeriaservercore.NewmeriaServerCore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Optional;
import java.util.Set;

/**
 * Commands open to every player, none of them taking arguments:
 * <ul>
 *   <li>{@code /sethome}: sets the home of the player where they stand (one per player - refused while they still have one).</li>
 *   <li>{@code /home}: teleports them to it.</li>
 *   <li>{@code /delhome}: forgets it.</li>
 *   <li>{@code /back}: teleports them to where they were before their last /home or /back, or to where they last died.</li>
 * </ul>
 */
@EventBusSubscriber(modid = NewmeriaServerCore.MOD_ID)
public class HomeCommands {
    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("sethome").executes(ctx -> setHome(ctx.getSource())));
        dispatcher.register(Commands.literal("home").executes(ctx -> home(ctx.getSource())));
        dispatcher.register(Commands.literal("delhome").executes(ctx -> delHome(ctx.getSource())));
        dispatcher.register(Commands.literal("back").executes(ctx -> back(ctx.getSource())));
    }

    /** Dying counts as leaving a place: /back then leads to where it happened. */
    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            rememberForBack(player);
        }
    }

    private static HomeData data(ServerPlayer player) {
        return player.getData(HomeAttachments.HOME_DATA);
    }

    private static void rememberForBack(ServerPlayer player) {
        player.setData(HomeAttachments.HOME_DATA, data(player).withBack(TeleportPoint.of(player)));
    }

    private static int setHome(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (data(player).home().isPresent()) {
            source.sendFailure(Component.translatable("commands.newmeriacore.sethome.already_set"));
            return 0;
        }
        player.setData(HomeAttachments.HOME_DATA, data(player).withHome(Optional.of(TeleportPoint.of(player))));
        source.sendSuccess(() -> Component.translatable("commands.newmeriacore.sethome.success"), false);
        return 1;
    }

    private static int home(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<TeleportPoint> home = data(player).home();
        if (home.isEmpty()) {
            source.sendFailure(Component.translatable("commands.newmeriacore.home.none"));
            return 0;
        }
        if (!teleport(source, player, home.get())) {
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.newmeriacore.home.success"), false);
        return 1;
    }

    private static int delHome(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (data(player).home().isEmpty()) {
            source.sendFailure(Component.translatable("commands.newmeriacore.home.none"));
            return 0;
        }
        player.setData(HomeAttachments.HOME_DATA, data(player).withHome(Optional.empty()));
        source.sendSuccess(() -> Component.translatable("commands.newmeriacore.delhome.success"), false);
        return 1;
    }

    private static int back(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<TeleportPoint> back = data(player).back();
        if (back.isEmpty()) {
            source.sendFailure(Component.translatable("commands.newmeriacore.back.none"));
            return 0;
        }
        if (!teleport(source, player, back.get())) {
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.newmeriacore.back.success"), false);
        return 1;
    }

    /** Teleports the player there, first remembering where they leave from - so /back undoes it. */
    static boolean teleport(CommandSourceStack source, ServerPlayer player, TeleportPoint point) {
        ServerLevel level = source.getServer().getLevel(point.dimension());
        if (level == null) {
            source.sendFailure(Component.translatable("commands.newmeriacore.home.missing_dimension"));
            return false;
        }
        rememberForBack(player);
        return player.teleportTo(level, point.position().x, point.position().y, point.position().z, Set.of(), point.yRot(), point.xRot(), true);
    }
}
