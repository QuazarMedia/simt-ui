"""Simt Sextant: Arabic glyphs.

Glyphs are drawn in the usual left-to-right coordinate frame. A joining glyph
connects to the previous letter on its right edge (x = advance) and to the
next letter on its left edge (x = 0), along the baseline connector.
Wedges rise to the LEFT (the Arabic reading direction), mirroring Latin.
"""
import math
from geom import Path, stroke, ring, poly, diag, Contour, shift, mirror_x, bbox
from latin import Glyph, B, X, CAP

ARS = 1.16     # Arabic optical scale: drawn small, scaled up to match Latin
CAPA = CAP / ARS   # alef / lam land on the Latin cap height after scaling
T = 430        # tooth height (reaches the Latin x-height after scaling)
TS = 330       # seen/sad tooth height
SB = 48        # side bearing on non-joining sides


class A:
    def __init__(self, P):
        self.P = P
        self.Wa = round(P["W"] * 0.97 / ARS)
        self.e = self.Wa - 68          # extra width in bolder weights
        self.cy = self.Wa / 2          # baseline connector centre line
        self.r = self.Wa * 0.78        # tooth foot radius
        self.k = P["k"] * 0.95 / ARS
        self.d = P["dot"] * 0.9 / ARS  # Arabic dot size
        self.c = []

    # ---------------- primitives
    def st(self, path, w=None, **kw):
        self.c += stroke(path, w or self.Wa, **kw)

    def conn(self, x0, x1):
        if x1 > x0:
            self.st(Path((x0, self.cy)).L((x1, self.cy)))

    def wedge_stem(self, xc, y0, y1, w=None):
        w = w or self.Wa
        a, b = xc - w / 2, xc + w / 2
        k = self.k * w / self.Wa
        self.c += poly((a, y0), (b, y0), (b, y1 - k), (a, y1))

    def stem(self, xc, y0, y1, w=None):
        w = w or self.Wa
        self.c += poly((xc - w / 2, y0), (xc + w / 2, y0), (xc + w / 2, y1), (xc - w / 2, y1))

    def tooth(self, xt, top, x_exit=0, wedge=True, joined_right=False):
        """A tooth: vertical stroke from top, rounded foot turning left to x_exit.
        joined_right: a connector arrives from the right, so square the foot."""
        r, cy = self.r, self.cy
        if joined_right:
            self.c += poly((xt - r, 0), (xt + self.Wa / 2, 0), (xt + self.Wa / 2, self.Wa), (xt - r, self.Wa))
        p = Path((xt, top - 12)).L((xt, cy + r)).C((xt, cy + r * 0.45), (xt - r * 0.45, cy), (xt - r, cy))
        if x_exit < xt - r:
            p.L((x_exit, cy))
        self.st(p)
        if wedge:
            self.wedge_stem(xt, top - 70, top + self.k * 0.0 + self.k)

    def dot(self, xc, y0):
        d = self.d
        s = d * 0.62
        a, b = xc - d / 2, xc + d / 2
        self.c += poly((a, y0 + s), (b, y0), (b, y0 + d), (a, y0 + s + d))

    def dots(self, n, xc, y0, below=False):
        d = self.d
        g = d * 0.34
        if n == 1:
            self.dot(xc, y0)
        elif n == 2:
            self.dot(xc - (d + g) / 2, y0)
            self.dot(xc + (d + g) / 2, y0)
        elif n == 3:
            if below:
                self.dot(xc - (d + g) / 2, y0 + d * 0.62 + d + g * 0.6)
                self.dot(xc + (d + g) / 2, y0 + d * 0.62 + d + g * 0.6)
                self.dot(xc, y0)
            else:
                self.dot(xc - (d + g) / 2, y0)
                self.dot(xc + (d + g) / 2, y0)
                self.dot(xc, y0 + d * 0.62 + d + g * 0.6)

    def ring(self, cx, cy, ri_x, ri_y, wx=None, wy=None):
        wx = wx or self.Wa
        wy = wy or self.Wa * 0.94
        self.c += ring(cx, cy, ri_x + wx, ri_y + wy, ri_x, ri_y)

    def taper_tip(self, path, w_end_frac=0.3, from_frac=0.7):
        self.c += stroke(path, self.Wa, w_end=self.Wa * w_end_frac, taper_from=from_frac)

    def glyph(self, name, adv, ax=None, kind="base", uni=None):
        g = Glyph(name, uni, round(adv), self.c, kind=kind)
        if ax is not None:
            g.anchors["ax"] = ax
        return g

    # ---------------- shared bodies
    def boat(self, x_right, xl, right_start=None):
        """Long flat bowl (beh): from right part along the baseline to a
        rising horizon tip on the left. right_start: (x, y) of a right upturn."""
        cy, r, Wa = self.cy, self.r, self.Wa
        R = 105 + self.e * 0.4
        if right_start:
            xr, yr = right_start
            p = Path((xr, yr - 12)).L((xr, cy + r)).C((xr, cy + r * 0.45), (xr - r * 0.45, cy), (xr - r, cy))
            p.L((xl + R, cy))
            self.wedge_stem(xr, yr - 70, yr + self.k)
        else:
            p = Path((x_right, cy)).L((xl + R, cy))
        p.C((xl + R * 0.45, cy), (xl, cy + R * 0.3), (xl, cy + R * 0.95))
        p.C((xl, cy + R * 1.5), (xl + 4, cy + R * 1.9), (xl + 14, cy + R * 2.25))
        self.taper_tip(p, 0.32, 0.8)

    def deep_bowl(self, xr, y_start, xl, depth=-200, tip_y=230, from_conn=None):
        """Noon-like deep bowl: from (xr, y_start) down below baseline and up
        to a tapered tip on the left. from_conn: x where an incoming baseline
        connector starts (final forms)."""
        Wa = self.Wa
        yb = depth + Wa / 2
        xm = (xr + xl) / 2
        if from_conn is not None:
            cy = self.cy
            p = Path((from_conn, cy)).L((xr + 60, cy)).C((xr + 10, cy), (xr - 6, cy - 30), (xr - 8, cy - 80))
            p.C((xr - 14, depth * 0.62), (xm + (xr - xm) * 0.55, yb), (xm, yb))
        else:
            p = Path((xr, y_start)).L((xr, 60)).C((xr, depth * 0.55), (xm + (xr - xm) * 0.55, yb), (xm, yb))
        p.C((xl + (xm - xl) * 0.45, yb), (xl, depth * 0.55), (xl, 40))
        p.C((xl, 110), (xl + 2, tip_y - 60), (xl + 8, tip_y))
        self.taper_tip(p, 0.3, 0.82)

    def hah_bowl(self, x0, y0, xl, xr_tip, depth=-230):
        """Jeem/hah/ain bowl: from (x0, y0) sweeping down-left and round to a
        tapered tip at the bottom right."""
        Wa = self.Wa
        yb = depth + Wa / 2
        p = Path((x0, y0)).C((x0 - 70, y0 - 40), (xl, y0 - 130), (xl, y0 - 260))
        p.C((xl, depth + 70), (xl + 90, yb), (xl + 200, yb))
        p.C((xr_tip - 110, yb), (xr_tip - 40, yb + 20), (xr_tip, yb + 70))
        self.taper_tip(p, 0.3, 0.84)

    def yeh_tail(self, x_from, y_from, x_down, xr_tip, depth=-205, head=None):
        """Returned yeh tail: from (x_from, y_from) leftwards and down, then
        sweeping back right under the letter to a tapered tip."""
        Wa, cy = self.Wa, self.cy
        yb = depth + Wa / 2
        if head:
            hx, hy = head
            p = Path((hx, hy - 10)).C((hx - 70, hy - 10), (x_down + 10, hy - 50), (x_down, hy - 120))
            p.C((x_down - 6, hy - 200), (x_down - 40, 20), (x_down - 30, -70))
            self.wedge_stem(hx, hy - 60, hy + self.k * 0.8)
        else:
            p = Path((x_from, cy)).L((x_down + 70, cy)).C((x_down + 20, cy), (x_down - 30, cy - 30), (x_down - 30, -70))
        p.C((x_down - 25, depth + 60), (x_down + 40, yb), (x_down + 150, yb))
        p.C((xr_tip - 140, yb), (xr_tip - 50, yb + 15), (xr_tip, yb + 75))
        self.taper_tip(p, 0.3, 0.83)

    def meem_loop(self, cx, cyl=None):
        ri = 40
        Wa = self.Wa
        cyl = cyl if cyl is not None else ri * 0.95 + Wa * 0.94
        self.ring(cx, cyl, ri, ri * 0.95)
        return ri + Wa, cyl

    def hamza(self, x, y, s=1.0, w=None):
        """Hamza: small c-shaped head with a slanted base stroke."""
        w = w or self.Wa * 0.62
        sc = s
        c = Path((x + 52 * sc, y + 150 * sc)).C((x + 20 * sc, y + 175 * sc), (x - 50 * sc, y + 165 * sc), (x - 50 * sc, y + 115 * sc))
        c.C((x - 50 * sc, y + 70 * sc), (x - 5 * sc, y + 58 * sc), (x + 40 * sc, y + 62 * sc))
        self.c += stroke(c, w)
        self.c += diag((x + 60 * sc, y + 74 * sc), (x - 75 * sc, y + 2 * sc), w)

    def madda(self, x, y, w=None):
        w = w or self.Wa * 0.6
        p = Path((x - 120, y + 20)).C((x - 85, y + 95), (x - 35, y + 95), (x, y + 50)).C((x + 35, y + 5), (x + 85, y + 5), (x + 120, y + 80))
        self.c += stroke(p, w)


