"""Generates the README/article diagrams (Spanish + English) as self-contained HTML files.

Style: diagram-design editorial system, skinned with the app palette (royal purple + gold).
Render to PNG with: uv run --with playwright python docs/diagrams/export.py
"""
from pathlib import Path
from html import escape

OUT = Path(__file__).parent

# ---- Skin (app palette) ----
PAPER = "#FBFAFE"
INK = "#1F1A33"
MUTED = "#4F4A66"
SOFT = "#7C7794"
ACCENT = "#5B3CC4"
LINK = "#B7791F"
RULE = "rgba(31,26,51,0.12)"

KINDS = {
    #          fill                        stroke                    tag color
    "focal": ("rgba(91,60,196,0.08)", ACCENT, ACCENT),
    "step": ("#ffffff", INK, INK),
    "store": ("rgba(31,26,51,0.05)", MUTED, MUTED),
    "cloud": ("rgba(183,121,31,0.06)", LINK, LINK),
    "input": ("rgba(79,74,102,0.10)", SOFT, SOFT),
}


def t(x, y, s, size=12, weight=400, color=INK, family="sans", anchor="middle", spacing=None):
    fam = {"sans": "'Geist', sans-serif", "mono": "'Geist Mono', monospace", "serif": "'Instrument Serif', serif"}[family]
    ls = f' letter-spacing="{spacing}"' if spacing else ""
    return (f'<text x="{x}" y="{y}" fill="{color}" font-size="{size}" font-weight="{weight}" '
            f'font-family="{fam}" text-anchor="{anchor}"{ls}>{escape(s)}</text>')


def node(x, y, w, h, kind, tag, name, sub=None):
    fill, stroke, tagc = KINDS[kind]
    cx = x + w / 2
    tw = len(tag) * 5.6 + 12
    parts = [
        f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="6" fill="{PAPER}"/>',
        f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="6" fill="{fill}" stroke="{stroke}" stroke-width="1"/>',
        f'<rect x="{x + 8}" y="{y + 7}" width="{tw}" height="13" rx="2" fill="none" stroke="{tagc}" stroke-opacity="0.45" stroke-width="0.8"/>',
        t(x + 8 + tw / 2, y + 16.5, tag, size=7.5, weight=500, color=tagc, family="mono", spacing="0.08em"),
        t(cx, y + h / 2 + (6 if sub else 10), name, size=13, weight=600),
    ]
    if sub:
        parts.append(t(cx, y + h / 2 + 22, sub, size=9.5, color=MUTED, family="mono"))
    return "\n".join(parts)


def label(cx, y_line, s, color=MUTED, side=None, x_left=None):
    """Arrow label with opaque mask. Above a horizontal line by default (8px gap), or beside a vertical one."""
    w = len(s) * 5.6 + 12
    if side == "right":
        x0, y0 = x_left, y_line - 6
    elif side == "left":
        x0, y0 = x_left - w, y_line - 6
    else:
        x0, y0 = cx - w / 2, y_line - 20
    return (f'<rect x="{x0}" y="{y0}" width="{w}" height="12" rx="2" fill="{PAPER}"/>'
            + t(x0 + w / 2, y0 + 9, s, size=8, weight=500, color=color, family="mono", spacing="0.06em"))


def arrow(d, color=MUTED, dashed=False, width=1.2):
    marker = {MUTED: "arrow", ACCENT: "arrow-accent", LINK: "arrow-link"}[color]
    dash = ' stroke-dasharray="4,3"' if dashed else ""
    return f'<path d="{d}" fill="none" stroke="{color}" stroke-width="{width}"{dash} marker-end="url(#{marker})"/>'


def zone(x, y, w, h, title, boundary=False):
    if boundary:
        box = (f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="8" fill="rgba(91,60,196,0.03)" '
               f'stroke="{ACCENT}" stroke-opacity="0.55" stroke-width="1" stroke-dasharray="4,4"/>')
        color = ACCENT
    else:
        box = (f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="8" fill="rgba(183,121,31,0.03)" '
               f'stroke="{LINK}" stroke-opacity="0.35" stroke-width="0.8"/>')
        color = LINK
    lw = len(title) * 6 + 16
    return box + (f'<rect x="{x + 16}" y="{y - 6}" width="{lw}" height="12" rx="2" fill="{PAPER}"/>'
                  + t(x + 16 + lw / 2, y + 3, title, size=8, weight=600, color=color, family="mono", spacing="0.14em"))


