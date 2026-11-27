#include "/lib/clouds.glsl"
#include "/lib/water.glsl"
#include "/lib/voxel.glsl"
#include "/lib/farshadow.glsl"
/** Bloc qui éclaire par lui-même (block.properties : 8, 16 à 22). */
bool emissive(int mat) { return mat == 8 || (mat >= 16 && mat <= 22); }

// Éclairage direct d'une surface : soleil ou lune avec ombre, ciel, blocs lumineux.

/** Normale (repère monde) rangée dans deux canaux, et relue. */
vec2 packNormal(vec3 n) {
    n /= abs(n.x) + abs(n.y) + abs(n.z);
    vec2 p = n.y >= 0.0 ? n.xz : (1.0 - abs(n.zx)) * vec2(n.x >= 0.0 ? 1.0 : -1.0, n.z >= 0.0 ? 1.0 : -1.0);
    return p * 0.5 + 0.5;
}

/** Bruit de valeur lisse sur une droite, pour le vacillement des flammes. */
float flickerNoise(float t) {
    float i = floor(t), f = fract(t);
    float a = fract(sin(i * 12.9898) * 43758.5453), b = fract(sin((i + 1.0) * 12.9898) * 43758.5453);
    return mix(a, b, f * f * (3.0 - 2.0 * f));
}

/**
 * La lumière du ciel qu'une surface reçoit selon où elle regarde : le ciel par le haut, l'horizon plus pâle sur les
 * côtés, et par le bas le sol qui renvoie le soleil (un vert-brun chaud). sky : la part de ciel que le jeu voit là.
 */
vec3 hemisphereLight(vec3 normal, float sky) {
    float up = normal.y * 0.5 + 0.5;
    vec3 zenith = gAmbient * 1.05;
    vec3 horizon = mix(gAmbient, vec3(luma(gAmbient)), 0.35) * 0.85;
    vec3 ground = vec3(0.30, 0.29, 0.20) * (gLightColor * max(gLightDir.y, 0.0) * 0.35 + gAmbient * 0.70);
    vec3 fromAbove = mix(horizon, zenith, smoothstep(0.5, 1.0, up));
    return mix(ground, fromAbove, smoothstep(0.0, 0.62, up)) * sky;
}

#if CANOPY_SHADE > 0 && defined FSH && defined VOXELS
/**
 * La part du ciel qui passe le feuillage au-dessus d'une surface. Le jeu ne retire qu'un cran de lumière du ciel par
 * bloc de feuilles : sous une forêt, le sol reste presque aussi clair qu'en plein champ. On remonte la colonne du
 * volume sur 24 blocs ; chaque feuillage en laisse passer les quatre cinquièmes, jamais moins d'un tiers en tout. La
 * colonne part d'un point tiré au sort dans le bloc à chaque image : l'anticrénelage temporel en fait une ombre douce.
 */
float canopyShade(vec3 pos, vec3 normal, vec2 frag) {
    vec2 jit = vec2(dither(frag + 1.7 * float(frameCounter & 31)), dither(frag.yx + 3.1 * float(frameCounter & 31))) - 0.5;
    vec3 start = pos + normal * 0.3 + vec3(jit.x, 0.6, jit.y);
    vec3 camF = fract(cameraPosition);
    float leaves = 0.0;
    for (int i = 0; i < 16; i++) {
        ivec3 c = voxelCell(start + vec3(0.0, float(i) * 1.5, 0.0), camF);
        if (!voxelInside(c)) break;
        if ((texelFetch(voxelTex, c, 0).r & VOX_LEAF) != 0u) leaves += 1.5;
    }
    float pass = max(pow(0.76, leaves), 0.22);
    // Loin de l'œil, le volume s'arrête : l'effet s'efface avant son bord.
    float fade = 1.0 - smoothstep(40.0, 58.0, length(pos.xz));
    return mix(1.0, pass, fade * (CANOPY_SHADE > 1 ? 1.0 : 0.88));
}
#endif

