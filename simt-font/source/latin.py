"""Simt Sextant: Latin glyphs."""
import math
from geom import Path, stroke, ring, poly, rect, diag, disc, Contour, shift, mirror_x, bbox

X, CAP, ASC, DESC, OV = 500, 700, 730, -210, 12


class Glyph:
    def __init__(self, name, uni=None, adv=0, contours=None, components=None,
                 kind="base", anchors=None):
        self.name = name
        self.uni = uni if isinstance(uni, (list, tuple)) else ([uni] if uni else [])
        self.adv = adv
        self.contours = contours or []
        self.components = components or []  # (name, dx, dy)
        self.kind = kind
        self.anchors = anchors or {}


class B:
    """Glyph builder in design space; scales horizontal positions by sx."""

    def __init__(self, P):
        self.P = P
        self.sx = P["sx"]
        self.W = P["W"]
        self.H = P["H"]
        self.g = P["g"]
        self.k = P["k"]
        self.c = []

    # --- coordinate helpers
    def x(self, v):
        return v * self.sx

    def pt(self, p):
        return (p[0] * self.sx, p[1])

    def path(self, start):
        sx = self.sx

        class SP(Path):
            def L(s, p):
                return Path.L(s, (p[0] * sx, p[1]))

            def C(s, c1, c2, p):
                return Path.C(s, (c1[0] * sx, c1[1]), (c2[0] * sx, c2[1]), (p[0] * sx, p[1]))
        return SP((start[0] * sx, start[1]))

    # --- primitives
    def stroke(self, path, w=None, **kw):
        self.c += stroke(path, w or self.W, **kw)

    def stem(self, xc, y0, y1, wedge=None, w=None):
        """Vertical stem centred on xc. wedge 'R': top rises to the right
        (Latin reading direction); 'L': rises to the left (Arabic)."""
        w = w or self.W
        xc = self.x(xc)
        a, b = xc - w / 2, xc + w / 2
        k = self.k * (w / self.W)
        if wedge == "R":
            self.c += poly((a, y0), (b, y0), (b, y1), (a, y1 - k))
        elif wedge == "L":
            self.c += poly((a, y0), (b, y0), (b, y1 - k), (a, y1))
        else:
            self.c += poly((a, y0), (b, y0), (b, y1), (a, y1))

    def hbar(self, x0, x1, yc, h=None, slit_left=False, slit_right=False):
        h = h or self.H
        x0, x1 = self.x(x0), self.x(x1)
        s = h * 0.55
        y0, y1 = yc - h / 2, yc + h / 2
        p0 = (x0, y0)
        p3 = (x0 + (s if slit_left else 0), y1)
        p1 = (x1, y0)
        p2 = (x1 - (s if slit_right else 0), y1)
        self.c += poly(p0, p1, p2, p3)

    def ring(self, cx, cy, rxo, ryo, wx=None, wy=None):
        wx, wy = wx or self.W, wy or self.H
        rxo_s = rxo * self.sx
        self.c += ring(self.x(cx), cy, rxo_s, ryo, rxo_s - wx, ryo - wy)

    def sector(self, cx, cy, rxo, ryo, a0, a1, wx=None, wy=None, n=None):
        """Part of an elliptical ring from angle a0 to a1 (degrees, CCW)."""
        wx, wy = wx or self.W, wy or self.H
        cx = self.x(cx)
        rxo = rxo * self.sx
        rxi, ryi = rxo - wx, ryo - wy
        if a1 < a0:
            a1 += 360
        n = n or max(8, int((a1 - a0) / 5))
        outer, inner = [], []
        for i in range(n + 1):
            a = math.radians(a0 + (a1 - a0) * i / n)
            on = i in (0, n)
            outer.append((cx + rxo * math.cos(a), cy + ryo * math.sin(a), on))
            inner.append((cx + rxi * math.cos(a), cy + ryi * math.sin(a), on))
        self.c.append(Contour(outer + inner[::-1]))

    def diag(self, p0, p1, w=None):
        self.c += diag(self.pt(p0), self.pt(p1), w or self.W)

    def poly(self, *pts):
        self.c += poly(*[self.pt(p) for p in pts])

    def dot(self, xc, y0, side="R", size=None):
        """Parallelogram dot: the signature Sextant dot."""
        d = size or self.P["dot"]
        s = d * 0.62
        xc = self.x(xc)
        a, b = xc - d / 2, xc + d / 2
        if side == "R":
            self.c += poly((a, y0), (b, y0 + s), (b, y0 + s + d), (a, y0 + d))
        else:
            self.c += poly((a, y0 + s), (b, y0), (b, y0 + d), (a, y0 + s + d))

    def disc(self, cx, cy, r):
        self.c += disc(self.x(cx), cy, r)

    def glyph(self, name, uni, adv, **kw):
        return Glyph(name, uni, round(adv * self.sx), self.c, **kw)


# ---------------------------------------------------------------- lowercase

def arch(b, x_left_center, x_right_center, ytop=X, end_down=True, end_x=None):
    """n-style arch leaving the left stem through a bearing slit."""
    W = b.W
    xe = x_left_center + W / 2 / b.sx  # stem right edge (design space)
    x0 = xe + (0.8 * W / 2 - 5) / b.sx
    y0 = ytop * 0.62
    yt = ytop + OV - W / 2
    xm = (x0 + x_right_center) / 2 + 8
    p = b.path((x0, y0)).C((x0 + 45, y0 + 60), (xm - 95, yt), (xm, yt))
    if end_down:
        p.C((xm + 100, yt), (x_right_center, yt - 34), (x_right_center, yt - 150))
        p.L((x_right_center, 0))
    else:
        ex = end_x
        p.C((xm + 55, yt), (ex - 20, yt - 6), (ex, yt - 34))
    b.stroke(p)


def g_n(P, name="n", uni=0x6E, asc=False):
    b = B(P)
    W = b.W / b.sx
    xl = 58 + W / 2
    xr = xl + 336
    b.stem(xl, 0, ASC if asc else X, "R")
    arch(b, xl, xr)
    return b.glyph(name, uni, xr + W / 2 + 58)


def g_h(P):
    return g_n(P, "h", 0x68, asc=True)


def g_m(P):
    b = B(P)
    W = b.W / b.sx
    xl = 58 + W / 2
    x2 = xl + 292
    x3 = x2 + 292
    b.stem(xl, 0, X, "R")
    arch(b, xl, x2)
    arch(b, x2, x3)
    return b.glyph("m", 0x6D, x3 + W / 2 + 58)


def g_r(P):
    b = B(P)
    W = b.W / b.sx
    xl = 58 + W / 2
    b.stem(xl, 0, X, "R")
    arch(b, xl, None, end_down=False, end_x=xl + 250) if False else None
    # short arch
    xe = xl + W / 2
    x0 = xe + (0.8 * b.W / 2 - 5) / b.sx
    y0 = X * 0.62
    yt = X + OV - b.W / 2
    p = b.path((x0, y0)).C((x0 + 45, y0 + 60), (x0 + 80, yt), (x0 + 140, yt)).L((x0 + 190, yt))
    b.stroke(p)
    return b.glyph("r", 0x72, x0 + 190 + 30)


