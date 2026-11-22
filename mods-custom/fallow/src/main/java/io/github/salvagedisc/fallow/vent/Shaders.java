package io.github.salvagedisc.fallow.vent;

import io.github.salvagedisc.fallow.ModIds;

/** Un pack de shaders tourne-t-il ? Iris n'est lu que s'il est installé. */
final class Shaders {
    private static final boolean IRIS = ModIds.isLoaded(ModIds.IRIS);

    private Shaders() {
    }

    static boolean inUse() {
        return IRIS && IrisHolder.inUse();
    }

    /** Iris n'est chargé qu'ici : sans lui, la classe n'est jamais lue. */
    private static final class IrisHolder {
        static boolean inUse() {
            return net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse();
        }
    }
}
