# Round carried light

Carried light keeps vanilla's shape: the diamond that any single light source lights. The Pillar does
not try to make a carried Light Source's light round.

## Why this is out of scope

Carried light is one invisible `atouchofdarkness:carried_light` block at the carrier's eyes (ADR 0002),
so it lights the world exactly as a placed Light Source of the same level would. Vanilla's block light
engine spreads light only to a block's six neighbours, one level less at each step
(`BlockLightEngine.propagateIncrease`), so the light falls off with taxicab distance: a diamond on open
ground. A placed lantern or torch lights the same diamond, and the Lamp's, at 15, is only the largest
and easiest to see.

The ways to change that shape all cost more than they give:

- **A client dynamic-lights mod** draws round light, but only on the client and only where it is
  installed. The light the Dark, A Touch of Madness and mob spawning read stays a diamond. The
  integration was dropped as won't fix (#11, ADR 0002's amendment).
- **Dimmer light blocks around the carrier** to round off the corners mean several block updates and
  light recalculations each time the carrier's block position changes, and cleanup of every one of
  them by ADR 0002's rules. They can only go into air or water, so walls and caves leave the shape
  ragged anyway, and a moving player gets an octagon at best, not a circle.
- **A mixin into the light engine** for round falloff is the deep, fragile change ADR 0002 already
  turned down, and it would reshape every light in the world, not just carried ones.

Matching vanilla is the point of ADR 0002: carried light is real light, the same as a placed one.

## Prior requests

- #21: "A carried Lamp's light falls off in a diamond, not a circle"