def legend(y, items, width=1000):
    out = [f'<line x1="40" y1="{y}" x2="{width - 40}" y2="{y}" stroke="{RULE}" stroke-width="0.8"/>']
    x = 40
    for kind, text in items:
        if kind in KINDS:
            fill, stroke, _ = KINDS[kind]
            out.append(f'<rect x="{x}" y="{y + 18}" width="16" height="11" rx="2" fill="{fill}" stroke="{stroke}" stroke-width="1"/>')
            tx = x + 22
        elif kind == "boundary":
            out.append(f'<rect x="{x}" y="{y + 18}" width="16" height="11" rx="2" fill="rgba(91,60,196,0.03)" stroke="{ACCENT}" stroke-width="1" stroke-dasharray="3,2"/>')
            tx = x + 22
        elif kind == "diamond":
            out.append(f'<polygon points="{x + 8},{y + 16} {x + 16},{y + 23.5} {x + 8},{y + 31} {x},{y + 23.5}" fill="rgba(91,60,196,0.08)" stroke="{ACCENT}" stroke-width="1"/>')
            tx = x + 22
        else:  # arrows: "a-muted", "a-link", "a-dashed"
            color = LINK if kind == "a-link" else MUTED
            out.append(arrow(f"M {x},{y + 23.5} H {x + 28}", color=color, dashed=kind == "a-dashed"))
            tx = x + 36
        out.append(t(tx, y + 27, text, size=10, color=MUTED, anchor="start"))
        x = tx + len(text) * 5.6 + 28
    return "\n".join(out)


DEFS = f"""<defs>
  <marker id="arrow" markerWidth="8" markerHeight="6" refX="7" refY="3" orient="auto"><polygon points="0 0, 8 3, 0 6" fill="{MUTED}"/></marker>
  <marker id="arrow-accent" markerWidth="8" markerHeight="6" refX="7" refY="3" orient="auto"><polygon points="0 0, 8 3, 0 6" fill="{ACCENT}"/></marker>
  <marker id="arrow-link" markerWidth="8" markerHeight="6" refX="7" refY="3" orient="auto"><polygon points="0 0, 8 3, 0 6" fill="{LINK}"/></marker>
</defs>"""


def page(slug, lang, eyebrow, title, desc, height, body):
    return f"""<!DOCTYPE html>
<html lang="{lang}">
<head>
<meta charset="UTF-8">
<title>{escape(title)}</title>
<link href="https://fonts.googleapis.com/css2?family=Instrument+Serif:ital@0;1&family=Geist:wght@400;500;600&family=Geist+Mono:wght@400;500;600&display=swap" rel="stylesheet">
<style>
  *, *::before, *::after {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{ font-family: 'Geist', sans-serif; background: {PAPER}; color: {INK}; padding: 3rem 2rem; display: flex; justify-content: center; }}
  .frame {{ max-width: 1100px; width: 100%; }}
  .eyebrow {{ font-family: 'Geist Mono', monospace; font-size: .66rem; font-weight: 500; letter-spacing: .18em; text-transform: uppercase; color: {MUTED}; margin-bottom: .5rem; }}
  h1 {{ font-family: 'Instrument Serif', serif; font-size: 2rem; font-weight: 400; letter-spacing: -.02em; margin-bottom: 1.5rem; }}
  svg {{ width: 100%; min-width: 900px; display: block; }}
</style>
</head>
<body>
<div class="frame">
<p class="eyebrow">{escape(eyebrow)}</p>
<h1>{escape(title)}</h1>
<svg viewBox="0 0 1000 {height}" xmlns="http://www.w3.org/2000/svg" role="img" aria-labelledby="{slug}-title {slug}-desc">
<title id="{slug}-title">{escape(title)}</title>
<desc id="{slug}-desc">{escape(desc)}</desc>
{DEFS}
<rect width="100%" height="100%" fill="{PAPER}"/>
{body}
</svg>
</div>
</body>
</html>
"""


