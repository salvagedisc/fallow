// Uniformes communs, heure du jour, ciel.

uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform mat4 gbufferProjection;
uniform mat4 gbufferProjectionInverse;
uniform vec3 cameraPosition;
uniform vec3 sunPosition;
uniform vec3 shadowLightPosition;
uniform vec3 fogColor;
uniform float frameTimeCounter;
uniform float rainStrength;
uniform float far;
uniform float near;
uniform float viewWidth;
uniform float viewHeight;
uniform float nightVision;
uniform float blindness;
uniform float darknessFactor;
uniform int isEyeInWater;
uniform vec4 lightningBoltPosition; // xyz : l'éclair depuis la caméra ; w : 1 tant qu'il frappe
uniform ivec2 eyeBrightnessSmooth;

// Liaison avec le mod du pack. Sans lui, Iris laisse ces valeurs à zéro et le shader garde ses replis.
uniform float brumaireLink;
uniform vec4 brumaireMist;      // x : part de brume levée autour de la caméra, y : altitude du sol lissée
uniform vec4 brumaireStorm;     // x : force de la tempête de sable, y : de neige (module climat), de 0 à 1
uniform vec4 brumaireWind;      // xy : direction, z : force, w : turbulence
uniform vec4 brumaireClearing0; // xy : centre vu de la caméra, z : rayon, w : force
uniform vec4 brumaireClearing1;
uniform vec4 brumaireClearing2;
uniform vec4 brumaireClearing3;
uniform vec4 brumaireGust0;     // xy : direction / largeur, z : décalage / largeur, w : force de la crête
uniform vec4 brumaireGust1;
uniform vec4 brumairePush0;     // xyz : pieds d'un être vivant proche, depuis la caméra ; w : rayon d'herbes couchées
uniform vec4 brumairePush1;
uniform vec4 brumairePush2;
uniform vec4 brumairePush3;

#include "/lib/atmosphere.glsl"

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

vec3 gSunDir;     // vers le soleil, repère monde
vec3 gLightDir;   // vers l'astre qui éclaire (soleil ou lune)
vec3 gLightColor; // lumière directe
vec3 gAmbient;    // lumière du ciel
float gDay;       // 0 la nuit, 1 le jour
float gDusk;      // 1 quand le soleil rase l'horizon
float gFlash;     // l'éclair qui illumine le ciel et le paysage, 0..1
vec3 gLunar = vec3(1.0); // la teinte d'une lune d'Enhanced Celestials (lumière moyenne 1)
float gLunarK = 0.0;     // sa part dans la nuit : 0 sans lune colorée ou le jour

float luma(vec3 c) { return dot(c, LUMA); }
float sq(float x) { return x * x; }
vec3 toLinear(vec3 c) { return pow(c, vec3(2.2)); }

/** Coordonnées de lumière ramenées à 0..1 (x : blocs, y : ciel). */
vec2 lightCoord(vec2 raw) { return clamp((raw - 0.03125) * 1.0667, 0.0, 1.0); }

/** Bruit de tramage stable d'une image à l'autre. */
float dither(vec2 frag) { return fract(52.9829189 * fract(dot(frag, vec2(0.06711056, 0.00583715)))); }

