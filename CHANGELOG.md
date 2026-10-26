# Changelog

Written for Consumers: what a mod building against A Touch of Darkness can use, or will see change. A published version never changes: a fix is the next patch (`docs/agents/releases.md`).

## Unreleased

- The Pillar has its own creative tab, "A Touch of Darkness", and no longer nests `libworks-runtime` or fills its Works tab.
- The Dark: a survival player in a dimension type tagged `atouchofdarkness:dark` (by default the Overworld, the Nether and the End) whose eyes are below light 5, as the open sky is at night, loses a 10 s Grace Period, which refills after 2 s of unbroken light and on respawn. When it runs out, the Babau, never seen, lands a Strike, then every 10 s in the Dark: the damage type `atouchofdarkness:strike`, 10 on Normal before armour, scaled to 0 / 5 / 15 on Peaceful / Easy / Hard by the `DamageScaling` `atouchofdarkness:strike` and the `DeathMessageType` `atouchofdarkness:strike`, which picks one of four death messages naming the Babau. Creative and Spectator players and non-players are never struck, and Night Vision isn't light.
- The sky lights the Dark: a player whose eyes see the sky is Lit at night under a full moon unless it's raining, and always under the sky of a dimension type tagged `atouchofdarkness:sky_always_lit` (by default the End). Under a roof, or in the Nether, only the light at their eyes counts.
- The Dark's cues: a player hears entering the Dark with a full Grace Period, a warning 2 s before each Strike, and each Strike, the sound events `atouchofdarkness:dark.enter`, `dark.warning` and `dark.strike`, each with a subtitle and heard by that player alone. A vignette darkens the screen's edge as the Grace Period drains and clears in light, following the payload `atouchofdarkness:dark_state` the server sends the client.
- The common config, `atouchofdarkness-common.toml`, sets the lit threshold, the Grace Period, the refill delay, and the Strike's damage and interval.