# ---------------------------------------------------------------- 1. Split brain overview
def split_brain(L):
    b = [
        zone(24, 64, 680, 352, L["phone_zone"], boundary=True),
        zone(752, 64, 224, 352, L["cloud_zone"]),
        # arrows (behind nodes)
        arrow("M 176,144 H 248"),
        label(212, 144, L["real_text"]),
        arrow("M 424,144 H 496"),
        label(460, 144, L["anonymized"]),
        arrow("M 656,144 H 792", color=LINK, width=1.5),
        label(724, 144, L["only_summary"], color=LINK),
        arrow("M 336,176 V 224"),
        label(0, 200, L["never_leaves"], side="right", x_left=346),
        arrow("M 336,288 V 336"),
        arrow("M 872,176 V 360 Q 872,368 864,368 H 424", color=LINK, dashed=True),
        label(620, 368, L["epic_excuse"], color=LINK),
        arrow("M 256,368 H 176"),
        # nodes
        node(40, 112, 136, 64, "input", L["t_user"], L["story"], L["story_sub"]),
        node(248, 112, 176, 64, "focal", L["t_local"], "Gemma 4 E2B", L["gemma_sub"]),
        node(496, 112, 160, 64, "step", L["t_code"], L["leak"], "LeakDetector"),
        node(792, 112, 160, 64, "cloud", L["t_cloud"], "Gemini", "gemini-3.8-flash"),
        node(248, 224, 176, 64, "store", L["t_memory"], L["map"], "[PERSON_1] → Laura"),
        node(248, 336, 176, 64, "step", L["t_code"], L["restore"], "Deanonymizer"),
        node(40, 336, 136, 64, "input", L["t_result"], L["excuse"], L["excuse_sub"]),
        legend(440, [("focal", L["lg_local"]), ("step", L["lg_code"]), ("store", L["lg_memory"]),
                     ("cloud", L["lg_cloud"]), ("boundary", L["lg_boundary"]), ("a-link", L["lg_cloud_arrow"])]),
    ]
    return "\n".join(b)


# ---------------------------------------------------------------- 2. Pipeline flowchart
def pipeline(L):
    diamond = (f'<polygon points="728,52 784,96 728,140 672,96" fill="{PAPER}"/>'
               f'<polygon points="728,52 784,96 728,140 672,96" fill="rgba(91,60,196,0.08)" stroke="{ACCENT}" stroke-width="1.2"/>'
               + t(728, 93, L["q1"], size=11, weight=600) + t(728, 107, L["q2"], size=11, weight=600))
    end = (f'<rect x="424" y="392" width="136" height="48" rx="20" fill="{PAPER}"/>'
           f'<rect x="424" y="392" width="136" height="48" rx="20" fill="rgba(79,74,102,0.10)" stroke="{SOFT}" stroke-width="1"/>'
           + t(492, 420, L["excuse"], size=13, weight=600))
    b = [
        arrow("M 16,96 H 112"),
        label(64, 96, L["real_story"]),
        arrow("M 256,96 H 296"),
        arrow("M 440,96 H 480"),
        arrow("M 640,96 H 672"),
        arrow("M 784,96 H 832", color=LINK, width=1.5),
        label(808, 96, L["no"], color=LINK),
        arrow("M 728,140 V 224"),
        label(0, 182, L["yes"], side="right", x_left=736),
        arrow("M 880,128 V 224", color=LINK),
        label(0, 176, L["excuse_lbl"], side="left", x_left=872),
        arrow("M 928,224 V 128", dashed=True),
        label(0, 176, L["retry"], side="right", x_left=936),
        arrow("M 728,288 V 384"),
        arrow("M 904,288 V 408 Q 904,416 896,416 H 800"),
        label(852, 416, L["approved"]),
        arrow("M 656,416 H 560"),
        node(112, 64, 144, 64, "step", L["t_local"], L["s1"], "[PERSON_1] · [PLACE_1]"),
        node(296, 64, 144, 64, "store", L["t_code"], L["leak"], L["leak_sub"]),
        node(480, 64, 160, 64, "step", L["t_local"], L["s25"], L["s25_sub"]),
        diamond,
        node(832, 64, 144, 64, "cloud", L["t_cloud"], L["s6b"], "Firebase AI Logic"),
        node(656, 224, 144, 64, "step", L["t_local"], L["s6a"], L["s6a_sub"]),
        node(832, 224, 144, 64, "step", L["t_local"], L["s7"], L["s7_sub"]),
        node(656, 384, 144, 64, "store", L["t_code"], L["s8"], "Deanonymizer"),
        end,
        legend(472, [("step", L["lg_local2"]), ("store", L["lg_code"]), ("cloud", L["lg_cloud"]),
                     ("diamond", L["lg_decision"]), ("a-link", L["lg_cloud_arrow"]), ("a-dashed", L["lg_retry"])]),
    ]
    return "\n".join(b)


