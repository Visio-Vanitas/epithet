package top.atdove.epithet.platform;

import top.atdove.epithet.Epithet;
import top.atdove.epithet.platform.services.IConfigHelper;
import top.atdove.epithet.platform.services.INetworkHelper;
import top.atdove.epithet.platform.services.IPlatformHelper;
import top.atdove.epithet.platform.services.ITitleDataHelper;

import java.util.ServiceLoader;

public class Services {
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final INetworkHelper NETWORK = load(INetworkHelper.class);
    public static final ITitleDataHelper TITLE_DATA = load(ITitleDataHelper.class);
    public static final IConfigHelper CONFIG = load(IConfigHelper.class);

    private static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("[" + Epithet.MOD_ID + "] Failed to load service for " + clazz.getName()));
    }
}
