# Master Dice PRD

Oct 9, 2026 · @H

## Overview

Master Dice is a single-player Mastermind-style puzzle where the player deduces a hidden sequence of 3 dice, each defined by a colour and a face value, in at most 10 guesses. It ships as one Kotlin Multiplatform codebase with a Compose Multiplatform UI on Android, iOS and desktop.

The twist on classic Mastermind: the player is handed the exact dice that make up the solution, plus one decoy. The puzzle is choosing which 3 of the 4 dice belong and in what order, using per-cell colour feedback.

**Goals**

- A complete, playable v1: deal, guess by drag and drop, per-cell feedback, win and loss.
- Shared game logic in `commonMain` with full unit-test coverage, and one shared Compose UI.
- Touch-first drag and drop that also works with a mouse on desktop.

**Non-goals for v1**

- Multiplayer, accounts, leaderboards or cloud sync.
- Monetisation, ads or in-app purchases.
- Web (Wasm) target, unless it falls out cheaply from the Compose setup.

## Core concepts

| Term | Definition |
| --- | --- |
| Die | A game piece with a colour (one of 6) and a face value (1 to 6). |
| Solution | 3 dice dealt at game start, hidden from the player. Their order is the secret sequence. |
| Column header | The hidden solution die for a column. Columns 1 to 3 each have one header; column 4 has none. |
| Deal | 4 dice: the 3 solution dice plus 1 random decoy, shuffled. |
| Tray | The 4-slot area at the foot of the screen that holds the deal. |
| Grid | 10 rows by 4 columns. Columns 1 to 3 hold dice; column 4 is the enter cell. |
| Active row | The first row that has not been submitted. Only this row accepts dice. |
| Enter cell | Column 4 of the active row. Enabled only when columns 1 to 3 all hold a die; tapping it submits the guess. |
| Guess | A submitted row of 3 dice, locked with its feedback. |

## Game setup

A new game produces an empty 10 x 4 grid, a hidden 3-die solution and a 4-die tray.

1. Create the grid: 10 rows by 4 columns, all cells empty. Row 1 (top) is the active row.
2. For Level 1, select a puzzle from the validated bank below. Other levels generate a solution and decoys using that level's configured column and tray counts; colours or values may repeat, but no two dealt dice are identical.
3. Shuffle the deal into random tray slots so position gives nothing away.
4. Show the tray; keep the solution hidden.

Randomness comes from an injectable seeded generator so tests and a future daily-puzzle mode can reproduce a game.

Level 1 uses a library of 1,000 prevalidated puzzles. Each puzzle has exactly four dealt dice: three ordered secret dice and one decoy, with all four dice fixed throughout the game. No colour/value pair repeats within a deal. The selected puzzle id is saved with game state, and puzzles are drawn at random without replacement until the full library has been used; then a new cycle begins.

Every library puzzle is solvable in at most four guesses, so it is within the 10-guess limit. For any four-die deal ordered d0–d3, the first three probe rows can be (d0,d1,d2), (d1,d2,d3), and (d2,d3,d0). Each row uses three distinct dice. In each secret column, a green response identifies the exact die; if none of the three probes is green, the only untested die is the answer. The three inferred dice form the exact ordered secret, which solves the puzzle on guess four. This gives a worst-case guarantee for every possible ordered secret in the deal.


## Gameplay

The player builds each guess by dragging 3 of the 4 tray dice into columns 1 to 3 of the active row, then taps the enter cell. A guess uses each selected die once, and repeating an earlier ordered guess is allowed.

**Allowed moves** (only these three; any other drop snaps the die back to where it came from):

| # | From | To | Result |
| --- | --- | --- | --- |
| 1 | Tray slot | Empty cell in the active row (columns 1 to 3) | Die moves; tray slot becomes empty. |
| 2 | Cell in the active row | Empty tray slot | Die moves back; row cell becomes empty. |
| 3 | Cell in the active row | Another cell in the active row | The two cells swap contents (a swap with an empty cell is a plain move). |

**Not allowed**: dropping onto an occupied tray slot or an occupied cell from the tray, dropping on any submitted row or on the enter cell, and moving dice in submitted rows.

**Submitting**

1. When columns 1 to 3 all hold a die, the enter cell becomes enabled (visibly styled as a button).
2. Tapping the enter cell locks the row and runs validation.
3. If the game continues, the dice return to the tray in their original slots and the next row becomes active. The submitted row keeps a copy of its dice and feedback as history.

## Validation, win and loss

Each submitted cell is compared only with its own column header, and its border shows the result.

| Cell die vs column header | Border |
| --- | --- |
| Colour AND value match | Green |
| Colour OR value matches (exactly one) | Amber |
| Neither matches | Neutral (no highlight) |

Unlike classic Mastermind there is no "right die, wrong place" signal: amber means a partial match with that column only.

**Worked example.** Solution: Red 4, Blue 2, Green 6.

| Guess | Col 1 | Col 2 | Col 3 |
| --- | --- | --- | --- |
| Blue 2, Red 4, Green 6 | Neutral | Neutral | Green |
| Red 4, Yellow 2, Green 6 | Green | Amber (value) | Green |

**Win**: all three cells are green. The game shows a win state with the guess count, and reveals the headers.

