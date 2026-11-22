package example;

import arc.Events;
import arc.struct.ObjectFloatMap;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;

public class ExampleJavaMod extends Mod {
    // Tracks remaining Plasma burn time (in ticks) for each building
    public static ObjectFloatMap<Building> plasmaFuel = new ObjectFloatMap<>();

    @Override
    public void loadContent() {
        // 1. After HJSON finishes loading, allow every power-consuming block to accept items
        Events.on(ContentInitEvent.class, e -> {
            Item plasma = Vars.content.item("plasma-mod-plasma");
            if (plasma == null) return;

            for (Block block : Vars.content.blocks()) {
                // Skip power generators/nodes/batteries; only target machines that consume power
                if (block.consPower != null && !(block instanceof PowerGenerator) && !(block instanceof PowerNode) && !(block instanceof Battery)) {
                    block.hasItems = true;
                    if (block.itemCapacity < 10) {
                        block.itemCapacity = 10;
                    }
                    // Allow conveyors/unloaders to feed Plasma into blocks that normally don't take items
                    block.acceptsItems = true;
                }
            }
        });

        // 2. Every game tick, burn 1 Plasma in any powered machine to fill its internal power buffer
        Events.run(Trigger.update, () -> {
            if (!Vars.state.isPlaying()) return;
            Item plasma = Vars.content.item("plasma-mod-plasma");
            if (plasma == null) return;

            Groups.build.each(build -> {
                if (build.block.consPower == null || build.power == null) return;
                if (build.block instanceof PowerGenerator || build.block instanceof PowerNode || build.block instanceof Battery) return;

                float burnLeft = plasmaFuel.get(build, 0f);

                // If out of burn time, consume 1 Plasma from the building's inventory for 3 seconds (180 ticks) of power
                if (burnLeft <= 0f && build.items != null && build.items.has(plasma)) {
                    build.items.remove(plasma, 1);
                    burnLeft = 180f;
                    Fx.generatespark.at(build.x, build.y);
                }

                // While burning Plasma, keep the machine's power satisfaction at 100% (1.0f)
                if (burnLeft > 0f) {
                    build.power.status = 1f;
                    plasmaFuel.put(build, burnLeft - arc.util.Time.delta);
                }
            });
        });
    }
}
