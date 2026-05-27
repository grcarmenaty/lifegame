# Daemons & Quests — Design Plan (v2)

> Status: post-council revision. v1 lives in git history. This revision
> resolves the Architect's data-model gaps, demotes Epics, promotes
> failure-handling to a pillar, and confronts the Demolisher's fatal-flaw
> critique with a concrete onboarding answer.

## Thesis (one sentence)

A productivity app whose reward language is **relationship**, not points
— the user authors a small pantheon of self-personifications that ask
things of them in a voice they wrote themselves, and the app's only job
is to keep that conversation alive.

This is not gamification-by-points-and-badges. The pantheon, the voice,
and the apotheosis ritual *are* the entire reward economy. If we drift
toward XP and streaks, we have built a worse Habitica.

## Premise

The user models their life as a small pantheon of **daemons** —
personifications of the life-aspects they choose to track ("Athleta" for
fitness, "Sage" for learning, "Hearth" for relationships, "Coin" for
finances). Each daemon is a questgiver with its own voice, arc, and
powers. Productivity becomes a negotiation with this inner pantheon.

The system is **user-authored**, but the cold start is **guided**: we
ship structure, ritual, and voice scaffolding; the user supplies the
specific content through prompts, not blank pages. (See "Onboarding".)

## Core Entities

### Daemon
- User-defined personification of one life-aspect.
- Carries: `name`, `archetype`, `voice_preset` (oracle | drill-sergeant
  | gentle-mentor | trickster | custom), current `level` (derived, see
  below), active major-quest list, available wish balance.
- The daemon is the **questgiver**, not the avatar. The user is not the
  daemon. The daemon addresses the user in second person. This framing
  is the entire psychological move; do not break it.

### Major Quest
- An overarching goal tied to **exactly one** daemon.
- Has: `title`, `description`, `completion_rule` (one of: N minor-quest
  completions | weighted point total | manual mark-done), `deadline`
  (optional).
- Completion is the only event that triggers apotheosis.

### Minor Quest
- A concrete task that feeds **exactly one** major quest. (Decision:
  strict tree; no shared minors. Reuse is cheap via duplication, and
  shared ownership creates progress-accounting nightmares the Architect
  correctly flagged.)
- Two flavors:
  - **One-off**: single completion, `weight` (default 1).
  - **Repeatable**: `cadence` (per-day | per-week | per-month),
    `target_per_cadence` (e.g., 3), `weight_per_completion`. Each tap
    contributes `weight_per_completion` to the major quest's progress
    total, capped at `target_per_cadence × weight_per_completion` per
    cadence window.
- Editable at any time. (Decision: revision is **not** apotheosis-gated;
  the Architect's cliff critique is real. Apotheosis is the *prompted*
  revision moment, but the user can edit minors on day 3.)

### Wish
- A spendable benefit granted by a daemon.
- v1 ships **one wish type per daemon**: a `boon` — a single
  user-defined favor the user grants themselves in the daemon's voice
  ("Athleta grants you a rest day without guilt"). The user authors the
  boon text at daemon creation.
- Wish **count** scales with level; wish **nature** does not in v1. (We
  ship the verb without the meta-economy. Compromise between Believer
  and Architect; resolves the "wish catalog who-authors" question by
  making the user the catalog, exactly once, at summoning.)
- Lifecycle: wishes accrue at apotheosis, **do not expire**, **do not
  stack across daemons** (each daemon's boon is its own).

### Epic (demoted to optional)
- A user-authored chapter log per daemon, advanced at apotheosis.
- **Decorative, opt-in, not gameplay-required, no empty-state prompt
  that nags.** If the user never writes a chapter, the app never asks.
- Surfaced as a "scripture" view per daemon for users who want to read
  their accumulated narrative back.
- Demoted because: (a) the Architect identified it as the easiest cut,
  (b) the Demolisher correctly called it designer-fan-service for the
  pre-author-everything user, (c) the Believer's "never a gate" rule is
  preserved by making it fully optional.

## The Loop

### Daily surface (new — was missing from v1)
Opening the app shows a single view: **today's quests, grouped by
daemon, in each daemon's voice.** One tap to complete a minor quest.
Long-press to open the daemon's full page (major quests, wish balance,
epic if authored). The daily view is the product; everything else is
secondary navigation.

### Full loop
1. **Summon**: guided onboarding produces one daemon + one major quest
   + 2-3 minor quests + one boon (see "Onboarding").
2. **Quest**: user taps to complete minor quests from the daily view.
   Progress accrues on the parent major quest, narrated in the daemon's
   voice (templated from the voice preset, not free-form authored per
   beat — see "Hidden authoring debt, resolved" below).
3. **Apotheosis**: major quest completes →
   1. Daemon levels up.
   2. New wish slots accrue.
   3. User is prompted (but not required) to retire/refine/replace the
      minor-quest slate and write the next epic chapter.
4. **Wish**: user spends a wish to claim their boon, a one-tap ritual
   with a tactile beat (held-press + in-voice acknowledgment).
5. Loop continues.

## Onboarding: The Summoning Ritual

This section exists because v1's worst failure mode — the Demolisher's
"open it once, see 'Name your first daemon,' never return" — is real.

**Goal:** produce a complete, usable daemon in **under 3 minutes**, with
the user authoring answers (not essays) to 4-5 prompts.

The flow:
1. Dim screen, single prompt: *"What part of your life is asking to be
   heard?"* → free text, one line.
