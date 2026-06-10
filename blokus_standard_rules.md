# Blokus Standard Rules

This document summarizes the official standard four-player rules for the original Blokus board game.

## Game overview

Blokus is an abstract strategy board game for four players. Each player tries to place as much of their own color as possible onto the board while blocking opponents from doing the same.

The central rule is simple: your pieces may touch your own pieces only at corners, never edge-to-edge. Pieces of different colors may touch in any way.

## Components

The standard game uses:

- 1 board with 400 squares, arranged as a 20 by 20 grid.
- 84 total pieces in four colors: blue, yellow, red, and green.
- 21 pieces per color.

Each color has the same set of 21 different shapes:

- 1 one-square piece.
- 1 two-square piece.
- 2 three-square pieces.
- 5 four-square pieces.
- 12 five-square pieces.

Each player's full set contains 89 unit squares in total.

## Objective

Place as many of your 21 pieces on the board as possible.

At the end of the game, the winner is determined by the number of unit squares left unplayed. Under basic scoring, the player with the fewest remaining squares wins. Under advanced scoring, players count remaining squares as negative points and may earn bonuses for placing every piece.

## Setup

Each player chooses one color and takes all 21 pieces of that color.

Players sit around the board with their pieces in front of them.

For the original standard four-player game, the turn order is:

1. Blue
2. Yellow
3. Red
4. Green

Play continues in that order throughout the game.

## Starting rule

Each player's first piece must cover one corner square of the board.

In a standard four-player game, each player begins from a different corner. The first piece may be any piece from that player's set, as long as it covers that player's starting corner square.

## Turn structure

On your turn, place exactly one unused piece of your color onto the board.

A placed piece must:

1. Fit completely inside the board.
2. Cover only empty squares.
3. Follow the first-piece corner rule if it is your first move.
4. Follow the corner-contact rule if it is not your first move.
5. Avoid edge contact with your own color.

Once a piece is placed, it cannot be moved later.

## Corner-contact rule

After your first move, every new piece you place must touch at least one of your previously placed pieces by a corner.

Corner contact means diagonal contact between unit squares. A corner of the new piece must meet a corner of an existing piece of the same color.

Your own pieces may not touch each other along an edge. This means no side-to-side contact between pieces of the same color.

## Contact with opponents

There are no contact restrictions between different colors.

Your pieces may touch opponents' pieces by corners or by edges. They may be directly adjacent to opponents' pieces, as long as they still follow all rules for your own color.

## Passing

If you cannot place any remaining piece legally, you must pass.

After passing, you take no more turns unless the rules being used at the table allow otherwise. In normal play, a player who is blocked has no legal move and is effectively out for the rest of the game.

## End of the game

The game ends when no player can place another piece.

This includes the case where a player has successfully placed all 21 pieces.

## Basic scoring

For basic scoring, each player counts the number of unit squares in their unplayed pieces.

The player with the lowest number of remaining squares wins.

Example: if you have one five-square piece and one three-square piece left, you have 8 remaining squares.

## Advanced scoring

Under advanced scoring, remaining squares are counted as negative points:

- Each unplayed unit square is worth -1 point.
- A player who places all 21 pieces earns a +15 point bonus.
- If that player's final placed piece was the one-square piece, they earn an additional +5 point bonus.

This means a player can score up to +20 points by placing every piece and saving the one-square piece for last.

The player with the highest score wins under advanced scoring.

## Practical legality examples

A legal same-color placement:

- The new piece touches an older piece of the same color diagonally at one or more corners.
- It does not touch any same-color piece side-to-side.

An illegal same-color placement:

- The new piece touches no same-color piece by a corner.
- The new piece touches a same-color piece along an edge.
- The new piece overlaps any existing piece.
- The new piece extends outside the board.

A legal opponent-color contact:

- Your piece touches an opponent's piece along an edge.
- Your piece touches an opponent's piece at a corner.

Different colors are not restricted by the same edge-contact rule.

## Implementation notes for a digital standard-mode version

For a standard four-player digital implementation, the core validation rules should be:

1. Board size: 20 by 20.
2. Four players: blue, yellow, red, green.
3. Each player has 21 unique pieces.
4. First move must cover the player's assigned corner square.
5. Later moves must have at least one diagonal same-color contact.
6. Any same-color orthogonal contact makes the move illegal.
7. Different-color contact is always allowed if the target squares are empty and the piece is inside the board.
8. A piece cannot be moved after placement.
9. A player with no legal placement must pass.
10. The game ends when all players are unable to move.
11. Basic winner: fewest remaining unit squares.
12. Advanced winner: highest score after negative remaining-square points and applicable bonuses.

## Notes on official rule-sheet differences

Mattel's older English instruction sheet for Blokus states the standard four-player order as blue, yellow, red, green. A later BJV44 English instruction sheet says to choose a starting player and then continue clockwise. For an implementation aiming at the original standard four-player rules, blue, yellow, red, green is the safer fixed rule because it is explicitly tied to the four-player rule text in the older official sheet.

## Official sources checked

- Mattel, `R1983 - Blokus.pdf`, official Blokus English instruction sheet.
- Mattel, `X3128-ENG.pdf`, official Blokus English instruction sheet.
- Mattel, `BJV44-Eng.pdf`, official Blokus English instruction sheet.
- Mattel Consumer Support product page for `BJV44`, Blokus Game.
