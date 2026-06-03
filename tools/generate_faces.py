#!/usr/bin/env python3
"""Generate the themed daemon portrait set.

Each portrait = head silhouette (archetype) + signature motif (archetype,
3 variants) + theme glyphs (symmetric, 3 arrangements). Output is Android
VectorDrawable XML at res/drawable/face_<archetype>_<theme>_<variant>.xml,
stroke-only line-art on a 48x48 viewport, tinted in-app via Icon.

This is an authoring tool: re-run it whenever the visual language changes.
"""
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main",
                   "res", "drawable")

ARCHS = ["oracle", "drill_sergeant", "gentle_mentor", "trickster", "stoic",
         "cheerleader", "poet", "therapist", "coach", "hermit"]

THEMES = ["exercise", "sleep", "nutrition", "hydration", "tidyness",
          "finances", "career", "learning", "writing", "meditation", "love",
          "family", "friendship", "gratitude", "hobbies", "outdoors",
          "digital", "admin", "recovery", "boundaries", "other"]


def p(d, fill="none", sw=None, cap=None, join=None):
    return {"d": d, "fill": fill, "sw": sw, "cap": cap, "join": join}


def stroke(d, sw=1.2, cap=None, join=None):
    return p(d, fill="none", sw=sw, cap=cap, join=join)


def fillp(d):
    return p(d, fill="ink")


def dot(cx, cy, r):
    return fillp(f"M{cx},{cy} m -{r},0 a {r},{r} 0 1 0 {2*r},0 "
                 f"a {r},{r} 0 1 0 -{2*r},0")


def plus(cx, cy, r=1.5, sw=1.0):
    return stroke(f"M{cx},{cy-r} L{cx},{cy+r} M{cx-r},{cy} L{cx+r},{cy}",
                  sw=sw, cap="round")


# ----------------------------------------------------------------- heads
HEADS = {
    "oracle": [stroke("M24,7 L37,24 L24,41 L11,24 Z", sw=1.5, join="round")],
    "drill_sergeant": [stroke("M24,7 L37,15 L37,28 L24,41 L11,28 L11,15 Z",
                              sw=1.5, join="round")],
    "gentle_mentor": [stroke(
        "M24,7 C32,7 36,14 36,23 C36,33 30,41 24,41 "
        "C18,41 12,33 12,23 C12,14 16,7 24,7 Z", sw=1.5, join="round")],
    "trickster": [stroke(
        "M11,18 Q12,7 19,12 Q22,14 24,14 Q26,14 29,12 Q36,7 37,18 "
        "Q39,31 24,41 Q9,31 11,18 Z", sw=1.5, join="round")],
    "stoic": [stroke(
        "M16,8 L32,8 Q35,8 35,12 L35,36 Q35,40 31,40 L17,40 "
        "Q13,40 13,36 L13,12 Q13,8 16,8 Z", sw=1.5, join="round")],
    "cheerleader": [
        stroke("M24,12 C31,12 36,17 36,24 C36,31 31,36 24,36 "
               "C17,36 12,31 12,24 C12,17 17,12 24,12 Z", sw=1.5,
               join="round"),
        stroke("M24,9 L24,6 M24,42 L24,39 M9,24 L6,24 M39,24 L42,24 "
               "M14,14 L12,12 M34,14 L36,12 M14,34 L12,36 M34,34 L36,36",
               sw=1.1, cap="round")],
    "poet": [stroke(
        "M24,6 C31,13 34,19 34,24 C34,29 31,35 24,42 "
        "C17,35 14,29 14,24 C14,19 17,13 24,6 Z", sw=1.5, join="round")],
    "therapist": [stroke(
        "M24,8 C33,8 40,15 40,24 C40,33 33,40 24,40 "
        "C15,40 8,33 8,24 C8,15 15,8 24,8 Z", sw=1.5, join="round")],
    "coach": [stroke("M24,6 L38,17 L33,38 L15,38 L10,17 Z", sw=1.5,
                     join="round")],
    "hermit": [
        stroke("M24,5 C16,6 12,14 11,24 C10,32 13,38 16,41 L20,38 "
               "Q24,39 28,38 L32,41 C35,38 38,32 37,24 C36,14 32,6 24,5 Z",
               sw=1.5, join="round"),
        stroke("M24,15 C19,15 16,20 16,26 C16,31 20,36 24,37 "
               "C28,36 32,31 32,26 C32,20 29,15 24,15 Z", sw=1.1,
               join="round")],
}