2. *"Give it a name."* → free text.
3. *"How does it speak?"* → pick a voice preset (4 options + custom).
   Show one sample sentence per preset, generated from the user's
   answers to #1-2.
4. *"What's one thing it wants from you in the next month?"* → this
   becomes the first major quest.
5. *"What are 2-3 small acts that would feed that?"* → these become the
   first minor quests.
6. *"What favor will it grant you for the work?"* → this becomes the
   boon.

No epics in the summoning ritual. No archetypes lecture. No fantasy
vocabulary lookup. The daemon exists after step 6. The user is dropped
into the daily view, with the first daemon waiting.

## Failure Handling (promoted from "tension" to pillar)

When a minor quest lapses, the daemon's **voice tone shifts gently** —
warmer when engaged, terser/quieter when ignored. Returning to the app
after a lapse triggers a **reconciliation beat**, not a guilt-trip.
Templated language ships per voice preset.

**Explicit rule:** the app never says "you failed," never tracks streaks
that break audibly, never shows red. Lapses are tonal, not numerical.

This resolves the Believer's "soul of the product" point and the
Demolisher's shame-amplifier critique simultaneously: the daemon
notices, but the user's return is always rewarded with warmth, not
penance.

## Data Model Resolutions (from Architect's audit)

| Question | v1 status | v2 decision |
| --- | --- | --- |
| What is "weighted progress"? | undefined | `weight_per_completion` on the minor quest; major-quest threshold is a sum or count |
| Can a minor quest feed multiple majors? | ambiguous | **No.** Strict tree. Duplicate if you need reuse. |
| What if the user writes no epic? | undefined | Epic is fully optional; apotheosis no-ops the chapter-unlock prompt silently |
| Is `level` stored or derived? | both | **Derived** from completed-major-quest count. Single source of truth. |
| Do wishes carry over / stack / expire? | undefined | Carry over. Do not stack across daemons. Do not expire. |
| Repeatable completion semantics? | undefined | Tap per completion; capped per cadence window; week resets Monday local time |

## MVP Cut (from Architect, sharpened)

**Ship in v1:**
- Daemon, Major Quest, Minor Quest (one-off + repeatable), derived level
- Daily view + per-daemon page
- Apotheosis prompt (level + wish accrual + revision prompt)
- One boon per daemon
- Voice presets with templated voice-of-daemon strings
- Guided summoning ritual

**Defer to v2+:**
- Multiple wish types per daemon / wish-nature scaling
- Cross-daemon `@Hearth` reference tokens (cheap; second-priority post-v1)
- Weekly "council" view across all daemons
- Export as bound "tome"
- Any cross-daemon mechanic (borrow progress, conflict, etc.)
- Seasons-of-the-self / daemon mortality

## Hidden Authoring Debt, Resolved

The Architect was right that v1 quietly assumed the user would write
every notification beat. v2 says:

- The **user authors the daemon's name, archetype, boon, quest titles,
  and (optionally) epic chapters.** That's it.
- The app's **voice presets** ship templated lines for: greeting,
  quest-completion acknowledgment, lapse reconciliation, apotheosis
  speech. Each preset has a handful of variants per slot. No LLM
  dependency in v1. The user can override any line, but isn't required
  to.
- **Epic chapters** are pure user prose, opt-in, never prompted by the
  app unless the user opens the epic view.

## Open Risks (named honestly, per Skeptic's intrinsic-vs-execution split)

**Intrinsic risks** (the design itself bets on these; cannot be patched):
- Even guided, summoning is a higher-friction onboarding than zero-input
  habit apps. We are betting the resulting daemon is worth the 3
  minutes.
- "Daemon," "pantheon," "apotheosis" vocabulary will repel some users.
  We are betting the audience that loves it is large enough.
- Narrative framing's behavior-change effect is empirically weak. We are
  betting that the *relationship* framing (mood-decay, reconciliation)
  is doing different work than the *fantasy* framing Habitica tried.

**Execution risks** (solvable with mechanism design, not yet solved):
- Voice-preset templated lines must not feel canned by week 3. Needs
  variant volume + an "I'll write my own" escape hatch per slot.
- Multi-daemon attention collapse (Skeptic): mitigate by encouraging
  one-daemon-first via onboarding flow and not surfacing "add daemon"
  prominently until at least one apotheosis has occurred.
- Self-imposed wish economy is self-cheatable: accept it. The boon is a
  permission, not a currency. The ritual matters more than the
  enforcement.

## Confronting the Demolisher's Verdict

The Demolisher predicted: *"Beloved by 400 power users on a subreddit,
abandoned by everyone else by day three."*

The v2 changes that try to falsify that prediction:
- Guided summoning replaces blank-page authoring as the first
  experience.
- Epics are demoted from "everyone writes a saga" to "opt-in scripture
  view."
- Failure handling is tonal, not punitive — removing the
  shame-amplifier mechanic.
- The daily view is the product surface, not the authoring tools.

If v2 still produces the Demolisher's epitaph, the intrinsic bets above
were wrong. Acceptable; we'd learn that cheaply if the MVP cut holds.

## Deliberately Out of Scope for v1

- Multiplayer / social features
- External integrations beyond export
- Monetization model
- Cross-daemon mechanics (beyond `@token` flavor references, which are
  v2 candidates)
- A shipped quest catalog
- Any LLM dependency
