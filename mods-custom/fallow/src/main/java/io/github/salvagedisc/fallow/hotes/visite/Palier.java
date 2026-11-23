package io.github.salvagedisc.fallow.hotes.visite;

/** Les quatre paliers du lien entre un peuple et une clairière, selon les points gagnés par les dons. */
public enum Palier {
    INCONNUS("inconnus", 0), VOISINS("voisins", 6), PROCHES("proches", 16), FAMILIERS("familiers", 32);

    private final String id;
    private final int seuil;

    Palier(String id, int seuil) {
        this.id = id;
        this.seuil = seuil;
    }

    public String id() {
        return id;
    }

    /** Les points qu'il faut pour atteindre ce palier. */
    public int seuil() {
        return seuil;
    }

    public static Palier de(int points) {
        Palier palier = INCONNUS;
        for (Palier p : values()) {
            if (points >= p.seuil) {
                palier = p;
            }
        }
        return palier;
    }

    /** Le palier d'en dessous (les Inconnus restent inconnus). */
    public Palier precedent() {
        return this == INCONNUS ? INCONNUS : values()[ordinal() - 1];
    }
}
