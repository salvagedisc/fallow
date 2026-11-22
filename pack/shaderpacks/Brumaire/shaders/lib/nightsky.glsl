// Le ciel de nuit : des étoiles qui tournent avec le ciel autour de son pôle, de toutes les tailles et de toutes les
// couleurs (bleu-blanc à orangé), qui scintillent d'autant plus qu'elles sont basses ; la Voie lactée en travers,
// plus dense vers son centre, coupée de bandes de poussière sombre ; et l'auréole que la brume fait autour de la lune.
// La lune pleine pâlit la Voie lactée et noie les étoiles les plus faibles ; la pluie et l'aube les effacent.
// Demande clouds.glsl avant lui (le bruit de la Voie lactée). Directions en repère monde.

#define NIGHTSKY_GLSL
uniform int moonPhase;
uniform int worldDay;

float starHash(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.x + p.y) * p.z);
}

/**
 * Le repère du ciel étoilé : le pôle autour duquel tournent les astres (l'axe nord-sud, penché de sunPathRotation),
 * et deux axes qui suivent le soleil. Les étoiles y sont fixes : elles tournent avec lui.
 */
mat3 celestialFrame() {
    float tilt = radians(sunPathRotation);
    vec3 a = vec3(0.0, sin(tilt), cos(tilt));
    vec3 b = vec3(0.0, -sin(tilt), cos(tilt));
    // Le bon pôle est perpendiculaire au soleil ; le doute n'existe qu'au ras de l'horizon, où aucune étoile ne se voit.
    vec3 pole = abs(dot(gSunDir, a)) < abs(dot(gSunDir, b)) ? a : b;
    vec3 e1 = normalize(gSunDir - pole * dot(gSunDir, pole));
    vec3 e2 = cross(pole, e1);
    return mat3(e1, e2, pole);
}

/** Une face de cube et sa position dessus (-1..1) : un découpage du ciel en cellules presque égales. */
vec3 cubeFace(vec3 c, out float face) {
    vec3 a = abs(c);
    if (a.x >= a.y && a.x >= a.z) { face = c.x > 0.0 ? 0.0 : 1.0; return vec3(c.yz / a.x, a.x); }
    if (a.y >= a.z) { face = c.y > 0.0 ? 2.0 : 3.0; return vec3(c.xz / a.y, a.y); }
    face = c.z > 0.0 ? 4.0 : 5.0;
    return vec3(c.xy / a.z, a.z);
}

/** Le point de la sphère qui correspond à la position uv (-1..1) sur une face du cube. */
vec3 cubePoint(float face, vec2 uv) {
    vec3 p = face < 1.5 ? vec3(face < 0.5 ? 1.0 : -1.0, uv) : (face < 3.5 ? vec3(uv.x, face < 2.5 ? 1.0 : -1.0, uv.y)
                                                                          : vec3(uv, face < 4.5 ? 1.0 : -1.0));
    return normalize(p);
}

/** Couleur d'une étoile selon sa température, de 0 (rouge-orangé) à 1 (bleu-blanc). */
vec3 starColor(float k) {
    vec3 warm = vec3(1.00, 0.70, 0.45);
    vec3 sun = vec3(1.00, 0.94, 0.86);
    vec3 hot = vec3(0.70, 0.80, 1.00);
    return k < 0.5 ? mix(warm, sun, k * 2.0) : mix(sun, hot, k * 2.0 - 1.0);
}

/**
 * Une couche d'étoiles : une cellule sur le cube en porte une au plus, à une place, un éclat et une couleur tirés au
 * sort pour elle. density : part des cellules qui en ont une ; pix : taille d'un pixel (radians).
 */
vec3 starLayer(vec3 c, float cells, float density, float seed, float pix, float t, float low) {
    float face;
    vec3 f = cubeFace(c, face);
    vec2 g = f.xy * cells;
    vec2 cell = floor(g);
    vec3 id = vec3(cell, face * 131.0 + seed);
    if (starHash(id) > density) return vec3(0.0);
    vec2 at = 0.25 + 0.5 * vec2(starHash(id + 17.1), starHash(id + 41.7));
    // Distance angulaire au centre de l'étoile, mesurée sur la sphère : ronde partout, même au bord d'une face.
    float d = length(c - cubePoint(face, (cell + at) / cells));
    float r = pix * 0.85;
    float core = exp(-sq(d / r));
    if (core < 0.004) return vec3(0.0);
    float flux = pow(starHash(id + 7.3), 9.0) * 4.0 + 0.025;
    // Le scintillement : l'air qui tremble devant l'étoile, plus fort près de l'horizon où il y en a plus.
    float h = starHash(id + 3.9);
    float twinkle = sin(t * (6.0 + 7.0 * h) + h * 40.0) * sin(t * (2.3 + 3.1 * h) + h * 11.0);
    flux *= 1.0 + twinkle * mix(0.18, 0.75, low);
    return starColor(starHash(id + 29.3)) * flux * core;
}

