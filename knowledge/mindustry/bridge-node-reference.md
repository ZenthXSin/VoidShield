# Bridge and Node Reference

## Source Basis

- `C:\Users\Administrator\Desktop\Mindustry\源码\core\src\mindustry\world\blocks\distribution\ItemBridge.java`
- `C:\Users\Administrator\Desktop\Mindustry\源码\core\src\mindustry\world\blocks\power\PowerNode.java`
- `C:\Users\Administrator\Desktop\Mindustry\源码\core\src\mindustry\entities\comp\BuildingComp.java`
- `C:\Users\Administrator\Desktop\Mindustry\源码\core\src\mindustry\graphics\Drawf.java`

## Confirmed API Notes

- `Building.version()` default returns `0`; custom build serialization can override it for backward-compatible reads.
- `ItemBridge.handlePlacementLine(...)` only configures the current plan to the next plan when the pair is valid; it does not build a full graph.
- `ItemBridge.playerPlaced(...)` uses `findLink(...)` plus `lastBuild` to auto-connect the most recently placed compatible bridge.
- `ItemBridge.onConfigureBuildTapped(...)` returns `false` when it handled the tap itself, and `true` when the normal configure flow should continue.
- `PowerNode.drawPlace(...)` and `PowerNode.drawConfigure(...)` use circular range visuals via `Drawf.circles(...)`, not cross-axis bridge visuals.
- `PowerNode.changePlacementPath(...)` relies on geometric overlap/range checks instead of straight-line-only axis checks.
- `Drawf.dashCircle(...)` expects a float radius in world units.
- `arc.struct.IntSeq` supports `add`, `addUnique`, `contains`, `removeValue`, `removeIndex`, `clear`, and is suitable for compact link persistence.

## Modding Guidance

- For bridge-like blocks, use `ItemBridge` as the reference for placement chaining, auto-link-on-place, and configure tap semantics.
- For node-like multi-link blocks, use `PowerNode` as the reference for range visualization and multi-target connection behavior.
- If a custom block mixes both patterns, explicitly choose which behavior owns placement, configuration, and persistence; Mindustry does not provide a shared hybrid base class.