# ====================================================================== letters
# Each builder returns {form: Glyph}. Forms: isol, init, medi, fina.

def beh_family(P, name, dots_n, below, uni):
    out = {}
    # init
    a = A(P)
    adv = SB + a.Wa + 140
    xt = adv - SB - a.Wa / 2
    a.tooth(xt, T)
    if dots_n:
        if below:
            a.dots(dots_n, xt - 10, -a.d - 75 - (a.d * 1.6 if dots_n == 3 else 0), below=True)
        else:
            a.dots(dots_n, xt - 10, T + a.k + 60)
    out["init"] = a.glyph(name + ".init", adv, xt)
    # medi
    a = A(P)
    adv = a.Wa + 150
    xt = adv / 2 + 10
    a.tooth(xt, T, joined_right=True)
    a.conn(xt, adv)
    if dots_n:
        if below:
            a.dots(dots_n, xt - 5, -a.d - 75 - (a.d * 1.6 if dots_n == 3 else 0), below=True)
        else:
            a.dots(dots_n, xt - 5, T + a.k + 60)
    out["medi"] = a.glyph(name + ".medi", adv, xt)
    # isol / fina: boat
    for form in ("isol", "fina"):
        a = A(P)
        xl = SB + 10
        if form == "isol":
            adv = 560 + a.e
            xr = adv - SB - a.Wa / 2
            a.boat(None, xl, right_start=(xr, 250))
        else:
            adv = 520 + a.e
            a.boat(adv, xl)
        xm = (xl + adv) / 2 - 10
        if dots_n:
            if below:
                a.dots(dots_n, xm, -a.d - 85 - (a.d * 1.6 if dots_n == 3 else 0), below=True)
            else:
                a.dots(dots_n, xm, a.Wa + 70)
        out[form] = a.glyph(name + ("" if form == "isol" else ".fina"), adv, xm, uni=uni if form == "isol" else None)
    return out


def noon(P):
    out = beh_family(P, "uni0646", 1, False, 0x646)
    for form in ("isol", "fina"):
        a = A(P)
        xl = SB + 8
        if form == "isol":
            adv = 400 + a.e
            xr = adv - SB - a.Wa / 2
            a.deep_bowl(xr, 300, xl)
            a.wedge_stem(xr, 230, 300 + a.k)
            xm = (xr + xl) / 2
            a.dot(xm, 150)
        else:
            adv = 430 + a.e
            xr = adv - 140
            a.deep_bowl(xr, None, xl, from_conn=adv)
            xm = (xr + xl) / 2
            a.dot(xm, 150)
        out[form] = a.glyph("uni0646" + ("" if form == "isol" else ".fina"), adv, xm, uni=0x646 if form == "isol" else None)
    return out


