# Two-decoy bank generation

The next normal-play level uses stable engine index 2: three secret positions and two decoys. The four-position bank stays at index 1 for save/stat compatibility.

Run `./gradlew :shared:generateTwoDecoyBank` (or `gradlew.bat` on Windows). The task writes `bank.txt`, `audit.csv`, and `summary.txt` into the root `build/two-decoy-bank` directory. It does not overwrite the shipped bank.

Generation uses Kotlin Random seed 20261019, shuffles all 36 colour/value pairs, and keeps the first 1,000 distinct five-die sets. Each row stores the three ordered secret dice followed by both decoys. IDs are row numbers and must remain stable once saves use the bank. The generator was run with Kotlin 2.4.20.

Exact adaptive minimax checks all 60 possible secrets and all 60 legal guesses, including guesses outside the remaining candidate set. A singleton still requires the final winning submission. Non-winning unchanged partitions are rejected; count-only response partitions use green and amber counts. All searches must finish: budget exhaustion throws and aborts generation rather than accepting an unvalidated entry.

Equality plus the undirected colour-or-value similarity graph determines all feedback. We canonicalize this graph across all five-die permutations and reuse exact results for isomorphic graphs. The 1,000 deals cover 17 such structures. The recorded histogram is 832 deals at four guesses, 154 at five, and 14 at six. Maximum search size was 111,112 states; total initial generation/validation took approximately 2.5 seconds on the development machine. Timings are observational, not a test requirement.

`two-decoy-bank-audit.csv` records every shipped ID, encoded row, canonical graph, and exact worst-case guess count. Normal common tests independently revalidate the bank with no opt-in property. Keep the original bank and its IDs unchanged.

## Four-position count-only validation

Run `./gradlew :shared:validateFourPositionDeals`. It writes `structures.csv` and `summary.txt` to root `build/four-position-validation`. This audit enumerates all 376,992 unordered five-die deal sets, including the rare all-same-colour or all-same-value case absent from the current two-decoy bank. There are 18 realizable similarity graphs. Four-position secrets and legal guesses each number 120.

Exact results: 327,600 sets have worst case four guesses, 45,000 have five, and 4,392 have six. The final winning submission counts. This proves a six-guess guarantee for every random deal in the current four-position, one-decoy level; the separate four-position bank is selected from these proven valid candidates.

`FourPositionMinimax` uses two 64-bit words to represent all 120 candidates. It tests exact depth feasibility from one through ten guesses. Response-capacity pruning is an upper bound on decision-tree leaves; sequential candidate submission is a constructive bound. At the unrestricted root only, permutations of columns are equivalent under count-only feedback, so the five sorted four-die guesses cover every possible omitted die. Non-root searches retain all 120 legal guesses. Budget exhaustion throws and is never reported as a proof.

Common tests check all 18 representative structures and reversed die orderings. Desktop tests re-enumerate every deal and compare results against a separate BigInteger reference using the actual engine evaluator, all root guesses, and no sequential-submission shortcut. The independent reference agreed on all 18 results. `four-position-structures.csv` records representative dice, population counts, exact guarantees and observational search statistics.

## Third stored bank: four positions, one decoy

Run `./gradlew :shared:generateFourPositionBank`. Outputs go to root `build/four-position-bank` (`bank.txt`, `audit.csv`, `summary.txt`) and never overwrite the shipped bank. Kotlin Random seed 2026101904 selects the first 1,000 distinct five-die sets from shuffled colour/value IDs. Four ordered secret dice precede the decoy in each ten-character row. Published row numbers are stable IDs. Generation uses Kotlin 2.4.20.

The exact selected-bank histogram is 857 at four guesses, 134 at five, and 9 at six, including the winning submission. All 17 represented graph classes are evaluated with FourPositionMinimax, never accepted from a budget-limited run. `four-position-bank-audit.csv` records every row, canonical structure, and exact optimum. Common tests revalidate the bank, and a desktop test regenerates every row exactly.

The shared decoder now accepts a solution-size parameter (default three), leaving both existing encoded banks unchanged. The new bank is `FourPositionPuzzleLibrary` at stable engine index 1 with independent history key 1. Old in-progress four-position games remain resumable before their first bank draw.