def g_u(P):
    """u is the n arch rotated 180 degrees, so both share one curve."""
    b = B(P)
    W = b.W / b.sx
    xl = 58 + W / 2
    xr = xl + 336
    tmp = B(P)
    arch(tmp, xl, xr)
    cx = (xl + xr) / 2 * b.sx
    b.c += [Contour([(2 * cx - x, X - y, on) for x, y, on in c.pts], c.hole) for c in tmp.c]
    b.stem(xr, 0, X, "R")
    b.stem(xl, 250, X, "R")
    return b.glyph("u", 0x75, xr + W / 2 + 58)


def g_i(P, dotted=True, name="i", uni=0x69):
    b = B(P)
    W = b.W / b.sx
    xc = 58 + W / 2
    b.stem(xc, 0, X, "R")
    if dotted:
        b.dot(xc, X + 95)
    return b.glyph(name, uni, xc + W / 2 + 58)


def g_dotless(P):
    return g_i(P, False, "dotlessi", 0x131)


def g_j(P):
    b = B(P)
    W = b.W / b.sx
    xc = 150
    yb = DESC + b.W / 2
    p = b.path((xc, X - 60)).L((xc, DESC + 140)).C((xc, yb + 30), (xc - 30, yb), (xc - 90, yb)).L((xc - 150, yb))
    b.stroke(p)
    b.stem(xc, 0, X, "R")
    b.dot(xc, X + 95)
    return b.glyph("j", 0x6A, xc + W / 2 + 58)


def g_l(P):
    b = B(P)
    W = b.W / b.sx
    xc = 58 + W / 2
    yb = b.W / 2
    p = b.path((xc, ASC - 60)).L((xc, 150)).C((xc, yb + 40), (xc + 30, yb), (xc + 90, yb)).L((xc + 140, yb))
    b.stroke(p)
    b.stem(xc, 200, ASC, "R")
    return b.glyph("l", 0x6C, xc + 140 + 34)


def g_t(P):
    b = B(P)
    W = b.W / b.sx
    xc = 130
    yb = b.W / 2
    p = b.path((xc, 600)).L((xc, 150)).C((xc, yb + 40), (xc + 30, yb), (xc + 90, yb)).L((xc + 150, yb))
    b.stroke(p)
    b.stem(xc, 250, 650, "R")
    b.hbar(xc - 108, xc + 150, X - b.H / 2)
    return b.glyph("t", 0x74, xc + 150 + 30)


def g_f(P):
    b = B(P)
    xc = 140
    yt = ASC + OV - b.W / 2
    p = b.path((xc, 0)).L((xc, 520)).C((xc, 660), (xc + 40, yt), (xc + 120, yt)).L((xc + 185, yt))
    b.stroke(p)
    b.hbar(xc - 108, xc + 160, X - b.H / 2)
    return b.glyph("f", 0x66, xc + 185 + 20)


def bowl_side(b, stem_c, cx_side, ybot, ytop, rxo, toward="L"):
    """Bowl attached to a stem with bearing slits. toward: side the bowl
    extends to ('L' for d/q, 'R' for b/p)."""
    W = b.W
    cy = (ybot + ytop) / 2
    ryo = (ytop - ybot) / 2 + OV
    if toward == "L":
        sl = stem_c * b.sx - W / 2
        cx = (sl + W / 2 - rxo * b.sx) / b.sx
        cos_t = (rxo * b.sx - W / 2 + 10) / (rxo * b.sx)
        t = math.degrees(math.acos(max(-1, min(1, cos_t))))
        b.sector(cx, cy, rxo, ryo, t, 360 - t)
    else:
        sr = stem_c * b.sx + W / 2
        cx = (sr - W / 2 + rxo * b.sx) / b.sx
        cos_t = (rxo * b.sx - W / 2 + 10) / (rxo * b.sx)
        t = math.degrees(math.acos(max(-1, min(1, cos_t))))
        b.sector(cx, cy, rxo, ryo, 180 + t, 180 - t + 360)
    return cx


def g_d(P, name="d", uni=0x64, desc=False):
    b = B(P)
    W = b.W / b.sx
    rxo = 232
    xs = 40 + 2 * rxo - W / 2 - 10
    if desc:
        b.stem(xs, DESC, X)
    else:
        b.stem(xs, 0, ASC, "R")
    bowl_side(b, xs, None, 0, X, rxo, "L")
    return b.glyph(name, uni, xs + W / 2 + 58)


def g_q(P):
    return g_d(P, "q", 0x71, desc=True)


def g_b(P, name="b", uni=0x62, desc=False):
    b = B(P)
    W = b.W / b.sx
    xs = 58 + W / 2
    rxo = 232
    if desc:
        b.stem(xs, DESC, X, "R")
    else:
        b.stem(xs, 0, ASC, "R")
    cx = bowl_side(b, xs, None, 0, X, rxo, "R")
    return b.glyph(name, uni, cx + rxo + 40)


def g_p(P):
    return g_b(P, "p", 0x70, desc=True)


def g_g(P):
    b = B(P)
    W = b.W / b.sx
    rxo = 232
    xs = 40 + 2 * rxo - W / 2 - 10
    b.stem(xs, 120, X)
    cx = bowl_side(b, xs, None, 0, X, rxo, "L")
    yb = DESC + b.W / 2
    p = b.path((xs, 200)).L((xs, DESC + 150)).C((xs, yb + 40), (xs - 50, yb), (cx + 20, yb))
    p.C((cx - 120, yb), (cx - 190, yb + 30), (cx - 205, yb + 80))
    b.stroke(p)
    return b.glyph("g", 0x67, xs + W / 2 + 58)


def g_o(P):
    b = B(P)
    rxo = 236
    cx = 40 + rxo
    b.ring(cx, X / 2, rxo, X / 2 + OV)
    return b.glyph("o", 0x6F, cx + rxo + 40)


def g_c(P):
    b = B(P)
    rxo = 226
    cx = 40 + rxo
    b.sector(cx, X / 2, rxo, X / 2 + OV, 42, 318)
    return b.glyph("c", 0x63, cx + rxo * 0.78 + 40)


def g_e(P):
    b = B(P)
    rxo = 234
    cx = 40 + rxo
    cy = X / 2
    b.sector(cx, cy, rxo, X / 2 + OV, 0, 322)
    h = b.H * 0.92
    b.hbar(cx - rxo + b.W / b.sx - 4, cx + rxo, cy - 8, h)
    return b.glyph("e", 0x65, cx + rxo + 40)