/** La Voie lactée en c (repère du ciel) : sa lueur, et rend aussi dans band la densité d'étoiles qu'elle ajoute. */
vec3 milkyWay(vec3 c, out float band) {
    // Son plan est penché sur l'équateur du ciel ; son centre, plus épais et plus chaud, est d'un côté.
    vec3 galPole = normalize(vec3(0.46, 0.58, 0.67));
    vec3 galCentre = normalize(cross(galPole, vec3(0.0, 0.0, 1.0)));
    float b = dot(c, galPole);
    float l = dot(c, galCentre);
    // Le bord du disque se froisse : la largeur du ruban varie le long de lui.
    float wobble = cloudNoise(c * 0.8 + 0.31).r - 0.5;
    float lat = b + wobble * 0.07;
    band = exp(-sq(lat / 0.17));
    float bulge = exp(-sq(lat / 0.10)) * smoothstep(0.1, 1.0, l) * smoothstep(0.1, 1.0, l);
    float n1 = cloudNoise(c * 1.7 + 0.11).r;
    float n2 = cloudNoise(c * 4.6 + 0.37).r;
    float n3 = cloudNoise(c * 13.0 + 0.73).g;
    float glow = band * (0.35 + 0.9 * n1 * n2) + bulge * (0.9 + 0.6 * n2);
    // Des grains : les étoiles trop faibles pour se voir seules, en nuées.
    glow += band * pow(n3, 6.0) * 1.6;
    // La poussière : des bandes sombres qui suivent le plan, un peu décalées de son milieu, déchirées.
    float dustShape = cloudNoise(c * 3.2 + 0.53).r * 0.6 + cloudNoise(c * 9.1 + 0.19).g * 0.4;
    float dust = smoothstep(0.42, 0.70, dustShape) * exp(-sq((lat - 0.02) / 0.06));
    glow *= 1.0 - 0.78 * dust;
    vec3 tint = mix(vec3(0.74, 0.82, 1.00), vec3(1.00, 0.86, 0.66), clamp(bulge * 1.5, 0.0, 1.0));
    return tint * glow;
}

/** Bruit de valeur lissé sur un plan, pour les draperies de l'aurore. */
float auroraNoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = starHash(vec3(i, 7.0)), b = starHash(vec3(i + vec2(1.0, 0.0), 7.0));
    float c = starHash(vec3(i + vec2(0.0, 1.0), 7.0)), e = starHash(vec3(i + 1.0, 7.0));
    return mix(mix(a, b, f.x), mix(c, e, f.x), f.y);
}

/**
 * L'aurore polaire, dans le froid seulement, certaines nuits plus que d'autres : des draperies qui pendent d'un ruban
 * tordu, vertes en bas et pourpres en haut, qui ondulent lentement. Le ruban est un tracé sur un plan du ciel, à
 * quelques hauteurs empilées ; chaque hauteur le décale un peu, ce qui fait les plis verticaux du rideau.
 * strength : 0..1, selon la nuit, le froid et le ciel clair.
 */