# ---------------------------------------------------------------- 3. Layers
def layers(L):
    rows = L["layers"]
    out = []
    x, w, h, y0 = 136, 824, 60, 32
    for i, (tag, name, sub) in enumerate(rows):
        y = y0 + i * h
        focal = i == 2
        fill = "rgba(91,60,196,0.08)" if focal else ("#ffffff" if i % 2 == 0 else "rgba(31,26,51,0.03)")
        out.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{fill}" stroke="{RULE}" stroke-width="1"/>')
        out.append(t(x + 20, y + h / 2 + 3, tag, size=8.5, weight=600, color=ACCENT if focal else SOFT, family="mono", anchor="start", spacing="0.14em"))
        out.append(t(x + 170, y + h / 2 + 5, name, size=15, weight=600, anchor="start"))
        out.append(t(x + w - 20, y + h / 2 + 4, sub, size=10, color=MUTED, family="mono", anchor="end"))
    total = len(rows) * h
    out.append(f'<rect x="{x}" y="{y0}" width="{w}" height="{total}" fill="none" stroke="{MUTED}" stroke-width="1"/>')
    focal_y = y0 + 2 * h
    out.append(f'<rect x="{x}" y="{focal_y}" width="{w}" height="{h}" fill="none" stroke="{ACCENT}" stroke-width="1.4"/>')
    # direction indicator on the left margin
    out.append(t(72, y0 + 14, L["top"], size=8.5, color=SOFT, family="mono", spacing="0.1em"))
    out.append(arrow(f"M 72,{y0 + 26} V {y0 + total - 26}"))
    out.append(t(72, y0 + total - 6, L["bottom"], size=8.5, color=SOFT, family="mono", spacing="0.1em"))
    return "\n".join(out)


ES = {
    "phone_zone": "EL TELÉFONO · DATOS PRIVADOS", "cloud_zone": "LA NUBE",
    "real_text": "TEXTO REAL", "anonymized": "ANONIMIZADO", "only_summary": "SOLO EL RESUMEN",
    "never_leaves": "NUNCA SALE", "epic_excuse": "EXCUSA CON [PERSON_1]",
    "t_user": "USUARIO", "t_local": "LOCAL", "t_code": "CÓDIGO", "t_cloud": "NUBE", "t_memory": "MEMORIA", "t_result": "RESULTADO",
    "story": "Historia real", "story_sub": "nombres y detalles", "gemma_sub": "anonimiza · decide · revisa",
    "leak": "Revisión de fugas", "map": "Mapa de nombres", "restore": "Restaurar nombres",
    "excuse": "Excusa épica", "excuse_sub": "con nombres reales",
    "lg_local": "Modelo local", "lg_code": "Código determinista", "lg_memory": "Memoria del teléfono", "lg_cloud": "Nube",
    "lg_boundary": "Límite de privacidad", "lg_cloud_arrow": "Viaja a la nube",
    # pipeline
    "q1": "¿Leve o", "q2": "sin internet?", "real_story": "HISTORIA REAL", "no": "NO", "yes": "SÍ",
    "excuse_lbl": "EXCUSA", "retry": "RECHAZO", "approved": "APROBADA",
    "s1": "1 · Anonimizar", "leak_sub": "regex + mapa", "s25": "2–5 · Analizar", "s25_sub": "resumen · gravedad · tono",
    "s6b": "6b · Gemini escribe", "s6a": "6a · Gemma escribe", "s6a_sub": "gratis · sin conexión",
    "s7": "7 · Gemma revisa", "s7_sub": "máx. 1 reintento", "s8": "8 · Restaurar",
    "lg_local2": "Gemma (local)", "lg_decision": "Decisión de ruta", "lg_retry": "Reintento",
    # layers
    "top": "UI", "bottom": "MODELO",
    "layers": [
        ("INTERFAZ", "Jetpack Compose", "MainScreen · ModelSetupScreen · SettingsScreen"),
        ("ESTADO", "ExcuseViewModel", "StateFlow<UiState> · inyección manual"),
        ("ORQUESTACIÓN", "ExcusePipeline", "el split brain, en Kotlin simple"),
        ("CEREBROS", "LocalBrain · CloudBrain · código", "Gemma · Gemini · LeakDetector · Deanonymizer"),
        ("AGENTES", "ADK for Kotlin", "LlmAgent · InMemoryRunner · sesión por paso"),
        ("MODELOS", "LiteRT-LM · Firebase AI Logic", "Gemma en el teléfono · Gemini + App Check"),
    ],
}