**Loss**: row 10 is submitted without a win. The game reveals the solution in the column headers and shows a loss state.

Both end states offer New game. Input is disabled once the game ends.

## UX and screen layout

One portrait screen: hidden headers on top, the 10-row grid in the middle, the tray at the foot.

&#91;embedded content: main screen · portrait layout, mid-game\]

In this example the solution is the first, second and third dice of the active row; row 2 shows a green, an amber (value match only) and a green.

- **Dice** show colour plus pips, never colour alone.
- **Dragging** lifts the die with a shadow and scale; legal drop targets highlight while dragging; illegal drops animate back.
- **Enter cell** is greyed out until the row is full, then styled as a button.
- **Submit** reveals feedback borders cell by cell with a short stagger.
- **End of game** shows a win or loss panel over the grid, reveals the headers, and offers New game.
- **Landscape and desktop** keep the same layout centred, sized so all 10 rows fit without scrolling.

## Technical requirements

All game rules live in a pure Kotlin `shared` module; the Compose UI only renders state and sends intents.

| Area | Requirement |
| --- | --- |
| Targets | Android (minSdk 24), iOS (arm64 + simulator), Desktop JVM. |
| UI | Compose Multiplatform, one shared `GameScreen` in `commonMain`; thin platform entry points only. |
| Architecture | Unidirectional data flow: immutable `GameState`, sealed `GameIntent` (`Move`, `Submit`, `NewGame`), a pure `reduce(state, intent)` function, exposed via a `StateFlow` from a `GameViewModel`. |
| Domain model | `Die(colour, value)`, `DieColour` enum (6 values), `Slot` (tray index or grid row/col), `Feedback` enum (`GREEN`, `AMBER`, `NONE`), `GameStatus` (`PLAYING`, `WON`, `LOST`). |
| Rules engine | `isLegalMove(state, from, to)` encodes the three allowed moves; `evaluate(guess, solution)` returns per-column feedback. Both pure and unit tested. |
| Randomness | `kotlin.random.Random` injected with a seed; no platform RNG in game logic. |
| Drag and drop | Built with Compose pointer input (`detectDragGestures`) and a shared drop-target registry of slot bounds, so it behaves the same on touch and mouse. Invalid drops animate back. |
| Persistence | Save the in-progress game on background or close and restore it on launch (multiplatform-settings or DataStore). |
| Accessibility | Each die shows its value as pips and carries a colour-blind-safe pattern or label; borders use thickness and an icon as well as colour; content descriptions on all cells. |
| Testing | `commonTest` covers setup invariants, every legal and illegal move, evaluation, win and loss. Compose UI test for one full game on Desktop. |
| CI | GitHub Actions: build all targets and run `allTests` on every PR. |

## Acceptance criteria and milestones

v1 is done when a player can finish a full game on all three targets and every criterion below passes.

- [ ] A new game always has a 4-die tray containing the 3 solution dice plus one non-identical decoy.
- [ ] Only the three allowed moves change state; every other drop leaves state unchanged.
- [ ] The enter cell is enabled only when columns 1 to 3 of the active row are filled.
- [ ] Feedback per cell is green, amber or neutral exactly as defined, checked against the column header only.
- [ ] Three greens ends the game as a win; a non-winning 10th row ends it as a loss with the solution revealed.
- [ ] After a non-final submit, the tray refills and the next row becomes active.
- [ ] An in-progress game survives closing and reopening the app.
- [ ] Feedback is distinguishable without colour.

**Milestones**

1. Domain and rules engine in `shared`, with tests.
2. Static Compose layout of grid and tray on all targets.
3. Drag and drop with move validation.
4. Submit, feedback, win and loss states.
5. Persistence, accessibility pass, polish and animations.

## Assumptions and open questions

The brief left these points open; the PRD uses the default shown until you decide otherwise.

| # | Question | Default in this PRD |
| --- | --- | --- |
| 1 | What is a "column header" when the solution has 3 dice and the grid 4 columns? | Columns 1 to 3 each have a hidden header (one solution die); column 4 is only the enter cell. |
| 2 | How many colours and values? | 6 colours, values 1 to 6. |
| 3 | Can solution dice repeat a colour or a value? Can the decoy duplicate a solution die? | Colours and values may repeat, but no two dice in the deal are identical. |
| 4 | What happens after 10 rows without a win? | Loss; solution revealed; New game offered. |
| 5 | Are headers hidden for the whole game? | Hidden until win or loss. Alternative: reveal a header once its column goes green. |
| 6 | What happens to the tray after a submit? | All 4 dice return to their original slots. |
| 7 | Is drag the only input? | Drag is primary. Tap-to-select then tap-to-place could be added for accessibility. |
| 8 | Difficulty: there are only 24 ordered picks of 3 from 4 dice, and per-column feedback is precise, so 10 rows may be generous. Should it be harder? | Keep v1 as specified. Options: fewer rows, more decoys, or classic Mastermind-style feedback that doesn't say which cell matched. |
| 9 | Scoring, timer, stats or a daily seeded puzzle? | Not in v1; the seeded generator keeps a daily puzzle possible. |
| 10 | Which platforms ship first? | Android, iOS and Desktop together; Web deferred. |
