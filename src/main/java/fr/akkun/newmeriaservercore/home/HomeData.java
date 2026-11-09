package fr.akkun.newmeriaservercore.home;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * Per-player data behind /sethome, /home, /delhome and /back.
 *
 * @param home the single home of the player, if they have set one
 * @param back where /back leads: where the player was right before their last /home or /back, or
 *             where they last died
 */
public record HomeData(Optional<TeleportPoint> home, Optional<TeleportPoint> back) {
    public static final HomeData EMPTY = new HomeData(Optional.empty(), Optional.empty());

    public static final MapCodec<HomeData> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TeleportPoint.CODEC.optionalFieldOf("home").forGetter(HomeData::home),
            TeleportPoint.CODEC.optionalFieldOf("back").forGetter(HomeData::back)
    ).apply(instance, HomeData::new));

    public HomeData withHome(Optional<TeleportPoint> home) {
        return new HomeData(home, this.back);
    }

    public HomeData withBack(TeleportPoint back) {
        return new HomeData(this.home, Optional.of(back));
    }
}
