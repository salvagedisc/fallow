// L'eau : sa couleur, ce qu'elle absorbe et ce qu'elle diffuse, la manière dont sa surface renvoie et laisse passer la
// lumière, et les filets de lumière que ses vagues dessinent au fond.
// Unités : blocs. Lu par la surface (solid.fsh), le fond (light.glsl), la vue sous l'eau (mist.fsh) et le lointain.
// Demande clouds.glsl avant lui (le bruit des caustiques des rayons est celui des nuages).

#include "/lib/waves.glsl"

const float WATER_IOR = 1.333;

/** Teinte de l'eau : celle du biome, ramenée vers un vert-de-gris d'eau douce. */
vec3 waterHue(vec3 biome) {
    vec3 h = biome / max(max(biome.r, max(biome.g, biome.b)), 1e-4);
    vec3 sage = vec3(0.50, 0.78, 0.62);
    return mix(h, sage * (0.4 + 0.6 * luma(h) / luma(sage)), 0.85);
}

/**
 * Lumière perdue par bloc traversé (absorbée ou renvoyée ailleurs) : le rouge part le premier, puis le bleu ; un fond de
 * sable clair vire au vert-gris d'un lac et non au turquoise d'un lagon. Une eau verte ou brune est plus trouble.
 */
vec3 waterExtinction(vec3 hue) {
    return (vec3(0.62, 0.40, 0.56) + (1.0 - hue) * 0.10) * WATER_MURK;
}

/**
 * Part de la lumière perdue qui est diffusée plutôt qu'absorbée (albédo de diffusion) : les fines particules en
 * suspension renvoient surtout la couleur de l'eau, le reste est bu.
 */
vec3 waterAlbedo(vec3 hue) {
    return hue * 0.48;
}

/** Ce que renvoie l'eau profonde vue de loin : la lumière du ciel et de l'astre diffusée dans la masse. */
vec3 waterMurk(vec3 hue, float sky, float lit) {
    return hue * hue * (gAmbient * sky * 0.24 + gLightColor * lit * 0.04 + vec3(0.004, 0.006, 0.007));
}

/** Direction de la lumière de l'astre une fois entrée dans l'eau (vers le bas), redressée par la réfraction. */
vec3 waterSunDown() {
    vec3 down = refract(-gLightDir, vec3(0.0, 1.0, 0.0), 1.0 / WATER_IOR);
    return down.y < -0.05 ? down : vec3(0.0, -1.0, 0.0);
}

/**
 * Fresnel exact d'un dioptre non polarisé : la part de lumière renvoyée quand elle passe d'un milieu d'indice n1 à un
 * milieu d'indice n2 sous l'angle de cosinus cosi. 1 au-delà de l'angle limite (réflexion totale).
 */
float fresnel(float cosi, float n1, float n2) {
    cosi = clamp(cosi, 0.0, 1.0);
    float sint2 = sq(n1 / n2) * (1.0 - cosi * cosi);
    if (sint2 >= 1.0) return 1.0;
    float cost = sqrt(1.0 - sint2);
    float rs = (n1 * cosi - n2 * cost) / (n1 * cosi + n2 * cost);
    float rp = (n2 * cosi - n1 * cost) / (n2 * cosi + n1 * cost);
    return 0.5 * (rs * rs + rp * rp);
}

/** Distribution GGX des micro-facettes : la part tournée vers h, pour une rugosité a (au carré de la pente). */
float ggx(float nh, float a) {
    float a2 = a * a;
    float d = nh * nh * (a2 - 1.0) + 1.0;
    return a2 / (3.14159265 * d * d);
}

/** Masquage de Smith pour GGX : les facettes cachées par leurs voisines, vues sous le cosinus nx. */
float smithG1(float nx, float a) {
    float a2 = a * a;
    return 2.0 * nx / (nx + sqrt(a2 + (1.0 - a2) * nx * nx));
}

