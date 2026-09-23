# GhostSpear

A Paper 1.21.11 plugin based on ManePear's ghost spear idea.

- **Ghost Mode** – swing the Ghost Spear and you turn invisible, while a decoy copy of you
  (your skin, armor, held items, name, sneaking, and the exact way you were facing) stays standing
  exactly where you were. To everyone else it looks like nothing happened.
- **Leaving Ghost Mode** – swing the spear again, or attack anything (melee or projectiles).
  The decoy vanishes and you reappear where you actually are.
- **Soul Pierce** – while in Ghost Mode, hold right-click (spear charge) into someone to one-tap them.
  This counts as an attack, so it also reveals you.
- The decoy never takes damage, but flinches, makes the hurt sound, and gets knocked back like a real player.

## Commands
| Command | Permission | Default |
|---|---|---|
| `/ghostspear give [player]` | `ghostspear.give` | OP |
| `/ghostspear reload` | `ghostspear.reload` | OP |
| (using the spear) | `ghostspear.use` | everyone |

Alias: `/gspear`

## Invisibility modes (`ghost-mode.invisibility` in config.yml)
- `POTION` (default): invisibility effect with no particles or icon, and your armor and held items are hidden
  from everyone. You stay in the tab list. People can still hit you if they find you, and they can
  hear your footsteps.
- `HIDE`: you're removed from other players' screens entirely. No footsteps, and you can't be hit by
  players, but you also disappear from the tab list, which smart players might notice.

## Requirements
- **Paper** (or a Paper fork like Purpur) **1.21.11**. Plain Spigot won't work, because the decoy uses Paper's Mannequin API.
- Java 21.

## Building
GitHub Actions builds the plugin automatically every time you push. You can get the jar from either place:
1. The **Releases** section on the right side of the repo page.
2. **Actions** tab → latest run → **Artifacts** → `GhostSpear-jar`.

Local build: `mvn package` → `target/GhostSpear-1.0.0.jar`.
