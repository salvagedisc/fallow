package io.github.salvagedisc.fallow.livres.client;

/**
 * What a page of a Hands-On Vanilla recipe book being drawn has shown so far ({@code mixin.livres.HovRecipeSheetMixin} keeps it on
 * the page's canvas): which page it is, and whether something it shows reaches into the corner the page turns by.
 */
public interface HovSheetState {
    boolean fallow$left();

    /** How far right what the page shows reaches into its turned-down corner; 0 when nothing does. */
    float fallow$taken();
}
