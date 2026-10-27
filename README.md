# A Touch of Darkness

![A Touch of Darkness](https://raw.githubusercontent.com/5thlayer/atouchofdarkness/main/publish/atouchofdarkness-cover.png)

<!-- For players, written by the maintainer and never by an agent: what A Touch of Darkness is, why it exists
and how it feels to play. It is also the Modrinth and CurseForge description
(docs/agents/publishing.md). The technical sections go below it. -->

The Dark strikes anyone who stays unlit, and every light burns out.

## For pack authors

- **Where the Dark falls** is the dimension type tag `atouchofdarkness:dark`, by default the Overworld, the Nether and the End.
- **Where the sky always lights** is the dimension type tag `atouchofdarkness:sky_always_lit`, by default the End. Elsewhere the open sky lights only under a full moon without rain.
- **Unlit blocks** are the block tag `atouchofdarkness:unlit`: they give no light at all, for players, mobs and other mods. A pack can add another mod's lamp to it.
- **The Strike** is the damage type `atouchofdarkness:strike`, so its scaling and death messages follow the damage type's data.
- **Vanilla's light** comes back with the `vanillaLight` switch below: the recipes of vanilla's torches, lanterns, candles, campfires and other cheap light, and the light of the Unlit blocks, whose tag it empties.

The terms are in [GLOSSARY.md](GLOSSARY.md), and what each release changed for a pack is in [CHANGELOG.md](CHANGELOG.md).

## Configuration

Both files are common configs, in `config/`. Times are in ticks, 20 to a second.

- **`atouchofdarkness-common.toml`**, the Dark: `litThreshold`, the light at a player's eyes at or above which they are Lit (5); `gracePeriod` (200), `refillDelay` (40), `strikeDamage` on Normal before armour (10) and `strikeInterval` (200).
- **`atouchofdarkness-light.toml`**, the lights: `vanillaLight` (off); `torch.burnTime` (6000, the soul Torch twice as long); `campfire.cap` (12000) and `lamp.cap` (24000), each doubled for its soul kind; `nightbrand.gracePeriods` (3); `lampad.binding` (168000, seven days) and `lampad.lampsPerNugget` (8).

The numbers are provisional while A Touch of Darkness is below 1.0.

## Compatibility

All optional, and none nested in the jar:

- **[Jade](https://www.curseforge.com/minecraft/mc-mods/jade)** shows a placed Campfire's fuel left and the light it gives, and a placed Lamp's on or off and its fuel left.
- **[EMI](https://www.curseforge.com/minecraft/mc-mods/emi)** shows information pages on fuelling the lights, relighting a Spent Torch, vanilla's lights becoming the mod's own, the Nightbrand, the Lampad and her Votive, and the `vanillaLight` switch.
- **[Tough As Nails](https://www.curseforge.com/minecraft/mc-mods/tough-as-nails)**: a lit Campfire warms those near it and a lit Soul Campfire cools them.

## Dependencies

None beyond NeoForge, on Minecraft 26.1.2.

## License

Per file: the code is LGPL-3.0-only and the content, textures and docs CC BY 4.0, as [LICENSE](LICENSE) says. Source is on [GitHub](https://github.com/5thlayer/atouchofdarkness). The Votive's and the Lampad's textures are built from Malcolm Riley's [unused-textures](https://github.com/malcolmriley/unused-textures), under CC BY 4.0; [NOTICE](NOTICE) credits each.
