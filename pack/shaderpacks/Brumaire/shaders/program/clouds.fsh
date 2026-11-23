// Le ciel en volume, marché sur un demi-écran : la lumière diffusée vers l'œil (rgb) et la part du fond qui passe (a).
// D'avant en arrière : l'arc-en-ciel dans la pluie qui s'éloigne, la couche de nuages (lib/clouds.glsl), puis tout
// en haut le voile de cirrus et le halo que ses cristaux dessinent autour de l'astre (lib/skyoptics.glsl).
// mist.fsh relit le tout, le lisse et le pose sur le ciel et le paysage.
//
// La lumière dans la couche :
//  - l'astre, atténué le long d'un cône de pas qui s'allongent vers lui ; la diffusion multiple est faite d'octaves
//    (chacune moins éteinte, moins orientée, moins forte que la précédente), qui laissent la lumière traverser le
//    cœur d'un cumulus au lieu de le laisser noir ;
//  - le ciel, d'autant moins qu'il y a de nuage au-dessus ;
//  - le sol, qui renvoie un peu de soleil sous le ventre des nuages ;
//  - l'éclair, qui allume les tours de l'intérieur.
#define FSH
#include "/lib/settings.glsl"
#include "/lib/common.glsl"
#include "/lib/clouds.glsl"
#include "/lib/skyoptics.glsl"
#include "/program/scene.glsl"
#if FOG_STEPS > 0
#include "/lib/shadow.glsl"
#include "/lib/mist.glsl"
#include "/lib/voxel.glsl"
#include "/lib/fogmarch.glsl"
#endif

uniform float wetness;

in vec2 vUv;

// Format et fond de colortex3 : dans mist.fsh, toujours actif (cette passe est coupée sans nuages).

/* RENDERTARGETS: 3,11 */
layout(location = 0) out vec4 outClouds;
// La brume proche en volume (lib/fogmarch.glsl) : la lumière qu'elle ajoute (rgb) et la part du fond qui passe (a).
layout(location = 1) out vec4 outFog;

// Extinction des gouttelettes par unité de densité (par bloc).
const float CLOUD_SIGMA = 0.09;

/** Diffusion des gouttelettes (Henyey-Greenstein), normalisée sur la sphère. */
float cloudPhase(float mu, float g) {
    return 0.0795775 * (1.0 - g * g) / pow(max(1.0 + g * g - 2.0 * g * mu, 1e-4), 1.5);
}

/**
 * La phase des gouttelettes de nuage : un pic très en avant (la frange d'argent quand l'astre est derrière), un lobe
 * en arrière plus large, et la gloire qui revient tout droit vers l'astre. g se resserre avec les octaves.
 */
float dropletPhase(float mu, float spread) {
    float forward = cloudPhase(mu, 0.82 * spread);
    float back = cloudPhase(mu, -0.28 * spread);
    float wide = cloudPhase(mu, 0.35 * spread);
    return forward * 0.42 + wide * 0.40 + back * 0.18;
}

/** Huit directions fixes, pour écarter les pas de la marche vers l'astre en un cône. */
vec3 coneKernel(int i) {
    const vec3 k[8] = vec3[8](
        vec3(0.38, -0.60, 0.70), vec3(-0.75, 0.24, -0.62), vec3(0.62, 0.48, -0.62), vec3(-0.28, 0.86, 0.43),
        vec3(0.09, -0.92, -0.38), vec3(0.86, -0.12, 0.49), vec3(-0.59, -0.55, 0.59), vec3(-0.13, 0.40, -0.91));
    return k[i & 7];
}

float cloudHash(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.x + p.y) * p.z);
}

/**
 * Épaisseur optique vers l'astre, depuis p : des pas qui doublent, écartés en un cône qui s'ouvre, et un dernier pas
 * très loin pour qu'une tour voisine fasse de l'ombre. Le détail fin n'est lu que sur les deux premiers pas.
 */
float lightDepth(vec3 p, float coverage, float jitter) {
    float od = 0.0;
    float reach = 5.0;
    float walked = 0.0;
    vec3 side = normalize(cross(gLightDir, vec3(0.0, 1.0, 0.001)));
    vec3 up = cross(side, gLightDir);
    for (int j = 0; j < CLOUD_LIGHT_STEPS; j++) {
        float stride = reach * (0.75 + 0.5 * jitter);
        walked += stride;
        vec3 k = coneKernel(j + int(jitter * 8.0));
        vec3 q = p + gLightDir * walked + (side * k.x + up * k.y) * walked * 0.22;
        od += cloudDensity(q, coverage, j < 2) * stride;
        reach *= 1.8;
    }
    // Un dernier regard loin vers l'astre : l'ombre d'une tour sur sa voisine.
    vec3 far_ = p + gLightDir * (walked + 260.0);
    od += cloudDensity(far_, coverage, false) * 120.0;
    return od;
}