/** Fonction de phase de Henyey-Greenstein, ramenée à une moyenne de 1 sur la sphère. */
float henyeyGreenstein(float mu, float g) {
    float g2 = g * g;
    return (1.0 - g2) / pow(1.0 + g2 - 2.0 * g * mu, 1.5);
}

/**
 * La lumière que l'eau renvoie d'elle-même le long d'un trajet sous la surface, en une seule diffusion, intégrée
 * exactement : un point du trajet à la distance x reçoit le soleil qui a traversé x·cv/cs blocs d'eau (cv, cs :
 * cosinus du regard et du soleil avec la verticale) et le renvoie vers l'œil à travers x blocs.
 *
 * @param down direction du regard dans l'eau (unitaire), len longueur du trajet (blocs)
 * @param sun lumière de l'astre passée sous la surface ; sky lumière du ciel passée sous la surface
 */
vec3 waterScatterPath(vec3 ext, vec3 albedo, vec3 down, float len, vec3 sun, vec3 sky) {
    vec3 sunDown = waterSunDown();
    float cv = clamp(abs(down.y), 0.03, 1.0);
    float cs = max(-sunDown.y, 0.1);
    // Le soleil se diffuse surtout vers l'avant : l'eau s'allume quand on regarde vers lui depuis le fond.
    float mu = dot(sunDown, -down);
    float phase = mix(henyeyGreenstein(mu, 0.72), 1.0, 0.35);
    // Descendre (down.y < 0) éloigne le point de la surface ; remonter l'en rapproche (vue d'en dessous).
    float rise = down.y < 0.0 ? cv : -cv * 0.6;
    vec3 kSun = ext * max(1.0 + rise / cs, 0.15);
    vec3 kSky = ext * max(1.0 + rise * 1.25, 0.15);
    float l = min(len, 256.0);
    vec3 fromSun = sun * phase * 0.42 * (1.0 - exp(-kSun * l)) / kSun;
    vec3 fromSky = sky * (1.0 - exp(-kSky * l)) / kSky;
    return albedo * ext * (fromSun + fromSky);
}

vec3 waterInscatter(vec3 hue, vec3 down, float len, vec3 sun, vec3 sky) {
    vec3 ext = waterExtinction(hue);
    return waterScatterPath(ext, waterAlbedo(hue), down, len, sun, sky)
        + vec3(0.004, 0.006, 0.007) * hue * (1.0 - exp(-ext * min(len, 256.0)));
}

/** Taille d'un pixel (blocs) à la distance dist de la caméra. */
float pixelFootprint(float dist) {
    return dist * 2.0 / (viewHeight * gbufferProjection[1][1]);
}

/**
 * Caustiques : la lumière que la surface concentre ou disperse sur un point du fond, en couleur.
 *
 * Un rayon de l'astre frappe la surface en x et ressort dévié de c·∇h (c = 1 - 1/n : la loi de Snell aux petites
 * pentes) ; à depth blocs plus bas il arrive en x + depth·c·∇h. La lumière d'un petit carré de surface se retrouve
 * donc sur un carré déformé par I + depth·c·H (H : courbure de la surface), et son éclat est divisé par l'aire de
 * ce carré. Les pentes de la surface sont celles des vagues (lib/waves.glsl) : les filets suivent les vagues qu'on
 * voit. Le rouge et le bleu ne se réfractent pas tout à fait pareil : le bord des filets s'irise.
 *
 * @param world point du fond (repère monde) ; depth épaisseur d'eau au-dessus (blocs) ; count trains d'onde
 */
