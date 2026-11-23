// Les nuages : une couche en volume poussée par le vent, faite de plusieurs familles selon une carte du temps qui
// dérive lentement : des bancs plats de stratocumulus, des cumulus ronds, et sous l'orage des tours qui montent
// jusqu'au sommet de la couche et s'y étalent en enclume. Le vent souffle plus fort en haut : les tours penchent.
// Au-dessus, une nappe mince d'altocumulus en petites cellules, et très haut un voile de cirrus en traînées
// (program/clouds.fsh).
// Le bruit vient de setup.csh (64³ texels qui se répètent ; rouge : la forme, vert : les cellules fines).
// Unités : blocs, repère monde.

uniform sampler3D cloudNoiseTex;
uniform float thunderStrength;

const float CLOUD_TOP_SPAN = 90.0;       // épaisseur de la couche
const float CLOUD_SHAPE_SCALE = 1.0 / 420.0;
const float CLOUD_DETAIL_SCALE = 1.0 / 70.0;
// Le voile de cirrus, bien au-dessus de la couche.
const float CIRRUS_LIFT = 760.0;

float cloudBase() { return CLOUD_HEIGHT; }
float cloudTop() { return CLOUD_HEIGHT + CLOUD_TOP_SPAN; }
float cirrusHeight() { return CLOUD_HEIGHT + CIRRUS_LIFT; }
// La nappe d'altocumulus, entre la couche et les cirrus.
const float ALTO_LIFT = 340.0;
float altoHeight() { return CLOUD_HEIGHT + ALTO_LIFT; }

/** Lecture du bruit qui se répète : les coordonnées sont ramenées aux centres des texels du bord. */
vec2 cloudNoise(vec3 p) {
    return texture(cloudNoiseTex, fract(p) * (63.0 / 64.0) + 0.5 / 64.0).rg;
}

/** La direction du vent des nuages (unitaire) et sa force (0..1) : celui du pack, sinon une brise régulière. */
vec2 cloudWind(out float force) {
    force = 0.35;
    if (brumaireLink > 0.5 && dot(brumaireWind.xy, brumaireWind.xy) > 0.01) {
        force = clamp(brumaireWind.z, 0.0, 1.0);
        return normalize(brumaireWind.xy);
    }
    return normalize(vec2(0.8, 0.35));
}

/** Décalage des nuages avec le temps : le vent du pack, sinon une brise régulière. */
vec3 cloudDrift() {
    vec2 wind = vec2(0.8, 0.35);
    if (brumaireLink > 0.5 && dot(brumaireWind.xy, brumaireWind.xy) > 0.01) wind = normalize(brumaireWind.xy) * (0.6 + 0.6 * brumaireWind.z);
    return vec3(wind.x, 0.0, wind.y) * frameTimeCounter * 4.0 * CLOUD_SPEED;
}

/** Part du ciel couverte : plus sous la pluie, un peu plus dans un biome humide. */
float cloudCoverage() {
    return clamp(CLOUD_COVERAGE * mix(0.85, 1.15, airHumid) + 0.45 * rainStrength, 0.0, 1.0);
}

float cloudRemap(float v, float lo, float hi) {
    return clamp((v - lo) / max(hi - lo, 1e-4), 0.0, 1.0);
}

/**
 * La carte du temps en un point (xz) : x la couverture locale (des trouées et des bancs), y la famille (0 banc plat,
 * 1 cumulus bourgeonnant), z l'enclume (orage).
 */
