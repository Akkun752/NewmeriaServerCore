package fr.akkun.newmeriaservercore.home;

import com.mojang.serialization.Codec;
import fr.akkun.newmeriacore.NewmeriaCore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Map;

public class HomeAttachments {
    // Registered under the "newmeriacore" namespace on purpose: these attachments used to live in NewmeriaCore,
    // and an attachment whose id is unknown is dropped when the data is loaded. Keeping the same ids
    // (newmeriacore:home_data, newmeriacore:warps) keeps the homes and warps already saved in the world.
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, NewmeriaCore.MOD_ID);

    /** Saved with the player and kept through death. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<HomeData>> HOME_DATA = ATTACHMENT_TYPES.register("home_data",
            () -> AttachmentType.builder(() -> HomeData.EMPTY)
                    .serialize(HomeData.MAP_CODEC)
                    .copyOnDeath()
                    .build());

    /** Server-wide warps by name, set by admins. Lives on the Overworld, so it is saved with the world. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Map<String, TeleportPoint>>> WARPS = ATTACHMENT_TYPES.register("warps",
            () -> AttachmentType.<Map<String, TeleportPoint>>builder(() -> Map.of())
                    .serialize(Codec.unboundedMap(Codec.STRING, TeleportPoint.CODEC).fieldOf("warps"))
                    .build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