vec3 aurora(vec3 d, float strength) {
    if (strength <= 0.001 || d.y < 0.02) return vec3(0.0);
    float t = frameTimeCounter * 0.025;
    vec3 sum = vec3(0.0);
    const int LAYERS = 14;
    for (int i = 0; i < LAYERS; i++) {
        float k = float(i) / float(LAYERS - 1);
        // Le ciel vu comme un plafond : plus haut à chaque couche, la draperie se lit de bas en haut.
        vec2 p = d.xz / (d.y + 0.12) * (1.0 + 0.55 * k) * 1.6;
        // Le ruban : une ligne tordue par deux octaves de bruit, qui dérive.
        float warp = auroraNoise(p * 0.55 + vec2(t, -t * 0.6)) * 2.2 + auroraNoise(p * 1.7 - vec2(t * 1.7, 0.0)) * 0.6;
        float along = p.x * 0.35 + p.y * 0.18 + warp;
        float line = abs(fract(along * 0.5) - 0.5);
        // Le bas du rideau est un bord net ; plus haut, la lumière s'étale et pâlit.
        float width = 0.018 + 0.05 * k;
        float ribbon = exp(-sq(line / width)) + 0.25 * exp(-sq(line / (width * 4.0)));
        // Les rais verticaux : des fils serrés le long du ruban, qui glissent et changent d'éclat.
        float u = (p.x - p.y) * 9.0;
        float rays = auroraNoise(vec2(u + t * 6.0, float(i) * 0.15)) * 0.6 + auroraNoise(vec2(u * 2.7 - t * 9.0, 5.0)) * 0.4;
        rays = 0.15 + 1.4 * rays * rays;
        vec3 hue = mix(vec3(0.10, 1.00, 0.40), vec3(0.70, 0.18, 0.95), smoothstep(0.30, 1.0, k));
        sum += hue * ribbon * rays * pow(1.0 - k, 1.4);
    }
    // Basse sur l'horizon, elle s'enfonce dans l'air ; tout en haut, elle s'amincit.
    float fade = smoothstep(0.02, 0.18, d.y) * (1.0 - 0.5 * smoothstep(0.6, 1.0, d.y));
    return sum / float(LAYERS) * fade * strength * 0.30;
}

/**
 * Les étoiles filantes : toutes les quelques secondes, parfois, un trait vif traverse une part du ciel en une
 * demi-seconde et laisse une traînée qui s'efface. c : direction dans le repère du ciel ; pix : taille d'un pixel.
 */
vec3 meteors(vec3 c, float pix) {
    vec3 sum = vec3(0.0);
    for (int lane = 0; lane < 2; lane++) {
        float period = 7.0 + 4.0 * float(lane);
        float slot = floor((frameTimeCounter + float(lane) * 3.1) / period);
        float phase = fract((frameTimeCounter + float(lane) * 3.1) / period) * period;
        vec3 id = vec3(slot, float(lane) * 17.0, 5.0);
        if (starHash(id) > 0.45) continue;
        float life = 0.6 + 0.5 * starHash(id + 1.3);
        if (phase > life + 0.8) continue;
        // Un départ et une direction tirés au sort, haut dans le ciel.
        vec3 start = normalize(vec3(starHash(id + 2.1) * 2.0 - 1.0, starHash(id + 3.7) * 2.0 - 1.0, 0.45 + starHash(id + 4.9)));
        vec3 side = normalize(cross(start, vec3(starHash(id + 6.2) - 0.5, starHash(id + 7.4) - 0.5, 0.3)));
        float span = 0.25 + 0.3 * starHash(id + 8.8);
        float head = span * clamp(phase / life, 0.0, 1.0);
        // Position le long du trait (angle depuis le départ) et écart au trait.
        float along = atan(dot(c, side), dot(c, start));
        float off = abs(dot(c, normalize(cross(start, side))));
        if (along < 0.0 || along > head) continue;
        float width = pix * 0.9;
        float core = exp(-sq(off / width));
        // La traînée : vive à la tête, qui s'éteint derrière elle et après le passage.
        float tail = exp(-(head - along) / (0.06 + 0.10 * span));
        float fadeOut = 1.0 - smoothstep(life, life + 0.8, phase);
        sum += vec3(0.85, 0.92, 1.0) * core * tail * fadeOut * (1.2 + 2.0 * starHash(id + 9.9));
    }
    return sum;
}

#if SKY_TWILIGHT_BELT > 0
/**
 * L'ombre de la Terre et la ceinture de Vénus : au coucher et au lever, à l'opposé du soleil, une bande gris-bleu
 * monte de l'horizon (le ciel qui n'est plus au soleil), coiffée d'une bande rose (l'air encore éclairé par le soleil
 * rouge). L'ombre monte à mesure que le soleil descend. Rend le ciel sky retouché.
 */