def g_a(P):
    b = B(P)
    W = b.W / b.sx
    xs = 400
    yt = X + OV - b.W / 2
    p = b.path((70, 430)).C((95, 480), (160, yt), (240, yt)).C((340, yt), (xs, yt - 50), (xs, 330)).L((xs, 0))
    b.stroke(p)
    # bowl
    yb = -OV + b.W / 2
    ybar = 300
    x_end = xs - W / 2 + 6 / b.sx
    p2 = b.path((x_end, ybar)).L((210, ybar)).C((115, ybar), (55, 250), (55, 160))
    p2.C((55, 65), (120, yb), (210, yb)).C((290, yb), (330, yb + 10), (xs - W * 0.2, yb + 30))
    b.stroke(p2, b.W * 0.94)
    return b.glyph("a", 0x61, xs + W / 2 + 56)


def s_path(b, x0, x1, y0, y1, ov=OV):
    """S-curve spine path from top-right terminal to bottom-left terminal."""
    w = x1 - x0
    h = y1 - y0
    W = b.W
    yt, yb = y1 + ov - W / 2, y0 - ov + W / 2
    ym = y0 + h * 0.52
    xm = (x0 + x1) / 2
    p = b.path((x1 - w * 0.02, y1 - h * 0.18))
    p.C((x1 - w * 0.12, yt - h * 0.02), (xm + w * 0.18, yt), (xm, yt))
    p.C((xm - w * 0.32, yt), (x0 + W / 2 / b.sx, yt - h * 0.1), (x0 + W / 2 / b.sx, y1 - h * 0.25))
    p.C((x0 + W / 2 / b.sx, ym + h * 0.06), (xm - w * 0.2, ym + h * 0.02), (xm, ym - h * 0.02))
    p.C((xm + w * 0.25, ym - h * 0.06), (x1 - W / 2 / b.sx, ym - h * 0.14), (x1 - W / 2 / b.sx, y0 + h * 0.24))
    p.C((x1 - W / 2 / b.sx, yb + h * 0.1), (xm + w * 0.32, yb), (xm, yb))
    p.C((xm - w * 0.22, yb), (x0 + w * 0.08, yb + h * 0.03), (x0, y0 + h * 0.2))
    return p


def g_s(P):
    b = B(P)
    b.stroke(s_path(b, 42, 412, 0, X))
    return b.glyph("s", 0x73, 412 + 42)


def g_v(P, name="v", uni=0x76, y_extend=False):
    b = B(P)
    x0, x1 = 30 + b.W * 0.6 / b.sx, 470
    xm = (x0 + x1) / 2
    b.diag((x0, X), (xm, 0))
    if y_extend:
        b.diag((x1, X), (xm - (x1 - xm) * (-DESC) / X, DESC))
    else:
        b.diag((x1, X), (xm, 0))
    return b.glyph(name, uni, x1 + b.W * 0.6 / b.sx + 30)


def g_y(P):
    return g_v(P, "y", 0x79, True)


def g_w(P):
    b = B(P)
    xs = [56, 225, 395, 565, 734]
    b.diag((xs[0], X), (xs[1], 0))
    b.diag((xs[2], X - 20), (xs[1], 0))
    b.diag((xs[2], X - 20), (xs[3], 0))
    b.diag((xs[4], X), (xs[3], 0))
    return b.glyph("w", 0x77, xs[4] + 56)


def g_x(P):
    b = B(P)
    x0, x1 = 60, 440
    b.diag((x0, X), (x1, 0))
    b.diag((x1, X), (x0, 0))
    return b.glyph("x", 0x78, x1 + 60)


def g_z(P):
    b = B(P)
    x0, x1 = 50, 420
    b.hbar(x0, x1, X - b.H / 2)
    b.hbar(x0, x1 + 10, b.H / 2)
    b.diag((x1 - 50, X - b.H), (x0 + 50, b.H))
    return b.glyph("z", 0x7A, x1 + 50)


def g_k(P):
    b = B(P)
    W = b.W / b.sx
    xs = 58 + W / 2
    b.stem(xs, 0, ASC, "R")
    xe = xs + W / 2
    b.diag((xe + 20 / b.sx, 150), (430, X))
    b.diag((xe + 120, 255), (450, 0))
    return b.glyph("k", 0x6B, 450 + b.W * 0.55 / b.sx + 26)


# ---------------------------------------------------------------- uppercase

def g_H(P):
    b = B(P)
    W = b.W / b.sx
    xl, xr = 70 + W / 2, 70 + W / 2 + 440
    b.stem(xl, 0, CAP)
    b.stem(xr, 0, CAP)
    b.hbar(xl, xr, CAP * 0.5)
    return b.glyph("H", 0x48, xr + W / 2 + 70)


def g_I(P):
    b = B(P)
    W = b.W / b.sx
    xc = 70 + W / 2
    b.stem(xc, 0, CAP)
    return b.glyph("I", 0x49, xc + W / 2 + 70)


def arms(b, xs, specs):
    sr = xs + b.W / 2 / b.sx
    for (y, x1) in specs:
        b.hbar(sr - 4 / b.sx, x1, y, slit_left=True)
        # slant start: bearing slit (thin wedge gap)


def g_E(P, name="E", uni=0x45, F=False):
    b = B(P)
    W = b.W / b.sx
    xs = 72 + W / 2
    b.stem(xs, 0, CAP)
    h = b.H
    specs = [(CAP - h / 2, 480), (CAP * 0.5 + 6, 430)]
    if not F:
        specs.append((h / 2, 492))
    arms(b, xs, specs)
    return b.glyph(name, uni, 492 + 40 if not F else 480 + 30)


def g_F(P):
    return g_E(P, "F", 0x46, True)


def g_L(P):
    b = B(P)
    W = b.W / b.sx
    xs = 72 + W / 2
    b.stem(xs, 0, CAP)
    arms(b, xs, [(b.H / 2, 470)])
    return b.glyph("L", 0x4C, 470 + 30)


def g_T(P):
    b = B(P)
    x0, x1 = 34, 554
    xm = (x0 + x1) / 2
    b.hbar(x0, x1, CAP - b.H / 2)
    b.stem(xm, 0, CAP - b.H - b.g)
    return b.glyph("T", 0x54, x1 + 34)


def g_O(P, name="O", uni=0x4F):
    b = B(P)
    rxo = 330
    cx = 44 + rxo
    b.ring(cx, CAP / 2, rxo, CAP / 2 + OV)
    return b.glyph(name, uni, cx + rxo + 44)


def g_Q(P):
    b = B(P)
    rxo = 330
    cx = 44 + rxo
    b.ring(cx, CAP / 2, rxo, CAP / 2 + OV)
    b.diag((cx + 110, 150), (cx + 300, -110))
    return b.glyph("Q", 0x51, cx + rxo + 44)


