"""Simt geometry engine.

Glyphs are described as centre-line strokes (lines and cubic curves) with a
width, plus plain polygons and elliptical rings. Strokes are expanded into
outlines by sampling the centre line and offsetting it. Smooth runs are written
as TrueType quadratic splines (consecutive off-curve points), corners and cap
ends as on-curve points, so outlines are smooth curves, not polygons.
"""
import math

STEP = 22.0  # sampling step along curves, in font units


def _norm(v):
    l = math.hypot(v[0], v[1])
    return (v[0] / l, v[1] / l) if l > 1e-9 else (0.0, 0.0)


def _cubic(p0, p1, p2, p3, t):
    mt = 1 - t
    a, b, c, d = mt ** 3, 3 * mt * mt * t, 3 * mt * t * t, t ** 3
    return (a * p0[0] + b * p1[0] + c * p2[0] + d * p3[0],
            a * p0[1] + b * p1[1] + c * p2[1] + d * p3[1])


def _cubic_d(p0, p1, p2, p3, t):
    mt = 1 - t
    a, b, c = 3 * mt * mt, 6 * mt * t, 3 * t * t
    return (a * (p1[0] - p0[0]) + b * (p2[0] - p1[0]) + c * (p3[0] - p2[0]),
            a * (p1[1] - p0[1]) + b * (p2[1] - p1[1]) + c * (p3[1] - p2[1]))


class Path:
    """A centre-line path made of line and cubic segments."""

    def __init__(self, start):
        self.start = tuple(start)
        self.segs = []
        self.cur = tuple(start)

    def L(self, p):
        self.segs.append(("L", self.cur, tuple(p)))
        self.cur = tuple(p)
        return self

    def C(self, c1, c2, p):
        self.segs.append(("C", self.cur, tuple(c1), tuple(c2), tuple(p)))
        self.cur = tuple(p)
        return self

    def samples(self):
        """Return list of (point, tangent_in, tangent_out, is_corner)."""
        pts = []
        for si, s in enumerate(self.segs):
            if s[0] == "L":
                p0, p1 = s[1], s[2]
                t = _norm((p1[0] - p0[0], p1[1] - p0[1]))
                seq = [(p0, t), (p1, t)]
            else:
                p0, c1, c2, p3 = s[1:]
                est = (math.dist(p0, c1) + math.dist(c1, c2) + math.dist(c2, p3))
                n = max(3, int(math.ceil(est / STEP)))
                seq = []
                for i in range(n + 1):
                    tt = i / n
                    d = _cubic_d(p0, c1, c2, p3, tt)
                    if math.hypot(*d) < 1e-6:
                        d = _cubic_d(p0, c1, c2, p3, min(max(tt, 0.001), 0.999))
                    seq.append((_cubic(p0, c1, c2, p3, tt), _norm(d)))
            if pts:
                # merge boundary point
                prev_p, tin, _, _ = pts[-1]
                tout = seq[0][1]
                pts[-1] = (prev_p, tin, tout, None)
                seq = seq[1:]
                for p, t in seq:
                    pts.append((p, t, t, None))
            else:
                for p, t in seq:
                    pts.append((p, t, t, None))
        out = []
        for i, (p, tin, tout, _) in enumerate(pts):
            ang = math.degrees(math.acos(max(-1, min(1, tin[0] * tout[0] + tin[1] * tout[1]))))
            out.append((p, tin, tout, ang > 6))
        return out

    def length(self):
        s = self.samples()
        return sum(math.dist(s[i][0], s[i + 1][0]) for i in range(len(s) - 1))


class Contour:
    """Closed contour: list of (x, y, on_curve)."""

    def __init__(self, pts, hole=False):
        self.pts = pts
        self.hole = hole

    def transformed(self, f):
        return Contour([(*f(x, y), on) for x, y, on in self.pts], self.hole)


def stroke(path, w, w_end=None, taper_from=0.0, cap_start="butt", cap_end="butt"):
    """Expand an open path into a closed outline.

    w: width; if w_end is given the width changes linearly from w to w_end,
    starting at fraction taper_from of the path length.
    """
    s = path.samples()
    # cumulative length
    cum = [0.0]
    for i in range(1, len(s)):
        cum.append(cum[-1] + math.dist(s[i - 1][0], s[i][0]))
    total = cum[-1] or 1.0

    def width_at(i):
        if w_end is None:
            return w
        f = cum[i] / total
        if f <= taper_from:
            return w
        k = (f - taper_from) / (1 - taper_from)
        k = k * k * (3 - 2 * k) if False else k
        return w + (w_end - w) * k

    left, right = [], []
    n = len(s)
    for i, (p, tin, tout, corner) in enumerate(s):
        hw = width_at(i) / 2
        n1 = (-tin[1], tin[0])
        n2 = (-tout[1], tout[0])
        if corner and 0 < i < n - 1:
            nm = _norm((n1[0] + n2[0], n1[1] + n2[1]))
            c = nm[0] * n1[0] + nm[1] * n1[1]
            sc = min(1 / max(c, 0.25), 3.0)
            off = (nm[0] * hw * sc, nm[1] * hw * sc)
            on = True
        else:
            nn = n1 if i == n - 1 else n2
            if 0 < i < n - 1:
                nn = _norm((n1[0] + n2[0], n1[1] + n2[1]))
            off = (nn[0] * hw, nn[1] * hw)
            on = (i == 0 or i == n - 1)
        left.append((p[0] + off[0], p[1] + off[1], on))
        right.append((p[0] - off[0], p[1] - off[1], on))
    pts = left + right[::-1]
    return [Contour(pts)]