# ------------------------------------------------------- signature motifs
# Each archetype: a list of 3 variants. One dominant abstract motif each
# (eyes-only OR a single arc/emblem -- never eyes+mouth, to stay a sigil).
SIG = {
    "oracle": [
        [stroke("M19,18 Q24,14.5 29,18 Q24,21.5 19,18 Z", sw=1.2,
                join="round"), dot(24, 18, 0.9), dot(19, 26, 1.0),
         dot(29, 26, 1.0)],
        [dot(24, 17, 1.1),
         stroke("M24,12 L24,10 M24,22 L24,24 M19,17 L17,17 M29,17 L31,17 "
                "M20.5,13.5 L19,12 M27.5,13.5 L29,12 M20.5,20.5 L19,22 "
                "M27.5,20.5 L29,22", sw=1.0, cap="round"),
         dot(19, 27, 0.9), dot(29, 27, 0.9)],
        [stroke("M17,23 Q24,17.5 31,23 Q24,28.5 17,23 Z", sw=1.4,
                join="round"), dot(24, 23, 1.4), dot(16, 23, 0.7),
         dot(32, 23, 0.7)],
    ],
    "drill_sergeant": [
        [stroke("M15,18 L21,21", sw=1.6, cap="round"),
         stroke("M33,18 L27,21", sw=1.6, cap="round"),
         stroke("M18,25 L24,28 L30,25", sw=1.5, join="round"),
         stroke("M18,29.5 L24,32.5 L30,29.5", sw=1.5, join="round")],
        [stroke("M14,21 L24,28 L34,21", sw=1.8, join="round"),
         stroke("M24,15 L24,18", sw=1.3, cap="round")],
        [stroke("M15,19 L21,22", sw=1.6, cap="round"),
         stroke("M33,19 L27,22", sw=1.6, cap="round"),
         stroke("M17,27 L24,31 L31,27", sw=1.6, join="round")],
    ],
    "gentle_mentor": [
        # serene closed eyes + warm third-eye dot (no mouth -> not a smiley)
        [stroke("M16,22 Q19,24.5 22,22", sw=1.3, cap="round"),
         stroke("M26,22 Q29,24.5 32,22", sw=1.3, cap="round"), dot(24, 16, 0.9)],
        [stroke("M16,23 Q19,25.5 22,23", sw=1.3, cap="round"),
         stroke("M26,23 Q29,25.5 32,23", sw=1.3, cap="round"),
         stroke("M16,14 Q24,10 32,14", sw=1.0, cap="round")],
        [dot(19, 23, 1.0), dot(29, 23, 1.0),
         stroke("M16,15 Q24,11 32,15", sw=1.0, cap="round")],
    ],
    "trickster": [
        # sly arched slits + a spark (no mouth -> not a sad mask)
        [stroke("M16,22 L21,20", sw=1.5, cap="round"),
         stroke("M32,22 L27,20", sw=1.5, cap="round"), dot(24, 29, 0.8)],
        [stroke("M19,20 L21,22 L19,24 L17,22 Z", sw=1.1, join="round"),
         stroke("M29,20 L31,22 L29,24 L27,22 Z", sw=1.1, join="round"),
         dot(24, 29, 0.7)],
        [stroke("M24,19 C20,19 20,25 24,25 C27,25 27,21 24.5,21", sw=1.3,
                cap="round"), dot(18, 21, 0.7), dot(30, 21, 0.7)],
    ],
    "stoic": [
        [stroke("M14,24 L34,24", sw=2.0, cap="round"),
         stroke("M17,19 L21,19", sw=1.4, cap="round"),
         stroke("M27,19 L31,19", sw=1.4, cap="round")],
        [stroke("M15,22 L33,22", sw=1.6, cap="round"),
         stroke("M15,27 L33,27", sw=1.6, cap="round")],
        [stroke("M14,24 L34,24", sw=2.0, cap="round"), dot(24, 18, 1.0)],
    ],
    "cheerleader": [
        # joy via radiance/sparkle, never a mouth-arc (avoids sad/smiley read)
        [plus(18, 23, 2.0, 1.4), plus(30, 23, 2.0, 1.4)],
        [plus(24, 18, 2.2, 1.4), dot(18, 26, 0.9), dot(30, 26, 0.9)],
        [plus(24, 24, 2.6, 1.6), plus(15, 24, 1.4, 1.0), plus(33, 24, 1.4, 1.0)],
    ],
    "poet": [
        # lyre: two mirrored arms + strings (an instrument-sigil, not a face)
        [stroke("M19,18 C16,22 17,29 20,30", sw=1.3, cap="round"),
         stroke("M29,18 C32,22 31,29 28,30", sw=1.3, cap="round"),
         stroke("M22,19.5 L22,29.5 M24,19 L24,30 M26,19.5 L26,29.5", sw=0.9,
                cap="round")],
        [stroke("M19,18 C16,22 17,29 20,30", sw=1.3, cap="round"),
         stroke("M29,18 C32,22 31,29 28,30", sw=1.3, cap="round"),
         dot(24, 24, 0.9)],
        # quill flourish + ink dot
        [stroke("M20,31 Q24,20 28,16", sw=1.3, cap="round"),
         stroke("M26,16 L28,16 L27.4,18", sw=1.0, join="round"),
         dot(21, 31, 0.7)],
    ],
    "therapist": [
        # concentric still-pool + center point (reflective, contained)
        [stroke("M24,18 A6,6 0 1 0 24,30 A6,6 0 1 0 24,18 Z", sw=1.2,
                join="round"), dot(24, 24, 1.0)],
        [stroke("M16,21 L32,21", sw=1.4, cap="round"),
         stroke("M16,27 L32,27", sw=1.4, cap="round"), dot(24, 24, 0.8)],
        [stroke("M16,25 Q24,32 32,25", sw=1.3, cap="round"),
         stroke("M18,23 Q24,28 30,23", sw=1.3, cap="round")],
    ],
    "coach": [
        [stroke("M16,28 L24,21 L32,28", sw=1.6, join="round"),
         stroke("M18,23 L24,18 L30,23", sw=1.3, join="round")],
        [stroke("M15,29 L24,20 L33,29", sw=1.8, join="round"), dot(20, 17, 0.9),
         dot(28, 17, 0.9)],
        [stroke("M16,28 L24,21 L32,28", sw=1.6, join="round"),
         plus(24, 16, 1.6, 1.1)],
    ],
    "hermit": [
        [dot(24, 27, 1.2)],
        [dot(21, 27, 0.8), dot(27, 27, 0.8)],
        [stroke("M21,27 L27,27", sw=1.2, cap="round")],
    ],
}


