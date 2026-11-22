// L'image finie : halo des hautes lumières, exposition, courbe de tons, étalonnage. Le résultat, déjà en tons d'écran,
// passe par l'anticrénelage (smaa.fsh) avant l'écran.
#include "/lib/settings.glsl"

/*
const int colortex0Format = R11F_G11F_B10F;
const int colortex7Format = RGBA8;
*/
const vec4 colortex0ClearColor = vec4(0.0, 0.0, 0.0, 1.0);
#ifdef BLOOM
const bool colortex0MipmapEnabled = true;
#endif
uniform float viewWidth;
uniform float viewHeight;
uniform sampler2D lensTex;
uniform sampler2D depthtex0;
uniform float near;
uniform float far;

uniform sampler2D colortex0;
uniform vec3 sunPosition;
uniform mat4 gbufferModelViewInverse;
uniform ivec2 eyeBrightnessSmooth;
#if RAIN_GLOOM > 0
uniform float rainStrength;
#endif
uniform mat4 gbufferProjection;
uniform float frameTimeCounter;
#if NIGHT_GRAIN > 0
uniform int frameCounter;
#endif
in vec2 vUv;

/* RENDERTARGETS: 7 */
layout(location = 0) out vec4 outColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);
#define TONE_HUE_NORM 0.5

/** Courbe de tons : une épaule douce pour les hautes lumières, un pied qui garde du noir. */
float tone(float x) {
    return clamp(x * (2.51 * x + 0.03) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

/**
 * La courbe appliquée à l'intensité plutôt qu'à chaque canal : un sable ou une neige au soleil gardent leur teinte
 * au lieu de blanchir. L'intensité mêle luminance et canal le plus fort, pour qu'un rouge vif ne déborde pas ;
 * seul ce qui dépasse encore le blanc pâlit, comme une pellicule qui sature.
 */
vec3 toneColor(vec3 x) {
    float n = max(mix(dot(x, LUMA), max(x.r, max(x.g, x.b)), TONE_HUE_NORM), 1e-5);
    vec3 c = x * (tone(n) / n);
    float over = max(c.r, max(c.g, c.b));
    if (over > 1.0) c = mix(c / over, vec3(1.0), clamp((over - 1.0) * 0.5, 0.0, 1.0));
    return clamp(c, 0.0, 1.0);
}

float sq(float x) { return x * x; }

/** Distance (blocs) d'un point de l'écran ; -1 pour la main. */
float viewDistance(vec2 uv) {
    float d = texture(depthtex0, uv).r;
    if (d < 0.56) return -1.0;
    if (d >= 1.0) return far * 4.0;
    float z = d * 2.0 - 1.0;
    return 2.0 * near * far / (far + near - z * (far - near));
}

#if DOF > 0
/**
 * La profondeur de champ : ce qui est loin du point devient flou comme dans un objectif, d'un disque dont la taille
 * suit l'écart entre l'inverse des distances. Vingt-quatre prises en spirale ; chacune ne compte que si elle est elle-
 * même assez floue pour s'étendre jusqu'ici, sans quoi un premier plan net bavait sur le fond. La main reste nette.
 */
vec3 depthOfField(vec3 color, float focus) {
    float d = viewDistance(vUv);
    if (d < 0.0) return color;
    float aperture = DOF == 1 ? 18.0 : 42.0;
    float maxR = DOF == 1 ? 6.0 : 12.0;
    float scale = viewHeight / 720.0;
    float coc = min(abs(1.0 / focus - 1.0 / d) * aperture * scale, maxR * scale);
    if (coc < 0.6) return color;
    vec2 pixel = 1.0 / vec2(viewWidth, viewHeight);
    vec3 sum = color;
    float total = 1.0;
    float turn = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715)))) * 6.2831853;
    for (int i = 0; i < 24; i++) {
        float r = sqrt((float(i) + 0.5) / 24.0) * coc;
        float a = float(i) * 2.3999632 + turn;
        vec2 uv = vUv + vec2(cos(a), sin(a)) * r * pixel;
        float sd = viewDistance(uv);
        if (sd < 0.0) continue;
        float scoc = min(abs(1.0 / focus - 1.0 / sd) * aperture * scale, maxR * scale);
        // Une prise plus proche que le point compte selon son propre flou ; plus loin, elle ne peut pas déborder sur
        // ce qui est devant elle.
        float w = clamp(min(scoc, sd < d ? scoc : coc) - r + 1.0, 0.0, 1.0);
        sum += texture(colortex0, uv).rgb * w;
        total += w;
    }
    return sum / total;
}
#endif

