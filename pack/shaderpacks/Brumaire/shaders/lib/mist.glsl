// La brume : une nappe posée sur le sol, percée par les clairières, et un anneau au loin qui ne se lève jamais.

#ifdef DISTANT_HORIZONS
uniform int dhRenderDistance;
#endif

/** Intégrale de exp(-(y0 + t dy - base) / h) pour t de t0 à t1 : la brume traversée dans une nappe. */
float mistLayer(float y0, float dy, float t0, float t1, float h, float base) {
    float span = t1 - t0;
    if (span <= 0.0) return 0.0;
    float start = exp(clamp((base - y0 - t0 * dy) / h, -30.0, 3.0));
    float x = clamp(dy / h * span, -30.0, 1e6);
    float mean = abs(x) < 1e-3 ? 1.0 - 0.5 * x : (1.0 - exp(-x)) / x;
    return min(start * mean, 2.2) * span;
}

float mistAnchor() {
    // Altitude du pied de la nappe : le sol autour du joueur, sinon un peu sous la caméra.
    float a = brumaireLink > 0.5 ? brumaireMist.y : cameraPosition.y - 3.0;
    // Le jour, la nappe redescend au fond des vallées (MIST_DAY_LEVEL) : vu d'une colline, les pentes et les cimes
    // restent nettes et la brume dort en bas, comme dans Recrafted, au lieu d'un voile pâle sur tout le paysage. À
    // l'aube, au soir et la nuit, elle remonte au pied du joueur.
    return mix(a, min(a, MIST_DAY_LEVEL), smoothstep(0.0, 0.3, gSunDir.y));
}

float mistNearDensity() {
    // Plus dense à l'aube, la nuit et sous la pluie ; elle ne descend pas dans les grottes.
    float open = smoothstep(0.0, 0.55, float(eyeBrightnessSmooth.y) / 240.0);
    // Le jour garde sa nappe légère jusqu'au coucher : un soleil bas ne l'épaissit pas, sinon le couchant se noie dans un
    // halo orangé au lieu d'un paysage sombre sous un ciel coloré. Elle s'épaissit une fois le soleil passé.
    float hour = mix(0.80, 0.32, smoothstep(-0.14, 0.0, gSunDir.y));
    // L'air du biome : plus de brume là où il est humide, moins dans un désert.
    float biome = mix(0.75, 1.3, airHumid) * (1.0 - 0.85 * airDust());
    // Sous un couvert, de jour, l'air se voit : entre les troncs, ce qui est à vingt blocs pâlit dans un voile clair
    // (Recrafted). Le paysage à découvert garde sa nappe légère, sans quoi une vallée vue d'en haut se noie.
    float eye = float(eyeBrightnessSmooth.y) / 240.0;
    float under = (1.0 - smoothstep(0.86, 0.99, eye)) * smoothstep(0.25, 0.45, eye) * smoothstep(0.0, 0.3, gSunDir.y);
    // Au couchant, une nappe moitié moins épaisse : le paysage reste sombre et coloré, pas noyé dans un voile laiteux.
    return 0.010 * mix(1.0, 0.5, gDusk) * MIST_DENSITY * hour * biome * (1.0 + 3.0 * rainStrength) * open * (1.0 + 1.5 * under);
}

/** Brume retirée par une clairière : un cylindre vertical, au bord adouci par deux rayons. */
float mistHole(vec4 c, vec3 dir, float len, float y0, float h, float base) {
    if (c.z <= 0.0) return 0.0;
    float flat_ = length(dir.xz);
    float hole = 0.0;
    for (int i = 0; i < 2; i++) {
        float r = c.z * (i == 0 ? 1.0 : 0.72);
        if (flat_ < 1e-4) {
            if (dot(c.xy, c.xy) < r * r) hole += 0.5 * mistLayer(y0, dir.y, 0.0, len, h, base);
            continue;
        }
        vec2 d = dir.xz / flat_;
        float b = dot(d, c.xy);
        float disc = r * r - (dot(c.xy, c.xy) - b * b);
        if (disc <= 0.0) continue;
        float w = sqrt(disc);
        float t0 = max(b - w, 0.0) / flat_;
        float t1 = min((b + w) / flat_, len);
        hole += 0.5 * mistLayer(y0, dir.y, t0, t1, h, base);
    }
    return hole * c.w;
}

/**
 * Épaisseur optique de brume le long d'un rayon parti de la caméra. dir : unitaire, repère monde ;
 * len : longueur du rayon ; nearOnly rend aussi la part de la nappe proche, sans l'anneau.
 */