def ring(cx, cy, rxo, ryo, rxi, ryi, n=56, rot=0.0):
    """Elliptical ring: outer ellipse and inner hole. All points off-curve."""
    def ell(rx, ry):
        out = []
        cr, sr = math.cos(math.radians(rot)), math.sin(math.radians(rot))
        for i in range(n):
            a = 2 * math.pi * i / n
            x, y = rx * math.cos(a), ry * math.sin(a)
            out.append((cx + x * cr - y * sr, cy + x * sr + y * cr, False))
        return out
    res = [Contour(ell(rxo, ryo))]
    if rxi > 0 and ryi > 0:
        res.append(Contour(ell(rxi, ryi), hole=True))
    return res


def disc(cx, cy, rx, ry=None, n=40):
    return ring(cx, cy, rx, ry if ry is not None else rx, 0, 0, n)


def poly(*pts):
    return [Contour([(x, y, True) for x, y in pts])]


def rect(x0, y0, x1, y1):
    return poly((x0, y0), (x1, y0), (x1, y1), (x0, y1))


def diag(p0, p1, w):
    """Straight stroke whose ends are cut horizontally (for diagonals)."""
    (x0, y0), (x1, y1) = p0, p1
    dy = y1 - y0
    dx = x1 - x0
    L = math.hypot(dx, dy)
    sin = abs(dy) / L
    hw = (w / 2) / max(sin, 0.2)
    return poly((x0 - hw, y0), (x0 + hw, y0), (x1 + hw, y1), (x1 - hw, y1))


def signed_area(pts):
    a = 0.0
    for i in range(len(pts)):
        x0, y0 = pts[i][0], pts[i][1]
        x1, y1 = pts[(i + 1) % len(pts)][0], pts[(i + 1) % len(pts)][1]
        a += x0 * y1 - x1 * y0
    return a / 2


def draw_contours(pen, contours):
    """Write contours to a TrueType pen with correct orientation.

    TrueType: filled contours clockwise, holes counter-clockwise (y up).
    """
    for c in contours:
        pts = [(int(round(x)), int(round(y)), on) for x, y, on in c.pts]
        # drop consecutive duplicates
        clean = []
        for p in pts:
            if not clean or (p[0], p[1]) != (clean[-1][0], clean[-1][1]):
                clean.append(p)
        if len(clean) > 1 and (clean[0][0], clean[0][1]) == (clean[-1][0], clean[-1][1]):
            clean.pop()
        if len(clean) < 3:
            continue
        a = signed_area(clean)
        want_cw = not c.hole
        if (a > 0 and want_cw) or (a < 0 and not want_cw):
            clean = clean[::-1]
        ons = [i for i, p in enumerate(clean) if p[2]]
        if not ons:
            pen.qCurveTo(*[(p[0], p[1]) for p in clean], None)
            pen.closePath()
            continue
        k = ons[0]
        seq = clean[k:] + clean[:k]
        pen.moveTo((seq[0][0], seq[0][1]))
        offs = []
        for p in seq[1:]:
            if p[2]:
                if offs:
                    pen.qCurveTo(*offs, (p[0], p[1]))
                else:
                    pen.lineTo((p[0], p[1]))
                offs = []
            else:
                offs.append((p[0], p[1]))
        if offs:
            pen.qCurveTo(*offs, (seq[0][0], seq[0][1]))
        pen.closePath()


def transform(contours, f):
    return [c.transformed(f) for c in contours]


def mirror_x(contours, axis):
    out = []
    for c in contours:
        out.append(Contour([(2 * axis - x, y, on) for x, y, on in c.pts], c.hole))
    return out


def shift(contours, dx=0, dy=0):
    return [Contour([(x + dx, y + dy, on) for x, y, on in c.pts], c.hole) for c in contours]


def bbox(contours):
    xs = [p[0] for c in contours for p in c.pts]
    ys = [p[1] for c in contours for p in c.pts]
    if not xs:
        return (0, 0, 0, 0)
    return (min(xs), min(ys), max(xs), max(ys))
