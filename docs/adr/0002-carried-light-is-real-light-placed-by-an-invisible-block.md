---
status: provisional
---

# Carried light is real light, placed by an invisible block

A Touch Of ADR-0002 has the Pillars meet only in vanilla state, so A Touch of Madness drains Sanity
on low light without knowing this Pillar exists. A torch in the hand changes nothing in vanilla's
light level, so a player carrying light would still be in the Dark for Madness, for mob spawning and
for the Dark itself.

**Decision.** A burning Light Source in a player's main or off hand lights the world as a placed one
would. Nothing dropped on the ground gives light. The Pillar places an invisible light block of its own
at the carrier's position when their block position changes, and removes it behind them. The block
gives the Light Source's own light level, goes only into air or water, and removes itself when no
carrier is near, so a crash or an unloaded chunk leaves no permanent light. Mobs and other entities
holding light carry none.

A client dynamic-lights mod is recommended for light that moves smoothly rather than a block at a
time. The Pillar ships the data such a mod reads for its Light Sources, and tells it to skip a held
one the Pillar already lights, so the light is never doubled and dropped items stay dark.

**Considered.** Leaving carried light to a dynamic-lights mod alone, with a rule that only counts a carrier as lit: such a mod draws light on the client only, so the Dark would have its own idea of light
apart from vanilla's, Madness would not see it, and mobs would spawn at a torch-bearer's feet. A
mixin into vanilla's light engine so an entity emits light: deep, fragile across versions, and in
conflict with other lighting mods. Vanilla's `minecraft:light` block: players place it too, so an
orphan could not be told from a placed one and removed.
