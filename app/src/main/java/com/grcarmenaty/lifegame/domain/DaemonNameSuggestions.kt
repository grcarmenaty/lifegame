package com.grcarmenaty.lifegame.domain

/**
 * Name suggestions per (voice-preset, life-theme) cell — at least three
 * per combination, in the preset's voice and shaped to the theme.
 *
 * Surfaced in the summoning ritual after both preset and theme are
 * chosen. The user can tap a chip to fill the name field or write
 * their own; suggestions never overwrite what they've typed.
 *
 * When [LifeTheme] is null (the user picked "Other"), [forPair] returns
 * an empty list — the UI falls back to the free-text input.
 */
object DaemonNameSuggestions {

    fun forPair(preset: VoicePreset, theme: LifeTheme?): List<String> {
        if (theme == null) return emptyList()
        return TABLE[preset to theme].orEmpty()
    }

    private val TABLE: Map<Pair<VoicePreset, LifeTheme>, List<String>> =
        buildMap {

            // ---------- DRILL_SERGEANT (military, gruff) ----------
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.EXERCISE,
                "Sergeant Iron", "Reveille", "Hardline")
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.SLEEP,
                "Curfew", "Taps", "Bunk Order")
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.NUTRITION,
                "Mess", "Ration", "Field Cook")
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.HYDRATION,
                "Canteen", "Reservoir", "Hydra")
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.TIDYNESS,
                "Inspection", "Footlocker", "Field Order")
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.FINANCES,
                "Paymaster", "Stocktake", "Treasury")
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.CAREER,
                "Rank", "Promotion", "Theater")
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.LEARNING,
                "Bootcamp", "Recruit", "Drill Manual")
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.WRITING,
                "Logbook", "After-Action", "Dispatch")
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.MEDITATION,
                "Sentry", "Standby", "Watchpost")
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.LOVE,
                "Standing Order", "Garrison", "Vow")
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.FAMILY,
                "Roll Call", "Reunion", "Kin Detail")
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.FRIENDSHIP,
                "Squad", "Fireteam", "Wingman")
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.GRATITUDE,
                "Citation", "Commendation", "Medal")
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.HOBBIES,
                "Off-Duty", "Furlough", "Side Drill")
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.OUTDOORS,
                "Recon", "Field March", "Long Tour")
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.DIGITAL,
                "Radio Silence", "Comms Lockdown", "Signal Discipline")
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.ADMIN,
                "Paperwork Detail", "Filing Order", "Quartermaster")
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.RECOVERY,
                "Sick Bay", "Convalescence", "R&R")
            add(VoicePreset.DRILL_SERGEANT, LifeTheme.BOUNDARIES,
                "Perimeter", "No-Go", "Standing No")

            // ---------- COACH (sports, tactical) ----------
            add(VoicePreset.COACH, LifeTheme.EXERCISE,
                "Coach Iron", "Captain", "Trainer")
            add(VoicePreset.COACH, LifeTheme.SLEEP,
                "Recovery Coach", "Halftime", "Bench")
            add(VoicePreset.COACH, LifeTheme.NUTRITION,
                "Fuel Captain", "Plate Coach", "Skipper")
            add(VoicePreset.COACH, LifeTheme.HYDRATION,
                "Hydration Coach", "Bottle Captain", "Water Skip")
            add(VoicePreset.COACH, LifeTheme.TIDYNESS,
                "Locker Coach", "Equipment", "Clubhouse")
            add(VoicePreset.COACH, LifeTheme.FINANCES,
                "Salary Cap", "Treasurer", "Front Office")
            add(VoicePreset.COACH, LifeTheme.CAREER,
                "Head Coach", "Manager", "GM")
            add(VoicePreset.COACH, LifeTheme.LEARNING,
                "Film Coach", "Scout", "Tape")
            add(VoicePreset.COACH, LifeTheme.WRITING,
                "Playbook", "Chalkboard", "Notes Coach")
            add(VoicePreset.COACH, LifeTheme.MEDITATION,
                "Halftime Sit", "Stillness Coach", "Pre-Game")
            add(VoicePreset.COACH, LifeTheme.LOVE,
                "Pairs Coach", "Tandem", "Doubles")
            add(VoicePreset.COACH, LifeTheme.FAMILY,
                "Family Coach", "Lineup", "Roster")
            add(VoicePreset.COACH, LifeTheme.FRIENDSHIP,
                "Squad Captain", "Team Coach", "Bench Mob")
            add(VoicePreset.COACH, LifeTheme.GRATITUDE,
                "Highlight", "Game Ball", "Recap")
            add(VoicePreset.COACH, LifeTheme.HOBBIES,
                "Sandlot", "Pickup", "Backyard Coach")
            add(VoicePreset.COACH, LifeTheme.OUTDOORS,
                "Trail Coach", "Field Captain", "Cross-Country")
            add(VoicePreset.COACH, LifeTheme.DIGITAL,
                "Screen Coach", "Off-Court", "Bench Phone")
            add(VoicePreset.COACH, LifeTheme.ADMIN,
                "Front Office", "Roster Manager", "Logistics")
            add(VoicePreset.COACH, LifeTheme.RECOVERY,
                "Trainer", "Bench Captain", "Recovery Coach")
            add(VoicePreset.COACH, LifeTheme.BOUNDARIES,
                "Whistle", "Foul", "Hard Line")

            // ---------- HERMIT (quiet, old) ----------
            add(VoicePreset.HERMIT, LifeTheme.EXERCISE,
                "Footpath", "Pace", "Slow Step")
            add(VoicePreset.HERMIT, LifeTheme.SLEEP,
                "Lamp", "Quiet Hour", "Vessel")
            add(VoicePreset.HERMIT, LifeTheme.NUTRITION,
                "Hearth", "Bowl", "Larder")
            add(VoicePreset.HERMIT, LifeTheme.HYDRATION,
                "Kettle", "Well", "Spring")
            add(VoicePreset.HERMIT, LifeTheme.TIDYNESS,
                "Sweep", "Threshold", "Broom")
            add(VoicePreset.HERMIT, LifeTheme.FINANCES,
                "Ledger", "Tally", "Small Coin")
            add(VoicePreset.HERMIT, LifeTheme.CAREER,
                "Long Room", "Workbench", "Slow Work")
            add(VoicePreset.HERMIT, LifeTheme.LEARNING,
                "Margin", "Page", "Reader")
            add(VoicePreset.HERMIT, LifeTheme.WRITING,
                "Quill", "Ink", "Scribe")
            add(VoicePreset.HERMIT, LifeTheme.MEDITATION,
                "Cushion", "Breath", "Stillness")
            add(VoicePreset.HERMIT, LifeTheme.LOVE,
                "Companion", "Far Kin", "Hearthmate")
            add(VoicePreset.HERMIT, LifeTheme.FAMILY,
                "Kin Room", "Lineage", "Old House")
            add(VoicePreset.HERMIT, LifeTheme.FRIENDSHIP,
                "Pen Friend", "Far Friend", "Hearth Guest")
            add(VoicePreset.HERMIT, LifeTheme.GRATITUDE,
                "Small Grace", "Noticer", "Witness")
            add(VoicePreset.HERMIT, LifeTheme.HOBBIES,
                "Idle Hands", "Slow Craft", "Workbench Hours")
            add(VoicePreset.HERMIT, LifeTheme.OUTDOORS,
                "Path", "Wanderer", "Way")
            add(VoicePreset.HERMIT, LifeTheme.DIGITAL,
                "Latch", "Quiet Phone", "Drawer")
            add(VoicePreset.HERMIT, LifeTheme.ADMIN,
                "Pile", "Slow Pile", "Filing Hour")
            add(VoicePreset.HERMIT, LifeTheme.RECOVERY,
                "Bench", "Long Rest", "Tide")
            add(VoicePreset.HERMIT, LifeTheme.BOUNDARIES,
                "Hermitage", "Door", "Quiet No")

            // ---------- POET (lyrical) ----------
            add(VoicePreset.POET, LifeTheme.EXERCISE,
                "Cadence", "Stride", "Pulse")
            add(VoicePreset.POET, LifeTheme.SLEEP,
                "Nocturne", "Lullaby", "Dream")
            add(VoicePreset.POET, LifeTheme.NUTRITION,
                "Bread", "Feast", "Larder Song")
            add(VoicePreset.POET, LifeTheme.HYDRATION,
                "Brook", "Rivulet", "Spring Song")
            add(VoicePreset.POET, LifeTheme.TIDYNESS,
                "Composer", "Trim", "Order")
            add(VoicePreset.POET, LifeTheme.FINANCES,
                "Coin Song", "Accounting", "Ledger Verse")
            add(VoicePreset.POET, LifeTheme.CAREER,
                "Vocation", "Craft", "Mason")
            add(VoicePreset.POET, LifeTheme.LEARNING,
                "Scholar", "Footnote", "Page Lover")
            add(VoicePreset.POET, LifeTheme.WRITING,
                "Stanza", "Sonnet", "Verse")
            add(VoicePreset.POET, LifeTheme.MEDITATION,
                "Caesura", "Pause", "Long Breath")
            add(VoicePreset.POET, LifeTheme.LOVE,
                "Beloved", "Petal", "Aubade")
            add(VoicePreset.POET, LifeTheme.FAMILY,
                "Hearth Verse", "Refrain", "Lineage")
            add(VoicePreset.POET, LifeTheme.FRIENDSHIP,
                "Chorus", "Companion Verse", "Antiphon")
            add(VoicePreset.POET, LifeTheme.GRATITUDE,
                "Praise", "Hymn", "Litany")
            add(VoicePreset.POET, LifeTheme.HOBBIES,
                "Idyll", "Pastime", "Play Song")
            add(VoicePreset.POET, LifeTheme.OUTDOORS,
                "Wanderer", "Vista", "Field Song")
            add(VoicePreset.POET, LifeTheme.DIGITAL,
                "White Space", "Quiet Page", "Bare Line")
            add(VoicePreset.POET, LifeTheme.ADMIN,
                "Index", "Annal", "Margin Notes")
            add(VoicePreset.POET, LifeTheme.RECOVERY,
                "Convalescence", "Soft Hour", "Slow Refrain")
            add(VoicePreset.POET, LifeTheme.BOUNDARIES,
                "Line Break", "Em Dash", "Stop")

            // ---------- THERAPIST (warm, listening) ----------
            add(VoicePreset.THERAPIST, LifeTheme.EXERCISE,
                "Steady Hand", "Pace Listener", "Mover")
            add(VoicePreset.THERAPIST, LifeTheme.SLEEP,
                "Anchor", "Lull", "Soft Hour")
            add(VoicePreset.THERAPIST, LifeTheme.NUTRITION,
                "Nourish", "Plate Tender", "Holding")
            add(VoicePreset.THERAPIST, LifeTheme.HYDRATION,
                "Cup", "Sip", "Tender Water")
            add(VoicePreset.THERAPIST, LifeTheme.TIDYNESS,
                "Room Tender", "Steady", "Soft Order")
            add(VoicePreset.THERAPIST, LifeTheme.FINANCES,
                "Patience", "Slow Ledger", "Holding Coin")
            add(VoicePreset.THERAPIST, LifeTheme.CAREER,
                "Witness", "Steady Craft", "Working")
            add(VoicePreset.THERAPIST, LifeTheme.LEARNING,
                "Curious", "Slow Reader", "Open")
            add(VoicePreset.THERAPIST, LifeTheme.WRITING,
                "Notebook", "Soft Pen", "Notes")
            add(VoicePreset.THERAPIST, LifeTheme.MEDITATION,
                "Breath Tender", "Stillness", "Soft Sit")
            add(VoicePreset.THERAPIST, LifeTheme.LOVE,
                "Holding", "Tender", "Soft Bond")
            add(VoicePreset.THERAPIST, LifeTheme.FAMILY,
                "Kin Tender", "Soft Kin", "Family Witness")
            add(VoicePreset.THERAPIST, LifeTheme.FRIENDSHIP,
                "Listener", "Friend-Witness", "Soft Squad")
            add(VoicePreset.THERAPIST, LifeTheme.GRATITUDE,
                "Noticer", "Soft Ledger", "Praise")
            add(VoicePreset.THERAPIST, LifeTheme.HOBBIES,
                "Soft Play", "Tender Craft", "Play Hands")
            add(VoicePreset.THERAPIST, LifeTheme.OUTDOORS,
                "Slow Step", "Air Tender", "Soft Path")
            add(VoicePreset.THERAPIST, LifeTheme.DIGITAL,
                "Quiet Phone", "Tender Hour", "Soft Off")
            add(VoicePreset.THERAPIST, LifeTheme.ADMIN,
                "Patience", "Soft Pile", "Slow Hand")
            add(VoicePreset.THERAPIST, LifeTheme.RECOVERY,
                "Tender", "Holding", "Soft Bench")
            add(VoicePreset.THERAPIST, LifeTheme.BOUNDARIES,
                "Soft No", "Witness", "Holding Line")

            // ---------- GENTLE_MENTOR (caring, guide) ----------
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.EXERCISE,
                "Steady Mentor", "Pace Companion", "Body Guide")
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.SLEEP,
                "Quiet Mentor", "Lull Guide", "Soft Companion")
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.NUTRITION,
                "Plate Mentor", "Tender Cook", "Nourish Guide")
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.HYDRATION,
                "Sip Friend", "Cup Mentor", "Hydra Guide")
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.TIDYNESS,
                "Room Mentor", "Tidy Guide", "Sweep Friend")
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.FINANCES,
                "Ledger Mentor", "Coin Friend", "Tally Guide")
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.CAREER,
                "Workmate", "Craft Mentor", "Slow Guide")
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.LEARNING,
                "Tutor", "Page Mentor", "Reading Friend")
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.WRITING,
                "Editor", "Page Friend", "Margin Mentor")
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.MEDITATION,
                "Breath Friend", "Sit Mentor", "Soft Guide")
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.LOVE,
                "Pair Mentor", "Vow Friend", "Tender Guide")
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.FAMILY,
                "Kin Mentor", "Hearth Friend", "Old Guide")
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.FRIENDSHIP,
                "Squad Mentor", "Friend-Mentor", "Circle Guide")
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.GRATITUDE,
                "Notice Friend", "Praise Mentor", "Soft Witness")
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.HOBBIES,
                "Play Mentor", "Craft Friend", "Hands Guide")
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.OUTDOORS,
                "Path Friend", "Walk Mentor", "Trail Guide")
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.DIGITAL,
                "Off-Screen", "Quiet Mentor", "Bench Phone")
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.ADMIN,
                "Pile Mentor", "Form Friend", "Slow Mentor")
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.RECOVERY,
                "Rest Mentor", "Slow Friend", "Bench Guide")
            add(VoicePreset.GENTLE_MENTOR, LifeTheme.BOUNDARIES,
                "Voice Mentor", "No-Friend", "Soft Line")

            // ---------- ORACLE (cryptic, seeing) ----------
            add(VoicePreset.ORACLE, LifeTheme.EXERCISE,
                "Tides", "Path-Seer", "Body Augur")
            add(VoicePreset.ORACLE, LifeTheme.SLEEP,
                "Dream-Seer", "Night Augur", "Moonkeeper")
            add(VoicePreset.ORACLE, LifeTheme.NUTRITION,
                "Hearthsight", "Bread Augur", "Salt Seer")
            add(VoicePreset.ORACLE, LifeTheme.HYDRATION,
                "Tidewatcher", "Spring Augur", "Wellsight")
            add(VoicePreset.ORACLE, LifeTheme.TIDYNESS,
                "Hearth-Augur", "Sweep-Seer", "Threshold Reader")
            add(VoicePreset.ORACLE, LifeTheme.FINANCES,
                "Coin Augur", "Tide-Counter", "Reckoner")
            add(VoicePreset.ORACLE, LifeTheme.CAREER,
                "Vocation-Seer", "Path Augur", "Craft Oracle")
            add(VoicePreset.ORACLE, LifeTheme.LEARNING,
                "Pythia", "Page-Reader", "Lorekeeper")
            add(VoicePreset.ORACLE, LifeTheme.WRITING,
                "Stylus-Seer", "Ink Augur", "Inscriber")
            add(VoicePreset.ORACLE, LifeTheme.MEDITATION,
                "Stillness Seer", "Breath Oracle", "Long Watcher")
            add(VoicePreset.ORACLE, LifeTheme.LOVE,
                "Knotseer", "Heart Augur", "Tide-Bound")
            add(VoicePreset.ORACLE, LifeTheme.FAMILY,
                "Lineage Seer", "Kin Augur", "Bloodtide")
            add(VoicePreset.ORACLE, LifeTheme.FRIENDSHIP,
                "Far-Sight", "Friend Augur", "Long Tide")
            add(VoicePreset.ORACLE, LifeTheme.GRATITUDE,
                "Litany", "Praise Augur", "Goldsight")
            add(VoicePreset.ORACLE, LifeTheme.HOBBIES,
                "Play Augur", "Idle Seer", "Sandsight")
            add(VoicePreset.ORACLE, LifeTheme.OUTDOORS,
                "Pathseer", "Sky Augur", "Wind Reader")
            add(VoicePreset.ORACLE, LifeTheme.DIGITAL,
                "Veil", "Shadow Reader", "Mist Augur")
            add(VoicePreset.ORACLE, LifeTheme.ADMIN,
                "Reckoner", "Ledger Seer", "Threadkeeper")
            add(VoicePreset.ORACLE, LifeTheme.RECOVERY,
                "Tide-Rest", "Healing Augur", "Slow Seer")
            add(VoicePreset.ORACLE, LifeTheme.BOUNDARIES,
                "Veilkeeper", "Boundary Seer", "Wardline")

            // ---------- CHEERLEADER (bright, peppy) ----------
            add(VoicePreset.CHEERLEADER, LifeTheme.EXERCISE,
                "Spark", "Pep", "Cheer")
            add(VoicePreset.CHEERLEADER, LifeTheme.SLEEP,
                "Sunny Dreams", "Cozy", "Bedtime Glow")
            add(VoicePreset.CHEERLEADER, LifeTheme.NUTRITION,
                "Snack Spirit", "Bites", "Yum")
            add(VoicePreset.CHEERLEADER, LifeTheme.HYDRATION,
                "Sips", "Splash", "Bottle Buddy")
            add(VoicePreset.CHEERLEADER, LifeTheme.TIDYNESS,
                "Sparkle", "Tidy Bee", "Sweep-Joy")
            add(VoicePreset.CHEERLEADER, LifeTheme.FINANCES,
                "Coin Joy", "Penny Pal", "Saver")
            add(VoicePreset.CHEERLEADER, LifeTheme.CAREER,
                "Champ", "Star", "Boss Energy")
            add(VoicePreset.CHEERLEADER, LifeTheme.LEARNING,
                "Bright Spark", "Brainy", "Curious")
            add(VoicePreset.CHEERLEADER, LifeTheme.WRITING,
                "Page Sparkle", "Inky", "Wordy")
            add(VoicePreset.CHEERLEADER, LifeTheme.MEDITATION,
                "Calm Spark", "Bubble", "Glow")
            add(VoicePreset.CHEERLEADER, LifeTheme.LOVE,
                "Hearts", "Love Spark", "Sweetie")
            add(VoicePreset.CHEERLEADER, LifeTheme.FAMILY,
                "Hug", "Kin Cheer", "Family Spark")
            add(VoicePreset.CHEERLEADER, LifeTheme.FRIENDSHIP,
                "Buddy", "Pal", "Squad Spark")
            add(VoicePreset.CHEERLEADER, LifeTheme.GRATITUDE,
                "Yay", "Praise Pal", "Joy")
            add(VoicePreset.CHEERLEADER, LifeTheme.HOBBIES,
                "Play Sparkle", "Joybringer", "Funny")
            add(VoicePreset.CHEERLEADER, LifeTheme.OUTDOORS,
                "Sunny", "Skybound", "Stomper")
            add(VoicePreset.CHEERLEADER, LifeTheme.DIGITAL,
                "Off-Sparkle", "Glow Free", "Eye-Up")
            add(VoicePreset.CHEERLEADER, LifeTheme.ADMIN,
                "Tick-Off", "Boxer", "List Pal")
            add(VoicePreset.CHEERLEADER, LifeTheme.RECOVERY,
                "Snug", "Cozy Cloud", "Recoverpup")
            add(VoicePreset.CHEERLEADER, LifeTheme.BOUNDARIES,
                "No-Smiley", "Power No", "Sparkly Stop")

            // ---------- STOIC (classical, grave) ----------
            add(VoicePreset.STOIC, LifeTheme.EXERCISE,
                "Atlas", "Reed", "Marcus")
            add(VoicePreset.STOIC, LifeTheme.SLEEP,
                "Nyx", "Hypnos", "Reed Night")
            add(VoicePreset.STOIC, LifeTheme.NUTRITION,
                "Salt", "Bread", "Sufficient")
            add(VoicePreset.STOIC, LifeTheme.HYDRATION,
                "Spring", "Ewer", "Sufficient Cup")
            add(VoicePreset.STOIC, LifeTheme.TIDYNESS,
                "Order", "Pillar", "Stone Sweep")
            add(VoicePreset.STOIC, LifeTheme.FINANCES,
                "Reckoner", "Ledger Stone", "Sufficient Coin")
            add(VoicePreset.STOIC, LifeTheme.CAREER,
                "Vocation", "Atlas Work", "Pillar Craft")
            add(VoicePreset.STOIC, LifeTheme.LEARNING,
                "Reader", "Marcus Studied", "Scholar Reed")
            add(VoicePreset.STOIC, LifeTheme.WRITING,
                "Stylus", "Notes-of-Self", "Page Stone")
            add(VoicePreset.STOIC, LifeTheme.MEDITATION,
                "Breath Pillar", "Stillness", "Atlas Sit")
            add(VoicePreset.STOIC, LifeTheme.LOVE,
                "Vow", "Companion", "Steady Bond")
            add(VoicePreset.STOIC, LifeTheme.FAMILY,
                "Lineage", "Hearth Stone", "Old House")
            add(VoicePreset.STOIC, LifeTheme.FRIENDSHIP,
                "Companion Reed", "Steady Friend", "Pillar Friend")
            add(VoicePreset.STOIC, LifeTheme.GRATITUDE,
                "Sufficient Praise", "Stone Gratitude", "Acceptance")
            add(VoicePreset.STOIC, LifeTheme.HOBBIES,
                "Pastime", "Quiet Hands", "Reed Play")
            add(VoicePreset.STOIC, LifeTheme.OUTDOORS,
                "Path", "Long Way", "Stone Walk")
            add(VoicePreset.STOIC, LifeTheme.DIGITAL,
                "Stylus Down", "Silence", "Stone Phone")
            add(VoicePreset.STOIC, LifeTheme.ADMIN,
                "Reckoner Form", "Tablet", "Stoic Pile")
            add(VoicePreset.STOIC, LifeTheme.RECOVERY,
                "Rest", "Convalescence", "Acceptance Bench")
            add(VoicePreset.STOIC, LifeTheme.BOUNDARIES,
                "Wall", "Stone No", "Border")

            // ---------- TRICKSTER (playful, mischievous) ----------
            add(VoicePreset.TRICKSTER, LifeTheme.EXERCISE,
                "Imp", "Pacer", "Sneaker")
            add(VoicePreset.TRICKSTER, LifeTheme.SLEEP,
                "Drowse-Imp", "Pillow Loki", "Snooze")
            add(VoicePreset.TRICKSTER, LifeTheme.NUTRITION,
                "Crumb", "Snackster", "Larder Imp")
            add(VoicePreset.TRICKSTER, LifeTheme.HYDRATION,
                "Sneak-Sip", "Splash Imp", "Carafe")
            add(VoicePreset.TRICKSTER, LifeTheme.TIDYNESS,
                "Tidy-Up Loki", "Sweep Imp", "Fox-Floor")
            add(VoicePreset.TRICKSTER, LifeTheme.FINANCES,
                "Coinster", "Pickpocket", "Penny Imp")
            add(VoicePreset.TRICKSTER, LifeTheme.CAREER,
                "Imposter Boss", "Cubicle Loki", "Promotion Imp")
            add(VoicePreset.TRICKSTER, LifeTheme.LEARNING,
                "Book Imp", "Quiz Loki", "Smartypants")
            add(VoicePreset.TRICKSTER, LifeTheme.WRITING,
                "Quill Imp", "Margin Loki", "Wordfox")
            add(VoicePreset.TRICKSTER, LifeTheme.MEDITATION,
                "Quiet Imp", "Breath Loki", "Mischief Sit")
            add(VoicePreset.TRICKSTER, LifeTheme.LOVE,
                "Love Imp", "Heart Fox", "Kiss-Trick")
            add(VoicePreset.TRICKSTER, LifeTheme.FAMILY,
                "Cousin Imp", "Family Loki", "Kin Fox")
            add(VoicePreset.TRICKSTER, LifeTheme.FRIENDSHIP,
                "Mischief Mate", "Loki Pal", "Fox Friend")
            add(VoicePreset.TRICKSTER, LifeTheme.GRATITUDE,
                "Thank-You Trick", "Imp Praise", "Gold Fox")
            add(VoicePreset.TRICKSTER, LifeTheme.HOBBIES,
                "Mischief Hands", "Hobby Imp", "Crafty Fox")
            add(VoicePreset.TRICKSTER, LifeTheme.OUTDOORS,
                "Trail Imp", "Path Loki", "Wild Fox")
            add(VoicePreset.TRICKSTER, LifeTheme.DIGITAL,
                "Off-Screen Loki", "Phone Snatcher", "Drawer Imp")
            add(VoicePreset.TRICKSTER, LifeTheme.ADMIN,
                "Forms Loki", "Pile Imp", "Filing Fox")
            add(VoicePreset.TRICKSTER, LifeTheme.RECOVERY,
                "Lazy Imp", "Sneaky Rest", "Couch Loki")
            add(VoicePreset.TRICKSTER, LifeTheme.BOUNDARIES,
                "No-Imp", "Sneak-Refuse", "Cheeky No")
        }

    private fun MutableMap<Pair<VoicePreset, LifeTheme>, List<String>>.add(
        preset: VoicePreset,
        theme: LifeTheme,
        vararg names: String,
    ) {
        put(preset to theme, names.toList())
    }
}