def yeh_family(P, name, uni, dots_below=True, hamza_above=False):
    out = {}
    if dots_below or hamza_above:
        base = beh_family(P, name, 2 if dots_below else 0, True, uni)
        out["init"], out["medi"] = base["init"], base["medi"]
        if hamza_above:
            for f in ("init", "medi"):
                a = A(P)
                ax = out[f].anchors["ax"]
                a.hamza(ax - 5, T + a.k + 50)
                out[f].contours += a.c
    else:
        base = beh_family(P, name, 0, True, uni)
        out["init"], out["medi"] = base["init"], base["medi"]
    for form in ("isol", "fina"):
        a = A(P)
        Wa, cy = a.Wa, a.cy
        yb = -205 + Wa / 2
        if form == "isol":
            adv = 560 + a.e
            xr = adv - SB - Wa / 2
            hx = xr - 150
            p = Path((hx, 300)).C((hx - 70, 300), (hx - 130, 262), (hx - 130, 205))
            p.C((hx - 130, 140), (hx - 40, 118), (hx + 50, 100)).C((xr - 20, 86), (xr, 40), (xr, -30))
            p.C((xr, -140), (xr - 110, yb), (xr - 230, yb)).C((xr - 340, yb), (SB + 20, -160), (SB, -90))
            p.C((SB - 4, -60), (SB - 2, -30), (SB + 6, 0))
            a.taper_tip(p, 0.3, 0.88)
            a.wedge_stem(hx, 240, 300 + a.k * 0.9)
            ax = xr - 220
        else:
            adv = 470 + a.e
            xr = adv - 150
            p = Path((adv, cy)).L((xr + 60, cy)).C((xr + 10, cy), (xr - 6, cy - 30), (xr - 8, cy - 80))
            xm = (xr + SB) / 2
            p.C((xr - 14, -150), (xm + (xr - xm) * 0.55, yb), (xm, yb))
            p.C((SB + (xm - SB) * 0.45, yb), (SB, -140), (SB, -40))
            p.C((SB, 10), (SB + 2, 50), (SB + 8, 100))
            a.taper_tip(p, 0.3, 0.85)
            ax = xm
        if dots_below:
            a.dots(2, ax, -205 - 60 - a.d * 1.62)
        if hamza_above:
            a.hamza(ax + 30, 390 if form == "isol" else 150, 0.9)
        out[form] = a.glyph(name + ("" if form == "isol" else ".fina"), adv, ax, uni=uni if form == "isol" else None)
    return out


def hah_family(P, name, uni, dot=None):
    """dot: None, 'below' (jeem), 'above' (khah), 'three' (tcheh)."""
    out = {}
    Tp = 400
    for form in ("init", "medi"):
        a = A(P)
        adv = 420 + a.e * 1.2 + (90 if form == "medi" else 0)
        bar_l, bar_r = SB + 20, 400 + a.e
        # top bar with mirrored bearing cut on its right end
        h = a.Wa
        a.c += poly((bar_l, Tp - h / 2), (bar_r - a.k * 0.6, Tp - h / 2), (bar_r, Tp + h / 2), (bar_l, Tp + h / 2))
        xb = 330 + a.e
        a.c += diag((bar_l + 40, Tp + h / 2), (xb, 0), a.Wa)
        a.conn(0, xb)
        if form == "medi":
            a.c += diag((adv, 0), (bar_r - 50, Tp + h / 2), a.Wa)
        if dot == "below":
            a.dot(xb - 140, -a.d - 80)
        elif dot == "three":
            a.dots(3, xb - 140, -a.d * 2.9 - 80, below=True)
        elif dot == "above":
            a.dot((bar_l + bar_r) / 2, Tp + h / 2 + 70)
        out[form] = a.glyph(name + "." + form, adv, (bar_l + bar_r) / 2)
    for form in ("isol", "fina"):
        a = A(P)
        h = a.Wa
        bar_l, bar_r = SB + 90, 400 + a.e
        adv = 470 + a.e + (80 if form == "fina" else 0)
        a.c += poly((bar_l, Tp - h / 2), (bar_r - a.k * 0.6, Tp - h / 2), (bar_r, Tp + h / 2), (bar_l, Tp + h / 2))
        a.hah_bowl(bar_l + 40, Tp, SB, adv - 30)
        if form == "fina":
            a.c += diag((adv, 0), (bar_r - 50, Tp + h / 2), a.Wa)
        if dot == "below":
            a.dot(240 + a.e / 2, 60)
        elif dot == "three":
            a.dots(3, 240 + a.e / 2, -80, below=True)
        elif dot == "above":
            a.dot((bar_l + bar_r) / 2, Tp + h / 2 + 70)
        out[form] = a.glyph(name + ("" if form == "isol" else ".fina"), adv, (bar_l + bar_r) / 2, uni=uni if form == "isol" else None)
    return out


def dal_family(P, name, uni, dot=False):
    out = {}
    for form in ("isol", "fina"):
        a = A(P)
        adv = 330 + a.e + (60 if form == "fina" else 0)
        cy = a.cy
        xc = 270 + a.e
        p = Path((120, 420)).C((175, 370), (xc, 260), (xc, 150)).C((xc, 70), (xc - 30, cy), (xc - 90, cy)).L((SB, cy))
        a.st(p)
        if form == "fina":
            a.conn(xc - 60, adv)
        if dot:
            a.dot(140, 500)
        out[form] = a.glyph(name + ("" if form == "isol" else ".fina"), adv, 180, uni=uni if form == "isol" else None)
    return out


def reh_family(P, name, uni, ndots=0):
    out = {}
    for form in ("isol", "fina"):
        a = A(P)
        cy = a.cy
        if form == "isol":
            adv = 290 + a.e
            xr = adv - SB - a.Wa / 2
            p = Path((xr, 240)).L((xr, 110)).C((xr, 10), (xr - 30, -70), (xr - 80, -120)).C((xr - 120, -160), (xr - 170, -190), (xr - 225, -200))
            a.wedge_stem(xr, 170, 240 + a.k)
            ax = xr
        else:
            adv = 300 + a.e
            xr = adv - 110
            p = Path((adv, cy)).L((xr + 50, cy)).C((xr + 5, cy), (xr - 10, -30), (xr - 40, -90)).C((xr - 75, -150), (xr - 130, -190), (xr - 195, -205))
            ax = xr + 20
        a.taper_tip(p, 0.3, 0.6)
        if ndots:
            a.dots(ndots, ax - 5, 330 if form == "isol" else 150)
        out[form] = a.glyph(name + ("" if form == "isol" else ".fina"), adv, ax, uni=uni if form == "isol" else None)
    return out


def seen_family(P, name, uni, dots3=False):
    out = {}
    for form in ("init", "medi", "isol", "fina"):
        a = A(P)
        sp = a.Wa + 72
        if form in ("init", "medi"):
            x1 = SB + 40
            exit_x = 0
        else:
            x1 = 330 + a.e
            exit_x = None
        xs = [x1, x1 + sp, x1 + 2 * sp]
        tops = [TS, TS, TS + 40]
        for i, xt in enumerate(xs):
            if i == 0 and exit_x is None:
                continue
            nxt = 0 if i == 0 else xs[i - 1]
            jr = i < 2 or form in ("medi", "fina")
            a.tooth(xt, tops[i], x_exit=nxt, joined_right=jr)
        if form in ("isol", "fina"):
            a.deep_bowl(x1, TS - 10, SB, tip_y=220)
            a.wedge_stem(x1, TS - 70, TS + a.k)
        right = xs[2] + a.Wa / 2
        if form in ("init", "isol"):
            adv = right + SB
        else:
            adv = right + 30
            a.conn(xs[2], adv)
        if dots3:
            a.dots(3, xs[1], TS + a.k + 60)
        nm = name if form == "isol" else name + "." + form
        out[form] = a.glyph(nm, adv, xs[1], uni=uni if form == "isol" else None)
    return out