#ifdef BLOOM
/** Halo des hautes lumières : l'image relue dans ses réductions successives, sans passe de flou à part. */
vec3 glow(float threshold) {
    vec3 sum = vec3(0.0);
    vec2 pixel = 1.0 / vec2(viewWidth, viewHeight);
    float weight = 0.40;
    // Du plus serré au plus large : un cœur net autour d'une source vive, puis une traîne qui s'étend loin.
    for (int level = 2; level <= 6; level++) {
        vec2 step_ = pixel * exp2(float(level)) * 0.75;
        vec3 c = textureLod(colortex0, vUv, float(level)).rgb * 0.5;
        c += textureLod(colortex0, vUv + step_ * vec2(1.0, 1.0), float(level)).rgb * 0.125;
        c += textureLod(colortex0, vUv + step_ * vec2(-1.0, 1.0), float(level)).rgb * 0.125;
        c += textureLod(colortex0, vUv + step_ * vec2(1.0, -1.0), float(level)).rgb * 0.125;
        c += textureLod(colortex0, vUv + step_ * vec2(-1.0, -1.0), float(level)).rgb * 0.125;
        // Un seuil au genou doux : ce qui approche du seuil luit déjà un peu, sans marche visible.
        float l = max(c.r, max(c.g, c.b));
        float knee = 0.6;
        float soft = sq(clamp(l - threshold + knee, 0.0, 2.0 * knee)) / (4.0 * knee);
        sum += c * (max(soft, l - threshold) / max(l, 1e-4)) * weight;
        weight *= 0.72;
    }
    return sum;
}
#endif

#if defined BLOOM && LENS_DIRT > 0
float dirtHash(vec2 p) { return fract(sin(dot(p, vec2(41.3, 289.1))) * 43758.5453); }

/**
 * La poussière et les traces sur l'objectif (0..1) : des taches rondes et floues de toutes tailles, posées une fois
 * pour toutes sur l'image, invisibles tant qu'aucune lumière vive ne passe derrière elles.
 */
float lensDirt(vec2 uv) {
    vec2 p = uv * vec2(viewWidth / viewHeight, 1.0) * 5.0;
    float dirt = 0.0;
    for (int layer = 0; layer < 2; layer++) {
        vec2 cell = floor(p);
        vec2 f = p - cell;
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                vec2 c = cell + vec2(x, y) + float(layer) * 17.0;
                float h = dirtHash(c);
                if (h < 0.35) continue;
                vec2 centre = vec2(x, y) + vec2(dirtHash(c + 3.1), dirtHash(c + 7.7));
                float r = 0.12 + 0.38 * dirtHash(c + 1.9);
                float spot = smoothstep(r, r * 0.25, length(f - centre));
                dirt += spot * (0.25 + 0.75 * dirtHash(c + 5.3)) * (layer == 0 ? 1.0 : 0.6);
            }
        }
        p = p * 2.7 + 3.3;
    }
    return clamp(dirt, 0.0, 1.5);
}
#endif

#if defined BLOOM && SUN_GLARE > 0
/**
 * L'éclat du soleil dans l'œil : un voile autour de lui ; au réglage 2, des rais fins qui en partent et quelques reflets
 * de l'objectif alignés sur le centre de l'image (un œil ne voit pas d'étoile autour du soleil : ils restent en option). Tout suit la lumière qui entoure vraiment le soleil à l'écran (un nuage, un arbre
 * ou une colline l'éteignent), lue dans un niveau flou de l'image.
 */
