# Changelog

Written for Consumers: what a mod building against A Touch of Darkness can use, or will see change. A published version never changes: a fix is the next patch (`docs/agents/releases.md`).

## Unreleased

- The Pillar has its own creative tab, "A Touch of Darkness", and no longer nests `libworks-runtime` or fills its Works tab.
- The Dark: a survival player in a dimension type tagged `atouchofdarkness:dark` (by default the Overworld, the Nether and the End) whose eyes are below light 3 loses a 10 s Grace Period, which refills after 2 s of unbroken light and on respawn. When it runs out, the Babau, never seen, lands a Strike, then every 10 s in the Dark: the damage type `atouchofdarkness:strike`, 10 on Normal before armour, scaled to 0 / 5 / 15 on Peaceful / Easy / Hard by the `DamageScaling` `atouchofdarkness:strike` and the `DeathMessageType` `atouchofdarkness:strike`, which picks one of four death messages naming the Babau. Creative and Spectator players and non-players are never struck, and Night Vision isn't light.
- The common config, `atouchofdarkness-common.toml`, sets the lit threshold, the Grace Period, the refill delay, and the Strike's damage and interval.