def sad_family(P, name, uni, dot=False):
    out = {}
    for form in ("init", "medi", "isol", "fina"):
        a = A(P)
        Wa = a.Wa
        ri_x, ri_y = 92 + a.e * 0.2, 46
        rox, roy = ri_x + Wa, ri_y + Wa * 0.94
        lw = 2 * rox
        if form in ("init", "medi"):
            xt = SB + 40
        else:
            xt = 330 + a.e
        cx = xt + Wa / 2 + 10 + rox
        cyl = roy
        a.ring(cx, cyl, ri_x, ri_y)
        if form in ("init", "medi"):
            a.tooth(xt, 260, x_exit=0, joined_right=True)
        else:
            a.deep_bowl(xt, 250, SB, tip_y=220)
            a.wedge_stem(xt, 190, 250 + a.k)
        a.conn(xt, cx)
        right = cx + rox
        if form in ("init", "isol"):
            adv = right + SB
        else:
            adv = right + 20
            a.conn(cx, adv)
        if dot:
            a.dot(cx, 2 * roy + 60)
        nm = name if form == "isol" else name + "." + form
        out[form] = a.glyph(nm, adv, cx, uni=uni if form == "isol" else None)
    return out


def tah_family(P, name, uni, dot=False):
    out = {}
    for form in ("init", "medi", "isol", "fina"):
        a = A(P)
        Wa = a.Wa
        ri_x, ri_y = 100 + a.e * 0.2, 48
        rox, roy = ri_x + Wa, ri_y + Wa * 0.94
        x0 = SB + (0 if form in ("isol", "fina") else 10)
        cx = x0 + rox
        a.ring(cx, roy, ri_x, ri_y)
        xs = cx - ri_x * 0.55
        a.wedge_stem(xs, roy, CAPA)
        if form in ("init", "medi"):
            a.conn(0, cx)
        right = cx + rox
        if form in ("init", "isol"):
            adv = right + SB
        else:
            adv = right + 20
            a.conn(cx, adv)
        if dot:
            a.dot(cx + ri_x * 0.55, 2 * roy + 70)
        nm = name if form == "isol" else name + "." + form
        out[form] = a.glyph(nm, adv, cx + 30, uni=uni if form == "isol" else None)
    return out


def ain_family(P, name, uni, dot=False):
    out = {}
    # init: c on the baseline
    a = A(P)
    Wa = a.Wa
    rx, ry = 115 + a.e * 0.3, 140
    adv = SB + 2 * rx + 30
    cx = adv - SB - rx + 20
    cyc = ry + 4
    a.c += _arc_sector(cx, cyc, rx, ry, Wa, Wa * 0.9, 40, 292)
    a.conn(0, cx)
    if dot:
        a.dot(cx, 2 * ry + 80)
    out["init"] = a.glyph(name + ".init", adv, cx)
    # medi: inverted triangle
    for form in ("medi", "fina"):
        a = A(P)
        Wa = a.Wa
        top = 330
        xlt, xrt = SB + 10, 260 + a.e
        xlb, xrb = 115 + a.e * 0.4, 185 + a.e * 0.6
        if form == "fina":
            xlt += 60
            xrt += 60
            xlb += 60
            xrb += 60
        adv = xrt + 60
        a.c += diag((xrb, 0), (xrt, top + Wa / 2), Wa)
        a.c += diag((xlb, 0), (xlt, top + Wa / 2), Wa)
        a.c += poly((xlt - Wa * 0.4, top - Wa / 2), (xrt + Wa * 0.4, top - Wa / 2), (xrt + Wa * 0.55, top + Wa / 2), (xlt - Wa * 0.55, top + Wa / 2))
        a.conn(xrb, adv)
        if form == "medi":
            a.conn(0, xlb)
        else:
            a.hah_bowl(xlb + 10, 30, SB - 20, adv - 10, depth=-250)
        if dot:
            a.dot((xlt + xrt) / 2, top + Wa / 2 + 70)
        out[form] = a.glyph(name + "." + form, adv, (xlt + xrt) / 2)
    # isol: c head + bowl
    a = A(P)
    Wa = a.Wa
    rx, ry = 110 + a.e * 0.3, 100
    adv = 470 + a.e
    cx = adv - SB - rx - 10
    cyc = 330
    a.c += _arc_sector(cx, cyc, rx, ry, Wa, Wa * 0.9, 40, 268)
    a.hah_bowl(cx - 10, cyc - ry + Wa * 0.45, SB, adv - 40)
    if dot:
        a.dot(cx, cyc + ry + 70)
    out["isol"] = a.glyph(name, adv, cx, uni=uni)
    return out


def _arc_sector(cx, cy, rxo, ryo, wx, wy, a0, a1):
    rxi, ryi = rxo - wx, ryo - wy
    n = max(8, int((a1 - a0) / 5))
    outer, inner = [], []
    for i in range(n + 1):
        t = math.radians(a0 + (a1 - a0) * i / n)
        on = i in (0, n)
        outer.append((cx + rxo * math.cos(t), cy + ryo * math.sin(t), on))
        inner.append((cx + rxi * math.cos(t), cy + ryi * math.sin(t), on))
    return [Contour(outer + inner[::-1])]


def feh_family(P, name, uni, qaf=False, dots_n=1):
    out = {}
    for form in ("init", "medi", "isol", "fina"):
        a = A(P)
        Wa = a.Wa
        ri = 46
        ro = ri + Wa
        cyl = 300
        if form in ("init", "medi"):
            adv = SB + 2 * ro + (10 if form == "init" else -10)
            cx = adv - (SB if form == "init" else 30) - ro
            neck = cx - ri - Wa / 2
            a.ring(cx, cyl, ri, ri)
            a.tooth(neck, cyl, x_exit=0, wedge=False, joined_right=True)
            if form == "medi":
                a.conn(neck, adv)
            ax = cx
        else:
            if qaf:
                adv = 470 + a.e + (40 if form == "fina" else 0)
                cx = adv - (SB if form == "isol" else 60) - ro
                neck = cx - ri - Wa / 2
                a.ring(cx, cyl, ri, ri)
                a.deep_bowl(neck, cyl, SB, tip_y=230)
            else:
                adv = 600 + a.e + (40 if form == "fina" else 0)
                cx = adv - (SB if form == "isol" else 60) - ro
                neck = cx - ri - Wa / 2
                a.ring(cx, cyl, ri, ri)
                a.tooth(neck, cyl, x_exit=SB + 120, wedge=False)
                a.boat(neck - a.r, SB + 10)
            if form == "fina":
                a.conn(neck, adv)
            ax = cx
        a.dots(2 if qaf else dots_n, cx, cyl + ro + 50)
        nm = name if form == "isol" else name + "." + form
        out[form] = a.glyph(nm, adv, ax, uni=uni if form == "isol" else None)
    return out