vec3 cloudWeather(vec2 q, float coverage) {
    float w1 = cloudNoise(vec3(q * (CLOUD_SHAPE_SCALE * 0.22), 0.37).xzy).r;
    float w2 = cloudNoise(vec3(q * (CLOUD_SHAPE_SCALE * 0.09) + 0.41, 0.83).xzy).r;
    float cover = clamp(coverage * (0.78 + 0.5 * w1), 0.0, 1.0);
#if RAIN_ROLLS > 0
    // Sous la pluie, la couche grise n'est pas égale : de longs rouleaux en travers du vent, séparés par des bandes plus
    // minces où le jour passe un peu, comme sous une nappe de stratocumulus ondulés.
    if (rainStrength > 0.01) {
        float force;
        vec2 wd = cloudWind(force);
        float across = dot(q, wd) / 520.0 + w1 * 1.6;
        float gap = smoothstep(0.55, 0.95, 0.5 + 0.5 * sin(across * 6.2832));
        cover *= 1.0 - gap * rainStrength * 0.32 * float(RAIN_ROLLS);
    }
#endif
#if CLOUD_TYPES > 0
    float kind = smoothstep(0.30, 0.72, w2);
    // L'orage fait monter les cumulus en tours, la pluie fine les étale en couche grise.
    kind = clamp(kind + 0.6 * thunderStrength - 0.25 * rainStrength * (1.0 - thunderStrength), 0.0, 1.0);
    float anvil = thunderStrength * smoothstep(0.55, 0.9, kind);
    // Par beau temps, surtout des bancs plats : de larges nappes aux bords flous plutôt que des flocons nets, comme le
    // ciel de Recrafted. L'orage garde ses tours et ses enclumes.
    kind = mix(kind * 0.3, kind, thunderStrength);
#else
    float kind = 0.6;
    float anvil = 0.0;
#endif
    return vec3(cover, kind, anvil);
}

/** Le profil d'une famille selon la hauteur dans la couche (0 bas, 1 haut). */
float cloudProfile(float h, float kind, float anvil, float coverage) {
    // Un banc plat ne monte pas haut ; un cumulus s'arrondit vers les deux tiers ; une tour atteint le sommet.
    float top = mix(0.34, mix(0.62, 0.85, coverage), smoothstep(0.0, 0.6, kind));
    top = mix(top, 0.98, anvil);
    float base = smoothstep(0.0, mix(0.04, 0.12, kind), h);
    float crown = 1.0 - smoothstep(top * mix(0.45, 0.62, kind), top, h);
    // L'enclume : la tour s'élargit en haut.
    crown = max(crown, anvil * smoothstep(0.72, 0.86, h) * (1.0 - smoothstep(0.93, 1.0, h)));
    return base * crown;
}

/** Densité en un point du monde (0 hors de la couche). detail : ajouter l'érosion fine des bords. */
float cloudDensity(vec3 p, float coverage, bool detail) {
    float h = (p.y - cloudBase()) / CLOUD_TOP_SPAN;
    if (h <= 0.0 || h >= 1.0) return 0.0;
    vec3 q = p + cloudDrift();
    // Le vent d'altitude pousse le haut plus loin que le bas : les tours penchent sous le vent.
    float force;
    vec2 wind = cloudWind(force);
    q.xz -= wind * h * h * (30.0 + 50.0 * force);
    vec3 weather = cloudWeather(q.xz, coverage);
    if (weather.x <= 0.01) return 0.0;
    // Grandes masses : le bruit de forme, creusé par la couverture locale.
    float shape = cloudNoise(q * CLOUD_SHAPE_SCALE).r;
    float profile = cloudProfile(h, weather.y, weather.z, coverage);
    float v = shape * profile * (0.6 + 0.8 * weather.x);
    // L'enclume s'étale : sa base est plus pleine.
    v += weather.z * smoothstep(0.75, 0.9, h) * 0.25 * shape;
    // Un seuil adouci : des bords qui s'effilochent dans le ciel plutôt que des découpes. Un banc plat est plus égal.
    float edge = mix(0.30, 0.22, weather.y) * 2.2;
    float d = smoothstep(1.0 - weather.x, 1.0 - weather.x + edge, v);
    if (detail && d > 0.0) {
        // Les cellules fines, déformées par un tourbillon lent pour ne pas montrer leur grille.
        vec3 r = q * CLOUD_DETAIL_SCALE + vec3(0.0, frameTimeCounter * 0.002, 0.0);
        vec2 swirl = cloudNoise(r * 0.27 + 0.13).rg - 0.5;
        float fine = cloudNoise(r + vec3(swirl.x, swirl.y, swirl.x - swirl.y) * 0.45).g;
        // En bas, des bords effilochés ; en haut, des choux bourgeonnants (le motif inversé).
        float erode = mix(1.0 - fine, fine, smoothstep(0.15, 0.7, h));
        d = clamp(d - erode * mix(0.45, 0.22, h) * (1.0 - d * 0.6), 0.0, 1.0);
    }
    // Un banc plat est moins épais qu'un cumulus, une tour d'orage plus noire.
    return d * mix(0.7, 1.0, weather.y) * (1.0 + 0.6 * weather.z);
}