float mistDepth(vec3 dir, float len, out float nearOnly) {
    float y0 = cameraPosition.y;
    float base = mistAnchor();
    float h = MIST_HEIGHT;
    float density = mistNearDensity() * (1.0 - MIST_LIFT * brumaireMist.x);
    float through = mistLayer(y0, dir.y, 0.0, len, h, base);
    through -= mistHole(brumaireClearing0, dir, len, y0, h, base);
    through -= mistHole(brumaireClearing1, dir, len, y0, h, base);
    through -= mistHole(brumaireClearing2, dir, len, y0, h, base);
    through -= mistHole(brumaireClearing3, dir, len, y0, h, base);
    nearOnly = density * max(through, 0.0);

    // L'anneau : au-delà d'un rayon lié à la distance de rendu, une nappe dense.
#ifdef DISTANT_HORIZONS
    float edge = min(float(dhRenderDistance), MIST_RING_MAX);
#else
    float edge = far;
#endif
    float inner = edge * 0.55;
    float flat_ = max(length(dir.xz), 1e-4);
    float open = smoothstep(0.0, 0.55, float(eyeBrightnessSmooth.y) / 240.0);
    float ring = 5.0 / (edge - inner) * MIST_RING * open * mistLayer(y0, dir.y, inner / flat_, len, h, base);
    return nearOnly + ring;
}

/**
 * L'air entre l'œil et un point du paysage : au loin, tout prend la couleur du ciel bas, le bleu d'abord.
 * Rend la part de la couleur du point qui passe (par canal) ; inscat reçoit la lumière ajoutée.
 */
vec3 airVeil(vec3 dir, float len, out vec3 inscat) {
    vec3 k = vec3(0.50, 0.72, 1.0) * (AIR_DENSITY / 2600.0) * mix(0.8, 1.8, airHumid) * (1.0 + 1.5 * airDust());
    vec3 pass = exp(-k * len);
    vec3 sky = skyAt(normalize(vec3(dir.x, max(dir.y, 0.03), dir.z)));
    // L'air lointain tire sur le gris chaud des poussières plus que sur le bleu du ciel.
    inscat = mix(sky, vec3(luma(sky)) * vec3(1.04, 1.0, 0.92), 0.45) * (1.0 - pass);
    // Au couchant, le ciel bas vers le soleil est la partie la plus claire de l'image : l'air n'en prend qu'une part,
    // sinon le paysage face à l'astre se noie dans un halo au lieu de rester une silhouette sombre.
    inscat *= mix(1.0, 0.55, gDusk);
    return pass;
}

/** Teinte de la brume selon le biome : olive dans un marais ou une jungle, bleutée dans le froid, sable au désert. */
vec3 mistTint() {
    vec3 t = vec3(1.0);
    t = mix(t, vec3(0.94, 1.0, 0.86), smoothstep(0.55, 0.9, airHumid) * smoothstep(0.5, 0.9, airHeat));
    t = mix(t, vec3(0.92, 0.97, 1.06), 1.0 - smoothstep(0.05, 0.4, airHeat));
    return mix(t, vec3(1.08, 0.98, 0.84), airDust());
}

/** Diffusion vers l'avant des gouttelettes (Henyey-Greenstein). */
float mistPhase(float mu, float g) {
    return 0.0795775 * (1.0 - g * g) / pow(1.0 + g * g - 2.0 * g * mu, 1.5);
}

/** Couleur de la brume vue dans une direction ; sun : part de soleil qui l'atteint (0 sous un couvert). */
vec3 mistColor(vec3 dir, float sun) {
    vec3 body = mix(gAmbient, vec3(luma(gAmbient)), 0.75) * vec3(1.03, 1.0, 0.95) * 0.95;
    body *= mistTint();
    // Le jour, un voile à peine bleuté, comme l'air lointain de Recrafted, et non le gris chaud d'une poussière.
    body *= mix(vec3(1.0), vec3(0.97, 1.0, 1.04), gDay * (1.0 - gDusk));
    // La nuit, la brume ne reçoit que la lune et le ciel noir : elle reste sombre, un voile et pas un mur gris qui
    // éclaircirait tout le paysage.
    body *= mix(0.45, 1.0, gDay);
    // Au couchant, la brume n'a plus que la lueur d'un ciel qui baisse : elle s'assombrit avec le paysage au lieu de
    // le noyer dans un halo orangé ; elle reste lisible, une bande plus claire au creux des vallées.
    body *= mix(1.0, 0.5, gDusk);
    // Sous la pluie, une brume sépia qui avale le lointain, comme les averses de Recrafted.
    body = mix(body, vec3(luma(body)) * vec3(1.06, 0.99, 0.88), 0.7 * rainStrength);
    // Les gouttelettes renvoient surtout la lumière vers l'avant : un halo vers le soleil, un peu en arrière. Au
    // couchant ce halo est paille pâle, pas orange : le paysage reste vert sous un ciel coloré (Recrafted).
    float mu = dot(dir, gLightDir);
    float phase = 0.75 * mistPhase(mu, 0.5) + 0.25 * mistPhase(mu, -0.3);
    vec3 beam = mix(gLightColor, vec3(luma(gLightColor)) * vec3(1.0, 0.93, 0.78), 0.2 * gDusk) * (0.03 + 0.18 * phase * SHAFT_STRENGTH) * mix(1.0, 0.075, gDusk);
    return body + beam * sun;
}