vec3 sunGlare() {
    vec4 clip = gbufferProjection * vec4(sunPosition, 1.0);
    if (clip.w <= 0.0) return vec3(0.0);
    vec2 sun = clip.xy / clip.w * 0.5 + 0.5;
    // Hors de l'écran, l'éclat s'efface en quelques pas.
    vec2 out_ = max(-sun, sun - 1.0);
    float onScreen = 1.0 - smoothstep(0.0, 0.06, max(out_.x, out_.y));
    if (onScreen <= 0.0) return vec3(0.0);
    vec2 at = clamp(sun, vec2(0.002), vec2(0.998));
    // La lumière autour du soleil, moins le ciel qui l'entoure : ce qui reste, c'est l'astre lui-même, vu ou caché.
    vec3 disc = textureLod(colortex0, at, 3.0).rgb;
    vec3 around = textureLod(colortex0, at, 6.0).rgb;
    vec3 src = max(disc - around * 0.8, 0.0) * onScreen;
    if (dot(src, vec3(1.0)) <= 1e-4) return vec3(0.0);
    vec2 aspect = vec2(viewWidth / viewHeight, 1.0);
    vec2 d = (vUv - sun) * aspect;
    float r = length(d);
    // Les rais : six branches fines et quelques plus faibles, qui tournent à peine quand on bouge la tête.
    vec3 glare = src * (exp(-r * 30.0) * 0.20 + exp(-r * 4.0) * 0.012);
#if SUN_GLARE > 1
    float a = atan(d.y, d.x) + 0.15;
    float rays = pow(abs(cos(a * 3.0)), 140.0) + 0.35 * pow(abs(cos(a * 7.0 + 0.6)), 180.0);
    glare += src * rays * exp(-r * 11.0) * 0.16;
    // Les reflets de l'objectif : des disques pâles de couleur sur la ligne du soleil au centre, au-delà du centre.
    vec2 axis = (vec2(0.5) - sun);
    const vec3 ghostTint[4] = vec3[](vec3(1.0, 0.70, 0.35), vec3(0.45, 0.85, 1.0), vec3(0.70, 1.0, 0.60), vec3(1.0, 0.55, 0.80));
    const float ghostAt[4] = float[](0.55, 1.25, 1.55, 2.10);
    const float ghostSize[4] = float[](0.030, 0.070, 0.025, 0.110);
    for (int i = 0; i < 4; i++) {
        vec2 g = (vUv - (sun + axis * ghostAt[i])) * aspect;
        float disk = smoothstep(ghostSize[i], ghostSize[i] * 0.7, length(g));
        glare += src * ghostTint[i] * disk * 0.005;
    }
#endif
    return glare * SUN_GLARE_STRENGTH;
}
#endif

#if LENS_DROPS > 0
uniform float lensWet;
uniform float lensDive;
uniform int isEyeInWater;

float dropHash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }

/**
 * La goutte posée sur l'objectif en ce point de l'écran : (position dans la goutte, -1..1), sa présence (0..1) et son
 * rayon (hauteur d'écran). wet : 0..1. Trois tailles, les petites plus nombreuses ; chacune vit quelques secondes,
 * rétrécit en séchant, puis une autre se pose ailleurs dans sa case. Les grosses glissent lentement vers le bas.
 */
