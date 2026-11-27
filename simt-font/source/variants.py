"""Redesign candidates for the hah family (ج ح خ چ) and kaf family (ك ک گ).

Option A, "calligraphic": follows the Naskh skeleton closely: a short head
with a sharp corner, a deep bowl that bulges left, the kaf as a steep
diagonal with a head stroke.
Option B, "compass": the same skeletons built from compass arcs and
verticals, closer to the geometric Latin.
"""
import arabic as AR
from arabic import A, SB, CAPA, Path, poly, diag, _arc_sector

ORIG_HAH = AR.hah_family
ORIG_KAF = AR.kaf_family
Tp = 400  # hah head height


def _head(a, hx_l, hx_r):
    h = a.Wa
    a.c += poly((hx_l, Tp - h / 2), (hx_r, Tp - h / 2), (hx_r, Tp + h / 2 - a.k * 0.7), (hx_l, Tp + h / 2))


def _hah_dots(a, dot, form, x_in, hx_l, hx_r):
    h = a.Wa
    if dot == "above":
        a.dot((hx_l + hx_r) / 2, Tp + h / 2 + 70)
    elif form in ("isol", "fina"):
        if dot == "below":
            a.dot(x_in, 40)
        elif dot == "three":
            a.dots(3, x_in, -60, below=True)
    else:
        if dot == "below":
            a.dot(x_in, -a.d - 85)
        elif dot == "three":
            a.dots(3, x_in, -a.d * 2.9 - 85, below=True)


# ------------------------------------------------------------------ shared hah parts

def head_A(a, hx_l, hx_r):
    """Calligraphic head: downward tick at the right end, gentle wave leftwards,
    left end protruding as a small beak."""
    w = a.Wa * 1.02
    p = Path((hx_r + 4, Tp - 85)).C((hx_r + 2, Tp - 20), (hx_r - 25, Tp + 22), (hx_r - 80, Tp + 22))
    p.C((hx_r - 140, Tp + 22), (hx_l + 80, Tp - 2), (hx_l, Tp - 2))
    a.c += __import__("geom").stroke(p, w)


def head_B(a, hx_l, hx_r):
    """Geometric head: flat bar, right end turning down as a short tick cut
    at the Arabic bearing."""
    h = a.Wa
    a.c += poly((hx_l, Tp - h / 2), (hx_r - h * 0.2, Tp - h / 2), (hx_r - h * 0.2, Tp + h / 2), (hx_l, Tp + h / 2))
    a.c += poly((hx_r - h, Tp - h / 2 - 70 + a.k), (hx_r, Tp - h / 2 - 70), (hx_r, Tp + h / 2), (hx_r - h, Tp + h / 2))


def _jdots(a, dot, x_in_bowl, x_init, hx_l, hx_r, form):
    if dot == "above":
        a.dot((hx_l + hx_r) / 2 + 10, Tp + a.Wa / 2 + 70)
    elif form in ("isol", "fina"):
        if dot == "below":
            a.dot(x_in_bowl, 20)
        elif dot == "three":
            a.dots(3, x_in_bowl, -90, below=True)
    else:
        if dot == "below":
            a.dot(x_init, -a.d - 85)
        elif dot == "three":
            a.dots(3, x_init, -a.d * 2.9 - 85, below=True)


def make_hah(P, name, uni, dot, style):
    head = head_A if style == "A" else head_B
    out = {"medi": ORIG_HAH(P, name, uni, dot)["medi"]}
    # initial: head, then the body drops down-left straight into the join
    a = A(P)
    adv = 400 + a.e
    hx_r = adv - SB
    hx_l = hx_r - 225
    head(a, hx_l, hx_r)
    cy = a.cy
    if style == "A":
        p = Path((hx_l + 30, Tp)).C((hx_l - 25, Tp - 25), (hx_l - 42, Tp - 110), (hx_l - 42, 170))
        p.C((hx_l - 42, 80), (hx_l - 60, cy), (hx_l - 115, cy)).L((0, cy))
        a.st(p)
        x_body = hx_l - 42
    else:
        xs = hx_l + a.Wa / 2 - 18
        r = a.r
        p = Path((xs, Tp)).L((xs, cy + r)).C((xs, cy + r * 0.45), (xs - r * 0.45, cy), (xs - r, cy)).L((0, cy))
        a.st(p)
        x_body = xs
    _jdots(a, dot, 0, x_body + 10, hx_l, hx_r, "init")
    out["init"] = a.glyph(name + ".init", adv, (hx_l + hx_r) / 2)
    # isolated / final: head plus a large bowl open to the right
    for form in ("isol", "fina"):
        a = A(P)
        Wa = a.Wa
        adv = 610 + a.e + (80 if form == "fina" else 0)
        hx_l = SB + 75
        hx_r = hx_l + 300
        head(a, hx_l, hx_r)
        yb = -225 + Wa / 2
        xr_tip = hx_r + 40 + a.e * 0.5
        if style == "A":
            p = Path((hx_l + 32, Tp)).C((hx_l - 40, Tp - 25), (SB, Tp - 140), (SB, 110))
            p.C((SB, -120), (SB + 150, yb), (SB + 300, yb)).C((xr_tip - 130, yb), (xr_tip - 40, yb + 25), (xr_tip, yb + 100))
            a.taper_tip(p, 0.3, 0.86)
        else:
            top = Tp - Wa / 2 + 4
            bot = -225
            ryo = (top - bot) / 2
            cyb = bot + ryo
            rxo = 255 + a.e * 0.3
            cxb = SB + rxo
            a.c += _arc_sector(cxb, cyb, rxo, ryo, Wa * 1.05, Wa * 0.9, 96, 326)
            a.c += poly((hx_l, Tp - Wa / 2 - 2), (hx_l + 60, Tp - Wa / 2 - 2), (cxb - 25, top - Wa * 0.6), (hx_l, top - Wa * 0.6))
        if form == "fina":
            a.c += diag((adv, 0), (hx_r - 30, Tp + Wa / 2), Wa)
        _jdots(a, dot, SB + 250, 0, hx_l, hx_r, form)
        nm = name if form == "isol" else name + ".fina"
        out[form] = a.glyph(nm, adv, (hx_l + hx_r) / 2, uni=uni if form == "isol" else None)
    return out