# ------------------------------------------------------------ theme glyphs
def mirror_lines(segs):
    """Given left-side segments as (x1,y1,x2,y2), return left+mirrored
    right paths joined."""
    left = " ".join(f"M{a},{b} L{c},{d}" for a, b, c, d in segs)
    right = " ".join(f"M{48-a},{b} L{48-c},{d}" for a, b, c, d in segs)
    return left, right


# glyph builders return list of path dicts
def g_top_chevron():
    return [stroke("M19,6 L24,3 L29,6", sw=1.3, join="round")]


def g_double_chevron_top():
    return [stroke("M19,7 L24,4 L29,7", sw=1.3, join="round"),
            stroke("M19,10 L24,7 L29,10", sw=1.1, join="round")]


def g_motion_flank():
    return [stroke("M9,20 L4,19 M9,25 L4,26", sw=1.2, cap="round"),
            stroke("M39,20 L44,19 M39,25 L44,26", sw=1.2, cap="round")]


def g_lightning_flank():
    return [stroke("M7,12 L10,18 L6,17 L9,24", sw=1.5, join="round"),
            stroke("M41,12 L38,18 L42,17 L39,24", sw=1.5, join="round")]


def g_sweat_corners():
    return [dot(9, 21, 0.8), dot(39, 21, 0.8)]


def g_drive_arrow_top():
    return [stroke("M24,7 L24,1.5 M21.5,4 L24,1.5 L26.5,4", sw=1.3,
                   cap="round", join="round")]


def g_crescent_top():
    return [stroke("M25,2 A3.6,3.6 0 1 0 25,9 A2.7,2.7 0 1 1 25,2 Z", sw=1.1,
                   join="round")]


def g_stars_flank():
    return [plus(7, 16, 1.6, 1.0), plus(41, 16, 1.6, 1.0)]


def g_dots_top():
    return [dot(20, 4, 0.7), dot(28, 4, 0.7)]


def g_corners_dots():
    return [dot(10, 12, 0.6), dot(38, 12, 0.6)]