def g_C(P):
    b = B(P)
    rxo = 318
    cx = 44 + rxo
    b.sector(cx, CAP / 2, rxo, CAP / 2 + OV, 38, 322)
    return b.glyph("C", 0x43, cx + rxo * 0.8 + 44)


def g_G(P):
    b = B(P)
    rxo = 326
    cx = 44 + rxo
    b.sector(cx, CAP / 2, rxo, CAP / 2 + OV, 36, 352)
    xr = cx + rxo - b.W / 2 / b.sx
    b.stem(xr, 110, CAP * 0.47)
    b.hbar(cx + 40, xr + b.W / 2 / b.sx, CAP * 0.47 - b.H / 2)
    return b.glyph("G", 0x47, cx + rxo + 44)


def bowl_cap(b, xs, y0, y1, xr, slit=True):
    """D/P/B style bowl from a stem at xs (centre), spanning y0..y1."""
    W = b.W
    sr = xs + W / 2 / b.sx
    x0 = sr + (b.g if slit else -4) / b.sx
    yt, yb = y1 - b.H / 2, y0 + b.H / 2
    r = (yt - yb) / 2
    xc = xr - r / b.sx * 1.0 - W / 2 / b.sx
    xc = max(xc, x0 + 30)
    xrc = xr - W / 2 / b.sx
    p = b.path((x0, yt)).L((xc, yt))
    k = 0.56
    p.C((xc + (xrc - xc) * k * 1.4, yt), (xrc, yt - r * (1 - k)), (xrc, (yt + yb) / 2))
    p.C((xrc, yb + r * (1 - k)), (xc + (xrc - xc) * k * 1.4, yb), (xc, yb))
    p.L((x0, yb))
    b.stroke(p, b.H)
    # thicken verticals of the bowl a little
    return


def g_D(P):
    b = B(P)
    W = b.W / b.sx
    xs = 72 + W / 2
    b.stem(xs, 0, CAP)
    xr = 600
    sr = xs + W / 2
    x0 = sr + 30
    yt, yb = CAP - b.W * 0.95 / 2, b.W * 0.95 / 2
    b.hbar(sr - 4 / b.sx, x0 + 10, yt, b.W * 0.95, slit_left=True)
    b.hbar(sr - 4 / b.sx, x0 + 10, yb, b.W * 0.95, slit_left=True)
    xrc = xr - b.W / 2 / b.sx
    p = b.path((x0, yt)).L((260, yt)).C((480, yt), (xrc, 580), (xrc, CAP / 2))
    p.C((xrc, 120), (480, yb), (260, yb)).L((x0, yb))
    b.stroke(p, b.W * 0.95)
    return b.glyph("D", 0x44, xr + 50)


def g_P(P, name="P", uni=0x50, R=False):
    b = B(P)
    W = b.W / b.sx
    xs = 72 + W / 2
    b.stem(xs, 0, CAP)
    sr = xs + W / 2
    x0 = sr + 30
    yt, yb = CAP - b.W * 0.95 / 2, 300
    b.hbar(sr - 4 / b.sx, x0 + 10, yt, b.W * 0.95, slit_left=True)
    b.hbar(sr - 4 / b.sx, x0 + 10, yb, b.W * 0.95, slit_left=True)
    xrc = 500 - b.W / 2 / b.sx
    ymid = (yt + yb) / 2
    p = b.path((x0, yt)).L((280, yt)).C((420, yt), (xrc, yt - 40), (xrc, ymid))
    p.C((xrc, yb + 40), (420, yb), (280, yb)).L((x0, yb))
    b.stroke(p, b.W * 0.95)
    adv = 500 + 50
    if R:
        b.diag((330, yb), (520, 0))
        adv = 520 + b.W * 0.6 / b.sx + 40
    return b.glyph(name, uni, adv)


def g_R(P):
    return g_P(P, "R", 0x52, True)


def g_B(P):
    b = B(P)
    W = b.W / b.sx
    xs = 72 + W / 2
    b.stem(xs, 0, CAP)
    sr = xs + W / 2
    x0 = sr + 30
    # upper
    yt, ym = CAP - b.W * 0.95 / 2, 380
    for yy in (yt, ym, b.W * 0.95 / 2):
        b.hbar(sr - 4 / b.sx, x0 + 10, yy, b.W * 0.95, slit_left=True)
    xrc = 470 - b.W / 2 / b.sx
    p = b.path((x0, yt)).L((260, yt)).C((390, yt), (xrc, yt - 30), (xrc, (yt + ym) / 2))
    p.C((xrc, ym + 40), (390, ym), (260, ym)).L((x0, ym))
    b.stroke(p, b.W * 0.95)
    yb = b.W * 0.95 / 2
    xrc2 = 520 - b.W / 2 / b.sx
    p = b.path((260, ym)).C((430, ym), (xrc2, ym - 40), (xrc2, (ym + yb) / 2))
    p.C((xrc2, yb + 40), (430, yb), (270, yb)).L((x0, yb))
    b.stroke(p, b.W * 0.95)
    return b.glyph("B", 0x42, 520 + 50)


def g_J(P):
    b = B(P)
    xc = 400
    yb = -OV + b.W / 2
    p = b.path((xc, CAP)).L((xc, 230)).C((xc, 90), (330, yb), (230, yb)).C((140, yb), (70, 40), (44, 140))
    b.stroke(p)
    return b.glyph("J", 0x4A, xc + b.W / 2 / b.sx + 70)


def g_U(P):
    b = B(P)
    W = b.W / b.sx
    xl, xr = 70 + W / 2, 70 + W / 2 + 460
    xm = (xl + xr) / 2
    yb = -OV + b.W / 2
    p = b.path((xl, CAP)).L((xl, 250)).C((xl, 90), (xm - 120, yb), (xm, yb))
    p.C((xm + 120, yb), (xr, 90), (xr, 250)).L((xr, CAP))
    b.stroke(p)
    return b.glyph("U", 0x55, xr + W / 2 + 70)


def g_S(P):
    b = B(P)
    b.stroke(s_path(b, 44, 534, 0, CAP))
    return b.glyph("S", 0x53, 534 + 44)


def g_A(P):
    b = B(P)
    x0, x1 = 30 + b.W * 0.55 / b.sx, 630
    xm = (x0 + x1) / 2
    b.diag((x0, 0), (xm - 20, CAP))
    b.diag((x1, 0), (xm + 20, CAP))
    b.poly((xm - 20 - b.W * 0.55 / b.sx, CAP - 1), (xm + 20 + b.W * 0.55 / b.sx, CAP - 1),
           (xm + 20, CAP - 60), (xm - 20, CAP - 60))
    b.hbar(x0 + 150, x1 - 150, 220)
    return b.glyph("A", 0x41, x1 + b.W * 0.55 / b.sx + 30)


