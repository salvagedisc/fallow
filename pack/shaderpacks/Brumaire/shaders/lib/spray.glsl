// Les embruns des rivages par grand vent, et l'arc-en-ciel qui s'y forme. Ceux du pied des cascades sont partis le
// 2026-11-22 : les embruns et les nuages de cascade de Particular le font déjà, avec ou sans shader. Lit le volume des
// blocs (lib/voxel.glsl, bits VOX_WATER et VOX_SOLID). Demande voxel.glsl et clouds.glsl (cloudNoise) avant lui.

#if SHORE_SPRAY > 0 && defined VOXELS
/** Une case d'air au-dessus de l'eau, contre une rive (un bloc plein à un ou deux blocs, au niveau de l'eau) ? */
float shoreNear(ivec3 c) {
    ivec3 below = c - ivec3(0, 1, 0);
    if (!voxelInside(below) || (texelFetch(voxelTex, below, 0).r & VOX_WATER) == 0u) return 0.0;
    for (int k = 0; k < 8; k++) {
        ivec3 side = (k % 4 == 0 ? ivec3(1, 0, 0) : k % 4 == 1 ? ivec3(-1, 0, 0) : k % 4 == 2 ? ivec3(0, 0, 1) : ivec3(0, 0, -1)) * (k < 4 ? 1 : 2);
        ivec3 n = below + side;
        if (voxelInside(n) && (texelFetch(voxelTex, n, 0).r & VOX_SOLID) != 0u) return k < 4 ? 1.0 : 0.6;
    }
    return 0.0;
}

/**
 * Les embruns des rivages par grand vent (0..1) : au-dessus de l'eau qui bat une rive, des bouffées blanches que le
 * vent arrache et emporte, jusqu'à deux blocs. Douze prises jusqu'à 36 blocs. gale : force du coup de vent (0..1).
 */
float shoreSprayAlong(vec3 dir, float t1, float gale) {
    float reach = min(t1, 36.0);
    if (reach <= 0.5 || gale <= 0.01) return 0.0;
    float step_ = reach / 12.0;
    float jitter = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    float t = frameTimeCounter;
    vec2 wind = normalize(brumaireWind.xy + vec2(1e-4, 0.0));
    float opacity = 0.0;
    for (int i = 0; i < 12; i++) {
        float s = (float(i) + jitter) * step_;
        vec3 rel = dir * s;
        ivec3 c = voxelCell(rel, fract(cameraPosition));
        ivec3 c2 = c - ivec3(0, 1, 0);
        float lift = 0.0;
        if (!voxelInside(c) || texelFetch(voxelTex, c, 0).r != 0u) continue;
        float near = shoreNear(c);
        // Une case plus haut aussi : la bouffée monte jusqu'à deux blocs.
        if (near <= 0.0 && voxelInside(c2) && texelFetch(voxelTex, c2, 0).r == 0u) { near = shoreNear(c2) * 0.5; lift = 1.0; }
        if (near <= 0.0) continue;
        vec3 w = rel + cameraPosition;
        float above = fract(w.y) + lift;
        // Des bouffées qui partent par à-coups, emportées dans le sens du vent.
        vec2 q = vec2(dot(w.xz, wind) - t * 6.0 * gale, dot(w.xz, vec2(-wind.y, wind.x)));
        float burst = cloudNoise(vec3(q * 0.35, t * 0.25)).r;
        float n = cloudNoise(vec3(q * 1.1, w.y * 0.9 - t * 0.8) + 3.0).r;
        float puff = smoothstep(0.2, 0.5, burst) * smoothstep(0.15, 0.5, n) * near * exp(-above * 1.0)
                   * (1.0 - smoothstep(26.0, 36.0, s));
        opacity += puff * step_ * 0.75 * (1.0 - opacity);
    }
    return clamp(opacity * gale, 0.0, 0.8);
}
#if SPRAY_RAINBOW > 0
/** Les couleurs d'un arc, x de 0 (violet, dedans) à 1 (rouge, dehors), noires hors de l'arc. */
vec3 sprayBowColor(float x) {
    float r = smoothstep(0.42, 0.80, x) + 0.30 * (1.0 - smoothstep(0.0, 0.18, x));
    float g = 1.0 - clamp(abs(x - 0.52) / 0.30, 0.0, 1.0);
    float b = 1.0 - smoothstep(0.12, 0.48, x);
    return vec3(r, g, b) * smoothstep(-0.12, 0.08, x) * (1.0 - smoothstep(0.92, 1.12, x));
}

/**
 * L'arc des embruns : le soleil dans le dos, les embruns d'une rive dessinent un bout
 * d'arc-en-ciel à 42° du point opposé au soleil, et plus pâle le second, inversé, vers 52°. Il n'existe que dans les
 * gouttes : rend une lumière à multiplier par leur part.
 */
vec3 sprayBow(vec3 dir) {
    float day = smoothstep(0.0, 0.12, gSunDir.y) * step(0.5, dot(gLightDir, gSunDir));
    if (day <= 0.0) return vec3(0.0);
    float a = degrees(acos(clamp(dot(dir, -gSunDir), -1.0, 1.0)));
    if (a < 38.0 || a > 56.0) return vec3(0.0);
    vec3 bow = sprayBowColor((a - 40.4) / 2.0) + 0.35 * sprayBowColor((53.8 - a) / 3.4);
    return bow * gLightColor * day * 0.9;
}
#endif
#endif
