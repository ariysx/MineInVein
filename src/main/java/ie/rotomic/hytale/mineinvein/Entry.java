package ie.rotomic.hytale.mineinvein;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import ie.rotomic.hytale.mineinvein.systems.BreakBlockSystem;
import org.jetbrains.annotations.NotNull;

public class Entry extends JavaPlugin {
    private static Entry Instance;

    public Entry(@NotNull JavaPluginInit init) {
        super(init);
        Instance = this;
    }

    @Override
    protected void setup() {
        super.setup();
        getLogger().atInfo().log("Loading Mine In Vein");
        registerSystems();
    }

    private void registerSystems() {
        getLogger().atInfo().log("Mine In Vein Registering Systems");
        getEntityStoreRegistry().registerSystem(new BreakBlockSystem());
    }



    public static Entry getInstance() {
        return Instance;
    }
}