void initEnv() {
    gSunDir = normalize(mat3(gbufferModelViewInverse) * sunPosition);
    gLightDir = normalize(mat3(gbufferModelViewInverse) * shadowLightPosition);
    float e = gSunDir.y;
    gDay = smoothstep(-0.10, 0.22, e);
    gDusk = 1.0 - smoothstep(0.0, 0.42, abs(e + 0.02));
    // La lumière directe s'éteint quand l'astre passe l'horizon : le relais soleil/lune ne se voit pas.
    float fade = smoothstep(0.0, 0.07, abs(e));
    // Couleurs lues dans la table du ciel : ce que l'atmosphère laisse du soleil (blanc à midi, orangé puis rouge
    // au ras de l'horizon) et de la lune (selon sa phase), tirée vers le bleu comme l'œil la voit la nuit.
    // Un soleil un peu chaud même à midi : la lumière d'un après-midi, pas celle d'un projecteur.
    vec3 sun = skyRow(1) / vec3(0.97, 0.89, 0.78) * 3.4 * vec3(1.0, 0.97, 0.90);
    // À peine bleutée : chez Recrafted la lune verse une lumière presque neutre, le sol reste olive sombre.
    vec3 moon = luma(skyRow(2)) * vec3(0.44, 0.50, 0.62) * 0.25;
#if LUNAR_EVENTS > 0
    // Les lunes d'Enhanced Celestials (de sang, bleue, des moissons) : le jeu teinte la texture de la lune, que
    // gbuffers_skytextured.vsh range dans la table du ciel. Sa couleur gagne la nuit : la lune et son halo, la
    // lumière qu'elle verse, le ciel et sa lueur. Une lune blanche ne change rien.
    vec3 tint = skyRow(SKY_LUNAR);
    float top = max(max(tint.r, tint.g), tint.b);
    if (top > 0.05) {
        vec3 hue = tint / top;
        float sat = 1.0 - min(min(hue.r, hue.g), hue.b);
        vec3 l = mix(vec3(1.0), hue, 0.75);
        // Un jaune faible tourne au kaki dans la nuit : une lune chaude (rouge devant le vert, vert devant le bleu)
        // penche vers l'orangé.
        l.g *= 1.0 - 0.18 * sat * step(hue.b, hue.g) * step(hue.g, hue.r);
        gLunar = l / mix(1.0, max(luma(l), 0.05), 0.7);
        gLunarK = smoothstep(0.05, 0.30, sat) * (1.0 - smoothstep(-0.12, 0.0, e)) * LUNAR_EVENTS_STRENGTH;
    }
    moon = mix(moon, luma(moon) * gLunar, gLunarK);
#endif
    gLightColor = (e > 0.0 ? sun : moon) * fade * (1.0 - 0.85 * rainStrength);
    // Lumière du ciel : sa moyenne, un peu désaturée (le ciel n'est jamais d'un seul bleu), plus le soleil renvoyé
    // par le sol et une lueur nocturne qui ne laisse jamais le noir complet.
    vec3 sky = skyRow(0);
    vec3 amb = mix(sky, vec3(luma(sky)) * vec3(1.03, 1.0, 0.93), 0.78) * 1.1 + (e > 0.0 ? sun * fade * 0.06 : vec3(0.0));
    // Au couchant, le ciel encore clair n'éclaire plus guère le sol : le paysage à contre-jour reste sombre sous un
    // ciel lumineux, comme chez Recrafted.
    amb *= mix(1.0, 0.72, gDusk);
    amb += vec3(0.012, 0.016, 0.026);
    amb = mix(amb, vec3(luma(amb)) * gLunar, 0.7 * gLunarK);
    gAmbient = mix(amb, vec3(luma(amb)) * vec3(0.95, 0.95, 0.93) * 0.75, rainStrength * 0.7);
    // Un éclair qui frappe blanchit tout d'un coup, en vacillant, moins quand il tombe loin.
    float flicker = 0.55 + 0.45 * sin(frameTimeCounter * 47.0) * sin(frameTimeCounter * 13.0 + 1.3);
    gFlash = lightningBoltPosition.w * flicker / (1.0 + length(lightningBoltPosition.xyz) / 160.0);
    gAmbient += vec3(0.62, 0.68, 0.90) * gFlash;
}

/** Couleur du ciel dans une direction (repère monde), sans astres ni brume. */
vec3 skyAt(vec3 d) {
    vec3 c = skyScatter(d, gSunDir) + vec3(0.004, 0.006, 0.012) * (1.0 - gDay);
    // La nuit, l'œil voit un peu en bleu.
    float night = 1.0 - smoothstep(-0.20, 0.0, gSunDir.y);
    c = mix(c, vec3(luma(c)) * vec3(0.78, 0.88, 1.08), night * 0.3);
    c = mix(c, vec3(luma(c)) * gLunar * 1.1, 0.6 * gLunarK);
    // Un ciel d'été voilé, pâle, presque blanc vers l'horizon : jamais d'un bleu profond (celui d'une carte postale).
    // Au couchant, le ciel reprend ses couleurs : une bande pêche et rose au-dessus d'un horizon clair.
    c = mix(c, vec3(luma(c)) * vec3(0.95, 0.99, 1.05), (0.52 + 0.25 * (1.0 - smoothstep(0.0, 0.45, d.y))) * gDay * (1.0 - 0.75 * gDusk));
    // Sous la pluie, un gris un peu chaud, celui des nuages chargés.
    c = mix(c, vec3(luma(c)) * vec3(0.85, 0.84, 0.80) * 0.9, rainStrength * 0.85);
    return c + vec3(0.55, 0.60, 0.80) * gFlash * (0.6 + 0.4 * max(d.y, 0.0));
}

/** Le disque du soleil, à ajouter au ciel : rond, plus sombre au bord, de la couleur que l'air lui laisse. */
vec3 sunDisc(vec3 d) {
    float c = dot(d, gSunDir);
    float disc = smoothstep(0.99880, 0.99915, c);
    float r = clamp(sqrt(max(1.0 - c, 0.0) / (1.0 - 0.99897)), 0.0, 1.0);
    float limb = 1.0 - 0.55 * (1.0 - sqrt(max(1.0 - r * r, 0.0)));
    return skyRow(1) * (SUN_POWER * 1.8) * disc * limb * (1.0 - rainStrength);
}

/** Les verts tirés vers un vert olive chaud, à luminance égale. */
vec3 olive(vec3 c) {
    float green = c.g - max(c.r, c.b);
    float w = clamp(green / max(c.g, 1e-4) * 2.5, 0.0, 1.0) * OLIVE;
    float l = luma(c);
    vec3 hue = vec3(0.94, 1.0, 0.50);
    vec3 target = l * hue / luma(hue);
    return mix(c, target, w);
}
