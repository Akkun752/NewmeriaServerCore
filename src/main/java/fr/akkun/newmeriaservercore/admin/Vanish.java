package fr.akkun.newmeriaservercore.admin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import fr.akkun.newmeriaservercore.NewmeriaServerCore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

/**
 * {@code /vanish} (admins): makes the player who runs it fully invisible, or visible again if they already
 * were. While vanished:
 * <ul>
 *   <li>other players' clients are never sent the player at all - so no skin, armor, held items, arrows
 *       stuck in them, particles, shadow, name tag... (see {@code ServerPlayerMixin});</li>
 *   <li>they are not in the tab list of players who are not admins (admins still see them there);</li>
 *   <li>they are not on the other players' locator bar (the dots showing where the other players are);</li>
 *   <li>they do not count for skipping the night: the others do not need them to sleep (see
 *       {@code SleepStatusMixin}).</li>
 * </ul>
 * It is saved with the player: logging out and back in keeps them vanished.
 */
@EventBusSubscriber(modid = NewmeriaServerCore.MOD_ID)
public class Vanish {
    private static final PermissionCheck ADMIN_PERMISSION = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, NewmeriaServerCore.MOD_ID);

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> VANISHED = ATTACHMENT_TYPES.register("vanished",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL.fieldOf("vanished"))
                    .copyOnDeath()
                    .build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }

    public static boolean isVanished(ServerPlayer player) {
        return player.getData(VANISHED);
    }

    private static boolean isAdmin(ServerPlayer player) {
        return ADMIN_PERMISSION.check(player.permissions());
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("vanish")
                .requires(Commands.hasPermission(ADMIN_PERMISSION))
                .executes(ctx -> toggle(ctx.getSource())));
    }

    private static int toggle(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean vanished = !isVanished(player);
        player.setData(VANISHED, vanished);

        // Makes every other player's client forget the player, or meet them again (see ServerPlayerMixin).
        player.level().getChunkSource().chunkMap.move(player);

        for (ServerPlayer other : source.getServer().getPlayerList().getPlayers()) {
            if (other != player && !isAdmin(other)) {
                other.connection.send(vanished
                        ? new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID()))
                        : ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player)));
            }
        }

        source.sendSuccess(() -> vanished
                ? Component.translatableWithFallback("commands.newmeriaservercore.vanish.on", "Vous êtes maintenant invisible.")
                : Component.translatableWithFallback("commands.newmeriaservercore.vanish.off", "Vous êtes de nouveau visible."), true);
        return 1;
    }

    /**
     * Brings the range at which the player shows on the others' locator bar down to 0, like vanilla does
     * while sneaking: the game then stops sending them as a waypoint. The modifier is not saved, so it is
     * set again every tick - it also comes back by itself after a respawn, a dimension change or a login.
     */
    private static final AttributeModifier HIDE_FROM_LOCATOR_BAR = new AttributeModifier(
            Identifier.fromNamespaceAndPath(NewmeriaServerCore.MOD_ID, "vanish_hide_from_locator_bar"),
            -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        AttributeInstance transmitRange = player.getAttribute(Attributes.WAYPOINT_TRANSMIT_RANGE);
        if (transmitRange == null) {
            return;
        }
        if (isVanished(player)) {
            transmitRange.addOrUpdateTransientModifier(HIDE_FROM_LOCATOR_BAR);
        } else {
            transmitRange.removeModifier(HIDE_FROM_LOCATOR_BAR);
        }
    }

    /** Joining sends everyone's tab list entries both ways: the vanished ones are taken back out. */
    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joining)) {
            return;
        }
        List<ServerPlayer> players = joining.level().getServer().getPlayerList().getPlayers();
        if (!isAdmin(joining)) {
            List<java.util.UUID> vanished = players.stream()
                    .filter(other -> other != joining && isVanished(other))
                    .map(ServerPlayer::getUUID)
                    .toList();
            if (!vanished.isEmpty()) {
                joining.connection.send(new ClientboundPlayerInfoRemovePacket(vanished));
            }
        }
        if (isVanished(joining)) {
            ClientboundPlayerInfoRemovePacket remove = new ClientboundPlayerInfoRemovePacket(List.of(joining.getUUID()));
            for (ServerPlayer other : players) {
                if (other != joining && !isAdmin(other)) {
                    other.connection.send(remove);
                }
            }
        }
    }
}