def kaf_family(P, name, uni, keheh=False, gaf=False):
    out = {}
    for form in ("init", "medi"):
        a = A(P)
        Wa = a.Wa
        adv = 400 + a.e + (40 if form == "medi" else 0)
        top = 620
        xr_top = adv - SB - 20 - (40 if form == "medi" else 0)
        xv = SB + 40
        a.c += diag((xv, 0), (xr_top - 60, top), Wa)
        a.conn(0, (adv if form == "medi" else xr_top + 20))
        if gaf:
            a.c += diag((xv + 150, 150 + 200), (xr_top + 40, top + 150), Wa * 0.8)
        nm = name + "." + form
        out[form] = a.glyph(nm, adv, (xv + xr_top) / 2)
    for form in ("isol", "fina"):
        a = A(P)
        Wa = a.Wa
        if keheh:
            adv = 560 + a.e + (20 if form == "fina" else 0)
            top = 620
            xv = SB + 40
            xr_top = adv - 200
            a.c += diag((xv, 0), (xr_top, top), Wa)
            if form == "isol":
                xr = adv - SB - Wa / 2
                p = Path((SB, a.cy)).L((xr - a.r, a.cy)).C((xr - a.r * 0.45, a.cy), (xr, a.cy + a.r * 0.45), (xr, a.cy + a.r)).L((xr, 200))
                a.st(p)
                a.wedge_stem(xr, 140, 200 + a.k)
            else:
                a.conn(SB, adv)
            if gaf:
                a.c += diag((xv + 150, 350), (xr_top + 40, top + 150), Wa * 0.8)
            ax = (xv + xr_top) / 2
        else:
            adv = 470 + a.e + (20 if form == "fina" else 0)
            xs = SB + Wa / 2
            a.wedge_stem(xs, 0, CAPA - 40)
            if form == "isol":
                xr = adv - SB - Wa / 2
                p = Path((xs, a.cy)).L((xr - a.r, a.cy)).C((xr - a.r * 0.45, a.cy), (xr, a.cy + a.r * 0.45), (xr, a.cy + a.r)).L((xr, 210))
                a.st(p)
                a.wedge_stem(xr, 150, 210 + a.k)
            else:
                a.conn(xs, adv)
            a.hamza(xs + 170 + a.e * 0.5, a.Wa + 70, s=0.85)
            ax = xs + 160
        nm = name if form == "isol" else name + ".fina"
        out[form] = a.glyph(nm, adv, ax, uni=uni if form == "isol" else None)
    return out


def lam_family(P):
    out = {}
    for form in ("init", "medi"):
        a = A(P)
        adv = SB + a.Wa + 100 + (0 if form == "init" else -10)
        xt = adv - (SB if form == "init" else 60) - a.Wa / 2
        a.tooth(xt, CAPA, joined_right=(form == "medi"))
        if form == "medi":
            a.conn(xt, adv)
        out[form] = a.glyph("uni0644." + form, adv, xt)
    for form in ("isol", "fina"):
        a = A(P)
        adv = 420 + a.e + (40 if form == "fina" else 0)
        xs = adv - (SB if form == "isol" else 80) - a.Wa / 2
        a.deep_bowl(xs, CAPA - 20, SB, depth=-215, tip_y=240)
        a.wedge_stem(xs, CAPA - 90, CAPA)
        if form == "fina":
            a.conn(xs, adv)
        out[form] = a.glyph("uni0644" + ("" if form == "isol" else ".fina"), adv, xs, uni=0x644 if form == "isol" else None)
    return out


def meem_family(P):
    out = {}
    for form in ("init", "medi", "isol", "fina"):
        a = A(P)
        Wa = a.Wa
        ri = 40
        ro = ri + Wa
        cyl = ri * 0.95 + Wa * 0.94
        if form == "init":
            adv = SB + 2 * ro + 30
            cx = adv - SB - ro
            a.ring(cx, cyl, ri, ri * 0.95)
            a.conn(0, cx)
        elif form == "medi":
            adv = 2 * ro + 50
            cx = adv / 2
            a.ring(cx, cyl + 10, ri, ri * 0.95)
            a.conn(0, adv)
        else:
            adv = SB + 2 * ro + (30 if form == "isol" else 70)
            cx = SB + ro + 10
            a.ring(cx, cyl, ri, ri * 0.95)
            xt = cx - ro + Wa / 2
            p = Path((xt, cyl)).L((xt, -40)).C((xt, -120), (xt - 4, -190), (xt + 8, -260))
            a.taper_tip(p, 0.32, 0.45)
            if form == "fina":
                a.conn(cx, adv)
        nm = "uni0645" if form == "isol" else "uni0645." + form
        out[form] = a.glyph(nm, adv, cx, uni=0x645 if form == "isol" else None)
    return out


def heh_family(P, name="uni0647", uni=0x647, marbuta=False):
    out = {}
    forms = ("isol", "fina") if marbuta else ("init", "medi", "isol", "fina")
    for form in forms:
        a = A(P)
        Wa = a.Wa
        if form == "isol":
            ri_x, ri_y = 70 + a.e * 0.2, 95
            ro = ri_x + Wa
            adv = SB + 2 * ro + SB
            cx = adv / 2
            a.ring(cx, ri_y + Wa * 0.94, ri_x, ri_y)
            top = 2 * (ri_y + Wa * 0.94)
        elif form == "init":
            ri_x, ri_y = 52, 175
            ro = ri_x + Wa
            adv = SB + 2 * ro + 30
            cx = adv - SB - ro
            a.ring(cx, ri_y + Wa * 0.94, ri_x, ri_y)
            a.conn(0, cx)
            top = 2 * (ri_y + Wa * 0.94)
        elif form == "medi":
            ri_x, ri_y = 55, 120
            ro = ri_x + Wa
            adv = 2 * ro + 60
            cx = adv / 2
            a.ring(cx, a.cy + 30, ri_x, ri_y)
            a.conn(0, adv)
            top = a.cy + 30 + ri_y + Wa
        else:
            ri_x, ri_y = 62 + a.e * 0.2, 80
            ro = ri_x + Wa
            adv = SB + 2 * ro + 80
            cx = SB + ro
            a.ring(cx, ri_y + Wa * 0.94, ri_x, ri_y)
            a.conn(cx, adv)
            top = 2 * (ri_y + Wa * 0.94)
        if marbuta:
            a.dots(2, cx, top + 60)
        nm = name if form == "isol" else name + "." + form
        out[form] = a.glyph(nm, adv, cx, uni=uni if form == "isol" else None)
    return out