vec3 twilightBelt(vec3 d, vec3 sky) {
    float e = asin(clamp(gSunDir.y, -1.0, 1.0));
    float amount = (1.0 - smoothstep(0.02, 0.08, e)) * smoothstep(-0.20, -0.11, e) * (1.0 - rainStrength);
    if (amount <= 0.001 || d.y < -0.08) return sky;
    vec2 antiH = normalize(-gSunDir.xz + vec2(1e-5, 0.0));
    vec2 dirH = normalize(d.xz + vec2(1e-5, 0.0));
    float side = smoothstep(-0.35, 0.85, dot(antiH, dirH));
    float a = asin(clamp(d.y, -1.0, 1.0));
    // Le bord de l'ombre : au ras de l'horizon au coucher, jusqu'à une dizaine de degrés quand le soleil est descendu.
    // (Le brouillard couvre les premiers degrés : l'ombre part de plus haut pour rester lisible.)
    float edge = clamp(-e * 1.4, 0.0, 0.20) + 0.06;
    float shadow = 1.0 - smoothstep(edge - 0.025, edge + 0.015, a);
    float belt = exp(-sq((a - edge - 0.07) / 0.065));
    float l = luma(sky);
    vec3 dusk = mix(sky, vec3(l) * vec3(0.58, 0.66, 0.98) * 0.62, shadow * side * amount * 0.9);
    return dusk + vec3(1.0, 0.55, 0.65) * l * 1.3 * belt * side * amount;
}
#endif

#if SKY_RAYS > 0
/**
 * Les rayons crépusculaires du ciel : quand le soleil bas passe entre des nuages épars, leurs ombres tracent dans l'air
 * des bandes sombres et claires qui partent du soleil en éventail, traversent tout le ciel et se rejoignent au point
 * opposé (rayons anticrépusculaires). Rend le ciel sky retouché.
 */
vec3 skyRays(vec3 d, vec3 sky) {
    float e = gSunDir.y;
    float twilight = smoothstep(-0.16, -0.10, e) * (1.0 - smoothstep(0.06, 0.14, e));
    float c = cloudCoverage();
    float amount = twilight * clamp(4.0 * c * (1.0 - c), 0.0, 1.0) * (1.0 - rainStrength);
    if (amount <= 0.001 || d.y < -0.05) return sky;
    vec3 u = normalize(cross(gSunDir, vec3(0.0, 1.0, 0.0)) + vec3(1e-5, 0.0, 0.0));
    vec3 v = cross(u, gSunDir);
    float phi = atan(dot(d, v), dot(d, u));
    float theta = acos(clamp(dot(d, gSunDir), -1.0, 1.0));
    // Les ombres des nuages, tirées au sort chaque jour, qui glissent à peine. Quelques larges bandes, pas un éventail
    // de traits fins et réguliers (on aurait dit une étoile d'objectif ; Recrafted n'en montre presque pas).
    float n = cloudNoise(vec3(phi * 0.8, float(worldDay) * 0.37 + frameTimeCounter * 0.0015, 0.53)).r;
    float beam = smoothstep(0.30, 0.70, n);
    // Nets près du soleil, plus pâles au milieu du ciel, qui se resserrent de nouveau à l'opposé.
    float w = exp(-theta / 0.40) + 0.5 * exp(-(3.14159 - theta) / 0.35) + 0.05;
    // Surtout bas sur l'horizon : vers le zénith, l'air est trop mince pour bien les montrer.
    w *= smoothstep(-0.03, 0.06, d.y) * (0.35 + 0.65 * (1.0 - smoothstep(0.25, 0.75, d.y)));
    float k = clamp(w * amount, 0.0, 1.0);
    return sky * (1.0 - 0.14 * (1.0 - beam) * k) + sky * 0.06 * beam * k;
}
#endif

#if SKY_VENUS > 0
/**
 * L'étoile du berger : Vénus, à 25 à 46 degrés du soleil selon les semaines, qui s'allume avant toutes les étoiles
 * au crépuscule (ou les dernières à l'aube) et brille plus qu'aucune. Un point vif, sans scintillement, à peine
 * jaune, avec la petite auréole que l'œil lui voit.
 */
vec3 eveningStar(vec3 d, float pix) {
    float e = gSunDir.y;
    float seen = (1.0 - smoothstep(-0.06, 0.0, e)) * (1.0 - smoothstep(0.05, 0.25, rainStrength));
    if (seen <= 0.001) return vec3(0.0);
    mat3 f = celestialFrame();
    float elong = radians(25.0 + 21.0 * (0.5 + 0.5 * sin(float(worldDay) * 6.2832 / 58.0)));
    vec3 east = cos(elong) * f[0] + sin(elong) * f[1];
    vec3 west = cos(elong) * f[0] - sin(elong) * f[1];
    // Du côté où elle est le plus haut : après le soleil le soir, avant lui le matin.
    vec3 star = east.y > west.y ? east : west;
    if (star.y < -0.02) return vec3(0.0);
    float r = length(d - star);
    float core = exp(-sq(r / (pix * 0.95)));
    float glow = exp(-r / (pix * 4.0)) * 0.12;
    float air = exp(-0.10 / max(star.y + 0.04, 0.02));
    return vec3(1.0, 0.96, 0.86) * (core * 3.2 + glow) * seen * air;
}
#endif