// La lumière de l'astre qui atteint le dernier point éclairé par shade() (ombre, nuages, eau et vitraux compris).
vec3 gSunReach = vec3(0.0);
// La lumière des blocs au même point (gBlockReach), d'où elle vient (gBlockDir, unitaire) et combien elle vient d'un
// seul côté (gBlockSide : 0 de partout, 1 d'une seule source) : solid.fsh y prend le reflet des torches.
vec3 gBlockReach = vec3(0.0);
vec3 gBlockDir = vec3(0.0, 1.0, 0.0);
float gBlockSide = 0.0;

/**
 * albedo : couleur linéaire ; normal : repère monde ; lm : lumière des blocs et du ciel (0..1) ; ao : occlusion ;
 * pos : repère joueur ; leaf : 0 surface dure, 1 feuillage que la lumière traverse ; frag : gl_FragCoord.xy.
 *
 * La lumière directe passe l'ombre douce de la carte, puis les nuages, l'eau et les vitraux. Une feuille reçoit en
 * plus ce qui traverse le feuillage devant elle : d'autant moins qu'il est épais, teint du vert des feuilles qu'il a
 * traversées, plus vif à contre-jour. La lumière du ciel vient d'un hémisphère (ciel, horizon, sol). La lumière des
 * blocs prend sa couleur dans le volume, vient du côté de sa source et vacille un peu quand c'est une flamme.
 */
