// Ce qui entoure un point de l'eau, et l'écume qui en naît : la rive et son ressac, la chute qui tombe dans le bassin
// et le fait bouillonner, les moutons des vagues qui déferlent au large, l'eau qui
// dévale le long d'une cascade.
// Les ronds de pluie sur l'eau sont partis le 2026-11-22 : ceux de Particular le font déjà.
// Demande voxel.glsl, rain.glsl, clouds.glsl et waves.glsl avant lui. Fragments seulement.

#ifdef FSH

float foamHash(vec2 p) {
    p = fract(p * vec2(0.1031, 0.1030));
    p += dot(p, p.yx + 33.33);
    return fract((p.x + p.y) * p.x);
}

/** Ce que le monde dit de l'eau autour d'un point de sa surface (lu dans le volume des blocs, lib/voxel.glsl). */
struct WaterSurround {
    float shore;  // distance à la rive la plus proche (blocs), 3 au-delà de deux blocs
    vec2 toShore; // direction vers elle
    float fall;   // distance à une eau qui tombe dans le bassin (blocs), 3 si aucune
    vec2 toFall;  // direction vers elle
    float open;   // 0 mare close .. 1 eau libre à perte de vue : la place qu'a le vent pour lever la houle
};

WaterSurround waterSurround(vec3 pos) {
    WaterSurround w;
    w.shore = 3.0;
    w.toShore = vec2(0.0);
    w.fall = 3.0;
    w.toFall = vec2(0.0);
    w.open = 0.6;
#ifdef VOXELS
    vec3 world = pos + cameraPosition;
    ivec3 c = voxelCell(pos - vec3(0.0, 0.05, 0.0), fract(cameraPosition));
    if (!voxelInside(c)) return w;
    vec2 f = fract(world.xz);
    // La rive : le bloc plein le plus proche, à la hauteur de l'eau, dans un carré de cinq de côté. La chute : une
    // eau juste au-dessus du niveau, dans le carré de trois.
    for (int dz = -2; dz <= 2; dz++) {
        for (int dx = -2; dx <= 2; dx++) {
            ivec3 n = c + ivec3(dx, 0, dz);
            vec2 lo = vec2(dx, dz);
            vec2 q = max(max(lo - f, f - (lo + 1.0)), 0.0);
            float d = length(q);
            vec2 toward = lo + 0.5 - f;
            if (dx != 0 || dz != 0) {
                if (voxelInside(n) && (texelFetch(voxelTex, n, 0).r & VOX_SOLID) != 0u && d < w.shore) {
                    w.shore = d;
                    w.toShore = normalize(toward);
                }
            }
            if (abs(dx) <= 1 && abs(dz) <= 1) {
                ivec3 u = n + ivec3(0, 1, 0);
                if (voxelInside(u) && (texelFetch(voxelTex, u, 0).r & VOX_WATER) != 0u && d < w.fall) {
                    w.fall = d;
                    w.toFall = dot(toward, toward) > 1e-4 ? normalize(toward) : vec2(0.0);
                }
            }
        }
    }
    // L'étendue : huit rayons qui courent sur l'eau jusqu'à seize blocs, arrêtés par la première terre.
    float reach = 0.0;
    for (int a = 0; a < 8; a++) {
        float ang = float(a) * 0.7853982 + 0.3;
        vec2 d = vec2(cos(ang), sin(ang));
        float free = 16.0;
        for (int s = 0; s < 4; s++) {
            float r = s == 0 ? 3.0 : (s == 1 ? 6.0 : (s == 2 ? 10.0 : 16.0));
            ivec3 n = c + ivec3(int(floor(d.x * r + f.x)), 0, int(floor(d.y * r + f.y)));
            if (!voxelInside(n)) break;
            if ((texelFetch(voxelTex, n, 0).r & VOX_WATER) == 0u) {
                free = s == 0 ? 1.5 : (s == 1 ? 4.0 : (s == 2 ? 7.0 : 12.0));
                break;
            }
        }
        reach += free;
    }
    w.open = smoothstep(2.0, 14.0, reach / 8.0);
#endif
    return w;
}

/**
 * L'écume en motif : une dentelle de cellules qui s'ouvre quand il y en a moins et se referme en nappe quand il y en
 * a plus ; elle dérive lentement. Au pixel près (WATER_PIXELS) : des pixels blancs aux bords francs.
 */
float foamLace(vec3 world, float amount, float t) {
    if (amount <= 0.01) return 0.0;
    vec2 q = world.xz;
#if WATER_PIXELS > 0
    q = (floor(q * 16.0) + 0.5) / 16.0;
#endif
    float cells = cloudNoise(vec3(q * 0.23, 0.13 + t * 0.011)).g;
    float fine = cloudNoise(vec3(q.yx * 0.71 + 0.31, 0.57 + t * 0.023)).g;
    float body = cells * 0.6 + fine * 0.4;
    float cover = 1.0 - clamp(amount, 0.0, 1.0) * 0.9;
#if WATER_PIXELS > 0
    return step(cover, body + 0.08 * foamHash(floor(q * 16.0)));
#else
    return smoothstep(cover - 0.04, cover + 0.04, body);
#endif
}

/**
 * Le ressac le long d'une rive : une frange collée au bord qui respire, et des bandes qui arrivent du large et
 * meurent sur la rive. energy : la force des vagues (0..1).
 */