#if SKY_COMET > 0
/**
 * La comète : quelques nuits de suite, de loin en loin, une tête floue et deux queues tournées à l'opposé du soleil,
 * la queue de poussière, large, jaunâtre et courbée, et la queue de gaz, fine, droite et bleutée. Fixe parmi les
 * étoiles d'une nuit à l'autre. c est la direction dans le repère du ciel, où le soleil est sur le premier axe.
 */
vec3 comet(vec3 c, float pix) {
    float period = floor(float(worldDay) / 18.0);
    if (starHash(vec3(period, 23.0, 7.0)) > 0.45 || mod(float(worldDay), 18.0) > 4.0) return vec3(0.0);
    float th = radians(50.0 + 30.0 * starHash(vec3(period, 1.0, 2.0)));
    float ph = (starHash(vec3(period, 3.0, 4.0)) - 0.5) * 2.2;
    vec3 head = vec3(cos(th), sin(th) * cos(ph), sin(th) * sin(ph));
    if (starHash(vec3(period, 5.0, 6.0)) > 0.5) head.y = -head.y;
    vec3 rel = c - head;
    if (dot(rel, rel) > 0.25) return vec3(0.0);
    vec3 sun = vec3(1.0, 0.0, 0.0);
    vec3 t = normalize(head * dot(head, sun) - sun);
    vec3 b = cross(head, t);
    float along = dot(rel, t);
    float perp = dot(rel, b);
    float r = length(rel);
    vec3 light = vec3(0.0);
    // La tête : un noyau presque ponctuel dans une chevelure ronde.
    light += vec3(0.85, 1.0, 0.92) * (exp(-sq(r / (pix * 1.2))) * 1.4 + exp(-r / 0.006) * 0.35);
    if (along > 0.0) {
        float bend = perp - along * along * 0.9;
        float wDust = 0.005 + along * 0.16;
        light += vec3(1.0, 0.90, 0.72) * exp(-along / 0.11) * exp(-sq(bend / wDust)) * 0.45;
        float wGas = 0.0025 + along * 0.025;
        float streak = 0.75 + 0.25 * sin(perp / wGas * 3.0 + along * 40.0);
        light += vec3(0.55, 0.75, 1.0) * exp(-along / 0.22) * exp(-sq(perp / wGas)) * streak * 0.22;
    }
    return light;
}
#endif

#if SKY_ZODIACAL > 0
/**
 * La lumière zodiacale : par nuit noire, une heure après le coucher ou avant le lever, un cône de lueur très pâle qui
 * monte de l'horizon le long du chemin du soleil, la poussière du plan des planètes qu'il éclaire. c dans le repère du
 * ciel (soleil sur le premier axe, pôle sur le troisième).
 */
vec3 zodiacalLight(vec3 c) {
    float fromSun = acos(clamp(c.x, -1.0, 1.0));
    float width = 0.10 + 0.22 * smoothstep(1.4, 0.3, fromSun);
    float cone = exp(-sq(c.z / width)) * exp(-(fromSun - 0.3) / 0.45) * smoothstep(0.25, 0.45, fromSun);
    return vec3(1.0, 0.94, 0.84) * cone;
}
#endif

#if SKY_AIRGLOW > 0
/**
 * La luminescence de l'air : par nuit sans lune, loin des lumières, une bande d'un vert très pâle à une dizaine de
 * degrés au-dessus de l'horizon, l'oxygène de la haute atmosphère qui rend la nuit la lumière reçue le jour, en
 * grandes ondulations lentes.
 */
vec3 airglow(vec3 d) {
    float band = exp(-sq((d.y - 0.13) / 0.075));
    float waves = cloudNoise(vec3(d.xz / max(d.y + 0.3, 0.3) * 0.8, 0.61) + frameTimeCounter * 0.0015).r;
    return vec3(0.38, 1.0, 0.50) * band * (0.45 + 0.9 * smoothstep(0.30, 0.75, waves));
}
#endif

