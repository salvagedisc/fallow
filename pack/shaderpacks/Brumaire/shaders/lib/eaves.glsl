// Les gouttières : pendant la pluie et un moment après, l'eau tombe goutte à goutte du bord des toits (et de tout ce qui
// avance au-dessus du vide : un balcon, une corniche, un surplomb de roche ; pas d'un mur ni d'une falaise). Le bord se lit dans l'abri de la pluie
// (lib/precip.glsl) : une colonne dont la voisine est abritée bien plus haut qu'elle. Demande precip.glsl et
// motes.glsl (moteHash) avant lui.

#if EAVES_DRIPS > 0 && SHADER_RAIN > 0 && defined VOXELS
/**
 * Ce qui tombe du bord des toits le long d'un rayon jusqu'à t1 (blocs) : 1 si le rayon touche une goutte, et sa
 * distance dans dist. water : 0..1. Deux gouttes par colonne de bord, une toutes les demi-secondes à deux secondes
 * selon la pluie ; 40 cases au plus.
 */
float eavesAlong(vec3 dir, float t1, float water, out float dist) {
    dist = 0.0;
    if (t1 <= 0.4 || water <= 0.01) return 0.0;
    vec3 camFloor = floor(cameraPosition);
    vec3 o = cameraPosition - camFloor;
    vec3 d = vec3(abs(dir.x) < 1e-5 ? 1e-5 : dir.x, abs(dir.y) < 1e-5 ? 1e-5 : dir.y, abs(dir.z) < 1e-5 ? 1e-5 : dir.z);
    vec3 cell = floor(o + dir * 0.4);
    vec3 stepDir = sign(d);
    vec3 tDelta = abs(1.0 / d);
    vec3 next = (cell + max(stepDir, 0.0) - o) / d;
    float pixel = 2.0 / (gbufferProjection[1][1] * viewHeight);
    float time = frameTimeCounter;
    // Sous la pluie l'eau coule vite ; après, les gouttes s'espacent.
    float pace = mix(2.0, 0.6, smoothstep(0.1, 0.7, rainStrength));
    for (int i = 0; i < 40; i++) {
        float tExit = min(min(next.x, next.y), min(next.z, t1));
        vec3 wc = cell + camFloor;
        vec3 col = vec3(wc.x, 0.0, wc.z);
        if (moteHash(col, 61.0) < 0.75 * water) {
            vec2 rel = wc.xz + 0.5 - cameraPosition.xz;
            float ground = shelterAt(rel);
            if (wc.y >= ground - 1.0) {
                // Le côté d'où l'eau tombe : la première voisine abritée au moins deux blocs et demi plus haut.
                float roof = -1e6;
                vec2 side = vec2(0.0);
                for (int k = 0; k < 4; k++) {
                    vec2 n = k == 0 ? vec2(1.0, 0.0) : k == 1 ? vec2(-1.0, 0.0) : k == 2 ? vec2(0.0, 1.0) : vec2(0.0, -1.0);
                    float h = shelterAt(rel + n);
                    if (h - ground <= 2.5) continue;
                    // Un vrai bord : sous lui, du vide (un toit qui déborde, un balcon) ; contre un mur ou une falaise,
                    // l'eau ruisselle le long de la paroi et ne goutte pas.
                    ivec3 under = ivec3(vec3(wc.x + n.x, h - 2.0, wc.z + n.y) - camFloor) + VOX_HALF;
                    if (!voxelInside(under) || (texelFetch(voxelTex, under, 0).r & VOX_SOLID) != 0u) continue;
                    roof = h; side = n; break;
                }
                if (roof > -1e5 && wc.y < roof) {
                    for (int k = 0; k < 2; k++) {
                        float kk = float(k) * 7.0;
                        float period = pace * (1.0 + moteHash(col, 62.0 + kk));
                        float age = mod(time + moteHash(col, 63.0 + kk) * period, period);
                        // Elle grossit un instant sous le bord, puis tombe librement.
                        float fall = max(age - 0.25 * period, 0.0);
                        float y = roof - 1.05 - 10.0 * fall * fall;
                        if (y < ground) continue;
                        float size = 0.3 / 16.0;
                        float tall = size + 0.35 * min(fall, 1.0);
                        if (y + tall < wc.y || y - tall > wc.y + 1.0) continue;
                        // Collée au bord du toit, à un endroit tiré au sort le long de lui.
                        float along = 0.2 + 0.6 * moteHash(col, 64.0 + kk);
                        vec2 at = wc.xz + 0.5 + side * 0.42 + vec2(side.y, side.x) * (along - 0.5);
                        vec3 p = vec3(at.x, y, at.y) - cameraPosition;
                        float s = dot(p, dir);
                        if (s > 0.8 && s < t1) {
                            vec3 off = dir * s - p;
                            float w = max(size, s * pixel * 0.7);
                            if (length(off.xz) < w && abs(off.y) < max(tall, w)) {
                                dist = s;
                                float fade = 0.55 + 0.45 * clamp(0.5 - off.y / (2.0 * max(tall, w)), 0.0, 1.0);
                                // Tout près de l'œil, une goutte n'est qu'un éclair flou : elle s'efface au lieu de grossir en tache.
                                return fade * (1.0 - smoothstep(t1 * 0.6, t1, s)) * smoothstep(1.2, 3.5, s);
                            }
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