def g_V(P):
    b = B(P)
    x0, x1 = 30 + b.W * 0.55 / b.sx, 630
    xm = (x0 + x1) / 2
    b.diag((x0, CAP), (xm - 20, 0))
    b.diag((x1, CAP), (xm + 20, 0))
    b.poly((xm - 20 - b.W * 0.55 / b.sx, 1), (xm + 20 + b.W * 0.55 / b.sx, 1),
           (xm + 20, 60), (xm - 20, 60))
    return b.glyph("V", 0x56, x1 + b.W * 0.55 / b.sx + 30)


def g_W(P):
    b = B(P)
    xs = [56, 270, 480, 690, 904]
    b.diag((xs[0], CAP), (xs[1], 0))
    b.diag((xs[2], CAP - 30), (xs[1], 0))
    b.diag((xs[2], CAP - 30), (xs[3], 0))
    b.diag((xs[4], CAP), (xs[3], 0))
    return b.glyph("W", 0x57, xs[4] + 56)


def g_X(P):
    b = B(P)
    x0, x1 = 60, 560
    b.diag((x0, CAP), (x1, 0))
    b.diag((x1, CAP), (x0, 0))
    return b.glyph("X", 0x58, x1 + 60)


def g_Y(P):
    b = B(P)
    x0, x1 = 50, 570
    xm = (x0 + x1) / 2
    b.diag((x0, CAP), (xm, 300))
    b.diag((x1, CAP), (xm, 300))
    b.stem(xm, 0, 320)
    return b.glyph("Y", 0x59, x1 + 50)


def g_Z(P):
    b = B(P)
    x0, x1 = 50, 540
    b.hbar(x0 + 10, x1, CAP - b.H / 2)
    b.hbar(x0, x1 + 10, b.H / 2)
    b.diag((x1 - 70, CAP - b.H), (x0 + 70, b.H))
    return b.glyph("Z", 0x5A, x1 + 50)


def g_N(P):
    b = B(P)
    W = b.W / b.sx
    xl, xr = 72 + W / 2, 72 + W / 2 + 470
    b.stem(xl, 0, CAP)
    b.stem(xr, 0, CAP)
    b.diag((xl + 30, CAP), (xr - 30, 0))
    return b.glyph("N", 0x4E, xr + W / 2 + 72)


def g_M(P):
    b = B(P)
    W = b.W / b.sx
    xl, xr = 72 + W / 2, 72 + W / 2 + 600
    xm = (xl + xr) / 2
    b.stem(xl, 0, CAP)
    b.stem(xr, 0, CAP)
    b.diag((xl + 25, CAP), (xm, 120))
    b.diag((xr - 25, CAP), (xm, 120))
    return b.glyph("M", 0x4D, xr + W / 2 + 72)


def g_K(P):
    b = B(P)
    W = b.W / b.sx
    xs = 72 + W / 2
    b.stem(xs, 0, CAP)
    xe = xs + W / 2
    b.diag((xe + 24 / b.sx, 220), (560, CAP))
    b.diag((xe + 150, 360), (580, 0))
    return b.glyph("K", 0x4B, 580 + b.W * 0.55 / b.sx + 30)


# ---------------------------------------------------------------- figures

FIG = 680
FW = 590  # tabular figure width


def g_zero(P):
    b = B(P)
    cx = FW / 2
    b.ring(cx, FIG / 2, 235, FIG / 2 + OV)
    b.dot(cx, FIG / 2 - P["dot"] * 0.8)
    return b.glyph("zero", 0x30, FW)


def g_one(P):
    b = B(P)
    xc = 330
    b.stem(xc, 0, FIG, "R")
    b.diag((xc - 190, 520), (xc - 30, FIG - 30), b.W * 0.9)
    return b.glyph("one", 0x31, FW)


def g_two(P):
    b = B(P)
    yt = FIG + OV - b.W / 2
    p = b.path((92, 500)).C((105, 620), (185, yt), (295, yt)).C((410, yt), (488, 610), (488, 510))
    p.C((488, 410), (420, 350), (330, 285)).L((120, 110))
    b.stroke(p)
    b.hbar(95, 505, b.H / 2)
    b.poly((95, 0), (180, 0), (180, 150), (95, 80))
    return b.glyph("two", 0x32, FW)


def g_three(P):
    b = B(P)
    yt = FIG + OV - b.W / 2
    yb = -OV + b.W / 2
    p = b.path((96, 545)).C((125, 640), (205, yt), (290, yt)).C((395, yt), (465, 615), (465, 530))
    p.C((465, 440), (395, 392), (290, 392)).L((215, 392))
    b.stroke(p)
    p = b.path((215, 392)).L((300, 392)).C((420, 392), (495, 325), (495, 210))
    p.C((495, 90), (410, yb), (290, yb)).C((185, yb), (110, 60), (88, 150))
    b.stroke(p)
    return b.glyph("three", 0x33, FW)


def g_four(P):
    b = B(P)
    b.diag((395, FIG), (70, 175))
    b.hbar(60, 525, 175 + b.H / 2 - 30)
    b.stem(395, 0, 470, "R")
    return b.glyph("four", 0x34, FW)


def g_five(P):
    b = B(P)
    yb = -OV + b.W / 2
    b.hbar(150, 480, FIG - b.H / 2)
    b.poly((150 - b.W / 2 / b.sx, FIG), (150 + b.W / 2 / b.sx, FIG), (175 + b.W / 2 / b.sx, 380), (175 - b.W / 2 / b.sx, 380))
    p = b.path((175, 395)).C((210, 425), (250, 440), (300, 440)).C((420, 440), (500, 360), (500, 230))
    p.C((500, 95), (410, yb), (290, yb)).C((185, yb), (110, 60), (88, 150))
    b.stroke(p)
    return b.glyph("five", 0x35, FW)


def six_contours(P):
    b = B(P)
    b.ring(295, 222, 210, 222 + OV)
    yt = FIG + OV - b.W / 2
    p = b.path((85 + b.W / 2 / b.sx, 240)).C((85 + b.W / 2 / b.sx, 540), (180, yt), (310, yt)).C((380, yt), (430, 650), (470, 600))
    b.stroke(p)
    return b


def g_six(P):
    b = six_contours(P)
    return b.glyph("six", 0x36, FW)


def g_nine(P):
    b = six_contours(P)
    cx = FW * b.sx / 2
    b.c = [Contour([(2 * cx - x, FIG - y, on) for x, y, on in c.pts], c.hole) for c in b.c]
    return b.glyph("nine", 0x39, FW)


def g_seven(P):
    b = B(P)
    b.hbar(85, 505, FIG - b.H / 2)
    b.diag((470, FIG - b.H), (215, 0))
    return b.glyph("seven", 0x37, FW)


def g_eight(P):
    b = B(P)
    b.ring(295, 512, 180, 168 + OV / 2)
    b.ring(295, 196, 212, 196 + OV)
    return b.glyph("eight", 0x38, FW)


# ---------------------------------------------------------------- punctuation