def waw_family(P, name="uni0648", uni=0x648, hamza=False):
    out = {}
    for form in ("isol", "fina"):
        a = A(P)
        Wa = a.Wa
        ri = 44
        ro = ri + Wa
        cyl = 250
        adv = SB + 2 * ro + 120 + (40 if form == "fina" else 0)
        cx = adv - (SB if form == "isol" else 70) - ro
        a.ring(cx, cyl, ri, ri)
        xt = cx + ri + Wa / 2
        p = Path((xt, cyl)).L((xt, 110)).C((xt, -20), (xt - 60, -120), (xt - 150, -170)).C((xt - 200, -195), (xt - 250, -205), (SB - 10, -210))
        a.taper_tip(p, 0.3, 0.55)
        if form == "fina":
            a.conn(xt - 20, adv)
        if hamza:
            a.hamza(cx, cyl + ro + 40, 0.9)
        nm = name if form == "isol" else name + ".fina"
        out[form] = a.glyph(nm, adv, cx, uni=uni if form == "isol" else None)
    return out


def alef_family(P, name="uni0627", uni=0x627, mark=None):
    out = {}
    for form in ("isol", "fina"):
        a = A(P)
        Wa = a.Wa
        if form == "isol":
            adv = SB + Wa + SB + 20
            xs = adv / 2
        else:
            adv = SB + Wa + 110 + a.e * 0.5
            xs = SB + 10 + Wa / 2
            a.conn(xs, adv)
        a.wedge_stem(xs, 0, CAPA)
        if mark == "hamza_above":
            a.hamza(xs + 10, CAPA + 50)
        elif mark == "hamza_below":
            a.hamza(xs + 10, -230)
        elif mark == "madda":
            a.madda(xs, CAPA + 50)
        nm = name if form == "isol" else name + ".fina"
        out[form] = a.glyph(nm, adv, xs, uni=uni if form == "isol" else None)
    return out


def lam_alef(P, suffix, mark=None):
    out = {}
    for form in ("isol", "fina"):
        a = A(P)
        Wa = a.Wa
        adv = 470 + a.e + (50 if form == "fina" else 0)
        xs = adv - (SB if form == "isol" else 90) - Wa / 2
        xab, xat = 205 + a.e * 0.5, SB + 40
        cy = a.cy
        p = Path((xs, CAPA - 20)).L((xs, 150)).C((xs, 60), (xs - 40, cy), (xs - 100, cy)).L((xab, cy))
        a.st(p)
        a.wedge_stem(xs, CAPA - 90, CAPA)
        a.c += diag((xab, 0), (xat, CAPA), Wa)
        if form == "fina":
            a.conn(xs, adv)
        if mark == "hamza_above":
            a.hamza(xat - 10, CAPA + 50)
        elif mark == "hamza_below":
            a.hamza(xab - 30, -230)
        elif mark == "madda":
            a.madda(xat, CAPA + 50)
        nm = "uni0644_" + suffix + (".fina" if form == "fina" else "")
        out[form] = a.glyph(nm, adv, xs, kind="lig")
    return out


def hamza_glyph(P):
    a = A(P)
    a.hamza(170, 30, s=1.45, w=a.Wa * 0.9)
    return a.glyph("uni0621", 340, 170, uni=0x621)


def tatweel(P):
    a = A(P)
    a.conn(0, 220)
    return a.glyph("uni0640", 220, 110, uni=0x640)


# ====================================================================== marks

def mark_glyphs(P):
    gl = []
    Wa = round(P["W"] * 0.97 / ARS)
    w = Wa * 0.72

    def mk(name, uni, cs, where, height):
        g = Glyph(name, uni, 0, cs, kind="mark")
        g.anchors["where"] = where
        g.anchors["height"] = height
        gl.append(g)

    fat = lambda dy=0: diag((-62, 0 + dy), (62, 72 + dy), w)
    mk("uni064E", 0x64E, fat(), "top", 72)                      # fatha
    mk("uni064B", 0x64B, fat() + fat(95), "top", 167)           # fathatan
    mk("uni0650", 0x650, diag((-62, -72), (62, 0), w), "bottom", 72)   # kasra
    mk("uni064D", 0x64D, diag((-62, -72), (62, 0), w) + diag((-62, -167), (62, -95), w), "bottom", 167)

    def damma(x0=0, s=1.0):
        cs = ring(x0 + 10 * s, 110 * s, 52 * s, 50 * s, 52 * s - w * 0.9, 50 * s - w * 0.9)
        p = Path((x0 + 62 * s - w * 0.45, 110 * s)).L((x0 + 62 * s - w * 0.45, 60 * s)).C((x0 + 62 * s - w * 0.45, 20 * s), (x0 + 10 * s, 0), (x0 - 60 * s, -4 * s))
        cs += stroke(p, w * 0.9, w_end=w * 0.35, taper_from=0.4)
        return cs
    mk("uni064F", 0x64F, shift(damma(), 0, 10), "top", 170)     # damma
    mk("uni064C", 0x64C, shift(damma(-60, 0.85) + damma(70, 0.85), 0, 10), "top", 160)  # dammatan

    sk = ring(0, 70, 62, 62, 62 - w * 0.85, 62 - w * 0.85)
    mk("uni0652", 0x652, sk, "top", 132)                        # sukun

    p1 = Path((-100, 110)).L((-100, 50)).C((-100, 15), (-78, 4), (-50, 4)).C((-22, 4), (0, 15), (0, 50)).L((0, 110))
    p2 = Path((0, 50)).C((0, 15), (22, 4), (50, 4)).C((78, 4), (100, 15), (100, 50)).L((100, 110))
    mk("uni0651", 0x651, stroke(p1, w * 0.85) + stroke(p2, w * 0.85), "top", 110)  # shadda

    a = A(P)
    a.hamza(0, 0, s=0.95)
    mk("uni0654", 0x654, a.c, "top", 165)
    a = A(P)
    a.hamza(0, -170, s=0.95)
    mk("uni0655", 0x655, a.c, "bottom", 170)
    a = A(P)
    a.madda(0, 0)
    mk("uni0653", 0x653, shift(a.c, 0, -10), "top", 95)
    mk("uni0670", 0x670, [Contour([(-w / 2, 0, True), (w / 2, 0, True), (w / 2, 150, True), (-w / 2, 175, True)])], "top", 175)
    return gl


# ====================================================================== digits, punctuation