vec3 causticLight(vec3 world, float depth, int count) {
#if WATER_CAUSTICS > 0
    if (depth <= 0.02) return vec3(1.0);
    vec3 p = world;
#if WATER_PIXELS > 0
    p.xz = (floor(p.xz * 16.0) + 0.5) / 16.0;
#endif
    vec3 down = waterSunDown();
    // Le point de la surface que ce rayon a traversé.
    vec2 at = p.xz + down.xz / down.y * depth;
    WaveEnv env = waveEnvDefault();
    env.depth = depth;
    // Le pixel et le disque de l'astre brouillent les filets trop fins pour la profondeur, et les rides les plus fines
    // ne feraient qu'un grain sur le fond : seules la houle et les vagues du vent y dessinent des filets.
    float blur = max(max(pixelFootprint(length(world - cameraPosition)), depth * 0.05), 0.12);
    int big = count - count / 4;
    WaveSample s = waveSum(at, frameTimeCounter, env, count, 0, big, blur);
    // La loupe : physiquement depth·(1 - 1/n)·H. Les filets d'un vrai fond se forment plus bas que nos bassins de
    // quelques blocs ; la loupe est donc réglée sur la courbure moyenne de la surface (curv), pour que les filets se
    // serrent dès le premier bloc d'eau. Elle croît avec la profondeur jusqu'à un bloc, et reste plus douce quand
    // l'eau est calme (peu de pente) que sous le vent.
    float reach = depth / max(-down.y, 0.2);
    ivec3 trains;
    vec3 rms;
    waveBands(count, env, trains, rms);
    float energy = clamp(length(rms) / 0.07, 0.35, 1.6);
    float target = (WATER_CAUSTICS == 1 ? 0.26 : 0.38) * energy * smoothstep(0.0, 1.2, reach);
    float lens = target / max(sqrt(s.curv), 0.01);
    // Le rouge et le bleu ne se réfractent pas tout à fait pareil : la loupe de chaque couleur suit (1 - 1/n).
    vec3 eta = WATER_IOR + vec3(-0.004, 0.0, 0.006) * (1.0 + 3.0 * float(WATER_DISPERSION));
    vec3 c = lens * (1.0 - 1.0 / eta) / (1.0 - 1.0 / WATER_IOR);
    vec3 det = (1.0 + c * s.hess.x) * (1.0 + c * s.hess.y) - c * c * s.hess.z * s.hess.z;
    vec3 focus = 1.0 / max(abs(det), vec3(0.06));
    // Au-delà d'une douzaine de blocs les filets se mêlent jusqu'à se fondre.
    focus = mix(focus, vec3(1.0), smoothstep(10.0, 24.0, depth));
    // Les plus forts sont tassés : un filet net, pas une tache brûlée. Puis la moyenne est ramenée à 1 : la lumière
    // est déplacée, pas ajoutée (moyenne mesurée sur le motif, 1,07 et 1,2 selon le réglage).
    focus = focus / (1.0 + max(focus - 1.0, 0.0) * 0.3);
    focus /= mix(1.0, WATER_CAUSTICS == 1 ? 1.07 : 1.2, smoothstep(0.0, 1.2, reach) * (1.0 - smoothstep(10.0, 24.0, depth)));
    return clamp(focus, 0.2, 4.0);
#else
    return vec3(1.0);
#endif
}

/** Les caustiques en nuances de gris, sous une épaisseur d'eau moyenne (rayons sous l'eau). */
float caustics(vec3 p) {
#if WATER_CAUSTICS > 0
    return luma(causticLight(p, 2.5, max(WATER_WAVE_COUNT / 2, 4)));
#else
    return 1.0;
#endif
}

/** Pente de la surface (compatibilité) : la somme des vagues d'une eau moyenne. */
vec2 waveSlope(vec2 p, float t) {
#if WATER_PIXELS > 0
    p = (floor(p * 16.0) + 0.5) / 16.0;
#endif
    vec2 slope = waveSum(p, t, waveEnvDefault(), WATER_WAVE_COUNT, 0, WATER_WAVE_COUNT, 0.0).slope;
#if WATER_PIXELS > 0
    slope = floor(slope / 0.03 + 0.5) * 0.03;
#endif
    return slope;
}
