# A Touch of Darkness

The Pillar of A Touch Of that owns the Dark: a player who stays unlit too long is struck by something unseen until they reach light, and every Light Source burns out. Modpacks load it; the other Pillars meet it only through vanilla state.

## Language

The Pillar's own terms, each with what it is and the words to avoid. The suite's terms (Pillar, Runtime, Discovery, Dark, Light Source, Spent Torch) are defined in the A Touch Of glossary, in `5thlayer/atouchof`. `/domain-modeling` adds terms here as they are resolved.

### Parties

**A Touch of Darkness**:
This Pillar.
_Avoid_: Module (a FactoryWorks mod), Library, Binding

**A Touch Of**:
The suite this Pillar belongs to, with A Touch of Wickedness and A Touch of Madness.
_Avoid_: FactoryWorks (another suite), the Pack

**Consumer**:
A modpack that loads this Pillar.
_Avoid_: client (collides with the game's client side), dependent, integration

**Tough As Nails**:
The mod the suite leaves cold and heat to, which a Consumer may load beside this Pillar. The Pillar works with it where it is loaded and never requires it.
_Avoid_: TAN, A Touch of Cold (shelved), temperature mod

### The Dark

**Lit**:
A player is lit when the light at their eyes is enough: vanilla's light with the sky dimmed for the hour and the weather, or the light of a burning Light Source in either of their hands, whichever is brighter. They are also lit under the open sky of a dimension whose sky never darkens, such as the End, and on the surface at night under a full moon when it isn't raining. A burning Light Source a player carries lights the world around them as a placed one would.
_Avoid_: bright, safe

**Babau**:
The one being of Italian folklore that lives in the Dark and strikes whoever stays there too long; never seen, only felt.
_Avoid_: Bogeyman, Charlie (Klei's name), grue, the Dark (the state it lives in), monster

**Grace Period**:
How long a player can be in the Dark before the first Strike. It refills only after the player has been lit for a moment without a break.
_Avoid_: timer, countdown

**Strike**:
One blow from the Babau: heavy damage that armour, enchantments and effects soften as they do any other, scaled by difficulty to nothing on Peaceful, and repeated while the player stays in the Dark. Where A Touch of Madness is loaded it also costs Sanity, on Peaceful too. It never touches anything but players, nor players in Creative or Spectator.
_Avoid_: attack, hit

### Light

**Torch**:
This Pillar's torch: crafted burning, carried in the hand and never placed, and burning only while held. It goes out underwater and burns out into a Spent Torch.
_Avoid_: lit torch, burning torch (every torch is)

**Lamp**:
This Pillar's lantern: a Light Source carried or placed, turned on and off by hand or by redstone, fed fuel by hand up to what it can hold, and shining at its full light while it is on and has fuel.
_Avoid_: lantern (vanilla's block), FE lamp (not made yet)

**Campfire**:
This Pillar's placed fire, put down already burning and fed fuel by hand up to what it can hold; its light dims as its fuel runs down. Where Tough As Nails is loaded it warms those near it while it is lit, however full.
_Avoid_: fire pit, bonfire

**Soul Campfire**:
The Campfire's soul kind, dimmer, which cools those near it while it is lit where Tough As Nails is loaded, as the Campfire warms them.
_Avoid_: blue campfire, cold fire

**Fuel**:
The burn time a Lamp or a Campfire holds: ten times the furnace burn time of each item fed to it, up to what it can hold, the overflow lost. It burns down only while the Light Source is lit, a Campfire's twice as fast in rain under the open sky, and a Light Source with none left gives no light.
_Avoid_: charge, burn time (an item's, before it is fed), fuel level

**Lampad**:
A torch-bearing spirit of the underworld, after Hecate's nymphs, summoned from a Lamp set on gold and kindled with a Torch, the gold becoming her Votive. She carries fuel from her Votive into her Tended Lamps, never Campfires, and takes gold from it as her Claimed Lamps grow in number. Her binding fades within a week, and once it has faded she stands dormant until a Nightbrand renews it. Hurt, she retreats to the Dark and tends nothing until she has healed. She gives no light and is not a Light Source.
_Avoid_: golem (vanilla's kind), lamplighter, servant, Munaciello

**Claimed Lamp**:
A placed Lamp in a Votive's range, nearer it than any other Votive with a Lampad, which that Lampad owes gold for. A carried Lamp is never claimed, nor is a Campfire.
_Avoid_: Lamp in range, her Lamps

**Tended Lamp**:
A Claimed Lamp that the gold her binding has taken pays for, which she carries fuel to, nearest her Votive first.
_Avoid_: paid Lamp, served Lamp

**Votive**:
The offering stand a Lampad is summoned at and works from, holding the fuel, the Nightbrand and the gold she takes. She works from nothing else.
_Avoid_: shrine, chest, altar

**Nightbrand**:
A Spent Torch that a player has carried in the Dark until it has soaked the Dark up, which renews a Lampad's binding. Each player can darken only one a day. It gives no light and is not a Light Source.
_Avoid_: steeped torch, dark torch, Stygian Torch

**Vesper Dial**:
This Pillar's gold instrument, four gold ingots set around a Nightbrand, which, while in either of its carrier's hands, shows them the light at their eyes and whether they are Lit, as a meter on the Runtime's HUD. It shows nothing of the Grace Period, which is only ever felt. It gives no light, is not a Light Source, and never wears out.
_Avoid_: light meter (the row it shows), photometer, lux meter, compass

**Unlit**:
A block that would give light in vanilla but gives none at all here, for every player and every mob, because this Pillar or a pack has put it out. Cheap light that can't lose its recipe is closed this way.
_Avoid_: dimmed, ignored, light-blocked

### Dev runs

**Game-test pack**:
A datapack only this Pillar's game tests switch on, holding the data a Consumer would supply, such as a tag's entries or a recipe, so a test can stand on it. Never in what players download.
_Avoid_: test datapack, gametest datapack

**Dev pack**:
A datapack only this Pillar's dev client switches on, holding the data a Consumer would supply, so a feature a Consumer turns on with data can be tried by hand. Never in what players download.
_Avoid_: debug pack, test pack

### Shipped packs

**Gallery pack**:
A datapack in this Pillar's jar, off until a player turns it on, whose functions build a working scene of each of the Pillar's features, each with a sign naming it. Functions only: turning it on changes nothing about how the Pillar plays (libworks ADR 0010).
_Avoid_: demo pack, showcase pack, gallery datapack