/**
 * Le voile de cirrus en un point du ciel (xz, repère monde) : des traînées étirées dans le sens du vent d'altitude,
 * qui vont et viennent sur des heures. Rend leur opacité (0..1).
 */
float cirrusCover(vec2 xz) {
#if CLOUD_CIRRUS > 0
    float force;
    vec2 wind = cloudWind(force);
    vec2 q = xz + cloudDrift().xz * 1.7;
    // Repère du vent : le long des traînées, puis en travers.
    vec2 r = vec2(dot(q, wind), dot(q, vec2(-wind.y, wind.x)));
    r *= vec2(1.0 / 2400.0, 1.0 / 520.0);
    float slow = frameTimeCounter * 0.0004;
    // Où il y en a : une carte qui change lentement, plus fournie par temps humide et avant la pluie.
    float field = cloudNoise(vec3(xz * 0.00009, 0.19 + slow)).r;
    float amount = clamp((field - 0.42) * 2.0 + 0.2 * airHumid + 0.4 * rainStrength, 0.0, 1.0) * float(CLOUD_CIRRUS) * 0.42;
    if (amount <= 0.0) return 0.0;
    // Les traînées : trois octaves, les fines tirées par les grosses.
    float a = cloudNoise(vec3(r, 0.61)).r;
    float b = cloudNoise(vec3(r * 3.1 + a * 0.3, 0.27)).r;
    float c = cloudNoise(vec3(r * 9.7 + b * 0.2, 0.93)).g;
    float v = a * 0.55 + b * 0.30 + c * 0.15;
    return smoothstep(1.0 - amount * 0.7, 1.2 - amount * 0.45, v) * 0.42;
#else
    return 0.0;
#endif
}

/**
 * La nappe d'altocumulus en un point du ciel (xz, repère monde) : des bancs qui vont et viennent sur des heures, faits
 * de petites cellules rondes serrées (le ciel pommelé), plus fournis par temps humide et avant la pluie.
 * Rend leur épaisseur (0..1).
 */
float altoCover(vec2 xz) {
#if CLOUD_ALTO > 0
    vec2 q = xz + cloudDrift().xz * 1.3;
    float slow = frameTimeCounter * 0.0003;
    float field = cloudNoise(vec3(xz * 0.00012, 0.47 + slow)).r;
    float amount = clamp((field - 0.47) * 2.4 + 0.25 * airHumid + 0.3 * rainStrength, 0.0, 1.0) * float(CLOUD_ALTO) * 0.34;
    if (amount <= 0.0) return 0.0;
    // Les bancs, puis les cellules qui les découpent, un peu tirées par le vent.
    float bank = cloudNoise(vec3(q / 1100.0, 0.33)).r;
    float cells = cloudNoise(vec3(q / 260.0 + bank * 0.35, 0.71)).g;
    float v = cells * 0.65 + bank * 0.35;
    return smoothstep(1.0 - amount, 1.0 - amount + 0.20, v);
#else
    return 0.0;
#endif
}

/** Ombre des nuages sur le sol, au point p (repère monde) : la lumière qui traverse la couche vers l'astre. */
float cloudShadow(vec3 p, vec3 light) {
    if (light.y < 0.05) return 1.0;
    float coverage = cloudCoverage();
    float depth = 0.0;
    for (int i = 0; i < 3; i++) {
        float y = cloudBase() + CLOUD_TOP_SPAN * (0.2 + 0.3 * float(i));
        vec3 q = p + light * ((y - p.y) / light.y);
        depth += cloudDensity(q, coverage, false);
    }
    // Le voile de cirrus retient un peu du soleil, sans ombre nette.
    float veil = cirrusCover((p + light * ((cirrusHeight() - p.y) / light.y)).xz);
    // La nappe d'altocumulus, un peu plus, en taches douces.
    float alto = altoCover((p + light * ((altoHeight() - p.y) / light.y)).xz);
    return mix(1.0, exp(-depth * 2.2) * (1.0 - 0.3 * veil) * (1.0 - 0.35 * alto), CLOUD_SHADOWS);
}
