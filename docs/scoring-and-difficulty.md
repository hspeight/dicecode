# Master Dice: scoring analysis and ways to raise difficulty

Source: `commonMain` code shared on 10 Oct (`game/Engine.kt`, `game/Model.kt`, `ui/GameScreen.kt`).
Numbers come from simulations in this folder (`sim.py`, `sim_tray.py`, `sim_open.py`).

## How scoring works today

- **Deal** (`Engine.kt` `newGame`): 3 distinct random dice (6 colours x 6 values) form the hidden solution. One random decoy is added, and all 4 are shown in the tray.
- **Guess:** the player places 3 of the 4 tray dice in order and taps Enter. There are `ROWS = 10` guesses (the new Easy board shows 8).
- **Feedback** (`evaluate`): each column gets **green** (colour and value match), **amber** (colour *or* value matches) or **none**. The result is shown **on each cell** (coloured border and glow) and again as sorted pegs at the end of the row.
- **Result:** the game is won when all 3 are green and lost after the last row. **There is no points score.** The only outcome is win or lose, and nothing is stored between games.

## Why it is easy

The player can see every die that could be in the answer, so the only question is which 3 of 4 dice and in what order. That gives just **24 possible answers**. Per-cell feedback then tells them exactly which positions are right.

Simulated over 300 deals, a player who just makes any guess that still fits the clues solves it in **2.4 guesses on average and never more than 4**. Of the 10 rows (or 8 on Easy), 4 to 6 are never used. Even a careful player cannot do much better, because there is so little to work out.

## Difficulty levers (simulated)

"Casual" means a player who picks at random from guesses that still fit every clue so far. Real players make mistakes, so expect them to need somewhat more guesses than this.

| Variant | Possible answers | Avg guesses | 90% solved by | Worst |
|---|---|---|---|---|
| **Current** (3 dice, tray 4, per-cell) | 24 | 2.4 | 3 | 4 |
| Peg counts only (hide *which* cell) | 24 | 2.8 | 4 | 5 |
| Green-only count (no amber, no position) | 24 | 3.5 | 5 | 6 |
| Tray 5 (2 decoys), per-cell | 60 | 2.8 | 4 | 5 |
| Tray 8 (5 decoys), per-cell | 336 | 3.7 | 5 | 6 |
| Tray 8, peg counts | 336 | 4.4 | 6 | 7 |
| Tray 12, peg counts | 1,320 | 5.3 | 7 | 8 |
| 4 dice, tray 8, peg counts | 1,680 | 4.8 | 6 | 8 |
| 4 dice, tray 10, peg counts | 5,040 | 5.4 | 7 | 7 |
| Open pool: any colour + value per slot, per-cell | 46,656 | 6.1 | 7 | 7 |
| Open pool, peg counts | 46,656 | 8.0 | 10 | 12 |

What the table shows:

1. **More decoys in the tray** is the strongest single lever that keeps the current drag-from-tray play. Going from 4 to 8 dice raises the average from 2.4 to 3.7 guesses.
2. **Hiding which cell is right** (keep the peg summary, drop the per-cell borders) adds about half a guess. It costs nothing in UI, since the pegs already exist.
3. **Fewer rows** only matters once the game needs more guesses. Cutting rows on the current rules does nothing until you get below 4.
4. **4 dice instead of 3** adds little on its own when feedback stays per-cell. It works well combined with peg counts.
5. **Open pool** (the player picks colour and value freely, like classic Mastermind) is a big jump. It needs a new picker UI instead of the tray.
6. **Removing amber** feels harder, but with per-cell feedback it barely changes the numbers. It is a weaker lever than hiding position.

Not simulated, but cheap to add: **tricky decoys**. Generate the decoy so that it shares a colour or value with one or more solution dice. Then an amber no longer tells the player much, and the decoy is harder to rule out.

## Suggested levels

| Level | Rules | Rows | Expected feel |
|---|---|---|---|
| **Easy** | Current: tray 4, per-cell colours | 8 | Solved in about 2 to 4 guesses, basically never lost |
| **Medium** | Tray 8, peg counts only, tricky decoys | 6 | About 4 to 5 guesses. Roughly 12% of casual games would still be unsolved after 5 guesses, so a few will go to the last row |
| **Hard** | 4 dice, tray 10, peg counts | 6 | About 5 to 6 guesses. About 13% of casual games fail |
| **Master** | Open pool, per-cell colours | 7 | About 6 guesses. Roughly 30% of casual games run past 6 |

## Adding an actual score

Since there are no points today, a simple score would give each game stakes and make the harder levels worth choosing:

- **Base:** `(rows left + 1) x 100` on a win, so solving with guesses to spare scores more. A loss scores 0.
- **Level multiplier:** Easy x1, Medium x2, Hard x3, Master x5.
- **Optional:** a bonus for a solve with no ambers used, a win-streak multiplier (needs `GameStore` to keep stats beyond the current game), and a timer bonus for speed.

## Where to change things in the code

- `Model.kt`: `ROWS`, `GUESS_SIZE`, `TRAY_SIZE` are constants. They would become part of a `Level` stored in `GameState` (and `SAVE_VERSION` would need bumping).
- `Engine.kt newGame`: generate `TRAY_SIZE - GUESS_SIZE` decoys instead of one, with an optional "tricky" rule.
- `Engine.kt evaluate`: unchanged for peg-count mode. The UI simply stops passing per-cell `feedback` to submitted cells (`GameScreen.kt`, around the border and glow modifiers near line 780), and only the peg summary (around line 688) stays.
- Rules text (`GameScreen.kt` around line 551) mentions "one decoy" and the per-cell meaning, so it would need to change for each level.