/**
 * Éclairs dans les nuages : celui qui frappe allume la colonne qu'il traverse, et sous l'orage des lueurs naissent
 * au hasard dans des cellules de quatre cents blocs, vives puis éteintes en une fraction de seconde.
 */
vec3 cloudLightning(vec3 p, float h) {
#if CLOUD_LIGHTNING > 0
    vec3 glow = vec3(0.0);
    if (lightningBoltPosition.w > 0.5) {
        vec3 bolt = cameraPosition + lightningBoltPosition.xyz;
        float across = length(p.xz - bolt.xz);
        glow += gFlash * exp(-across / 140.0) * (0.6 + 0.4 * (1.0 - h)) * 9.0;
    }
    if (thunderStrength > 0.05) {
        vec2 cell = floor(p.xz / 400.0);
        float beat = floor(frameTimeCounter * 2.5);
        float roll = cloudHash(vec3(cell, beat));
        if (roll > 0.985) {
            vec2 centre = (cell + 0.5 + (vec2(cloudHash(vec3(cell, beat + 0.5)), cloudHash(vec3(cell + 3.1, beat))) - 0.5) * 0.6) * 400.0;
            float age = fract(frameTimeCounter * 2.5);
            float pulse = exp(-age * 7.0) * (0.7 + 0.3 * sin(age * 90.0));
            float near_ = exp(-length(p.xz - centre) / 90.0) * smoothstep(0.1, 0.5, h);
            glow += near_ * pulse * thunderStrength * 5.0;
        }
    }
    return glow * vec3(0.70, 0.76, 1.0);
#else
    return vec3(0.0);
#endif
}

#if HEAT_LIGHTNING > 0
/**
 * Les éclairs de chaleur : les nuits chaudes et lourdes d'été, sans pluie ici, des orages trop lointains pour qu'on
 * les entende allument par moments, en silence, des bancs de nuages au ras de l'horizon. Trois foyers, chacun dans
 * sa direction, qui changent tous les quarts d'heure ; une lueur qui clignote deux ou trois fois puis s'éteint.
 */
vec3 heatLightning(vec3 dir) {
    float night = 1.0 - smoothstep(0.0, 0.25, gDay);
    float sultry = smoothstep(0.62, 0.85, airHeat) * smoothstep(0.35, 0.65, airHumid) * (1.0 - rainStrength);
    float amount = night * sultry;
    if (amount <= 0.001 || dir.y < -0.02 || dir.y > 0.22) return vec3(0.0);
    float watch = floor(frameTimeCounter / 900.0);
    float az = atan(dir.z, dir.x);
    vec3 glow = vec3(0.0);
    for (int k = 0; k < 3; k++) {
        vec3 id = vec3(watch, float(k) * 7.3, 2.9);
        if (cloudHash(id) > 0.7) continue;
        float centre = (cloudHash(id + 1.7) * 2.0 - 1.0) * 3.14159;
        float off = abs(mod(az - centre + 3.14159, 6.28318) - 3.14159);
        if (off > 0.45) continue;
        // Des salves : un battement sur six environ s'allume, en deux ou trois éclats.
        float beat = floor(frameTimeCounter * 0.9 + float(k) * 0.37);
        if (cloudHash(vec3(beat, float(k), 5.1)) > 0.17) continue;
        float age = fract(frameTimeCounter * 0.9 + float(k) * 0.37);
        float pulse = exp(-age * 5.0) * max(sin(age * 38.0 + float(k)), 0.0) + exp(-max(age - 0.35, 0.0) * 9.0) * step(0.35, age) * 0.6;
        // Le banc lointain : des tours bosselées au ras de l'horizon, plus claires dans leur cœur.
        float bank = cloudNoise(vec3(az * 6.0, dir.y * 14.0, float(k) + watch * 0.13)).r;
        float shape = smoothstep(0.30, 0.62, bank - dir.y * 1.4) * smoothstep(-0.02, 0.01, dir.y);
        // Et l'air au-dessus, qui s'éclaire un peu autour du banc.
        float haze = exp(-max(dir.y, 0.0) / 0.05);
        glow += vec3(0.78, 0.74, 1.0) * pulse * exp(-sq(off / 0.22)) * (shape * 0.18 + haze * 0.04);
    }
    return glow * amount;
}
#endif