float shoreFoam(WaterSurround s, vec3 world, float energy, float t) {
#if WATER_FOAM > 0
    if (s.shore >= 2.0) return 0.0;
    float vary = cloudNoise(vec3(world.xz * 0.09, 0.71 + t * 0.006)).r;
    float breathe = 0.5 + 0.5 * sin(t * 0.85 + vary * 7.0);
    float contact = 1.0 - smoothstep(0.03 + 0.10 * breathe, 0.12 + 0.22 * breathe, s.shore);
    // Une crête à la distance d se retrouve plus près l'instant d'après : les bandes avancent vers la terre.
    float phase = s.shore * 2.6 + t * (0.9 + 0.7 * energy) + vary * 6.0;
    float band = smoothstep(0.70, 0.96, 0.5 + 0.5 * sin(phase)) * exp(-s.shore * 1.5) * smoothstep(0.05, 0.3, s.shore);
    return clamp(0.6 * contact + band * (0.2 + 0.6 * energy), 0.0, 1.0);
#else
    return 0.0;
#endif
}

/** Le bouillon au pied d'une chute : de l'écume qui roule, et des rides qui partent en rond. */
float fallChurn(WaterSurround s, vec3 world, float t, inout vec2 slope) {
#if WATER_FALLS > 0
    if (s.fall >= 2.5) return 0.0;
    float near = 1.0 - smoothstep(0.0, 2.2, s.fall);
    float ring = sin(s.fall * 13.0 + t * 9.0) * exp(-s.fall * 1.4);
    slope -= s.toFall * ring * 0.09 * near;
    float boil = cloudNoise(vec3(world.xz * 0.35, 0.29 + t * 0.09)).g;
    return near * (0.55 + 0.45 * boil);
#else
    return 0.0;
#endif
}

/**
 * Les moutons : là où la surface se replie (le pli du jacobien passe sous un seuil), la crête déferle. L'écume reste
 * un moment derrière elle et s'effiloche : le pli est relu une et deux secondes plus tôt.
 */
float whitecaps(vec2 p, float t, WaveEnv env, float foldNow) {
#if WATER_WHITECAPS > 0
    float drive = max(env.storm, smoothstep(0.45, 1.0, env.force)) * smoothstep(0.25, 0.75, env.open);
    if (drive <= 0.02) return 0.0;
    float edge = mix(0.60, 0.80, drive);
    float now = smoothstep(edge, edge - 0.22, foldNow);
    // La traînée ne suit que les grands trains : la moitié des vagues suffit à dire où une crête a déferlé.
    int big = WATER_WAVE_COUNT / 2;
    float before = waveFold(waveSum(p, t - 0.7, env, WATER_WAVE_COUNT, 0, big, 1.0 / 16.0));
    float older = waveFold(waveSum(p, t - 1.5, env, WATER_WAVE_COUNT, 0, big, 1.0 / 16.0));
    float trail = max(smoothstep(edge, edge - 0.22, before) * 0.55, smoothstep(edge, edge - 0.22, older) * 0.28);
    return drive * max(now, trail);
#else
    return 0.0;
#endif
}

/** L'eau de ce flanc tombe-t-elle (de l'eau juste au-dessus) ? pos : repère joueur, sur la face. */
bool waterFalling(vec3 pos, vec3 normal) {
#ifdef VOXELS
    ivec3 c = voxelCell(pos - normal * 0.05, fract(cameraPosition)) + ivec3(0, 1, 0);
    return voxelInside(c) && (texelFetch(voxelTex, c, 0).r & VOX_WATER) != 0u;
#else
    return false;
#endif
}

/**
 * Une face d'eau de côté : le flanc d'une cascade quand de l'eau tombe d'au-dessus (des traînées qui dévalent, plus
 * claires et plus vives que l'eau calme), sinon le simple bord d'une eau qui coule, à peine strié. Rend la normale
 * penchée par les traînées ; foam reçoit leur blanc.
 */
vec3 fallFace(vec3 world, vec3 normal, float t, bool falling, out float foam) {
    foam = 0.0;
#if WATER_FALLS > 0
    if (!falling) t *= 0.3;
    vec3 across = normalize(vec3(-normal.z, 0.0, normal.x));
    // Chaque colonne d'un seizième de bloc porte ses filets : leur vitesse, leur longueur et leurs trous sont tirés
    // au sort pour elle, et ils glissent vers le bas (le motif est lu à y + vitesse · t).
    float col = floor(dot(world, across) * 16.0);
    float h1 = foamHash(vec2(col, 7.13));
    float h2 = foamHash(vec2(col * 1.37, 3.31));
    float h3 = foamHash(vec2(col * 0.71, 9.77));
    float speed = 2.4 + 2.2 * h1;
    float y = world.y + t * speed;
#if WATER_PIXELS > 0
    y = floor(y * 16.0) / 16.0;
#endif
    float seg = fract(y * (0.45 + 0.6 * h2) + h3);
    float len = 0.16 + 0.36 * h1;
#if WATER_PIXELS > 0
    float streak = step(seg, len);
#else
    float streak = smoothstep(len + 0.08, len - 0.04, seg) * smoothstep(0.0, 0.06, seg);
#endif
    // Le bord d'une eau qui coule ne fait que frémir ; une cascade est striée de bout en bout.
    foam = streak * (falling ? 0.85 : 0.0);
    float tilt = (h2 - 0.5) * 0.5 + (streak - 0.5) * 0.25;
    return normalize(normal + across * tilt * (falling ? 1.0 : 0.3));
#else
    return normal;
#endif
}

#endif