def g_space(P):
    return Glyph("space", 0x20, round(240 * P["sx"]))


def g_nbsp(P):
    return Glyph("uni00A0", 0xA0, round(240 * P["sx"]))


def g_period(P, name="period", uni=0x2E):
    b = B(P)
    d = P["dot"]
    b.dot(60 + d / 2 / b.sx, 0)
    return b.glyph(name, uni, 120 + d / b.sx)


def g_comma(P, name="comma", uni=0x2C, y=0):
    b = B(P)
    d = P["dot"]
    x = 60 + d / 2 / b.sx
    b.dot(x, y)
    xa = b.x(x) - d / 2
    b.c += poly((xa, y + d), (xa + d * 0.5, y + d), (xa - d * 0.15, y - 150), (xa - d * 0.55, y - 150))
    return b.glyph(name, uni, 120 + d / b.sx)


def g_colon(P):
    b = B(P)
    d = P["dot"]
    x = 60 + d / 2 / b.sx
    b.dot(x, 0)
    b.dot(x, X - d - d * 0.62)
    return b.glyph("colon", 0x3A, 120 + d / b.sx)


def g_semicolon(P):
    g = g_comma(P, "semicolon", 0x3B)
    b = B(P)
    d = P["dot"]
    b.dot(60 + d / 2 / b.sx, X - d - d * 0.62)
    g.contours += b.c
    return g


def g_exclam(P):
    b = B(P)
    d = P["dot"]
    x = 60 + d / 2 / b.sx
    b.dot(x, 0)
    b.stem(x, 210, CAP, "R")
    return b.glyph("exclam", 0x21, 120 + d / b.sx)


def g_question(P):
    b = B(P)
    d = P["dot"]
    yt = CAP + OV - b.W / 2
    p = b.path((60, 540)).C((80, 650), (160, yt), (250, yt)).C((360, yt), (430, 640), (430, 555))
    p.C((430, 470), (370, 430), (300, 395)).C((260, 375), (245, 340), (245, 300)).L((245, 215))
    b.stroke(p)
    b.dot(245, 0)
    return b.glyph("question", 0x3F, 490)


def g_quotesingle(P):
    b = B(P)
    b.stem(80, 470, CAP, "R")
    return b.glyph("quotesingle", 0x27, 160)


def g_quotedbl(P):
    b = B(P)
    b.stem(80, 470, CAP, "R")
    b.stem(80 + b.W / b.sx + 60, 470, CAP, "R")
    return b.glyph("quotedbl", 0x22, 160 + b.W / b.sx + 60)


def g_quoteright(P):
    g = g_comma(P, "quoteright", 0x2019, y=CAP - P["dot"] * 1.6)
    return g


def g_quoteleft(P):
    g = g_comma(P, "quoteleft", 0x2018, y=CAP - P["dot"] * 1.6)
    cx = g.adv / 2
    yc = CAP - 90
    g.contours = [Contour([(2 * cx - x, 2 * yc - y, on) for x, y, on in c.pts]) for c in g.contours]
    return g


def g_hyphen(P, name="hyphen", uni=0x2D, w=280):
    b = B(P)
    b.hbar(50, 50 + w, 265)
    return b.glyph(name, uni, 100 + w)


def g_endash(P):
    return g_hyphen(P, "endash", 0x2013, 450)


def g_emdash(P):
    return g_hyphen(P, "emdash", 0x2014, 900)


def g_paren(P, name="parenleft", uni=0x28, flip=False):
    b = B(P)
    cx = 300
    b.sector(cx, 290, 210, 460, 112, 248, wx=b.W * 0.95, wy=b.W * 0.95)
    g = b.glyph(name, uni, 300)
    if flip:
        ax = g.adv / 2
        g.contours = mirror_x(g.contours, ax)
    # shift so left bearing is sensible
    bx = bbox(g.contours)
    g.contours = shift(g.contours, 60 - bx[0])
    g.adv = round(bx[2] - bx[0] + 120)
    return g


def g_parenright(P):
    return g_paren(P, "parenright", 0x29, True)


def g_bracket(P, name="bracketleft", uni=0x5B, flip=False):
    b = B(P)
    xs = 90
    b.stem(xs, -150, CAP + 30)
    b.hbar(xs, 230, CAP + 30 - b.H / 2)
    b.hbar(xs, 230, -150 + b.H / 2)
    g = b.glyph(name, uni, 270)
    if flip:
        g.contours = mirror_x(g.contours, g.adv / 2)
    return g


def g_bracketright(P):
    return g_bracket(P, "bracketright", 0x5D, True)


def g_slash(P, name="slash", uni=0x2F, back=False):
    b = B(P)
    if back:
        b.diag((40, CAP + 40), (360, -150))
    else:
        b.diag((40, -150), (360, CAP + 40))
    return b.glyph(name, uni, 400)


def g_backslash(P):
    return g_slash(P, "backslash", 0x5C, True)


def g_bar(P):
    b = B(P)
    b.stem(120, -180, CAP + 60)
    return b.glyph("bar", 0x7C, 240)


def g_underscore(P):
    b = B(P)
    b.hbar(0, 500, -120)
    return b.glyph("underscore", 0x5F, 500)


def g_plus(P):
    b = B(P)
    b.hbar(70, 490, 300)
    b.stem(280, 90, 510)
    return b.glyph("plus", 0x2B, 560)


def g_minus(P):
    b = B(P)
    b.hbar(70, 490, 300)
    return b.glyph("minus", 0x2212, 560)


def g_equal(P):
    b = B(P)
    b.hbar(70, 490, 390)
    b.hbar(70, 490, 210)
    return b.glyph("equal", 0x3D, 560)


def g_multiply(P):
    b = B(P)
    b.diag((120, 130), (440, 470), b.W * 0.9)
    b.diag((440, 130), (120, 470), b.W * 0.9)
    return b.glyph("multiply", 0xD7, 560)


def g_less(P, name="less", uni=0x3C, flip=False):
    b = B(P)
    p = b.path((470, 520)).L((100, 300)).L((470, 80))
    b.stroke(p)
    g = b.glyph(name, uni, 560)
    if flip:
        g.contours = mirror_x(g.contours, g.adv / 2)
    return g


def g_greater(P):
    return g_less(P, "greater", 0x3E, True)


def g_guil(P, name="guillemotleft", uni=0xAB, flip=False):
    b = B(P)
    w = b.W * 0.9
    for x in (60, 260):
        p = b.path((x + 190, 430)).L((x + 30, 260)).L((x + 190, 90))
        b.stroke(p, w)
    g = b.glyph(name, uni, 500)
    if flip:
        g.contours = mirror_x(g.contours, g.adv / 2)
    return g


def g_guilright(P):
    return g_guil(P, "guillemotright", 0xBB, True)


