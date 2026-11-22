// Ce qui tombe des arbres proches : l'eau qui s'égoutte après la pluie et la neige que le vent fait tomber des
// branches. Le feuillage de chaque colonne est tiré du volume des blocs par shadowcomp2. Demande voxel.glsl et
// motes.glsl (moteHash) avant lui. Les feuilles mortes, qui tombaient et roulaient au sol, sont parties le
// 2026-11-21 : ce sont les particules de Particular.

#if CANOPY_DRIPS > 0 && defined VOXELS
#define LEAFFALL_GLSL
uniform sampler2D canopyTex;

/** Le dessous du feuillage au-dessus d'une colonne (repère monde), ou -1e4 sans feuillage (shadowcomp2). */
float canopyUnder(vec2 xz) {
    ivec2 c = ivec2(floor(xz - cameraPosition.xz + fract(cameraPosition.xz))) + ivec2(VOX_W / 2);
    if (any(lessThan(c, ivec2(0))) || any(greaterThanEqual(c, ivec2(VOX_W)))) return -1e4;
    float a = texelFetch(canopyTex, c, 0).a;
    return a < -999.0 ? -1e4 : a + floor(cameraPosition.y);
}

#if CANOPY_DRIPS > 0
uniform float snowStorm;

/**
 * Ce qui tombe des branches le long d'un rayon jusqu'à t1 (blocs) : pendant et après la pluie (water, 0..1), des
 * gouttes d'eau qui se détachent du dessous du feuillage et chutent en accélérant ; quand la neige a chargé les arbres
 * et que le vent souffle (snow, 0..1), de rares paquets de neige qui tombent en s'effritant. Rend 1 si le rayon touche
 * quelque chose, sa distance dans dist, et dans kind 0 pour une goutte, 1 pour la neige. 40 cases au plus.
 */
float canopyDripAlong(vec3 dir, float t1, float water, float snow, out float dist, out float kind) {
    dist = 0.0;
    kind = 0.0;
    if (t1 <= 0.4 || max(water, snow) <= 0.01) return 0.0;
    vec3 camFloor = floor(cameraPosition);
    vec3 o = cameraPosition - camFloor;
    vec3 d = vec3(abs(dir.x) < 1e-5 ? 1e-5 : dir.x, abs(dir.y) < 1e-5 ? 1e-5 : dir.y, abs(dir.z) < 1e-5 ? 1e-5 : dir.z);
    vec3 cell = floor(o + dir * 0.4);
    vec3 stepDir = sign(d);
    vec3 tDelta = abs(1.0 / d);
    vec3 next = (cell + max(stepDir, 0.0) - o) / d;
    float pixel = 2.0 / (gbufferProjection[1][1] * viewHeight);
    float time = frameTimeCounter;
    bool isSnow = snow > water;
    float chance = isSnow ? 0.12 * snow : 0.5 * water;
    for (int i = 0; i < 40; i++) {
        float tExit = min(min(next.x, next.y), min(next.z, t1));
        vec3 wc = cell + camFloor;
        vec3 col = vec3(wc.x, 0.0, wc.z);
        float h = moteHash(col, 31.0);
        if (h < chance) {
            float under = canopyUnder(wc.xz);
            if (under > -1e3 && wc.y < under && wc.y > under - 14.0) {
                // Trois gouttes par colonne, décalées, une toutes les une à trois secondes ; pour la neige, un seul
                // paquet toutes les six à quinze secondes.
                for (int k = 0; k < 3; k++) {
                    if (isSnow && k > 0) break;
                    float kk = float(k) * 7.0;
                    float period = isSnow ? 6.0 + 9.0 * moteHash(col, 32.0) : 1.0 + 2.0 * moteHash(col, 32.0 + kk);
                    float age = mod(time + moteHash(col, 33.0 + kk) * period, period);
                    float fall = max(age - (isSnow ? 0.0 : 0.3), 0.0);
                    // L'eau tombe librement ; la neige, freinée par l'air, plafonne vers six blocs par seconde.
                    float y = under - 0.05 - (isSnow ? (6.0 * fall - 3.0 * (1.0 - exp(-2.0 * fall))) : 10.0 * fall * fall);
                    float size = (isSnow ? 3.0 : 0.3) / 16.0;
                    // Une goutte qui tombe file en un trait, d'autant plus long qu'elle va vite ; un paquet de neige
                    // s'effrite en tombant.
                    float tall = isSnow ? size : size + 0.35 * min(fall, 1.0);
                    if (y + tall < wc.y || y - tall > wc.y + 1.0) continue;
                    vec3 p = vec3(wc.x + 0.3 + 0.4 * moteHash(col, 34.0 + kk), y, wc.z + 0.3 + 0.4 * moteHash(col, 35.0 + kk)) - camFloor - o;
                    float s = dot(p, dir);
                    if (s > 1.0 && s < t1) {
                        vec3 off = dir * s - p;
                        size = max(size, s * pixel * 0.7);
                        bool hit = length(off.xz) < size && abs(off.y) < max(tall, size);
                        if (hit && isSnow) {
                            vec3 q = floor((p + off) * 16.0);
                            hit = moteHash(q, 36.0) > 0.25 + 0.5 * min(fall / 1.5, 1.0);
                        }
                        if (hit) {
                            dist = s;
                            kind = isSnow ? 1.0 : 0.0;
                            // Le trait d'une goutte est plus pâle vers le haut.
                            float fade = isSnow ? 1.0 : 0.55 + 0.45 * clamp(0.5 - off.y / (2.0 * max(tall, size)), 0.0, 1.0);
                            // Tout près de l'œil, une goutte n'est qu'un éclair flou : elle s'efface au lieu de grossir en tache.
                            return fade * (1.0 - smoothstep(t1 * 0.6, t1, s)) * (isSnow ? 1.0 : smoothstep(1.2, 3.5, s));
                        }
                    }
                }
            }
        }
        if (tExit >= t1) break;
        if (next.x < next.y && next.x < next.z) { next.x += tDelta.x; cell.x += stepDir.x; }
        else if (next.y < next.z) { next.y += tDelta.y; cell.y += stepDir.y; }
        else { next.z += tDelta.z; cell.z += stepDir.z; }
    }
    return 0.0;
}
#endif

#endif