/** Le ciel de nuit à ajouter au ciel peint, dans la direction d (repère monde). */
vec3 nightSky(vec3 d) {
#if SKY_STARS > 0 || SKY_MILKY_WAY > 0 || SKY_VENUS > 0 || SKY_COMET > 0 || SKY_ZODIACAL > 0 || SKY_AIRGLOW > 0
    float night = (1.0 - smoothstep(-0.30, -0.05, gSunDir.y)) * (1.0 - rainStrength);
    vec3 result = vec3(0.0);
    // L'auréole de la lune : la brume en diffuse un peu tout autour d'elle.
    vec3 moonDir = -gSunDir;
    if (moonDir.y > -0.08) {
        float a = acos(clamp(dot(d, moonDir), -1.0, 1.0));
        float haze = 0.6 + 0.8 * airHumid + 1.5 * rainStrength;
        result += gLightColor * (exp(-a / 0.030) * 0.30 + exp(-a / 0.20) * 0.05) * haze * smoothstep(-0.08, 0.05, moonDir.y);
    }
    float pix = 2.0 / (viewHeight * gbufferProjection[1][1]);
#if SKY_VENUS > 0
    // Vénus s'allume bien avant la nuit : elle ne suit pas le noir des étoiles.
    result += eveningStar(d, pix);
#endif
    if (night <= 0.001 || d.y < -0.05) return result;
    vec3 c = transpose(celestialFrame()) * d;
    // Plus d'air à traverser vers l'horizon : les astres bas pâlissent.
    float air = exp(-0.10 / max(d.y + 0.04, 0.02));
    float low = 1.0 - smoothstep(0.05, 0.5, d.y);
    // La phase 0 est la pleine lune, la phase 4 la nouvelle lune.
    float full = 1.0 - float(min(moonPhase, 8 - moonPhase)) / 4.0;
    float band = 0.0;
    vec3 sky = vec3(0.0);
#if SKY_MILKY_WAY > 0
    sky += milkyWay(c, band) * 0.032 * mix(1.0, 0.35, full);
#endif
#if SKY_STARS > 0
    float t = frameTimeCounter;
    // Trois couches : quelques étoiles vives, beaucoup de moyennes, et une poussière de faibles, dense dans la Voie lactée.
    // Peu d'étoiles franches, comme dans Recrafted : un ciel de nuit sombre et calme, pas un champ d'étoiles serré.
    // La lune pleine en éteint la plupart, d'autant plus qu'on regarde près d'elle.
    vec3 stars = starLayer(c, 90.0, 0.05, 1.0, pix, t, low) * 1.6;
    stars += starLayer(c, 210.0, 0.015 + 0.10 * band, 2.0, pix, t, low) * 0.5 * mix(1.0, 0.30, full);
    stars += starLayer(c, 420.0, 0.006 + 0.08 * band, 3.0, pix, t, low) * 0.3 * mix(1.0, 0.15, full);
    float nearMoon = smoothstep(0.80, 0.99, dot(d, -gSunDir)) * full;
    sky += stars * 0.12 * (1.0 - 0.8 * nearMoon);
#endif
#if SKY_METEORS > 0
    sky += meteors(c, pix) * 0.25;
#endif
#if SKY_COMET > 0
    sky += comet(c, pix) * 0.30 * mix(1.0, 0.55, full);
#endif
#if SKY_ZODIACAL > 0
    // Il faut une nuit vraiment noire : la lune et le crépuscule le noient.
    sky += zodiacalLight(c) * 0.16 * (1.0 - smoothstep(-0.30, -0.22, gSunDir.y)) * (1.0 - full * 0.85);
#endif
#if SKY_AIRGLOW > 0
    sky += airglow(d) * 0.035 * (1.0 - full * 0.8);
#endif
    vec3 lights = vec3(0.0);
#if SKY_AURORA > 0
    // L'aurore : seulement dans le froid (biome ou hiver), une nuit sur trois environ, plus ou moins forte.
    float cold = 1.0 - smoothstep(0.05, 0.35, airHeat);
    float tonight = smoothstep(0.62, 0.85, starHash(vec3(float(worldDay), 3.0, 11.0)));
    lights += aurora(d, cold * tonight * SKY_AURORA_STRENGTH);
#endif
    return result + (sky + lights) * night * air;
#else
    return vec3(0.0);
#endif
}
