package org.quipumetrics.mod;

import lombok.experimental.UtilityClass;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Identifies the mod loader and the Minecraft version through reflection.
 * <p>
 * Reflection is what keeps this module to a single artifact. Compiling against a
 * loader would pin the module to one Minecraft version, because a mod's classes
 * are remapped per version, and that is what forces other projects into a build
 * matrix. Nothing here touches a Minecraft class, so one Java 8 jar serves every
 * loader and every version.
 */
@UtilityClass
public class LoaderInfo {

    private final String MINECRAFT = "minecraft";

    private final String FABRIC_LOADER = "net.fabricmc.loader.api.FabricLoader";
    private final String NEOFORGE_MOD_LIST = "net.neoforged.fml.ModList";
    private final String FORGE_MOD_LIST = "net.minecraftforge.fml.ModList";
    private final String FORGE_LEGACY_LOADER = "net.minecraftforge.fml.common.Loader";

    /**
     * @return a platform key from section 9 of the protocol specification.
     */
    public String platform() {
        if (present(FABRIC_LOADER)) {
            // Quilt ships the Fabric loader API, so it reports as fabric by design.
            return "fabric";
        }
        if (present(NEOFORGE_MOD_LIST)) {
            return "neoforge";
        }
        // NeoForge 1.20.1 kept the net.minecraftforge packages, so it is reported
        // as forge. Known and accepted: the check above catches every later version.
        if (present(FORGE_MOD_LIST) || present(FORGE_LEGACY_LOADER)) {
            return "forge";
        }
        return "other";
    }

    /**
     * @return the raw Minecraft version, or "unknown". The backend parses it, and
     * treats an unrecognised value as Unknown rather than dropping the submission.
     */
    public String minecraftVersion() {
        String version = fabricVersion();
        if (version == null) {
            version = modListVersion(NEOFORGE_MOD_LIST);
        }
        if (version == null) {
            version = modListVersion(FORGE_MOD_LIST);
        }
        return version == null ? "unknown" : version;
    }

    private String fabricVersion() {
        try {
            Class<?> loader = Class.forName(FABRIC_LOADER);
            Object instance = loader.getMethod("getInstance").invoke(null);
            Object container = unwrap(loader.getMethod("getModContainer", String.class)
                    .invoke(instance, MINECRAFT));
            if (container == null) {
                return null;
            }
            Object version = call(call(container, "getMetadata"), "getVersion");
            try {
                return (String) call(version, "getFriendlyString");
            } catch (Exception olderLoader) {
                return version.toString();
            }
        } catch (Exception notFabric) {
            return null;
        }
    }

    private String modListVersion(String modListClass) {
        try {
            Class<?> modList = Class.forName(modListClass);
            Method lookup = modList.getMethod("getModContainerById", String.class);
            Object container;
            try {
                // ModList lost its singleton in 26.1: the lookup became static.
                container = unwrap(lookup.invoke(null, MINECRAFT));
            } catch (Exception stillAnInstanceMethod) {
                container = unwrap(lookup.invoke(modList.getMethod("get").invoke(null), MINECRAFT));
            }
            if (container == null) {
                return null;
            }
            return call(call(container, "getModInfo"), "getVersion").toString();
        } catch (Exception notThisLoader) {
            return null;
        }
    }

    private Object unwrap(Object optional) {
        return optional instanceof Optional ? ((Optional<?>) optional).orElse(null) : optional;
    }

    /**
     * Invokes a no-argument method declared anywhere in the object's hierarchy.
     * Loader APIs hand back implementation classes that are not public, so the
     * method has to be found on the interface that declares it.
     */
    private Object call(Object target, String name) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            Method method = declared(type, name);
            if (method == null) {
                for (Class<?> parent : type.getInterfaces()) {
                    method = declared(parent, name);
                    if (method != null) {
                        break;
                    }
                }
            }
            if (method != null) {
                method.setAccessible(true);
                return method.invoke(target);
            }
        }
        throw new NoSuchMethodException(name);
    }

    private Method declared(Class<?> type, String name) {
        try {
            return type.getDeclaredMethod(name);
        } catch (NoSuchMethodException absent) {
            return null;
        }
    }

    private boolean present(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Throwable absent) {
            return false;
        }
    }
}