vec4 lensDrop(vec2 uv, float wet, float t) {
    vec2 p = uv * vec2(viewWidth / viewHeight, 1.0);
    vec4 found = vec4(0.0);
    for (int layer = 0; layer < 3; layer++) {
        float size = layer == 0 ? 0.030 : (layer == 1 ? 0.055 : 0.090);
        vec2 g = p / size + float(layer) * 7.31;
        vec2 cell = floor(g);
        float period = 5.0 + 7.0 * dropHash(cell + 3.1);
        float phase = t / period + dropHash(cell);
        float age = fract(phase);
        vec2 seed = cell + floor(phase) * 13.7;
        if (dropHash(seed + 0.7) > wet * (layer == 0 ? 0.13 : (layer == 1 ? 0.09 : 0.05))) continue;
        float r = (0.20 + 0.16 * dropHash(seed + 1.9)) * (1.0 - 0.45 * smoothstep(0.3, 1.0, age));
        vec2 c = vec2(dropHash(seed + 4.3), dropHash(seed + 5.9)) * (1.0 - 2.0 * r) + r;
        if (layer == 2) c.y = mix(c.y, r, smoothstep(0.2, 1.0, age) * step(0.5, dropHash(seed + 8.2)));
        vec2 d = (fract(g) - c) / r;
        // Une goutte pend un peu : plus ronde en bas qu'en haut.
        d.y *= d.y > 0.0 ? 1.15 : 0.9;
        if (dot(d, d) >= 1.0) continue;
        float life = smoothstep(0.0, 0.03, age) * (1.0 - smoothstep(0.65, 1.0, age));
        found = vec4(d, life, r * size);
    }
    return found;
}
#endif

#if LENS_SNOW > 0
float frostHash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }

float frostNoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(frostHash(i), frostHash(i + vec2(1.0, 0.0)), f.x), mix(frostHash(i + vec2(0.0, 1.0)), frostHash(i + vec2(1.0, 1.0)), f.x), f.y);
}
#endif

#if LENS_SNOW > 0
uniform float lensSnow;

/**
 * Le flocon posé sur l'objectif en ce point de l'écran : sa présence (0..1) et sa fonte (0 entier, 1 devenu goutte).
 * Deux tailles ; chacun vit quatre à huit secondes : il se pose, ses six branches fondent et il finit en une goutte
 * ronde qui sèche.
 */
vec2 lensFlake(vec2 uv, float amount, float t) {
    vec2 p = uv * vec2(viewWidth / viewHeight, 1.0);
    vec2 found = vec2(0.0);
    for (int layer = 0; layer < 2; layer++) {
        float size = layer == 0 ? 0.06 : 0.10;
        vec2 g = p / size + float(layer) * 5.17 + 2.3;
        vec2 cell = floor(g);
        float period = 4.0 + 4.0 * frostHash(cell + 1.7);
        float phase = t / period + frostHash(cell + 0.3);
        float age = fract(phase);
        vec2 seed = cell + floor(phase) * 17.3;
        if (frostHash(seed + 0.9) > amount * (layer == 0 ? 0.08 : 0.05)) continue;
        float r = 0.22 + 0.14 * frostHash(seed + 2.1);
        vec2 c = vec2(frostHash(seed + 3.3), frostHash(seed + 4.7)) * (1.0 - 2.0 * r) + r;
        vec2 d = (fract(g) - c) / r;
        float melt = smoothstep(0.35, 0.8, age);
        // Six branches fines, tournées au hasard, qui se fondent en un disque.
        float ang = atan(d.y, d.x) + frostHash(seed + 6.1) * 3.14;
        float arms = 0.25 + 0.75 * pow(abs(cos(3.0 * ang)), 10.0);
        float twigs = 0.12 * pow(abs(cos(9.0 * ang)), 6.0) * step(0.4, length(d));
        float edge = mix(arms + twigs, 0.5, melt) * (1.0 - 0.35 * melt);
        float l = length(d);
        if (l >= edge) continue;
        float life = smoothstep(0.0, 0.04, age) * (1.0 - smoothstep(0.85, 1.0, age));
        found = vec2(life * (1.0 - 0.4 * smoothstep(0.7, 1.0, l / edge)), melt);
    }
    return found;
}
#endif



