// La brume proche en volume (fogMarch), marchée dans le demi-écran de program/clouds.fsh et relue par mist.fsh.
// Demande shadow.glsl, mist.glsl, clouds.glsl et voxel.glsl avant lui.

#if FOG_OPTICS > 0
/**
 * Ce que les gouttelettes de la brume renvoient vers l'arrière, autour du point opposé à l'astre (l'ombre de la tête) :
 * l'arc de brouillard, un arc blanc et large à 41 degrés, à peine orangé dehors et bleuté dedans ; et la gloire,
 * quelques anneaux irisés serrés autour du centre. À ajouter à la fonction de phase de la brume.
 */
vec3 fogOptics(vec3 dir) {
    float a = degrees(acos(clamp(dot(dir, -gLightDir), -1.0, 1.0)));
    if (a > 46.0) return vec3(0.0);
    vec3 bow = vec3(exp(-sq((a - 41.3) / 2.2)), exp(-sq((a - 40.7) / 2.2)), exp(-sq((a - 40.1) / 2.2)));
    bow = mix(vec3(dot(bow, vec3(0.333))), bow, 0.35) * 0.045;
    vec3 rings = (0.5 + 0.5 * cos(6.2832 * a / vec3(1.45, 1.25, 1.05))) * exp(-a / 2.2);
    vec3 glory = rings * 0.10 + exp(-sq(a / 0.9)) * 0.05;
    return bow + glory;
}
#endif

/**
 * La brume proche en volume, marchée de l'œil jusqu'à reach (au plus FOG_RANGE) : des pas resserrés près de l'œil.
 * À chaque pas :
 *  - sa densité : la nappe qui s'amincit avec la hauteur, roulée en bancs par deux octaves de bruit qui dérivent avec
 *    le vent (des écharpes plus denses, des trouées), retirée dans les clairières ; et sous terre, une fine poussière ;
 *  - sa lumière : le ciel, le soleil ou la lune là où la carte d'ombre les laisse passer (les rayons sous les arbres,
 *    colorés par les vitraux), et la lumière des blocs alentour lue dans le volume coloré : un halo orangé autour des
 *    torches, bleu autour des lanternes d'âme ;
 *  - l'air lui-même renvoie un peu de soleil, sans brume : les rayons par temps clair.
 * Intégrée exactement pas à pas. Rend la part du fond qui passe ; light reçoit ce que la brume ajoute.
 */