def g_asterisk(P):
    b = B(P)
    cx, cy = 250, 540
    for a in (90, 210, 330):
        r = math.radians(a)
        p = b.path((cx, cy)).L((cx + 150 * math.cos(r), cy + 150 * math.sin(r)))
        b.stroke(p, b.W * 0.85)
    return b.glyph("asterisk", 0x2A, 500)


def g_bullet(P):
    b = B(P)
    b.dot(170, 260, size=P["dot"] * 1.35)
    return b.glyph("bullet", 0x2022, 340)


def g_periodcentered(P):
    b = B(P)
    d = P["dot"]
    b.dot(60 + d / 2 / b.sx, 260)
    return b.glyph("periodcentered", 0xB7, 120 + d / b.sx)


def g_ellipsis(P):
    b = B(P)
    d = P["dot"]
    for i in range(3):
        b.dot(80 + i * 230, 0)
    return b.glyph("ellipsis", 0x2026, 80 + 2 * 230 + 80 + d / b.sx)


def g_numbersign(P):
    b = B(P)
    b.diag((180, 0), (250, CAP), b.W * 0.85)
    b.diag((370, 0), (440, CAP), b.W * 0.85)
    b.hbar(70, 560, 460, b.H * 0.9)
    b.hbar(50, 540, 240, b.H * 0.9)
    return b.glyph("numbersign", 0x23, 610)


def g_percent(P):
    b = B(P)
    b.ring(170, 545, 120, 155)
    b.ring(560, 155, 120, 155)
    b.diag((160, 0), (570, CAP), b.W * 0.85)
    return b.glyph("percent", 0x25, 730)


def g_degree(P):
    b = B(P)
    b.ring(170, 560, 110, 110, b.W * 0.8, b.W * 0.8)
    return b.glyph("degree", 0xB0, 340)


def g_at(P):
    b = B(P)
    cx, cy = 450, 290
    b.sector(cx, cy, 400, 400, 330, 315, wx=b.W * 0.8, wy=b.W * 0.8)
    b.ring(cx - 20, cy, 150, 160)
    b.stem(cx + 130, cy - 150, cy + 160)
    p = b.path((cx + 130, cy - 120)).C((cx + 130, cy - 210), (cx + 190, cy - 230), (cx + 250, cy - 230))
    p.C((cx + 340, cy - 230), (cx + 400 - b.W * 0.4 / b.sx, cy - 150), (cx + 400 - b.W * 0.4 / b.sx, cy - 30))
    b.stroke(p, b.W * 0.8)
    return b.glyph("at", 0x40, cx + 450)


def g_ampersand(P):
    b = B(P)
    yt = CAP + OV - b.W / 2
    yb = -OV + b.W / 2
    # upper loop
    p = b.path((575, 0)).L((205, 420)).C((150, 480), (130, 525), (130, 570)).C((130, 650), (200, yt), (280, yt))
    p.C((360, yt), (420, 650), (420, 580)).C((420, 500), (350, 460), (260, 410))
    p.C((130, 340), (60, 280), (60, 190)).C((60, 80), (150, yb), (265, yb)).C((390, yb), (480, 90), (520, 290))
    b.stroke(p)
    return b.glyph("ampersand", 0x26, 640)


def g_dollar(P):
    b = B(P)
    b.stroke(s_path(b, 60, 490, 30, CAP - 30, ov=0))
    b.stem(275, -90, CAP + 90)
    return b.glyph("dollar", 0x24, 550)


def g_euro(P):
    b = B(P)
    rxo = 300
    cx = 60 + rxo + 60
    b.sector(cx, CAP / 2, rxo, CAP / 2 + OV, 40, 320)
    b.hbar(40, 420, 420, b.H * 0.85)
    b.hbar(40, 400, 270, b.H * 0.85)
    return b.glyph("Euro", 0x20AC, cx + rxo * 0.8 + 40)


def g_asciitilde(P, name="asciitilde", uni=0x7E, y=300):
    b = B(P)
    p = b.path((70, y - 30)).C((110, y + 60), (170, y + 60), (250, y)).C((330, y - 60), (390, y - 60), (430, y + 30))
    b.stroke(p, b.W * 0.85)
    return b.glyph(name, uni, 500)


def g_grave(P):
    b = B(P)
    b.diag((260, CAP - 160), (140, CAP), b.W * 0.85)
    return b.glyph("grave", 0x60, 400)


def g_brace(P, name="braceleft", uni=0x7B, flip=False):
    b = B(P)
    w = b.W * 0.9
    p = b.path((300, CAP + 30)).C((220, CAP + 30), (190, CAP), (190, CAP - 80)).L((190, 380))
    p.C((190, 310), (160, 275), (90, 275))
    b.stroke(p, w)
    p = b.path((90, 275)).C((160, 275), (190, 240), (190, 170)).L((190, -40))
    p.C((190, -120), (220, -150), (300, -150))
    b.stroke(p, w)
    g = b.glyph(name, uni, 360)
    if flip:
        g.contours = mirror_x(g.contours, g.adv / 2)
    return g


def g_braceright(P):
    return g_brace(P, "braceright", 0x7D, True)


def g_asciicircum(P):
    b = B(P)
    p = b.path((90, 380)).L((250, CAP - 20)).L((410, 380))
    b.stroke(p, b.W * 0.85)
    return b.glyph("asciicircum", 0x5E, 500)


def g_quotedblleft(P):
    g1 = g_quoteleft(P)
    g2 = g_quoteleft(P)
    sh = g1.adv - 40
    g1.contours += shift(g2.contours, sh)
    g1.name, g1.uni, g1.adv = "quotedblleft", [0x201C], g1.adv + sh
    return g1


def g_quotedblright(P):
    g1 = g_quoteright(P)
    g2 = g_quoteright(P)
    sh = g1.adv - 40
    g1.contours += shift(g2.contours, sh)
    g1.name, g1.uni, g1.adv = "quotedblright", [0x201D], g1.adv + sh
    return g1


def g_quotesinglbase(P):
    g = g_comma(P, "quotesinglbase", 0x201A)
    return g


def g_quotedblbase(P):
    g1 = g_comma(P, "quotedblbase", 0x201E)
    g2 = g_comma(P)
    sh = g1.adv - 40
    g1.contours += shift(g2.contours, sh)
    g1.adv += sh
    return g1


# ---------------------------------------------------------------- accents
# Accents are drawn centred on x=0 with their base at y=0.

def acc(P, name, uni, fn):
    b = B(P)
    fn(b)
    g = Glyph(name, uni, 0, b.c, kind="accent")
    return g


def _acute(b):
    b.c += diag((-60, 0), (60, 150), b.W * 0.9)


def _grave(b):
    b.c += diag((60, 0), (-60, 150), b.W * 0.9)


def _circ(b):
    w = b.W * 0.85
    p = Path((-150, 0)).L((0, 140)).L((150, 0))
    b.c += stroke(p, w)


