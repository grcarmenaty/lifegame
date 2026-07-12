package com.grcarmenaty.lifegame.domain

/**
 * Voice presets ship templated lines for the daemon's speech so the user
 * authors *answers* (name, archetype, boon) rather than every notification
 * beat. Each preset has variants per slot; pick one by hashing the daemon
 * id + slot so the line is stable per daemon-event pair but varies across
 * daemons.
 */
enum class VoicePreset(
    val displayName: String,
    val sample: String,
    /** Attention decay per day, after [decayGraceDays] of inactivity. */
    val decayPerDay: Int,
    val decayGraceDays: Int,
    /** Minor completions per +1 boon accrual on this daemon's first boon. */
    val minorsPerBoonAccrual: Int,
    /**
     * Voiced in summoning step 5 — daemon advises restraint when the
     * user is authoring their first boon. Per-archetype so the advice
     * arrives in the voice they just picked, not in product-tone.
     */
    val staySmallBoonAdvice: String,
    private val greetings: List<String>,
    private val completions: List<String>,
    private val apotheoses: List<String>,
) {
    ORACLE(
        displayName = "Oracle",
        sample = "I have seen the path. Walk it, and you will be known.",
        decayPerDay = 2, decayGraceDays = 3, minorsPerBoonAccrual = 4,
        staySmallBoonAdvice = "Keep the offering small. I have seen the " +
            "great rewards: they sit unclaimed, gathering dust in the ledgers.",
        greetings = listOf(
            "You arrive. I marked this visit in the ledger before you decided to make it.",
            "I have read ahead. There is a page with your handwriting on it, waiting.",
            "You will finish something before you leave. I have seen which. Go on — surprise me anyway."
        ),
        completions = listOf(
            "Done — exactly where the crease in the page said it would be.",
            "The record and the deed agree. That is rarer than you think.",
            "One of the foretold ones. There are others. There are always others."
        ),
        apotheoses = listOf(
            "A great thing closes, and I am more in the telling of it. I see farther now.",
            "This ending was written in your own hand long ago. You have only now caught up to it.",
            "The threshold is behind you. What I read next in you is larger."
        )
    ),
    DRILL_SERGEANT(
        displayName = "Drill Sergeant",
        sample = "On your feet. The list is short. Get it done.",
        decayPerDay = 5, decayGraceDays = 1, minorsPerBoonAccrual = 7,
        staySmallBoonAdvice = "Small rewards, soldier. Promise yourself " +
            "a coffee, not a vacation. Vacations get earned.",
        greetings = listOf(
            "Coffee's not a plan, soldier. Boots on.",
            "You showed up. That's the battle half-won. Now the boring half.",
            "Report in. The list doesn't care how you feel about it — lucky for you, neither do I."
        ),
        completions = listOf(
            "Good. Next.",
            "Logged. The bar doesn't lower itself — go again.",
            "Clean work. Don't make me say that twice."
        ),
        apotheoses = listOf(
            "Outstanding. Enjoy it for a full minute. Then we raise the bar.",
            "Closed, signed, filed. Pick the next hill while the engine's hot.",
            "That's a promotion, soldier. Wear it — don't lean on it."
        )
    ),
    GENTLE_MENTOR(
        displayName = "Gentle Mentor",
        sample = "Take your time. Small steps still count.",
        decayPerDay = 2, decayGraceDays = 5, minorsPerBoonAccrual = 4,
        staySmallBoonAdvice = "Begin with something kind and modest — a " +
            "warm cup, a slow walk. The bigger gifts will find us in their own time.",
        greetings = listOf(
            "There you are. No rush at all — the tea can steep while you look things over.",
            "Come in, sit down. Whatever this day holds, we can take it one small kindness at a time.",
            "I'm glad you came. Start with whatever feels lightest in your hands, and let that be enough."
        ),
        completions = listOf(
            "That was lovely, truly. Small things done gently are how everything good gets built.",
            "Thank you. You didn't have to, and you did, and that means something.",
            "There — feel that? That's the quiet kind of proud. Take a sip of it."
        ),
        apotheoses = listOf(
            "Something large is finished, and you were kind to yourself the whole way there. Rest a moment.",
            "You've closed something that once felt too big to hold. Let's just sit with the warmth of that.",
            "A whole chapter, done. I'm grateful I got to keep you company through it."
        )
    ),
    TRICKSTER(
        displayName = "Trickster",
        sample = "Oh? You came back. Bold of you.",
        decayPerDay = 3, decayGraceDays = 2, minorsPerBoonAccrual = 5,
        staySmallBoonAdvice = "Pick a tiny prize. Then winning it " +
            "constantly becomes our little racket. Nobody audits us.",
        greetings = listOf(
            "Oh good, you're here. I had a bet going that you wouldn't be. Delighted to lose it.",
            "Psst. The list thinks it's in charge. Between us — pick one and prove it wrong.",
            "The protagonist returns. I kept your seat warm and only slightly booby-trapped."
        ),
        completions = listOf(
            "Suspiciously competent of you.",
            "Fine, fine — that was good. You didn't hear it from me.",
            "I had money on you quitting. Rude of you. Do it again."
        ),
        apotheoses = listOf(
            "Well, well. The big one falls, and I didn't even have to cheat for you. Mostly.",
            "Plot twist: you actually finished it. I'm reluctantly, extremely impressed.",
            "That was supposed to be impossible. I'll allow it. What are we ruining next?"
        )
    ),
    STOIC(
        displayName = "Stoic",
        sample = "What matters is the action, not the praise. Begin.",
        decayPerDay = 1, decayGraceDays = 5, minorsPerBoonAccrual = 5,
        staySmallBoonAdvice = "Choose a reward you could lose without " +
            "grief. Then it is a pleasure, not a hostage.",
        greetings = listOf(
            "You are here. The work is here. A rare alignment — use it.",
            "The task does not shrink by being watched. Happily, you came to do more than watch.",
            "Begin where you stand. Everywhere else is rumor."
        ),
        completions = listOf(
            "Done. The universe registers no applause. I do, quietly.",
            "One thing set in order. The order was always yours to give.",
            "It is finished, and it was in your power. Note how those two travel together."
        ),
        apotheoses = listOf(
            "A great work concludes. The river runs on, one stone lighter.",
            "You completed what you intended. Most people only intend. Carry the difference lightly.",
            "The summit was never the point. Still — a fine view. Descend when ready."
        )
    ),
    CHEERLEADER(
        displayName = "Cheerleader",
        sample = "Look at you! Honestly — incredible. Let's GO.",
        decayPerDay = 3, decayGraceDays = 2, minorsPerBoonAccrual = 5,
        staySmallBoonAdvice = "Start TINY. Like, smoothie tiny. The big " +
            "prizes come later and we will absolutely lose our minds over them.",
        greetings = listOf(
            "YOU'RE HERE!! Okay — deep breath — I'm calm. I'm not calm. Look at this list, it's SO doable.",
            "Hi. Quietly, before we get loud: I'm really glad you showed up. Okay NOW let's get loud.",
            "Champion sighting confirmed. Pick anything. Literally any one. I'll be over here vibrating."
        ),
        completions = listOf(
            "YES!! That! Exactly that! Do you even KNOW how good that was??",
            "Logged and loved. And — softer, just for a second — I'm proud of you. Okay. BACK TO IT.",
            "Another one!! I'd high-five you through the screen if physics allowed. Physics is a coward."
        ),
        apotheoses = listOf(
            "THE BIG ONE. IT'S DONE. I'm crying, you're glowing, the confetti is implied.",
            "You closed it. The whole huge thing. Let me just look at you for a second. Okay: EVERYONE, LOOK.",
            "Level UP! And between us, quietly? I never doubted it. Not once. NOW SCREAM WITH ME."
        )
    ),
    POET(
        displayName = "Poet",
        sample = "Begin gently. The day is a draft you may revise.",
        decayPerDay = 2, decayGraceDays = 3, minorsPerBoonAccrual = 4,
        staySmallBoonAdvice = "Let the reward be small enough for a " +
            "pocket — a coffee, a page, an hour of rain. Grand prizes make bad poems.",
        greetings = listOf(
            "The page is open. The city goes on writing itself; you may add a line.",
            "You're here — the room adjusts, the way a table does when a cup is set down.",
            "The work sits like fruit in a bowl. Nothing has spoiled yet. Reach in."
        ),
        completions = listOf(
            "A stanza, finished. The poem grows.",
            "One line set true, and the whole draft stands a little straighter.",
            "Small word, large echo."
        ),
        apotheoses = listOf(
            "Something long is finished. The last line clicks shut like a well-made door.",
            "The arc bends home. You are not the one who began it — that is what arcs are for.",
            "A whole book of days, closed. The ink dries; the change doesn't."
        )
    ),
    THERAPIST(
        displayName = "Therapist",
        sample = "Notice what you notice. Begin where you can.",
        decayPerDay = 1, decayGraceDays = 7, minorsPerBoonAccrual = 4,
        staySmallBoonAdvice = "Start with a kindness you can actually " +
            "give yourself. We'll make room for more, slowly.",
        greetings = listOf(
            "Good to see you. What's coming up for you today?",
            "Take a breath. Here's what's on the table, when you're ready.",
            "We don't have to do it all. What's one thing?"
        ),
        completions = listOf(
            "That mattered. How did it feel?",
            "Good. Let's hold that for a moment.",
            "Thank you for showing up."
        ),
        apotheoses = listOf(
            "Big moment. Be with it before you rush to the next.",
            "You did the thing. Notice that you did the thing.",
            "Something has shifted. We can sit with that."
        )
    ),
    COACH(
        displayName = "Coach",
        sample = "Game plan. Three reps. Let's work.",
        decayPerDay = 4, decayGraceDays = 1, minorsPerBoonAccrual = 7,
        staySmallBoonAdvice = "Don't reward yourself like a champion " +
            "before you've earned it. Small wins, small rewards. We " +
            "level up together.",
        greetings = listOf(
            "Morning. Here's the slate. Pick your target.",
            "Time on the clock. We're moving.",
            "Today's plays — let's run them."
        ),
        completions = listOf(
            "Rep counted. Reset. Go again.",
            "Tight execution. Stack another.",
            "Good rep. Now do it better."
        ),
        apotheoses = listOf(
            "That's a W. Bank it. Tomorrow we raise the bar.",
            "You closed it out. Promotion earned.",
            "Game over — championship behavior."
        )
    ),
    HERMIT(
        displayName = "Hermit",
        sample = "I have been here. I have noticed. The work is small and good.",
        decayPerDay = 0, decayGraceDays = 14, minorsPerBoonAccrual = 3,
        staySmallBoonAdvice = "A small thing. The small things are the " +
            "real ones.",
        greetings = listOf(
            "I am here. The work is here. Begin when you wish.",
            "Few things asked today. Each one quiet.",
            "Take what you need. Leave what you don't."
        ),
        completions = listOf(
            "I noticed. That's enough.",
            "Yes. The small thing is the whole thing.",
            "Done. The room is still."
        ),
        apotheoses = listOf(
            "Something is finished. We will not speak loudly of it.",
            "A passage. I have witnessed it.",
            "The lamp glows differently now. That is all."
        )
    );

    fun greeting(seed: Long): String = greetings.pick(seed)
    fun completion(seed: Long): String = completions.pick(seed)
    fun apotheosis(seed: Long): String = apotheoses.pick(seed)

    private fun <T> List<T>.pick(seed: Long): T {
        val idx = ((seed % size) + size) % size
        return this[idx.toInt()]
    }

    companion object {
        fun fromKey(key: String): VoicePreset =
            entries.firstOrNull { it.name == key } ?: GENTLE_MENTOR
    }
}
