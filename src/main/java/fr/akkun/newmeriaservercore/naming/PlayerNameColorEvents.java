package fr.akkun.newmeriaservercore.naming;

import fr.akkun.newmeriaservercore.NewmeriaServerCore;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Colors the name of a player in chat ({@code NameFormat}) and in the tab list ({@code
 * TabListNameFormat}) according to {@link PlayerColors}, if they have a color set. In the tab list
 * the name is also followed by how many times the player has died (their vanilla "deaths"
 * statistic, so it counts every death since they first joined the world).
 */
@EventBusSubscriber(modid = NewmeriaServerCore.MOD_ID)
public class PlayerNameColorEvents {
    private static final int DEATH_COUNT_REFRESH_INTERVAL_TICKS = 20;
    // Death count currently shown in the tab list for each online player.
    private static final Map<UUID, Integer> SHOWN_DEATHS = new HashMap<>();

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ChatFormatting color = PlayerColors.get(player.level().getServer(), player.getGameProfile().name());
        if (color != null) {
            event.setDisplayname(event.getUsername().copy().withStyle(color));
        }
    }

    @SubscribeEvent
    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MutableComponent name = Component.literal(player.getGameProfile().name());
        ChatFormatting color = PlayerColors.get(player.level().getServer(), player.getGameProfile().name());
        if (color != null) {
            name.withStyle(color);
        }
        int deaths = deaths(player);
        SHOWN_DEATHS.put(player.getUUID(), deaths);
        event.setDisplayName(Component.empty().append(name)
                .append(Component.literal("  ☠ " + deaths).withStyle(ChatFormatting.GRAY)));
    }

    private static int deaths(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
    }

    /** The statistic only goes up partway through the death sequence, with no event of its own:
     *  once a second, any player whose shown count is out of date gets their tab entry rebuilt. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % DEATH_COUNT_REFRESH_INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            Integer shown = SHOWN_DEATHS.get(player.getUUID());
            if (shown == null || shown != deaths(player)) {
                player.refreshTabListName();
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SHOWN_DEATHS.remove(event.getEntity().getUUID());
    }
}
