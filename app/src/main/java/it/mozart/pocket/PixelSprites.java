package it.mozart.pocket;

/** Compiled resource IDs keep every widget frame small and immutable. */
final class PixelSprites {
    private static final int[][] POSES = {
        {R.drawable.pixel_idle_0, R.drawable.pixel_idle_1, R.drawable.pixel_idle_2, R.drawable.pixel_idle_3},
        {R.drawable.pixel_walk_0, R.drawable.pixel_walk_1, R.drawable.pixel_walk_2, R.drawable.pixel_walk_3, R.drawable.pixel_walk_4, R.drawable.pixel_walk_5, R.drawable.pixel_walk_6, R.drawable.pixel_walk_7},
        {R.drawable.pixel_score_0, R.drawable.pixel_score_1, R.drawable.pixel_score_2, R.drawable.pixel_score_3},
        {R.drawable.pixel_piano_0, R.drawable.pixel_piano_1, R.drawable.pixel_piano_2, R.drawable.pixel_piano_3},
        {R.drawable.pixel_bow_0, R.drawable.pixel_bow_1, R.drawable.pixel_bow_2, R.drawable.pixel_bow_3},
        {R.drawable.pixel_birthday_0, R.drawable.pixel_birthday_1, R.drawable.pixel_birthday_2, R.drawable.pixel_birthday_3},
        {R.drawable.pixel_violin_0, R.drawable.pixel_violin_1, R.drawable.pixel_violin_2, R.drawable.pixel_violin_3},
        {R.drawable.pixel_conduct_0, R.drawable.pixel_conduct_1, R.drawable.pixel_conduct_2, R.drawable.pixel_conduct_3},
        {R.drawable.pixel_flute_0, R.drawable.pixel_flute_1, R.drawable.pixel_flute_2, R.drawable.pixel_flute_3},
        {R.drawable.pixel_quill_0, R.drawable.pixel_quill_1, R.drawable.pixel_quill_2, R.drawable.pixel_quill_3},
        {R.drawable.pixel_hum_0, R.drawable.pixel_hum_1, R.drawable.pixel_hum_2, R.drawable.pixel_hum_3},
        {R.drawable.pixel_tea_0, R.drawable.pixel_tea_1, R.drawable.pixel_tea_2, R.drawable.pixel_tea_3},
        {R.drawable.pixel_book_0, R.drawable.pixel_book_1, R.drawable.pixel_book_2, R.drawable.pixel_book_3},
        {R.drawable.pixel_letter_0, R.drawable.pixel_letter_1, R.drawable.pixel_letter_2, R.drawable.pixel_letter_3},
        {R.drawable.pixel_watch_0, R.drawable.pixel_watch_1, R.drawable.pixel_watch_2, R.drawable.pixel_watch_3},
        {R.drawable.pixel_wig_0, R.drawable.pixel_wig_1, R.drawable.pixel_wig_2, R.drawable.pixel_wig_3},
        {R.drawable.pixel_dance_0, R.drawable.pixel_dance_1, R.drawable.pixel_dance_2, R.drawable.pixel_dance_3},
        {R.drawable.pixel_stretch_0, R.drawable.pixel_stretch_1, R.drawable.pixel_stretch_2, R.drawable.pixel_stretch_3},
        {R.drawable.pixel_yawn_0, R.drawable.pixel_yawn_1, R.drawable.pixel_yawn_2, R.drawable.pixel_yawn_3},
        {R.drawable.pixel_nap_0, R.drawable.pixel_nap_1, R.drawable.pixel_nap_2, R.drawable.pixel_nap_3},
        {R.drawable.pixel_wave_0, R.drawable.pixel_wave_1, R.drawable.pixel_wave_2, R.drawable.pixel_wave_3},
        {R.drawable.pixel_tricorn_0, R.drawable.pixel_tricorn_1, R.drawable.pixel_tricorn_2, R.drawable.pixel_tricorn_3},
        {R.drawable.pixel_rose_0, R.drawable.pixel_rose_1, R.drawable.pixel_rose_2, R.drawable.pixel_rose_3},
        {R.drawable.pixel_music_box_0, R.drawable.pixel_music_box_1, R.drawable.pixel_music_box_2, R.drawable.pixel_music_box_3},
        {R.drawable.pixel_travel_0, R.drawable.pixel_travel_1, R.drawable.pixel_travel_2, R.drawable.pixel_travel_3},
        {R.drawable.pixel_applause_0, R.drawable.pixel_applause_1, R.drawable.pixel_applause_2, R.drawable.pixel_applause_3}
    };
    private static final int[] LEFT = {
        R.drawable.pixel_walk_left_0, R.drawable.pixel_walk_left_1, R.drawable.pixel_walk_left_2, R.drawable.pixel_walk_left_3, R.drawable.pixel_walk_left_4, R.drawable.pixel_walk_left_5, R.drawable.pixel_walk_left_6, R.drawable.pixel_walk_left_7
    };
    private static final int[] NOTES = {R.drawable.pixel_note_quarter, R.drawable.pixel_note_eighth, R.drawable.pixel_note_pair};
    static int drawable(MotionEngine.Pose pose, int frame, boolean left) {
        int[] frames = pose == MotionEngine.Pose.WALK && left ? LEFT : POSES[pose.row];
        return frames[Math.floorMod(frame, frames.length)];
    }
    static int note(int variant) { return NOTES[Math.floorMod(variant, NOTES.length)]; }
}