float fogMarch(vec3 dir, float reach, float sunAmount, out vec3 light) {
    light = vec3(0.0);
    float pass = 1.0;
#if FOG_STEPS > 0
    vec3 origin = gbufferModelViewInverse[3].xyz;
    float base = mistAnchor();
    float h = MIST_HEIGHT;
    float open = smoothstep(0.0, 0.55, float(eyeBrightnessSmooth.y) / 240.0);
    float dens0 = mistNearDensity() * (1.0 - MIST_LIFT * brumaireMist.x);
    // Sous terre, une poussière que seules les torches font voir.
    float cave = (1.0 - open) * 0.0022 * CAVE_HAZE;
    float mu = dot(dir, gLightDir);
    float phaseMist = 0.75 * mistPhase(mu, 0.5) + 0.25 * mistPhase(mu, -0.3);
    float phaseAir = 0.7 * mistPhase(mu, 0.75) + 0.3 * mistPhase(mu, 0.2);
    vec3 body = mix(gAmbient, vec3(luma(gAmbient)), 0.75) * vec3(1.03, 1.0, 0.95) * 0.95 * mistTint() * mix(0.8, 1.0, gDay) * mix(vec3(1.12, 1.0, 0.85), vec3(1.0), gDay) * mix(1.0, 2.2, rainStrength) * mix(1.0, 0.5, gDusk)
        * mix(vec3(1.0), vec3(0.97, 1.0, 1.04), gDay * (1.0 - gDusk));
    // Au couchant, ambrée (voir mistColor).
    body = mix(body, vec3(luma(body)) * vec3(1.3, 0.95, 0.6), 0.8 * gDusk * (1.0 - rainStrength));
    body = mix(body, vec3(luma(body)) * vec3(1.06, 0.99, 0.88), 0.7 * rainStrength);
    // Vers l'astre bas, la brume s'allume sans noyer le paysage : on doit encore lire les reliefs à contre-jour.
    vec3 beam = mix(gLightColor, vec3(luma(gLightColor)) * vec3(1.0, 0.93, 0.78), 0.2 * gDusk) * (0.03 + 0.22 * phaseMist * SHAFT_STRENGTH) * sunAmount * mix(1.0, 0.075, gDusk);
    // La nuit, la lune éclaire la brume deux fois plus (voir mistColor) ; pas l'arc de brume, qui virait au faux arc-en-ciel.
    beam *= mix(2.0, 1.0, gDay);
#if FOG_OPTICS > 0
    beam += gLightColor * fogOptics(dir) * sunAmount;
#endif
    // Le jour, l'air seul ne renvoie que peu de soleil : face à l'astre bas, il ne doit pas noyer le paysage dans un voile
    // laiteux. La lune, faible, garde ses rayons. Au couchant, presque rien : c'était la bande crème sous le soleil.
    float airGlow = (mix(0.0045, 0.0016 * mix(1.0, 0.1, gDusk), smoothstep(-0.14, 0.0, gSunDir.y)) * SHAFT_STRENGTH) * mix(0.8, 1.6, airHumid) * smoothstep(0.2, 0.7, float(eyeBrightnessSmooth.y) / 240.0);
#if MOON_SHAFTS > 0
    // La nuit, l'œil fait à l'ombre voit les rayons de lune dans la brume bien plus nettement que la lumière qu'elle
    // verse au sol ne le laisse croire : les faisceaux sont renforcés, la lumière des surfaces ne change pas.
    float moonBoost = 1.0 + (1.0 - gDay) * 2.5 * float(MOON_SHAFTS);
    beam *= moonBoost;
    airGlow *= moonBoost;
#endif
    bool sunUp = dot(gLightColor, vec3(1.0)) > 0.01;
    // Les bancs dérivent avec le vent, plus vite en haut.
    vec2 wind = brumaireLink > 0.5 && dot(brumaireWind.xy, brumaireWind.xy) > 0.01 ? normalize(brumaireWind.xy) * (0.6 + 1.2 * brumaireWind.z) : vec2(0.8, 0.35);
    vec3 drift = vec3(wind.x, 0.0, wind.y) * frameTimeCounter * 0.9;
    float noise = dither(gl_FragCoord.xy);
    float dayOpen = smoothstep(0.0, 0.3, gSunDir.y) * smoothstep(0.86, 0.99, float(eyeBrightnessSmooth.y) / 240.0);
    float prev = 0.0;
    for (int i = 0; i < FOG_STEPS; i++) {
        float next = reach * sq(float(i + 1) / float(FOG_STEPS));
        float dt = next - prev;
        float t = prev + dt * noise;
        prev = next;
        vec3 p = origin + dir * t;
        vec3 w = p + cameraPosition;
        // Sous le pied de la nappe, au creux d'une vallée vue d'en haut, elle s'épaissit au plus de 2,2 fois, comme la
        // nappe lointaine (lib/mist.glsl) : pas un mur laiteux sur un lac en contrebas.
        float d = dens0 * min(exp(clamp((base - w.y) / h, -30.0, 3.0)), 2.2);
        // De jour, à découvert, l'air tout proche reste clair : le voile commence à une trentaine de blocs.
        d *= mix(1.0, smoothstep(0.0, 32.0, t), dayOpen);
#if FOG_BILLOWS > 0
        if (d > 1e-5) {
            float n1 = cloudNoise((w - drift) * 0.018 + vec3(0.0, frameTimeCounter * 0.0012, 0.0)).r;
            float n2 = cloudNoise((w - drift * 1.6) * 0.062 + 0.37).g;
            float n = n1 * 0.7 + n2 * 0.3;
            d *= max(mix(1.0, smoothstep(0.25, 0.75, n) * 2.4, 0.8 * FOG_BILLOWS), 0.0);
        }
#endif
        // Les clairières : la brume y est retirée, le bord adouci.
        vec4 cs[4] = vec4[4](brumaireClearing0, brumaireClearing1, brumaireClearing2, brumaireClearing3);
        for (int k = 0; k < 4; k++) {
            if (cs[k].z > 0.0) d *= 1.0 - cs[k].w * (1.0 - smoothstep(cs[k].z * 0.72, cs[k].z, length(p.xz - cs[k].xy)));
        }
        d += cave;
        // Soleil ou lune à ce point : la carte d'ombre, teinte des vitraux comprise.
        vec3 sun = vec3(sunUp ? 1.0 : 0.0);
#if LIGHT_SHAFTS > 0
        if (sunUp && t < shadowDistance * 0.9) {
            vec3 uv = shadowWarp((shadowProjection * (shadowModelView * vec4(p, 1.0))).xyz) * 0.5 + 0.5;
            float s = uv.z < 1.0 ? texture(shadowtex1, vec3(uv.xy, uv.z - 0.0002)) : 1.0;
            sun = s > 0.0 ? s * tintAt(uv) : vec3(0.0);
        }
#endif
        vec3 local = body * open + beam * sun;
        float through = exp(-d * dt);
        light += pass * (1.0 - through) * local;
        // L'air, sans brume, renvoie le soleil vers l'avant : ses rayons là où des feuillages le découpent.
        light += pass * gLightColor * sun * phaseAir * airGlow * dt;
        pass *= through;
        if (pass < 0.01) break;
    }
#if COLORED_LIGHT > 0
    // Le halo des torches, des lanternes et de la lave : une marche à part, serrée sur les premiers blocs, car un halo
    // n'a que quelques blocs de large et la marche de la brume y passe en grands pas. L'air humide autour d'une flamme
    // suffit à le montrer, même sans brume ; il s'éteint avec la brume traversée devant lui.
    if (FOG_BLOCK_GLOW > 0.0) {
        float haloReach = min(reach, 32.0);
        float hdt = haloReach / 10.0;
        float glowAir = 0.008 + 0.016 * airHumid + 0.012 * rainStrength + cave * 1.0;
        vec3 glow = vec3(0.0);
        for (int i = 0; i < 10; i++) {
            float t = (float(i) + noise) * hdt;
            vec4 blk = voxelLight(origin + dir * t, vec3(0.0));
            if (blk.a < 0.3) continue;
            // Le niveau de lumière baisse d'un cran par bloc : son cube fait la lueur large, une haute puissance
            // le cœur serré autour de la flamme.
            float a = blk.a;
            float k = a * a * a * 0.45 + pow(a, 10.0) * 5.0;
            glow += blk.rgb * k * exp(-(dens0 + cave) * t);
        }
        light += glow * hdt * (glowAir + dens0) * FOG_BLOCK_GLOW;
    }
#endif
#endif
    return pass;
}