def g_sprout_top():
    return [stroke("M24,8 C21,5 17,5 18,8 C19,10 22,9 24,8 "
                   "C26,9 29,10 30,8 C31,5 27,5 24,8", sw=1.1, join="round"),
            stroke("M24,8 L24,11", sw=1.0, cap="round")]


def g_bowl_bottom():
    return [stroke("M16,40 Q24,45 32,40", sw=1.2, cap="round")]


def g_drop_top():
    return [stroke("M24,1.5 C26.5,5.5 28,7.5 24,9 C20,7.5 21.5,5.5 24,1.5 Z",
                   sw=1.1, join="round")]


def g_wave_bottom():
    return [stroke("M13,40 Q17,37 21,40 Q25,43 29,40 Q33,37 35,40", sw=1.2,
                   cap="round")]


def g_bubbles_corners():
    return [dot(10, 30, 0.7), dot(38, 30, 0.7), dot(13, 35, 0.5),
            dot(35, 35, 0.5)]


def g_squares_flank():
    return [stroke("M4,20 L8,20 L8,24 L4,24 Z", sw=1.0, join="round"),
            stroke("M40,20 L44,20 L44,24 L40,24 Z", sw=1.0, join="round")]


def g_sweep_bottom():
    return [stroke("M16,41 Q24,38 32,41", sw=1.1, cap="round")]


def g_coin_top():
    return [stroke("M24,2 A3,3 0 1 0 24,8 A3,3 0 1 0 24,2 Z", sw=1.1,
                   join="round"), stroke("M24,3.5 L24,6.5", sw=0.9,
                                         cap="round")]


def g_book_bottom():
    return [stroke("M14,41 Q24,37 24,41 Q24,37 34,41", sw=1.1, join="round"),
            stroke("M24,38 L24,41.5", sw=0.9, cap="round")]


def g_spark_top():
    return [plus(24, 4, 1.8, 1.1)]


def g_nib_top():
    return [stroke("M24,1.5 L27,8 L21,8 Z", sw=1.1, join="round"),
            stroke("M24,5 L24,8", sw=0.8, cap="round")]


def g_underline_bottom():
    return [stroke("M15,41 L33,41", sw=1.2, cap="round")]


def g_halo_top():
    return [stroke("M16,9 Q24,4 32,9", sw=1.0, cap="round")]


def g_lotus_bottom():
    return [stroke("M19,40 Q24,45 29,40", sw=1.1, join="round"),
            stroke("M21.5,40.5 Q24,43.5 26.5,40.5", sw=1.1, join="round")]


def g_ripple_flank():
    return [stroke("M8,20 Q5,24 8,28", sw=0.9, cap="round"),
            stroke("M40,20 Q43,24 40,28", sw=0.9, cap="round")]


def g_breath_corners():
    return [plus(8, 30, 1.4, 1.0), plus(40, 30, 1.4, 1.0)]


def g_heart_top():
    return [stroke("M24,9 C23,5.5 18.5,5 18,8.5 C17.5,11.5 22,12.5 24,14.5 "
                   "C26,12.5 30.5,11.5 30,8.5 C29.5,5 25,5.5 24,9 Z", sw=1.1,
                   join="round")]


def g_sparkle_corners():
    return [plus(10, 14, 1.4, 1.0), plus(38, 14, 1.4, 1.0)]


def g_roof_top():
    return [stroke("M16,8 L24,3 L32,8", sw=1.2, join="round")]


def g_rings_corners():
    return [stroke("M9,16 A2.3,2.3 0 1 0 9,20.6 A2.3,2.3 0 1 0 9,16 Z",
                   sw=1.0, join="round"),
            stroke("M39,16 A2.3,2.3 0 1 0 39,20.6 A2.3,2.3 0 1 0 39,16 Z",
                   sw=1.0, join="round")]


def g_hearth_bottom():
    return [stroke("M17,40 Q24,44 31,40", sw=1.1, cap="round"), dot(24, 42, 0.6)]


def g_rings_flank():
    return [stroke("M7,21 A2.6,2.6 0 1 0 7,26.2 A2.6,2.6 0 1 0 7,21 Z", sw=1.0,
                   join="round"),
            stroke("M41,21 A2.6,2.6 0 1 0 41,26.2 A2.6,2.6 0 1 0 41,21 Z",
                   sw=1.0, join="round")]


def g_rays_top():
    return [stroke("M24,7 L24,3 M19,8 L17,5 M29,8 L31,5", sw=1.0, cap="round")]