void main() {
    vec3 color = texture(colortex0, vUv).rgb;
#if CHROMATIC_ABERRATION > 0
    // L'objectif sépare un peu les couleurs vers les bords : le rouge glisse dehors, le bleu dedans.
    {
        vec2 off = (vUv - 0.5) * sq(length(vUv - 0.5) * 1.6) * 0.0012;
        color.r = texture(colortex0, vUv + off).r;
        color.b = texture(colortex0, vUv - off).b;
    }
#endif
    vec4 lens = texelFetch(lensTex, ivec2(0), 0);
#if DOF > 0
    color = depthOfField(color, max(lens.y, 0.5));
#endif
#if LENS_DROPS > 0
    // Les gouttes sur l'objectif : des taches floues qui déforment un peu le paysage derrière elles. Moins quand on
    // regarde le sol.
    {
        float up = -gbufferModelViewInverse[2].y;
        float wet = isEyeInWater == 0 ? max(lensWet * (0.45 + 0.55 * smoothstep(-0.5, 0.4, up)), lensDive) : 0.0;
        if (wet > 0.01) {
            vec4 drop = lensDrop(vUv, wet, frameTimeCounter);
            if (drop.z > 0.0) {
                // Collée à l'objectif, la goutte est bien trop près pour être nette : une tache floue qui déforme un
                // peu ce qu'il y a derrière, sans contour dessiné ni point brillant (ce serait une bulle de savon).
                float l2 = dot(drop.xy, drop.xy);
                vec2 at = clamp(vUv - drop.xy * drop.w * vec2(viewHeight / viewWidth, 1.0) * 0.6, 0.002, 0.998);
#ifdef BLOOM
                vec3 inside = textureLod(colortex0, at, 2.5).rgb;
#else
                vec3 inside = texture(colortex0, at).rgb;
#endif
                // Le bord renvoie un peu moins de lumière, le bas en rassemble un peu plus : à peine.
                inside *= 1.0 - 0.10 * smoothstep(0.55, 1.0, l2);
                inside *= 1.0 + 0.08 * smoothstep(0.3, 0.9, l2) * smoothstep(0.2, 0.7, -drop.y / sqrt(max(l2, 1e-4)));
                drop.z *= 1.0 - smoothstep(0.55, 1.0, l2);
                // Hors de netteté, la goutte reste à demi transparente : une ondulation, pas une tache claire posée sur
                // l'image (sous les arbres, elle montrait le ciel retourné en ovale blanc).
                color = mix(color, inside, drop.z * 0.55);
            }
        }
    }
#endif
#if LENS_SNOW > 0
    // Les flocons posés sur l'objectif : blancs et nets, ils fondent en gouttes qui brouillent un peu l'image.
    if (lensSnow > 0.01) {
        vec2 flake = lensFlake(vUv, lensSnow, frameTimeCounter);
        if (flake.x > 0.0) {
            vec3 white = vec3(0.92, 0.96, 1.0) * max(lens.x, 0.02) * 5.0;
#ifdef BLOOM
            vec3 soft = textureLod(colortex0, vUv, 2.0).rgb;
#else
            vec3 soft = color;
#endif
            color = mix(color, mix(white, soft * 1.08, flake.y), flake.x * mix(0.8, 0.5, flake.y));
        }
    }
#endif
    // L'œil s'habitue : la nuit et sous terre, l'image s'éclaircit un peu.
    float sunUp = normalize(mat3(gbufferModelViewInverse) * sunPosition).y;
    // Peu, la nuit : une nuit de Recrafted reste noire (sol vers 20 sur 255), seuls la lune et les feux éclairent.
    float adapt = mix(1.1, 1.0, smoothstep(-0.18, 0.12, sunUp));
    adapt *= mix(1.5, 1.0, smoothstep(0.0, 0.5, float(eyeBrightnessSmooth.y) / 240.0));
    float exposure = 0.62 * EXPOSURE * adapt;
#if EXPOSURE_AUTO > 0
    // Et à ce qu'il regarde : une scène sombre s'ouvre, une scène éblouissante se ferme, à moitié seulement, comme un
    // œil et pas comme une caméra de surveillance, et en prenant son temps (lens.csh).
    float seen = max(lens.x, 1e-5) * exposure;
    // La nuit dehors, il ne rouvre pas plus grand : le noir reste noir. Sous terre non plus : une grotte reste sombre
    // autour de la lanterne, comme chez Recrafted. Une maison de jour, à demi ouverte sur le ciel, l'ouvre un peu.
    float skySeen = float(eyeBrightnessSmooth.y) / 240.0;
    float openMax = mix(1.0, 1.25, smoothstep(-0.18, 0.12, sunUp) * smoothstep(0.05, 0.40, skySeen));
    exposure *= clamp(pow(0.10 / seen, 0.3 * EXPOSURE_AUTO_AMOUNT), 0.75, openMax);
#endif
#if RAIN_GLOOM > 0
    // Sous la pluie, l'œil ne rouvre pas tout : un jour gris reste un jour sombre, d'un gris un peu chaud, celui des
    // nuages chargés. Seulement dehors : sous un toit ou sous terre, la pluie ne se voit pas.
    // Réglé sur les images de Recrafted : ciel autour de 160 sur 255, sol autour de 50.
    float gloom = rainStrength * smoothstep(0.3, 0.8, float(eyeBrightnessSmooth.y) / 240.0);
    // La nuit est déjà réglée pour elle-même : la pluie n'y pèse qu'à moitié.
    gloom *= mix(0.5, 1.0, smoothstep(-0.18, 0.12, sunUp));
    exposure *= mix(1.0, RAIN_GLOOM > 1 ? 0.58 : 0.68, gloom);
#endif
#ifdef BLOOM
    // Le seuil du halo suit l'exposition : ce qui éblouit l'œil, pas une valeur fixe.
    vec3 halo = glow(1.5 * 0.62 * EXPOSURE * adapt / exposure);
    color += halo * (0.5 * BLOOM_STRENGTH);
#if LENS_DIRT > 0
    // Face à une lumière vive, la poussière de l'objectif s'allume là où le halo passe, et un peu plus loin.
    vec3 wide = textureLod(colortex0, vUv, 7.0).rgb;
    // Seulement face au soleil ou à une lumière vive : un ciel clair ne suffit pas à la faire voir.
    vec3 dirtLight = max(halo - vec3(0.05), 0.0) + max(wide - vec3(2.5 * 0.62 * EXPOSURE * adapt / exposure), 0.0) * 0.6;
    color += dirtLight * lensDirt(vUv) * 1.4;
#endif
#endif
#if defined BLOOM && SUN_GLARE > 0
    color += sunGlare() * smoothstep(-0.02, 0.06, sunUp);
#endif
    color *= exposure;
#if RAIN_GLOOM > 0
    color *= mix(vec3(1.0), RAIN_GLOOM > 1 ? vec3(1.05, 1.0, 0.88) : vec3(1.03, 1.0, 0.94), gloom);
#endif
#if NIGHT_GRAIN > 0
    // La clarté vue par l'œil, avant les bâtonnets : le grain ne vient que dans le noir.
    float grainDark = 1.0 - smoothstep(0.003, 0.06, dot(color, LUMA));
#endif
#if PURKINJE > 0
    // Dans le noir, l'œil voit avec ses bâtonnets : les couleurs s'éteignent et l'image tire vers le bleu.
    float dark = 1.0 - smoothstep(0.004, 0.05, dot(color, LUMA));
    vec3 rod = vec3(0.62, 0.74, 1.0) * dot(color, vec3(0.08, 0.58, 0.34));
    color = mix(color, rod, dark * 0.55);
#endif
#if VIGNETTE > 0
    // L'objectif assombrit un peu les coins.
    vec2 q = (vUv - 0.5) * vec2(viewWidth / viewHeight, 1.0);
    color *= 1.0 - 0.22 * sq(sq(clamp(length(q) * 1.15, 0.0, 1.0)));
#endif
    color = toneColor(color);
    float l = dot(color, LUMA);
    // Ombres un peu froides, lumières un peu chaudes.
    color *= mix(vec3(0.99, 1.0, 1.01), vec3(1.04, 1.0, 0.93), smoothstep(0.05, 0.75, l));
#if FOLIAGE_TONE > 0
    {
        // La végétation : des verts jaunes et chauds d'herbe d'été, pas le vert gris que l'air lui laisse. Seulement ce
        // où le vert passe le bleu sans trop passer le rouge et garde un peu de couleur : ni le ciel ni ses nuages, ni la
        // brume, ni la pierre, ni l'eau.
        float mx = max(color.r, max(color.g, color.b));
        float chroma = (mx - min(color.r, min(color.g, color.b))) / max(mx, 1e-4);
        // Les écarts sont lus par rapport au plus fort canal, pour que l'herbe à l'ombre des arbres compte aussi. Une
        // herbe déjà très jaune (l'herbe sèche de l'hiver) n'est presque plus réchauffée : elle tournerait à la moutarde.
        float leaf = smoothstep(0.02, 0.17, (color.g - color.b) / max(mx, 1e-4))
                   * smoothstep(-0.40, -0.05, (color.g - color.r) / max(mx, 1e-4))
                   * smoothstep(0.003, 0.03, l) * smoothstep(0.08, 0.20, chroma) * (1.0 - 0.7 * smoothstep(0.45, 0.80, chroma)) * step(texture(depthtex0, vUv).r, 0.99999);
        vec3 warm = color * vec3(1.03, 1.0, 0.86);
#if RAIN_GLOOM > 0
        // L'herbe mouillée reste verte sous le gris chaud de la pluie (Recrafted : teinte 63-67°, pas un kaki).
        warm.r *= mix(1.0, 0.92, gloom);
#endif
        warm *= l / max(dot(warm, LUMA), 1e-5);
        warm = mix(vec3(l), warm, 1.0 + 0.45 * (1.0 - smoothstep(0.15, 0.50, chroma)));
        color = mix(color, warm, leaf * FOLIAGE_TONE);
    }
#endif
    // Les ombres gardent leur couleur : une pellicule, pas une vidéo délavée.
    color = mix(vec3(l), color, SATURATION * mix(1.18, 1.0, smoothstep(0.04, 0.45, l)));
    color = (color - 0.5) * CONTRAST + 0.5;
    // Une image mate : les noirs à peine levés, les blancs à peine retenus. La nuit, presque pas : 0,016 montait le
    // sol de nuit à 39 sur 255 et grisait tout, Recrafted le laisse vers 20.
    // Sous terre aussi : le noir d'une grotte reste noir (vers 10 sur 255 chez Recrafted).
    // De jour, à peine plus : 0,011 posait un plancher vers 33 sur 255 et l'ombre d'un arbre restait grise ; Recrafted
    // descend vers 15-30.
    float lift = mix(0.004, 0.005, smoothstep(-0.18, 0.12, sunUp) * smoothstep(0.05, 0.40, float(eyeBrightnessSmooth.y) / 240.0));
    color = color * (1.0 - 2.5 * lift) + lift;
    color = pow(max(color, 0.0), vec3(1.0 / 2.2));
#if NIGHT_GRAIN > 0
    // Le grain de la nuit : dans le noir, l'œil ne voit plus net et l'image fourmille d'un grain fin, sans couleur, qui
    // change à chaque image. Il suit la clarté : rien sur ce qui est éclairé, un peu dans la pénombre, plus dans le noir.
    if (grainDark > 0.01) {
        vec2 seed = gl_FragCoord.xy + vec2(float(frameCounter % 64) * 17.0, float(frameCounter % 61) * 29.0);
        float g1 = fract(sin(dot(seed, vec2(12.9898, 78.233))) * 43758.5453);
        float g2 = fract(sin(dot(seed, vec2(39.3468, 11.1351))) * 24634.6345);
        float grain = g1 + g2 - 1.0;
        color += grain * grainDark * (0.012 + 0.22 * dot(color, LUMA));
    }
#endif
    color += (fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715)))) - 0.5) / 255.0;
    outColor = vec4(color, 1.0);
}
