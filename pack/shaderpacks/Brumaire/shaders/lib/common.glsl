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
    // Un gris chaud, à peine olive : chez Recrafted la lune verse une lumière grise et le sol reste olive sombre.
    vec3 moon = luma(skyRow(2)) * vec3(0.54, 0.51, 0.38) * 0.15;
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
    // L'astre qui éclaire est celui de la carte d'ombre (gLightDir) : sa couleur suit le même choix, sans quoi, dans les
    // instants où le jeu a déjà passé l'ombre à la lune et où le soleil n'a pas fini de se coucher, la lumière venue de
    // la lune avait la couleur du soleil.
    gLightColor = (dot(gLightDir, gSunDir) > 0.0 ? sun : moon) * fade * (1.0 - 0.85 * rainStrength);
    // Lumière du ciel : sa moyenne, un peu désaturée (le ciel n'est jamais d'un seul bleu), plus le soleil renvoyé
    // par le sol et une lueur nocturne qui ne laisse jamais le noir complet.
    vec3 sky = skyRow(0);
    vec3 amb = mix(sky, vec3(luma(sky)) * vec3(1.03, 1.0, 0.93), 0.78) * 1.1 + (e > 0.0 ? sun * fade * 0.06 : vec3(0.0));
    // Au couchant, le ciel encore clair n'éclaire plus guère le sol : le paysage à contre-jour reste sombre sous un
    // ciel lumineux, comme chez Recrafted.
    amb *= mix(1.0, 0.72, gDusk);
    // La lueur nocturne, faible et d'un gris chaud : les nuits de Recrafted ne sont pas bleues.
    amb += vec3(0.0078, 0.0075, 0.006);
    amb = mix(amb, vec3(luma(amb)) * gLunar, 0.7 * gLunarK);
    // Sous la pluie, la lumière du ciel tire vers le même gris beige, sans éteindre la couleur de l'herbe mouillée, et
    // baisse : le sol reste sombre sous un ciel gris clair (Recrafted : sol vers 0,21-0,26, ciel vers 0,54).
    gAmbient = mix(amb, vec3(luma(amb)) * vec3(1.0, 0.95, 0.86) * 0.80, rainStrength * 0.45) * mix(1.0, 0.35, rainStrength);
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
    // Puis la plus grande part de ce bleu repart : le ciel de nuit de Recrafted est un gris presque neutre.
    c = mix(c, vec3(luma(c)) * vec3(0.96, 1.0, 1.02), 0.6 * night);
    // Un ciel de nuit quatre à cinq fois plus clair que le terrain (vers 0,40 sur une échelle de 0 à 1) : les crêtes se
    // découpent en noir dessus, comme dans Recrafted, au lieu de se fondre dans un ciel aussi sombre qu'elles. Vers
    // l'horizon, à peine éclairci, sur une large pente, et d'un gris chaud : chez Recrafted le bas du ciel de nuit est
    // plus sombre que le zénith, pas un mur clair et froid derrière les collines.
    c *= mix(1.0, mix(1.2, 4.5, smoothstep(0.0, 0.7, d.y)), night);
    c *= mix(vec3(1.0), vec3(1.15, 1.0, 0.85), night * exp(-max(d.y, 0.0) * 5.0));
    c += night * vec3(0.00105, 0.00108, 0.0009) * exp(-max(d.y, 0.0) * 7.0);
    c = mix(c, vec3(luma(c)) * gLunar * 1.1, 0.6 * gLunarK);
    // Au couchant, le ciel autour du soleil moins blanc : le carré du soleil s'y détache.
    c *= 1.0 - 0.8 * gDusk * pow(max(dot(d, gSunDir), 0.0), 3.0);
    // Un ciel d'été voilé, pâle, presque blanc vers l'horizon : jamais d'un bleu profond (celui d'une carte postale).
    // Au zénith, un bleu clair reste lisible (moitié grisé seulement).
    // Au couchant, le ciel reprend ses couleurs : une bande pêche et rose au-dessus d'un horizon clair.
    c = mix(c, vec3(luma(c)) * vec3(0.95, 0.99, 1.05), (0.6 + 0.35 * (1.0 - smoothstep(0.0, 0.45, d.y))) * gDay * (1.0 - 0.75 * gDusk));
    // Et plus clair en hauteur : le ciel de jour de Recrafted, voilé, approche le blanc.
    c *= mix(1.0, 1.7, gDay * (1.0 - gDusk) * smoothstep(0.05, 0.6, d.y));
    // Au couchant, le haut du ciel pâlit lui aussi : un bleu lavande et laiteux au-dessus de la bande pêche, pas un
    // bleu franc (Recrafted : saturation vers 0,10 au zénith, ici 0,22-0,26 avant).
    c = mix(c, vec3(luma(c)) * vec3(1.0, 0.98, 1.02), 0.55 * gDusk * smoothstep(0.08, 0.40, d.y));
    // Sous la pluie, un gris beige, celui des nuages chargés (Recrafted : teinte 36-38°, saturation 0,11-0,18).
    c = mix(c, vec3(luma(c)) * vec3(0.92, 0.83, 0.72) * 0.9, rainStrength * 0.85);
    return c + vec3(0.55, 0.60, 0.80) * gFlash * (0.6 + 0.4 * max(d.y, 0.0));
}

