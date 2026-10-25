---
status: provisional
---

# An Unlit block gives no light at all

A Touch Of ADR-0003 closes the cheap leaks of permanent light "by the lit threshold or a tag", and
lets a pack tag another mod's lamp `atouchofdarkness:unlit`. With the threshold at 3, redstone
torches, enchanting tables and ender chests (7) count as light, yet their recipes can't go, and
glow lichen, glow berries and sea pickles are free light anyone can farm.

**Decision.** A block in the `atouchofdarkness:unlit` tag gives off no light at all: vanilla's light
level reads 0 from it, for every player, mob and mod. It looks dark, mobs spawn beside it, and
A Touch of Madness's light drain sees the dark. The Pillar's default tag holds the redstone torch,
redstone ore, enchanting table, ender chest, magma block, glow lichen, cave vines, sea pickles,
amethyst clusters and buds, sculk catalyst and firefly bush, and the redstone lamps, copper bulbs,
candles and candle cakes whose recipes are removed. The config that restores vanilla's recipes also
empties the tag.

**Considered.** Glowing as before but not counting for the Dark: the Dark would need its own light
engine apart from vanilla's light level, which A Touch Of ADR-0003 rejected, and Madness and mob
spawning would disagree with the Dark. Raising the threshold above 7 to close them: a torch would
light only a few blocks, and the leaks of 14 and 15 would stay open.