vec3 shade(vec3 albedo, vec3 normal, vec2 lm, float ao, vec3 pos, float leaf, vec2 frag, int taps, out float indirect) {
    float facing = dot(normal, gLightDir);
    // Un feuillage reste plus clair du côté du soleil : il garde du volume tout en laissant passer la lumière. Le dessous
    // et le côté à l'ombre d'une couronne restent nettement plus sombres que son dessus.
    float diffuse = mix(max(facing, 0.0) * 0.8 + 0.2 * leaf, 0.72, leaf * 0.35);
    float lit = 0.0;
    float thick = 0.0;
    float dist = length(pos);
    if (diffuse > 0.0) {
        // Au-delà de la carte d'ombre, la lumière du ciel du jeu tient lieu d'ombre.
        float beyond = smoothstep(shadowDistance * 0.82, shadowDistance * 0.98, dist);
        float fallback = smoothstep(0.80, 0.94, lm.y);
#if FAR_SHADOWS > 0
        // Plus loin encore, le relief vu par la caméra fait de l'ombre (lib/farshadow.glsl).
        if (beyond > 0.0) fallback *= farShadow(pos, normal, dither(frag + 5.588238 * float(frameCounter & 63)));
#endif
        vec3 shadowPos = pos;
        float shadowNoise = dither(frag);
#if PIXEL_SHADOWS > 0 && defined PROGRAM_TERRAIN
        // Les ombres au pixel du jeu : chaque texel d'une face prend l'ombre de son centre, et le bord d'une ombre
        // avance texel par texel, comme le reste du dessin. De près seulement : au loin un texel est plus petit
        // qu'un pixel de l'écran.
        if (dist < 40.0 && max(abs(normal.x), max(abs(normal.y), abs(normal.z))) > 0.9) {
            vec3 w = pos + cameraPosition;
            vec3 cell = floor(w * 16.0 - normal * 0.25);
            shadowPos = (cell + 0.5) / 16.0 + normal * (0.5 / 16.0) - cameraPosition;
            shadowNoise = fract(sin(dot(cell, vec3(12.9898, 78.233, 37.719))) * 43758.5453);
        }
#endif
        float near_ = beyond < 1.0 ? shadowDepth(shadowPos, normal, shadowNoise, taps, thick) : 1.0;
        lit = beyond < 1.0 ? mix(near_, fallback, beyond) : fallback;
        thick *= 1.0 - beyond;
    }
    // Une feuille à l'ombre d'autres feuilles reçoit encore le soleil passé à travers elles, moins à chaque bloc
    // traversé ; au-delà de la carte d'ombre, une part fixe.
    float through = 0.0;
    if (leaf > 0.0) {
        float layers = thick > 0.0 ? thick : 1.2;
        through = exp(-layers * 0.7) * 0.75 * LEAF_TRANSLUCENCY * leaf;
        // Une part fixe seulement là où la carte d'ombre ne dit rien : une herbe sous un arbre, à quelques blocs de la
        // couronne qui lui fait de l'ombre, ne reçoit rien de plus que le sol à côté d'elle.
        if (thick <= 0.0) through = max(through, 0.12 * leaf);
        lit = max(lit, through);
    }
    // L'ombre des nuages qui passent.
    float clouds = diffuse > 0.0 ? cloudShadow(pos + cameraPosition, gLightDir) : 1.0;
    lit *= clouds;
    vec3 light = gLightColor * diffuse * lit;
    // Ce qui a traversé du feuillage en sort teint de vert.
    if (leaf > 0.0 && through > 0.0) {
        vec3 tint = albedo / max(max(albedo.r, max(albedo.g, albedo.b)), 1e-3);
        float share = clamp(through / max(lit, 1e-3), 0.0, 1.0);
        light *= mix(vec3(1.0), mix(vec3(1.0), tint, 0.6), share);
    }
    // Sous l'eau, la lumière arrive affaiblie et teintée, rassemblée en filets par les vagues.
    vec3 pass = vec3(1.0);
    if (lit > 0.0) {
        float water = waterAbove(pos, normal);
        if (water > 0.0) {
            vec3 hue = waterHue(vec3(0.25, 0.46, 0.89));
            pass *= exp(-waterExtinction(hue) * water) * causticLight(pos + cameraPosition, water, WATER_WAVE_COUNT);
        }
        // Passé par un vitrail, le soleil en prend la couleur.
        pass *= glassTint(pos, normal);
        light *= pass;
    }
    // Ce que l'astre apporte au point, ombre comprise : solid.fsh y prend le reflet des surfaces lisses.
    gSunReach = leaf > 0.0 ? vec3(0.0) : gLightColor * lit * pass;
    // Le feuillage à contre-jour : la lumière diffusée vers l'avant dans la feuille, verte et vive, d'autant plus que la
    // feuille est mince devant le soleil.
    vec3 view = normalize(pos);
    float behind = dot(view, gLightDir);
    if (leaf > 0.0) {
        float forward = pow(max(behind, 0.0), 4.0) * 0.55 + pow(max(behind, 0.0), 24.0) * 0.9;
        vec3 green = albedo / max(luma(albedo), 1e-3) * 0.5;
        float reach = exp(-max(thick, 0.0) * 0.5) * clouds * max(lit, through);
        light += gLightColor * leaf * forward * reach * mix(vec3(1.0), green, 0.7) * 0.6 * LEAF_TRANSLUCENCY;
    }
    // Le plein soleil un peu retenu et l'ombre nourrie du ciel : un jour voilé et doux, peu contrasté de près.
    light *= mix(1.0, 0.7, gDay * (1.0 - gDusk));
    float direct = luma(light);

    float sky = lm.y * lm.y;
    float occl = ao * ao;
#if CANOPY_SHADE > 0 && defined FSH && defined VOXELS
    if (sky > 0.01) {
        // Sous les feuilles, le ciel perdu n'est pas tout noir : le jour qui a traversé le feuillage et rebondi de
        // feuille en feuille descend jaune-vert et chaud. Une part de ce qui est retiré revient sous cette teinte.
        float open = canopyShade(pos, normal, frag);
        float covered = sky * (1.0 - open);
        vec3 filtered = vec3(luma(hemisphereLight(normal, 1.0))) * 0.30
            + gLightColor * max(gLightDir.y, 0.0) * 0.035;
        light += filtered * vec3(1.10, 1.0, 0.50) * covered * occl * (1.0 + 4.5 * gDay * (1.0 - gDusk));
        sky *= open;
    }
#endif
    // De jour, l'ombre reçoit franchement le ciel : sous un arbre ou un couvert, elle reste claire et douce (Recrafted :
    // sous-bois vers 78 sur 255, ici 51 avant). Sur les surfaces seulement : la brume garde sa lumière. Au couchant,
    // moitié moins : le sol reste sombre sous le ciel clair.
    light += hemisphereLight(normal, sky) * occl * (1.0 + 4.5 * gDay * (1.0 - gDusk)) * mix(1.0, 0.5, gDusk);
    // La lumière des blocs prend la couleur de ce qui éclaire autour (lib/voxel.glsl) : orangée pour une flamme,
    // bleue pour une âme, rouge pour la lave…
    float blockLm = lm.x;
    vec3 tint = blockLightTint(pos, normal, blockLm);
    // Une flamme éclaire fort tout près et moins au loin : comme chez Recrafted, une lanterne fait une large flaque
    // chaude de plusieurs blocs, sans remplir toute la salle. Assez forte pour qu'un sol de grotte éclairé monte vers
    // 0,2-0,25 comme chez Recrafted (0,13 avant).
#if COLORED_LIGHT > 0
    // Les lumières plus faibles que la flamme (âmes : niveau 10, redstone : 7) ont la même flaque autour d'elles, en plus
    // petit : leur niveau est lu par rapport au leur, sans quoi la forte pente ci-dessous les éteint presque. Un peu
    // moins fortes qu'une flamme, et moins saturées : une lanterne d'âme ne doit pas baigner une grotte de cyan.
    float weak = 1.0;
    if (distance(tint, lightTint(1)) < 0.2) weak = 10.0 / 15.0;
    else if (distance(tint, lightTint(7)) < 0.2) weak = 7.0 / 15.0;
    blockLm = min(blockLm / weak, 1.0) * mix(1.0, 0.9, step(weak, 0.99));
    // Et un peu moins saturées : un cyan franc bleuissait tout le sol d'une grotte ; un quart seulement, pour garder
    // le halo turquoise de Recrafted autour de la lanterne.
    tint = mix(tint, vec3(luma(tint)), 0.25 * step(weak, 0.99));
#endif
    float bl2 = blockLm * blockLm;
    float glow = bl2 * (0.42 + 3.9 * bl2 * bl2);
    gBlockSide = 0.0;
#if BLOCK_LIGHT_SHAPE > 0
    if (blockLm > 0.02) {
        // D'où vient la lumière : la face tournée vers la torche en reçoit plus que celle qui lui tourne le dos.
        vec3 slope = voxelLightSlope(pos, normal);
        float strength = clamp(length(slope), 0.0, 1.0);
        if (strength > 0.05) {
            float toward = dot(normal, slope / length(slope));
            glow *= mix(1.0, 0.62 + 0.55 * max(toward, -0.7), strength * 0.85);
            gBlockDir = slope / length(slope);
            gBlockSide = strength;
        }
        // Une flamme vacille : un souffle lent et un frémissement, différents d'un bloc à l'autre, d'autant moins
        // sensibles qu'on est loin d'elle.
        float flame = 1.0 - smoothstep(0.08, 0.35, length(tint - lightTint(0)));
        vec3 cell = floor(pos + cameraPosition + 0.5);
        float seed = dot(cell, vec3(0.37, 0.71, 0.13));
        float t = frameTimeCounter;
        float waver = (flickerNoise(t * 2.1 + seed) - 0.5) * 0.10 + (flickerNoise(t * 9.3 + seed * 3.0) - 0.5) * 0.05;
        glow *= 1.0 + waver * flame * blockLm;
    }
#endif
    light += tint * glow * mix(occl, 1.0, blockLm * blockLm);
    gBlockReach = tint * glow;
    // Le plancher de lumière : un rien sous terre, pour que le noir d'une grotte reste noir.
    // D'un gris chaud et non bleuté : le voile bleu-gris des grottes venait de là, et la nuit garde le sol olive.
    light += vec3(0.016, 0.0145, 0.0105) * mix(0.1, 1.0, smoothstep(0.0, 0.3, lm.y)) * occl;
    light += vec3(0.30, 0.32, 0.34) * nightVision;
    light *= 1.0 - 0.7 * darknessFactor;
    // Part de la lumière qui vient du ciel et des blocs : c'est elle que l'occlusion d'écran assombrit.
    indirect = clamp(1.0 - direct * (1.0 - 0.7 * darknessFactor) / max(luma(light), 1e-5), 0.0, 1.0);
    return albedo * light;
}