/** Le disque du soleil, à ajouter au ciel, de la couleur que l'air lui laisse : carré (SUN_SQUARE) ou rond. */
vec3 sunDisc(vec3 d) {
#if SUN_SQUARE > 0
    // Le soleil du jeu, carré comme la lune et comme celui de Recrafted : un cœur blanc éblouissant dans un cadre jaune
    // plus terne, les bords droits à l'écran.
    if (dot(d, gSunDir) <= 0.0) return vec3(0.0);
    // Les bords suivent la course du soleil, comme le carré du jeu : un côté parallèle à l'axe autour duquel il tourne
    // (l'axe nord-sud penché de sunPathRotation), l'autre le long de sa course.
    float tilt = radians(sunPathRotation);
    vec3 axisA = vec3(0.0, sin(tilt), cos(tilt));
    vec3 axisB = vec3(0.0, -sin(tilt), cos(tilt));
    vec3 side = abs(dot(axisA, gSunDir)) < abs(dot(axisB, gSunDir)) ? axisA : axisB;
    side = normalize(side - gSunDir * dot(side, gSunDir));
    vec3 up = cross(side, gSunDir);
    vec3 off = d - gSunDir * dot(d, gSunDir);
    float m = max(abs(dot(off, side)), abs(dot(off, up)));
    float half_ = 0.045 * SUN_SIZE;
    float px = fwidth(m);
    float disc = 1.0 - smoothstep(half_ - px, half_ + px, m);
    float core = 1.0 - smoothstep(half_ * 0.86 - px, half_ * 0.86 + px, m);
    // Le cœur reste blanc même au ras de l'horizon (une lumière trop vive pour garder sa couleur) ; le cadre prend
    // le jaune ou l'orangé que l'air laisse passer. Sans cela, un soleil bas très saturé sortait plus terne que son halo.
    vec3 t = skyRow(1);
    vec3 rim = max(luma(t), 0.6) * vec3(1.0, 0.86, 0.50) * 2.4 * smoothstep(-0.03, 0.04, gSunDir.y);
    // Un blanc franc qui ne dépend pas de la couleur du soir, sinon le carré se vidait au couchant (cadre seul visible).
    vec3 white = max(luma(t), 0.6) * vec3(1.0, 0.98, 0.92) * 3.0 * smoothstep(-0.03, 0.04, gSunDir.y);
    return mix(rim, white, core) * (SUN_POWER * 1.8) * disc * (1.0 - rainStrength);
#else
    float c = dot(d, gSunDir);
    float disc = smoothstep(0.99880, 0.99915, c);
    float r = clamp(sqrt(max(1.0 - c, 0.0) / (1.0 - 0.99897)), 0.0, 1.0);
    float limb = 1.0 - 0.55 * (1.0 - sqrt(max(1.0 - r * r, 0.0)));
    return skyRow(1) * (SUN_POWER * 1.8) * disc * limb * (1.0 - rainStrength);
#endif
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