def arabic_digits(P):
    gl = []
    FW = 590 / ARS
    H = 680 / ARS

    def g(name, uni, fn):
        a = A(P)
        fn(a)
        gl.append(a.glyph(name, FW, FW / 2, uni=uni))
    cx = FW / 2

    g("uni0660", 0x660, lambda a: a.dot(cx, 230))
    g("uni0661", 0x661, lambda a: a.wedge_stem(cx, 0, H))

    def two(a):
        a.wedge_stem(cx - 60, 0, 470)
        p = Path((cx - 60, 430)).C((cx - 60, 540), (cx - 10, H - a.Wa / 2), (cx + 70, H - a.Wa / 2)).L((cx + 160, H - a.Wa / 2))
        a.st(p)
    g("uni0662", 0x662, two)

    def three(a):
        a.stem(cx - 110, 0, 470)
        y = H - a.Wa / 2
        p = Path((cx - 110, 450)).C((cx - 110, 560), (cx - 60, y), (cx - 10, y)).C((cx + 30, y), (cx + 40, y - 30), (cx + 40, y - 70))
        a.st(p)
        p = Path((cx + 40, y - 40)).C((cx + 40, y - 10), (cx + 70, y), (cx + 110, y)).L((cx + 170, y))
        a.st(p)
    g("uni0663", 0x663, three)

    def four(a):
        p = Path((cx + 150, H - 30)).C((cx + 60, H), (cx - 110, H - 40), (cx - 110, 470)).C((cx - 110, 400), (cx - 40, 360), (cx + 40, 360))
        a.st(p)
        p = Path((cx + 40, 360)).C((cx - 80, 360), (cx - 160, 290), (cx - 160, 190)).C((cx - 160, 70), (cx - 60, 10), (cx + 60, 10)).L((cx + 160, 10))
        a.st(p)
    g("uni0664", 0x664, four)

    g("uni0665", 0x665, lambda a: a.ring(cx, 220, 70, 120))

    def six(a):
        a.wedge_stem(cx + 90, 0, H)
        a.c += poly((cx - 150, H - a.Wa), (cx + 90, H - a.Wa), (cx + 90, H), (cx - 150, H - a.k * 0.4))
    g("uni0666", 0x666, six)

    def seven(a):
        a.c += diag((cx - 170, H), (cx - 20, 0), a.Wa)
        a.c += diag((cx + 170, H), (cx + 20, 0), a.Wa)
        a.c += poly((cx - 20 - a.Wa * 0.55, 1), (cx + 20 + a.Wa * 0.55, 1), (cx + 20, 60), (cx - 20, 60))
    g("uni0667", 0x667, seven)

    def eight(a):
        a.c += diag((cx - 170, 0), (cx - 20, H), a.Wa)
        a.c += diag((cx + 170, 0), (cx + 20, H), a.Wa)
        a.c += poly((cx - 20 - a.Wa * 0.55, H - 1), (cx + 20 + a.Wa * 0.55, H - 1), (cx + 20, H - 60), (cx - 20, H - 60))
    g("uni0668", 0x668, eight)

    def nine(a):
        a.ring(cx - 40, 470, 60, 60)
        a.stem(cx + 60 + a.Wa / 2 - a.Wa / 2, 0, 480)
    g("uni0669", 0x669, nine)
    return gl


def arabic_punct(P, latin_by):
    """Arabic comma and semicolon use the comma turned 180 degrees;
    the Arabic question mark is the Latin one mirrored."""
    gl = []
    c = latin_by["comma"]
    bx = bbox(c.contours)
    cx, cy = (bx[0] + bx[2]) / 2, 140
    turn = lambda cs: [Contour([(2 * cx - x, 2 * cy - y, on) for x, y, on in k.pts]) for k in cs]
    gl.append(Glyph("uni060C", 0x60C, c.adv, turn(c.contours)))
    a = A(P)
    a.dot(cx, X - P["dot"] * 1.3)
    gl.append(Glyph("uni061B", 0x61B, c.adv, turn(c.contours) + a.c))
    q = latin_by["question"]
    gl.append(Glyph("uni061F", 0x61F, q.adv, mirror_x(q.contours, q.adv / 2)))
    return gl


# ====================================================================== assembly

DUAL = {}   # base glyph name -> dict of forms
RIGHT = {}


def build_arabic(P):
    import latin as L
    fam = {}

    def reg(forms, dual=True):
        base = forms["isol"].name
        (DUAL if dual else RIGHT)[base] = forms
        fam[base] = forms

    DUAL.clear(); RIGHT.clear()
    reg(beh_family(P, "uni0628", 1, True, 0x628))
    reg(beh_family(P, "uni062A", 2, False, 0x62A))
    reg(beh_family(P, "uni062B", 3, False, 0x62B))
    reg(beh_family(P, "uni067E", 3, True, 0x67E))
    reg(noon(P))
    reg(yeh_family(P, "uni064A", 0x64A, dots_below=True))
    reg(yeh_family(P, "uni0649", 0x649, dots_below=False))
    reg(yeh_family(P, "uni0626", 0x626, dots_below=False, hamza_above=True))
    # Farsi yeh: dotless in isol/fina, dotted in init/medi
    fy_iso = yeh_family(P, "uni06CC", 0x6CC, dots_below=False)
    fy_dot = yeh_family(P, "uni06CC", 0x6CC, dots_below=True)
    fy_iso["init"], fy_iso["medi"] = fy_dot["init"], fy_dot["medi"]
    reg(fy_iso)
    reg(hah_family(P, "uni062C", 0x62C, "below"))
    reg(hah_family(P, "uni062D", 0x62D, None))
    reg(hah_family(P, "uni062E", 0x62E, "above"))
    reg(hah_family(P, "uni0686", 0x686, "three"))
    reg(dal_family(P, "uni062F", 0x62F), dual=False)
    reg(dal_family(P, "uni0630", 0x630, dot=True), dual=False)
    reg(reh_family(P, "uni0631", 0x631), dual=False)
    reg(reh_family(P, "uni0632", 0x632, 1), dual=False)
    reg(reh_family(P, "uni0698", 0x698, 3), dual=False)
    reg(seen_family(P, "uni0633", 0x633))
    reg(seen_family(P, "uni0634", 0x634, dots3=True))
    reg(sad_family(P, "uni0635", 0x635))
    reg(sad_family(P, "uni0636", 0x636, dot=True))
    reg(tah_family(P, "uni0637", 0x637))
    reg(tah_family(P, "uni0638", 0x638, dot=True))
    reg(ain_family(P, "uni0639", 0x639))
    reg(ain_family(P, "uni063A", 0x63A, dot=True))
    reg(feh_family(P, "uni0641", 0x641))
    reg(feh_family(P, "uni0642", 0x642, qaf=True))
    reg(kaf_family(P, "uni0643", 0x643))
    reg(kaf_family(P, "uni06A9", 0x6A9, keheh=True))
    reg(kaf_family(P, "uni06AF", 0x6AF, keheh=True, gaf=True))
    reg(lam_family(P))
    reg(meem_family(P))
    reg(heh_family(P))
    reg(heh_family(P, "uni0629", 0x629, marbuta=True), dual=False)
    reg(waw_family(P), dual=False)
    reg(waw_family(P, "uni0624", 0x624, hamza=True), dual=False)
    reg(alef_family(P), dual=False)
    reg(alef_family(P, "uni0623", 0x623, "hamza_above"), dual=False)
    reg(alef_family(P, "uni0625", 0x625, "hamza_below"), dual=False)
    reg(alef_family(P, "uni0622", 0x622, "madda"), dual=False)

    glyphs = []
    Wa = round(P["W"] * 0.97 / ARS)
    OVL = 12  # joins overlap the neighbour slightly: no hairline seams

    def add_overlap(g, left, right):
        if left:
            g.contours += poly((-OVL, 0), (8, 0), (8, Wa), (-OVL, Wa))
        if right:
            g.contours += poly((g.adv - 8, 0), (g.adv + OVL, 0), (g.adv + OVL, Wa), (g.adv - 8, Wa))

    for base, forms in fam.items():
        for f in ("isol", "init", "medi", "fina"):
            if f in forms:
                add_overlap(forms[f], f in ("init", "medi"), f in ("medi", "fina"))
                glyphs.append(forms[f])
    ligs = {}
    for alef, suf, mark in (("uni0627", "uni0627", None), ("uni0623", "uni0623", "hamza_above"),
                            ("uni0625", "uni0625", "hamza_below"), ("uni0622", "uni0622", "madda")):
        la = lam_alef(P, suf, mark)
        ligs[alef] = la
        add_overlap(la["fina"], False, True)
        glyphs += [la["isol"], la["fina"]]
    glyphs.append(hamza_glyph(P))
    tw = tatweel(P)
    add_overlap(tw, True, True)
    glyphs.append(tw)
    marks = mark_glyphs(P)
    glyphs += marks
    glyphs += arabic_digits(P)
    for g in glyphs:
        g.contours = [Contour([(x * ARS, y * ARS, on) for x, y, on in c.pts], c.hole) for c in g.contours]
        g.adv = round(g.adv * ARS)
        if "ax" in g.anchors:
            g.anchors["ax"] *= ARS
        if "height" in g.anchors:
            g.anchors["height"] = round(g.anchors["height"] * ARS)
    latin_by = {g.name: g for g in L.build_latin(P)}
    glyphs += arabic_punct(P, latin_by)

    fea = arabic_features(glyphs, fam, ligs, marks)
    return glyphs, fea


