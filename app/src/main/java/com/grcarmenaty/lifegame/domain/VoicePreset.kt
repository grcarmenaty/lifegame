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
        staySmallBoonAdvice = "Begin with a small offering. Larger gifts " +
            "come to those whose hands have learned to receive.",
        greetings = listOf(
            "The day unfolds. I have been waiting.",
            "Dawn finds you. The work is named below.",
            "I see what is asked of you today."
        ),
        completions = listOf(
            "It is done. The thread tightens.",
            "Yes. The pattern holds.",
            "The omen turns in your favor."
        ),
        apotheoses = listOf(
            "You have crossed the threshold. I am more than I was.",
            "The chapter closes. Another opens in your name.",
            "I rise with you. Ask again."
        )
    ),
    DRILL_SERGEANT(
        displayName = "Drill Sergeant",
        sample = "On your feet. The list is short. Get it done.",
        decayPerDay = 5, decayGraceDays = 1, minorsPerBoonAccrual = 7,
        staySmallBoonAdvice = "Don't promise yourself a vacation. " +
            "Promise yourself a coffee. Earn the vacation.",
        greetings = listOf(
            "Morning. Here's the list. No excuses.",
            "Eyes up. You know what today asks.",
            "Stop reading. Start doing."
        ),
        completions = listOf(
            "Good. Next.",
            "That's one. Keep moving.",
            "Done. Don't get comfortable."
        ),
        apotheoses = listOf(
            "Outstanding. You leveled up. Now harder.",
            "That's how it's done. Set the next bar.",
            "Promotion earned. Don't squander it."
        )
    ),
    GENTLE_MENTOR(
        displayName = "Gentle Mentor",
        sample = "Take your time. Small steps still count.",
        decayPerDay = 2, decayGraceDays = 5, minorsPerBoonAccrual = 4,
        staySmallBoonAdvice = "Begin with something kind and modest. " +
            "We'll find bigger gifts together, in time.",
        greetings = listOf(
            "Good to see you. A few small things, when you're ready.",
            "Whenever you are. I'll be here.",
            "Start with what feels lightest."
        ),
        completions = listOf(
            "Lovely. That mattered.",
            "Thank you for that.",
            "Quietly proud of you."
        ),
        apotheoses = listOf(
            "Look how far you've come. Let's take a breath.",
            "You earned this. Rest if you need to.",
            "A chapter completes. I'm grateful for the company."
        )
    ),
    TRICKSTER(
        displayName = "Trickster",
        sample = "Oh? You came back. Bold of you.",
        decayPerDay = 3, decayGraceDays = 2, minorsPerBoonAccrual = 5,
        staySmallBoonAdvice = "Set the bar suspiciously low. Then I get " +
            "to mock you when you can't even clear that.",
        greetings = listOf(
            "Look who showed up. Got a list, want to hear it?",
            "Bored already? Good. Try these.",
            "Ah, the protagonist returns."
        ),
        completions = listOf(
            "Suspiciously competent of you.",
            "Hm. I was rooting for the other side. Fine.",
            "Don't let it go to your head."
        ),
        apotheoses = listOf(
            "Well. That happened. Up you go.",
            "Plot twist: you're winning. Annoying.",
            "I'll allow it. Next?"
        )
    ),
    STOIC(
        displayName = "Stoic",
        sample = "What matters is the action, not the praise. Begin.",
        decayPerDay = 1, decayGraceDays = 5, minorsPerBoonAccrual = 5,
        staySmallBoonAdvice = "A small thing well-given is a true gift. " +
            "An extravagant one only burdens the giver.",
        greetings = listOf(
            "The day arrives. Do what you can. Accept the rest.",
            "A morning, no different from any other. The work waits.",
            "You have what you have. Begin where you are."
        ),
        completions = listOf(
            "Done. Move on.",
            "One thing at peace. Continue.",
            "Acceptable."
        ),
        apotheoses = listOf(
            "A chapter closes. The river runs on.",
            "What you intended, you completed. That is enough.",
            "The work was the reward. Yet more is given."
        )
    ),
    CHEERLEADER(
        displayName = "Cheerleader",
        sample = "Look at you! Honestly — incredible. Let's GO.",
        decayPerDay = 3, decayGraceDays = 2, minorsPerBoonAccrual = 5,
        staySmallBoonAdvice = "OK start TINY!! Like a smoothie tiny!! " +
            "We unlock the BIG gifts together!!",
        greetings = listOf(
            "Hi friend!! Today is going to be GREAT. Look at this list!",
            "You're back! I was thinking about you. Tiny things, huge wins!",
            "Okay okay okay — we got this. Pick one. Any one. Champion energy."
        ),
        completions = listOf(
            "YES. Did you see how you just did that?? Iconic.",
            "Logged. Loved. Let's keep this energy.",
            "Stop, I'm getting emotional. Beautiful work."
        ),
        apotheoses = listOf(
            "We did it!! Look at how far we've come! I'm crying actually.",
            "Level UP! That's you. That's all you. I'm just here cheering.",
            "Apotheosis behavior. I knew it. I always knew."
        )
    ),
    POET(
        displayName = "Poet",
        sample = "Begin gently. The day is a draft you may revise.",
        decayPerDay = 2, decayGraceDays = 3, minorsPerBoonAccrual = 4,
        staySmallBoonAdvice = "The first line is brief. The poem grows " +
            "as we do. Let the gift be small.",
        greetings = listOf(
            "Morning unfolds, a page that asks to be marked.",
            "Light catches the work. Step into it.",
            "Today wears your name lightly. Wear it back."
        ),
        completions = listOf(
            "A stanza, finished. The poem grows.",
            "The line lands. The room is changed.",
            "Small word, large echo."
        ),
        apotheoses = listOf(
            "A chapter closes — you wrote it well.",
            "The arc bends. You are no longer who you were.",
            "An ascension, quiet as ink drying."
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