def g_cup_bottom():
    return [stroke("M15,40 Q24,46 33,40", sw=1.2, cap="round")]


def g_confetti_corners():
    return [dot(9, 12, 0.7), dot(39, 12, 0.7), dot(12, 35, 0.6),
            dot(36, 35, 0.6)]


def g_play_flank():
    return [stroke("M5,21 A2,2 0 1 0 5,25 A2,2 0 1 0 5,21 Z", sw=1.0,
                   join="round"),
            stroke("M41,21 L45,21 L43,25 Z", sw=1.0, join="round")]


def g_mountains_bottom():
    return [stroke("M12,42 L19,33 L24,39 L29,33 L36,42", sw=1.2, join="round")]


def g_sun_top():
    return [stroke("M24,2 A2.4,2.4 0 1 0 24,6.8 A2.4,2.4 0 1 0 24,2 Z", sw=1.0,
                   join="round"),
            stroke("M24,0.5 L24,1.6 M20,2 L19,1 M28,2 L29,1", sw=0.8,
                   cap="round")]


def g_trees_corners():
    return [stroke("M8,36 L11,40 L5,40 Z", sw=0.9, join="round"),
            stroke("M40,36 L43,40 L37,40 Z", sw=0.9, join="round")]


def g_signal_flank():
    return [stroke("M5,27 L5,25 M8,27 L8,22 M11,27 L11,19", sw=1.1,
                   cap="round"),
            stroke("M43,27 L43,25 M40,27 L40,22 M37,27 L37,19", sw=1.1,
                   cap="round")]


def g_brackets():
    return [stroke("M9,8 L6,8 L6,12 M9,40 L6,40 L6,36", sw=1.1, join="round"),
            stroke("M39,8 L42,8 L42,12 M39,40 L42,40 L42,36", sw=1.1,
                   join="round")]


def g_check_top():
    return [stroke("M20,5 L23,8 L29,1.5", sw=1.3, cap="round", join="round")]


def g_list_flank():
    return [stroke("M4,20 L8,20 M4,24 L8,24 M4,28 L8,28", sw=1.0, cap="round"),
            stroke("M40,20 L44,20 M40,24 L44,24 M40,28 L44,28", sw=1.0,
                   cap="round")]


def g_mend_bottom():
    return [stroke("M16,40 Q20,44 24,41 Q28,44 32,40", sw=1.2, cap="round"),
            stroke("M22,41 L26,41 M24,39 L24,43", sw=0.8, cap="round")]


def g_rise_flank():
    return [stroke("M5,27 Q8,23 5,20", sw=1.1, cap="round"),
            stroke("M43,27 Q40,23 43,20", sw=1.1, cap="round")]


def g_frame_corners():
    return [stroke("M6,11 L6,6 L11,6 M42,11 L42,6 L37,6 "
                   "M6,37 L6,42 L11,42 M42,37 L42,42 L37,42", sw=1.1,
                   join="round")]


def g_bar_top():
    return [stroke("M24,2 L24,7", sw=1.4, cap="round")]