def _anchor(g, where, window=115):
    pts = [(x, y) for c in g.contours for x, y, on in c.pts]
    ax = g.anchors.get("ax")
    if ax is None:
        bx = bbox(g.contours)
        ax = (bx[0] + bx[2]) / 2
    near = [y for x, y in pts if abs(x - ax) < window] or [y for x, y in pts]
    if where == "top":
        return round(ax), round(max(near) + 55)
    return round(ax), round(min(min(near), 0) - 50)


def arabic_features(glyphs, fam, ligs, marks):
    init, medi, fina = [], [], []
    for base, forms in fam.items():
        for f, lst in (("init", init), ("medi", medi), ("fina", fina)):
            if f in forms:
                lst.append((base, forms[f].name))
    out = []
    for tag, lst in (("init", init), ("medi", medi), ("fina", fina)):
        body = "\n".join(f"    sub {a} by {b};" for a, b in lst)
        out.append(f"feature {tag} {{\n  script arab;\n  lookup {tag}_arab {{\n{body}\n  }} {tag}_arab;\n}} {tag};\n")
    lig_rules = []
    for alef, la in ligs.items():
        lig_rules.append(f"    sub uni0644.init {alef}.fina by {la['isol'].name};")
        lig_rules.append(f"    sub uni0644.medi {alef}.fina by {la['fina'].name};")
    out.append("feature rlig {\n  script arab;\n  lookup rlig_lamalef {\n    lookupflag IgnoreMarks;\n"
               + "\n".join(lig_rules) + "\n  } rlig_lamalef;\n} rlig;\n")

    # mark positioning
    tops = [m for m in marks if m.anchors["where"] == "top"]
    bots = [m for m in marks if m.anchors["where"] == "bottom"]
    mc = []
    mc.append(f"markClass [{' '.join(m.name for m in tops)}] <anchor 0 0> @MC_top;")
    mc.append(f"markClass [{' '.join(m.name for m in bots)}] <anchor 0 0> @MC_bottom;")
    bases = [g for g in glyphs if g.kind in ("base", "lig") and g.name.startswith("uni06") or
             (g.kind in ("base", "lig") and g.name.startswith("uni064") and g.contours)]
    seen = set()
    base_rules = []
    for g in glyphs:
        if g.kind not in ("base", "lig") or not g.contours or g.name in seen:
            continue
        if not (g.name.startswith("uni06") or g.name.startswith("uni0644_")):
            continue
        if g.name.startswith("uni066") or g.name.startswith("uni060") or g.name.startswith("uni061"):
            continue
        seen.add(g.name)
        tx, ty = _anchor(g, "top")
        bx_, by_ = _anchor(g, "bottom")
        base_rules.append(f"    pos base {g.name} <anchor {tx} {ty}> mark @MC_top <anchor {bx_} {by_}> mark @MC_bottom;")
    out.append("\n".join(mc) + "\n")
    out.append("feature mark {\n  script arab;\n  lookup mark_arab {\n" + "\n".join(base_rules) + "\n  } mark_arab;\n} mark;\n")
    mk = []
    for m in tops:
        mk.append(f"    pos mark {m.name} <anchor 0 {m.anchors['height'] + 45}> mark @MC_top;")
    for m in bots:
        mk.append(f"    pos mark {m.name} <anchor 0 {-m.anchors['height'] - 45}> mark @MC_bottom;")
    out.append("feature mkmk {\n  script arab;\n  lookup mkmk_arab {\n" + "\n".join(mk) + "\n  } mkmk_arab;\n} mkmk;\n")

    # GDEF
    base_names = [g.name for g in glyphs if g.kind == "base" and g.contours]
    lig_names = [g.name for g in glyphs if g.kind == "lig"]
    mark_names = [m.name for m in marks]
    out.append("@GDEF_base = [" + " ".join(base_names) + "];\n@GDEF_lig = [" + " ".join(lig_names) + "];\n@GDEF_mark = [" + " ".join(mark_names) + "];\n")
    out.append("table GDEF {\n  GlyphClassDef @GDEF_base, @GDEF_lig, @GDEF_mark, ;\n} GDEF;\n")
    return "\n".join(out)