/** La marche dans la couche de nuages. light et pass s'accumulent d'avant en arrière ; rend la distance moyenne vue. */
float marchLayer(vec3 eye, vec3 dir, float sceneLen, float coverage, float noise, inout vec3 light, inout float pass) {
    if (abs(dir.y) < 1e-4) return 0.0;
    float a = (cloudBase() - eye.y) / dir.y;
    float b = (cloudTop() - eye.y) / dir.y;
    float t0 = max(min(a, b), 0.0);
    float t1 = min(max(a, b), min(sceneLen, CLOUD_RANGE));
    if (eye.y > cloudBase() && eye.y < cloudTop()) {
        t0 = 0.0;
        t1 = min(dir.y > 0.0 ? b : a, min(sceneLen, CLOUD_RANGE));
    }
    if (t1 <= t0) return 0.0;

    // Plus de pas quand le trajet est long (regard rasant), sans dépasser le double.
    float span = t1 - t0;
    int steps = int(float(CLOUD_STEPS) * mix(1.0, 2.0, smoothstep(CLOUD_TOP_SPAN * 2.0, CLOUD_TOP_SPAN * 12.0, span)));
    float stride = span / float(steps);
    float t = t0 + stride * noise;
    float mu = dot(dir, gLightDir);
    vec3 sun = gLightColor * CLOUD_LIGHT;
    // Le sol sous la couche : un vert-brun qui renvoie l'astre et un peu de ciel vers le ventre des nuages.
    vec3 ground = vec3(0.16, 0.17, 0.12) * (gLightColor * max(gLightDir.y, 0.0) * 0.9 + gAmbient * 0.5);
    float depthSum = 0.0, depthWeight = 0.0;
    int empty = 0;
    for (int i = 0; i < 64; i++) {
        if (i >= steps) break;
        vec3 p = eye + dir * t;
        // Hors des nuages, la forme seule suffit ; le détail n'est lu qu'au bord ou dedans.
        float d = cloudDensity(p, coverage, true);
        if (d > 0.002) {
            float h = clamp((p.y - cloudBase()) / CLOUD_TOP_SPAN, 0.0, 1.0);
            float od = lightDepth(p, coverage, fract(noise + float(i) * 0.618));
            // La poudre : un bord fin, peu chargé de gouttes, renvoie moins vers l'arrière qu'un cœur dense. Ne se voit
            // qu'à contre-jour de l'astre (dos au soleil, les nuages sont pleins de lumière).
            float powder = 1.0 - exp(-d * CLOUD_SIGMA * 24.0);
            powder = mix(1.0, powder, 0.65 * smoothstep(0.6, -0.4, mu));
            // Diffusion multiple en octaves.
            vec3 direct = vec3(0.0);
            float strength = 1.0, ext = 1.0, spread = 1.0;
            for (int n = 0; n < CLOUD_MULTISCATTER; n++) {
                direct += strength * exp(-od * CLOUD_SIGMA * ext) * dropletPhase(mu, spread);
                strength *= 0.5;
                ext *= 0.38;
                spread *= 0.55;
            }
            direct *= sun * powder * 4.4;
            // Le ciel : vu d'en haut un nuage est plein, dessous il en reçoit moins ; un nuage au-dessus l'assombrit.
            float above = cloudDensity(p + vec3(0.0, CLOUD_TOP_SPAN * 0.22, 0.0), coverage, false) * CLOUD_TOP_SPAN * 0.22;
            vec3 skyLit = gAmbient * mix(0.22, 1.0, h) * exp(-above * CLOUD_SIGMA * 0.35) * 0.95;
            vec3 bounce = ground * sq(1.0 - h) * 1.4;
            vec3 inscatter = direct + skyLit + bounce + cloudLightning(p, h);
            float sigma = d * CLOUD_SIGMA;
            float through = exp(-sigma * stride);
            // Intégrale exacte sur le pas : la lumière diffusée s'éteint déjà dans le pas lui-même.
            float taken = pass * (1.0 - through);
            light += taken * inscatter;
            depthSum += taken * t;
            depthWeight += taken;
            pass *= through;
            if (pass < 0.015) break;
            empty = 0;
        } else {
            // Dans le ciel libre, des pas plus longs jusqu'au prochain nuage.
            empty++;
        }
        t += stride * (empty > 2 ? 1.6 : 1.0);
        if (t > t1) break;
    }
    return depthWeight > 1e-4 ? depthSum / depthWeight : t0;
}