def _dier(b):
    d = b.P["dot"]
    for x in (-105, 105):
        b.c += poly((x - d / 2, 0), (x + d / 2, d * 0.62), (x + d / 2, d * 1.62), (x - d / 2, d))


def _ring(b):
    b.c += ring(0, 90, 95, 95, 95 - b.W * 0.75, 95 - b.W * 0.75)


def _tilde(b):
    p = Path((-160, 20)).C((-120, 120), (-60, 120), (0, 70)).C((60, 20), (120, 20), (160, 120))
    b.c += stroke(p, b.W * 0.8)


def _cedilla(b):
    p = Path((0, 0)).L((0, -60)).C((60, -60), (110, -90), (110, -140)).C((110, -200), (40, -220), (-70, -205))
    b.c += stroke(p, b.W * 0.7)


def _dot(b):
    d = b.P["dot"]
    b.c += poly((-d / 2, 0), (d / 2, d * 0.62), (d / 2, d * 1.62), (-d / 2, d))


ACCENTS = {
    "acutecomb": (0x301, _acute), "gravecomb": (0x300, _grave),
    "circumflexcomb": (0x302, _circ), "dieresiscomb": (0x308, _dier),
    "ringcomb": (0x30A, _ring), "tildecomb": (0x303, _tilde),
    "cedillacomb": (0x327, _cedilla), "dotaccentcomb": (0x307, _dot),
}

# composite table: name, unicode, base, accent, placement ('top'/'bottom')
LC = {"a": "àáâãäå", "e": "èéêë", "dotlessi": "ìíîï", "o": "òóôõö", "u": "ùúûü",
      "y": "ýÿ", "n": "ñ", "c": "ç"}
UC = {"A": "ÀÁÂÃÄÅ", "E": "ÈÉÊË", "I": "ÌÍÎÏ", "O": "ÒÓÔÕÖ", "U": "ÙÚÛÜ",
      "Y": "ÝŸ", "N": "Ñ", "C": "Ç"}
ACC_OF = {"à": "gravecomb", "á": "acutecomb", "â": "circumflexcomb", "ã": "tildecomb",
          "ä": "dieresiscomb", "å": "ringcomb", "ç": "cedillacomb"}


def accent_for(ch):
    import unicodedata
    d = unicodedata.normalize("NFD", ch)
    mark = d[1]
    return {"\u0300": "gravecomb", "\u0301": "acutecomb", "\u0302": "circumflexcomb",
            "\u0303": "tildecomb", "\u0308": "dieresiscomb", "\u030A": "ringcomb",
            "\u0327": "cedillacomb"}[mark]


import unicodedata as _ud


def _name_for(ch):
    n = _ud.name(ch)
    # AGL-ish production names
    table = {
        "à": "agrave", "á": "aacute", "â": "acircumflex", "ã": "atilde", "ä": "adieresis", "å": "aring",
        "è": "egrave", "é": "eacute", "ê": "ecircumflex", "ë": "edieresis",
        "ì": "igrave", "í": "iacute", "î": "icircumflex", "ï": "idieresis",
        "ò": "ograve", "ó": "oacute", "ô": "ocircumflex", "õ": "otilde", "ö": "odieresis",
        "ù": "ugrave", "ú": "uacute", "û": "ucircumflex", "ü": "udieresis",
        "ý": "yacute", "ÿ": "ydieresis", "ñ": "ntilde", "ç": "ccedilla",
    }
    if ch in table:
        return table[ch]
    if ch.lower() in table:
        t = table[ch.lower()]
        return t[0].upper() + t[1:]
    return "uni%04X" % ord(ch)


LATIN_FUNCS = [
    g_a, g_b, g_c, g_d, g_e, g_f, g_g, g_h, g_i, g_j, g_k, g_l, g_m, g_n, g_o, g_p, g_q, g_r,
    g_s, g_t, g_u, g_v, g_w, g_x, g_y, g_z, g_dotless,
    g_A, g_B, g_C, g_D, g_E, g_F, g_G, g_H, g_I, g_J, g_K, g_L, g_M, g_N, g_O, g_P, g_Q, g_R,
    g_S, g_T, g_U, g_V, g_W, g_X, g_Y, g_Z,
    g_zero, g_one, g_two, g_three, g_four, g_five, g_six, g_seven, g_eight, g_nine,
    g_space, g_nbsp, g_period, g_comma, g_colon, g_semicolon, g_exclam, g_question,
    g_quotesingle, g_quotedbl, g_quoteleft, g_quoteright, g_quotedblleft, g_quotedblright,
    g_quotesinglbase, g_quotedblbase,
    g_hyphen, g_endash, g_emdash, g_paren, g_parenright, g_bracket, g_bracketright,
    g_slash, g_backslash, g_bar, g_underscore, g_plus, g_minus, g_equal, g_multiply,
    g_less, g_greater, g_guil, g_guilright, g_asterisk, g_bullet, g_periodcentered,
    g_ellipsis, g_numbersign, g_percent, g_degree, g_at, g_ampersand, g_dollar, g_euro,
    g_asciitilde, g_asciicircum, g_grave, g_brace, g_braceright,
]


def build_latin(P):
    glyphs = [f(P) for f in LATIN_FUNCS]
    for name, (u, fn) in ACCENTS.items():
        glyphs.append(acc(P, name, u, fn))
    by = {g.name: g for g in glyphs}
    comps = []
    for table, dy_top in ((LC, X + 70), (UC, CAP + 60)):
        for base, chars in table.items():
            for ch in chars:
                a = accent_for(ch)
                bg = by[base]
                bx = bbox(bg.contours)
                cx = (bx[0] + bx[2]) / 2
                if base in ("a",):
                    cx = bx[0] + (bx[2] - bx[0]) * 0.46
                if a == "cedillacomb":
                    dy = 0
                    cx = (bx[0] + bx[2]) / 2 + (10 if base == "c" else 20)
                else:
                    dy = dy_top
                g = Glyph(_name_for(ch), ord(ch), bg.adv,
                          components=[(base, 0, 0), (a, round(cx), dy)])
                comps.append(g)
    # oe, OE, ae, AE
    o, e, A, E, a_ = by["o"], by["e"], by["A"], by["E"], by["a"]
    W = P["W"]
    ob, eb, Ob, Eb, ab = (bbox(g.contours) for g in (o, e, by["O"], E, a_))
    dx = round(ob[2] - W - eb[0] + 6)
    comps.append(Glyph("oe", 0x153, dx + e.adv, components=[("o", 0, 0), ("e", dx, 0)]))
    dx = round(Ob[2] - W - Eb[0] + 4)
    comps.append(Glyph("OE", 0x152, dx + E.adv, components=[("O", 0, 0), ("E", dx, 0)]))
    dx = round(ab[2] - W - eb[0] + 6)
    comps.append(Glyph("ae", 0xE6, dx + e.adv, components=[("a", 0, 0), ("e", dx, 0)]))
    return glyphs + comps