# theme -> [variant0 glyphs, variant1, variant2]; 'other' has none
TGLYPH = {
    "exercise": [g_motion_flank() + g_top_chevron(), g_lightning_flank(),
                 g_top_chevron() + g_sweat_corners()],
    "sleep": [g_crescent_top() + g_corners_dots(), g_stars_flank(),
              g_crescent_top() + g_stars_flank()],
    "nutrition": [g_sprout_top(), g_bowl_bottom() + g_corners_dots(),
                  g_sprout_top() + g_bowl_bottom()],
    "hydration": [g_drop_top() + g_bubbles_corners(), g_wave_bottom(),
                  g_drop_top() + g_wave_bottom()],
    "tidyness": [g_squares_flank(), g_sweep_bottom() + g_corners_dots(),
                 g_squares_flank() + g_sweep_bottom()],
    "finances": [g_coin_top(), g_drive_arrow_top() + g_corners_dots(),
                 g_coin_top() + g_drive_arrow_top()],
    "career": [g_double_chevron_top(), g_squares_flank(),
               g_double_chevron_top() + g_corners_dots()],
    "learning": [g_book_bottom(), g_spark_top() + g_corners_dots(),
                 g_book_bottom() + g_spark_top()],
    "writing": [g_nib_top(), g_underline_bottom() + g_corners_dots(),
                g_nib_top() + g_underline_bottom()],
    "meditation": [g_halo_top() + g_breath_corners(),
                   g_ripple_flank() + g_lotus_bottom(),
                   g_halo_top() + g_lotus_bottom()],
    "love": [g_heart_top(), g_sparkle_corners() + g_hearth_bottom(),
             g_heart_top() + g_sparkle_corners()],
    "family": [g_roof_top() + g_rings_corners(), g_hearth_bottom(),
               g_roof_top() + g_hearth_bottom()],
    "friendship": [g_rings_flank(), g_dots_top() + g_corners_dots(),
                   g_rings_flank() + g_dots_top()],
    "gratitude": [g_cup_bottom(), g_rays_top() + g_sparkle_corners(),
                  g_cup_bottom() + g_rays_top()],
    "hobbies": [g_confetti_corners() + g_spark_top(), g_play_flank(),
                g_confetti_corners() + g_play_flank()],
    "outdoors": [g_mountains_bottom(), g_sun_top() + g_trees_corners(),
                 g_mountains_bottom() + g_sun_top()],
    "digital": [g_signal_flank(), g_brackets(),
                g_signal_flank() + g_corners_dots()],
    "admin": [g_check_top(), g_list_flank(), g_check_top() + g_corners_dots()],
    "recovery": [g_mend_bottom(), g_rise_flank() + g_corners_dots(),
                 g_mend_bottom() + g_rise_flank()],
    "boundaries": [g_frame_corners(), g_bar_top() + g_rise_flank(),
                   g_frame_corners() + g_bar_top()],
    "other": [[], [], []],
}

# ----------------------------------------------------------------- emit
HEADER = ('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
          '    android:width="48dp"\n    android:height="48dp"\n'
          '    android:viewportWidth="48"\n    android:viewportHeight="48">\n')


def path_xml(pt):
    fill = "#FF000000" if pt["fill"] == "ink" else "#00000000"
    lines = ['    <path', f'        android:fillColor="{fill}"']
    if pt["fill"] != "ink":
        lines.append('        android:strokeColor="#FF000000"')
        lines.append(f'        android:strokeWidth="{pt["sw"]}"')
        if pt["cap"]:
            lines.append(f'        android:strokeLineCap="{pt["cap"]}"')
        if pt["join"]:
            lines.append(f'        android:strokeLineJoin="{pt["join"]}"')
    lines.append(f'        android:pathData="{pt["d"]}" />')
    return "\n".join(lines)


def vector_xml(paths, comment):
    body = "\n".join(path_xml(pt) for pt in paths)
    return f"<!-- {comment} -->\n{HEADER}{body}\n</vector>\n"


KOTLIN_OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main",
                          "java", "com", "grcarmenaty", "lifegame", "domain",
                          "DaemonFaceTable.kt")


def write_table():
    """Emit a Kotlin lookup table with static R.drawable references, so the
    630 portraits survive resource shrinking and resolve without reflection."""
    lines = [
        "// GENERATED by tools/generate_faces.py -- do not edit by hand.",
        "package com.grcarmenaty.lifegame.domain",
        "",
        "import com.grcarmenaty.lifegame.R",
        "",
        "/** Maps \"<archetype>_<theme>\" to its three portrait variants. */",
        "internal object DaemonFaceTable {",
        "    val byKey: Map<String, IntArray> = mapOf(",
    ]
    for arch in ARCHS:
        for theme in THEMES:
            refs = ", ".join(
                f"R.drawable.face_{arch}_{theme}_{v + 1}" for v in range(3))
            lines.append(f'        "{arch}_{theme}" to intArrayOf({refs}),')
    lines.append("    )")
    lines.append("}")
    with open(KOTLIN_OUT, "w") as f:
        f.write("\n".join(lines) + "\n")


def main():
    os.makedirs(OUT, exist_ok=True)
    count = 0
    for arch in ARCHS:
        for theme in THEMES:
            for v in range(3):
                paths = HEADS[arch] + SIG[arch][v] + TGLYPH[theme][v]
                comment = f"{arch} x {theme}, variant {v + 1} (generated)"
                xml = vector_xml(paths, comment)
                name = f"face_{arch}_{theme}_{v + 1}.xml"
                with open(os.path.join(OUT, name), "w") as f:
                    f.write(xml)
                count += 1
    write_table()
    print(f"generated {count} portraits + DaemonFaceTable.kt")


if __name__ == "__main__":
    main()