void main() {
    outClouds = vec4(0.0, 0.0, 0.0, 1.0);
    outFog = vec4(0.0, 0.0, 0.0, 1.0);
#if FOG_STEPS > 0
    // La brume proche se marche ici, sur un demi-écran : mist.fsh la ramène au pixel en suivant la profondeur.
    if (isEyeInWater == 0) {
        bool fogSky, fogHand;
        vec3 fogPos = scenePosition(vUv, fogSky, fogHand);
        if (!fogHand) {
            initEnv();
            float fogLen = fogSky ? 16384.0 : length(fogPos);
            vec3 fogLight;
            float fogPass = fogMarch(normalize(fogPos), min(fogLen, FOG_RANGE), 1.0, fogLight);
            outFog = vec4(fogLight, fogPass);
        }
    }
#endif
#if CLOUD_STEPS > 0
    bool sky, hand;
    vec3 pos = scenePosition(vUv, sky, hand);
    if (hand) return;
    initEnv();
    vec3 dir = normalize(pos);
    float sceneLen = sky ? 1e9 : length(pos);
    vec3 eye = cameraPosition;
    float coverage = cloudCoverage();
    float noise = dither(gl_FragCoord.xy);

    vec3 light = vec3(0.0);
    float pass = 1.0;

    // L'arc-en-ciel est le plus proche : dans la pluie qui s'en va, entre l'œil et les nuages.
    if (sky) {
        vec4 bow = rainbow(dir, wetness, eye);
        light += bow.rgb;
        pass *= bow.a;
#if SKY_NOCTILUCENT > 0
        light += noctilucent(dir);
#endif
#if SUN_PILLAR > 0
        light += sunPillar(dir);
#endif
    }

    vec3 cloudLight = vec3(0.0);
    float cloudPass = 1.0;
    float seen = marchLayer(eye, dir, sceneLen, coverage, noise, cloudLight, cloudPass);
    // Au loin, les nuages se fondent dans le ciel bas : la perspective aérienne, à la distance où on les voit vraiment.
    float fade = exp(-seen / (CLOUD_RANGE * 0.5));
    vec3 horizon = skyAt(normalize(vec3(dir.x, max(dir.y, 0.03), dir.z)));
    cloudLight = mix(horizon * (1.0 - cloudPass), cloudLight, fade);
    // Sous la pluie, des nuages gris.
    cloudLight = mix(cloudLight, vec3(luma(cloudLight)) * vec3(1.05, 0.98, 0.86) * 0.85, rainStrength * 0.6);
    // La nuit, les nuages sont des ombres : des bancs gris ardoise devant la lueur du ciel et de la lune, pas des
    // masses plus claires que lui (Recrafted : nuages vers 70 sur 255 devant un ciel à 115). Leur liseré vers la lune
    // reste, à la même proportion.
    float nightC = 1.0 - smoothstep(-0.14, 0.0, gSunDir.y);
    cloudLight = mix(cloudLight, vec3(luma(cloudLight)) * vec3(0.92, 0.97, 1.04), 0.5 * nightC) * mix(1.0, 0.45, nightC);
#if CLOUD_IRIDESCENCE > 0
    // Les franges minces d'un cumulus près de l'astre s'irisent aussi, moins qu'une nappe fine.
    if (cloudPass < 0.98) {
        float thin = 4.0 * cloudPass * (1.0 - cloudPass);
        vec3 at = eye + dir * seen;
        cloudLight *= max(vec3(1.0) + cloudIridescence(dir, at.xz) * thin * 0.5 * (1.0 - rainStrength), 0.0);
    }
#endif
    light += pass * cloudLight;
    pass *= cloudPass;
#if HEAT_LIGHTNING > 0
    // Les orages lointains, derrière les nuages proches.
    if (sky) light += pass * heatLightning(dir);
#endif

    // Au-dessus : la nappe d'altocumulus, puis tout en haut le voile de cirrus et le halo que ses cristaux dessinent.
    if (sky && dir.y > 0.0) {
        vec4 alto = altoLayer(eye, dir);
        light += pass * alto.rgb;
        pass *= 1.0 - alto.a;
        vec4 veil = cirrusLayer(eye, dir);
        light += pass * veil.rgb;
        pass *= 1.0 - veil.a;
    }
    outClouds = vec4(light, pass);
#endif
}