def hah_A(P, name, uni, dot=None):
    return make_hah(P, name, uni, dot, "A")


def hah_B(P, name, uni, dot=None):
    return make_hah(P, name, uni, dot, "B")


# ------------------------------------------------------------------ kaf

def _kaf_base_isol(a, x0, xr, up=230):
    """Base running right from x0 to a short upturn at xr, rounded corner."""
    cy, r = a.cy, a.r
    p = Path((x0, cy)).L((xr - r, cy)).C((xr - r * 0.45, cy), (xr, cy + r * 0.45), (xr, cy + r)).L((xr, up))
    a.st(p)
    a.wedge_stem(xr, up - 60, up + a.k)


def _kaf_stem_L(a, xs, top):
    """Tall stem with a rounded foot turning right (isolated / final kaf)."""
    cy, r = a.cy, a.r
    p = Path((xs, top - 12)).L((xs, cy + r)).C((xs, cy + r * 0.45), (xs + r * 0.45, cy), (xs + r, cy))
    a.st(p)
    a.wedge_stem(xs, top - 80, top)


def kaf_arms(a, xv, style, gaf=False, yv=300, top=600):
    """The '<': upper arm from the vertex up-right, lower arm down-right to the
    baseline. Returns x of the lower arm's foot."""
    Wa = a.Wa
    xu, xl = xv + 185, xv + 175
    if style == "A":
        up = Path((xv + 8, yv)).C((xv + 70, yv + 90), (xu - 50, top - 60), (xu, top))
        a.c += __import__("geom").stroke(up, Wa, w_end=Wa * 0.55, taper_from=0.35)
        lo = Path((xv + 8, yv)).C((xv + 70, yv - 90), (xl - 40, 70), (xl, a.cy))
        a.st(lo)
        a.c += __import__("geom").disc(xv + 8, yv, Wa * 0.52)
    else:
        a.c += diag((xv, yv), (xu, top), Wa)
        a.c += diag((xv, yv), (xl, 0), Wa)
    if gaf:
        dx, dy = xu - xv, top - yv
        L = (dx * dx + dy * dy) ** 0.5
        nx, ny = -dy / L, dx / L
        off = Wa + 48
        s0 = (xv + dx * 0.42 + nx * off, yv + dy * 0.42 + ny * off)
        s1 = (xu + nx * off + 10, top + ny * off)
        if style == "A":
            a.c += __import__("geom").stroke(Path(s0).L(s1), Wa * 0.85, w_end=Wa * 0.5, taper_from=0.3)
        else:
            a.c += diag(s0, s1, Wa * 0.85)
    return xl


def make_kaf(P, name, uni, keheh, gaf, style):
    out = {}
    for form in ("init", "medi"):
        a = A(P)
        xv = SB + 20
        xl = kaf_arms(a, xv, style, gaf)
        adv = xl + a.Wa / 2 + (SB if form == "init" else 40)
        a.conn(0, adv if form == "medi" else xl + 20)
        out[form] = a.glyph(name + "." + form, adv, xv + 120)
    for form in ("isol", "fina"):
        a = A(P)
        Wa, cy, r = a.Wa, a.cy, a.r
        if keheh:
            xv = SB + 10
            xl = kaf_arms(a, xv, style, gaf)
            adv = 580 + a.e + (10 if form == "fina" else 0)
            if form == "isol":
                _kaf_base_isol(a, xl - 20, adv - SB - Wa / 2, up=230)
            else:
                a.conn(xl - 20, adv)
            ax = xv + 140
        else:
            adv = 430 + a.e + (10 if form == "fina" else 0)
            xs = SB + Wa / 2
            top = CAPA - 20
            if style == "A":
                _kaf_stem_L(a, xs, top)
                fl = Path((xs - 4, top - 30)).C((xs + 35, top - 38), (xs + 70, top - 60), (xs + 95, top - 95))
                a.c += __import__("geom").stroke(fl, Wa * 0.85, w_end=Wa * 0.45, taper_from=0.3)
            else:
                a.wedge_stem(xs, 0, top)
                a.c += diag((xs - Wa * 0.2, top), (xs + 90, top - 120), Wa * 0.85)
            xr = adv - SB - Wa / 2
            if form == "isol":
                _kaf_base_isol(a, xs + (r - 5 if style == "A" else 0), xr, up=250)
            else:
                a.conn(xs + (r - 5 if style == "A" else 0), adv)
            hx = xs + 125 + a.e * 0.6
            a.hamza(hx, Wa + 175, s=0.66, w=Wa * 0.55)
            ax = hx
        nm = name if form == "isol" else name + ".fina"
        out[form] = a.glyph(nm, adv, ax, uni=uni if form == "isol" else None)
    return out


def kaf_A(P, name, uni, keheh=False, gaf=False):
    return make_kaf(P, name, uni, keheh, gaf, "A")


def kaf_B(P, name, uni, keheh=False, gaf=False):
    return make_kaf(P, name, uni, keheh, gaf, "B")
