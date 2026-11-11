package fr.akkun.newmeriaservercore;

import com.mojang.logging.LogUtils;
import fr.akkun.newmeriaservercore.admin.Vanish;
import fr.akkun.newmeriaservercore.home.HomeAttachments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Server-side features of Newmeria: homes, warps, /back and player name colors. Only installed on the
 * server - players only need NewmeriaCore.
 *
 * <p>The messages of the commands are translation keys ({@code commands.newmeriacore.*}) that live in the
 * language files of NewmeriaCore, since it is the client that translates them.
 */
// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(NewmeriaServerCore.MOD_ID)
public class NewmeriaServerCore {
    public static final String MOD_ID = "newmeriaservercore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NewmeriaServerCore(IEventBus modEventBus, ModContainer modContainer) {
        HomeAttachments.register(modEventBus);
        Vanish.register(modEventBus);
    }
}