EN = {
    "phone_zone": "THE PHONE · PRIVATE DATA", "cloud_zone": "THE CLOUD",
    "real_text": "REAL TEXT", "anonymized": "ANONYMIZED", "only_summary": "SUMMARY ONLY",
    "never_leaves": "NEVER LEAVES", "epic_excuse": "EXCUSE WITH [PERSON_1]",
    "t_user": "USER", "t_local": "LOCAL", "t_code": "CODE", "t_cloud": "CLOUD", "t_memory": "MEMORY", "t_result": "RESULT",
    "story": "Real story", "story_sub": "names and details", "gemma_sub": "anonymize · route · review",
    "leak": "Leak check", "map": "Name map", "restore": "Restore names",
    "excuse": "Epic excuse", "excuse_sub": "with real names",
    "lg_local": "Local model", "lg_code": "Deterministic code", "lg_memory": "Phone memory", "lg_cloud": "Cloud",
    "lg_boundary": "Privacy boundary", "lg_cloud_arrow": "Goes to the cloud",
    "q1": "Minor or", "q2": "offline?", "real_story": "REAL STORY", "no": "NO", "yes": "YES",
    "excuse_lbl": "EXCUSE", "retry": "REJECTED", "approved": "APPROVED",
    "s1": "1 · Anonymize", "leak_sub": "regex + map", "s25": "2–5 · Analyze", "s25_sub": "summary · severity · tone",
    "s6b": "6b · Gemini writes", "s6a": "6a · Gemma writes", "s6a_sub": "free · offline",
    "s7": "7 · Gemma reviews", "s7_sub": "max. 1 retry", "s8": "8 · Restore",
    "lg_local2": "Gemma (local)", "lg_decision": "Routing decision", "lg_retry": "Retry",
    "top": "UI", "bottom": "MODEL",
    "layers": [
        ("UI", "Jetpack Compose", "MainScreen · ModelSetupScreen · SettingsScreen"),
        ("STATE", "ExcuseViewModel", "StateFlow<UiState> · manual DI"),
        ("ORCHESTRATION", "ExcusePipeline", "the split brain, in plain Kotlin"),
        ("BRAINS", "LocalBrain · CloudBrain · code", "Gemma · Gemini · LeakDetector · Deanonymizer"),
        ("AGENTS", "ADK for Kotlin", "LlmAgent · InMemoryRunner · session per step"),
        ("MODELS", "LiteRT-LM · Firebase AI Logic", "Gemma on the phone · Gemini + App Check"),
    ],
}

SPECS = [
    ("split-brain", split_brain, 500,
     {"es": ("Arquitectura · split brain", "Qué corre en el teléfono y qué en la nube",
             "Diagrama de arquitectura: Gemma y el código procesan la historia real dentro del teléfono; a Gemini solo llega un resumen anonimizado y los nombres reales se restauran en el teléfono."),
      "en": ("Architecture · split brain", "What runs on the phone and what runs in the cloud",
             "Architecture diagram: Gemma and code process the real story on the phone; Gemini only receives an anonymized summary and real names are restored on the phone.")}),
    ("pipeline", pipeline, 520,
     {"es": ("Flowchart · pipeline", "El pipeline: cada paso tiene su cerebro",
             "Diagrama de flujo del pipeline: anonimizar, revisar fugas y analizar en local; si es leve o no hay internet escribe Gemma, si no escribe Gemini y Gemma revisa; al final se restauran los nombres."),
      "en": ("Flowchart · pipeline", "The pipeline: every step has its brain",
             "Pipeline flowchart: anonymize, leak check and analysis run locally; minor or offline cases are written by Gemma, otherwise Gemini writes and Gemma reviews; names are restored at the end.")}),
    ("layers", layers, 420,
     {"es": ("Capas · arquitectura de la app", "Las capas de la app",
             "Pila de seis capas: interfaz Compose, ViewModel, ExcusePipeline como orquestación, cerebros, ADK for Kotlin y los runtimes LiteRT-LM y Firebase AI Logic."),
      "en": ("Layers · app architecture", "The layers of the app",
             "Six-layer stack: Compose UI, ViewModel, ExcusePipeline orchestration, brains, ADK for Kotlin, and the LiteRT-LM and Firebase AI Logic runtimes.")}),
]

if __name__ == "__main__":
    for slug, build, height, meta in SPECS:
        for lang, strings in (("es", ES), ("en", EN)):
            eyebrow, title, desc = meta[lang]
            name = f"{slug}.{lang}"
            (OUT / f"{name}.html").write_text(page(f"{slug}-{lang}", lang, eyebrow, title, desc, height, build(strings)), encoding="utf-8")
            print("wrote", name + ".html")
