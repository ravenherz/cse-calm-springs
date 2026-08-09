(function (root) {
  function x(i, fretCount) {
    const headstock = 10;
    const span = 100 - headstock;
    return headstock + (i / fretCount) * span;
  }

  function bandCenter(i, fretCount) {
    if (i === 0) return x(0, fretCount) - (bandCenter(1, fretCount) - x(0, fretCount));
    return (x(i - 1, fretCount) + x(i, fretCount)) / 2;
  }

  function leftBound(i, fretCount) {
    return x(i - 1, fretCount) - 0.2 * (x(1, fretCount) - x(0, fretCount));
  }

  function rightBound(i, fretCount) {
    return Math.min(100, x(i, fretCount) + 0.2 * (x(1, fretCount) - x(0, fretCount)));
  }

  function fretFromX(pct, fretCount) {
    for (let i = 0; i <= fretCount; i++) {
      if (pct < x(i, fretCount)) return i;
    }
    return fretCount;
  }

  function yArea(stringCount) {
    const topPad = 46;
    const bottomPad = 16 + 42;
    const n = stringCount;
    return {
      topPad: topPad,
      height: topPad + n * 42 + bottomPad,
      step: 42,
      n: n
    };
  }

  function yPx(idx, area) {
    return area.topPad + (area.n - idx - 0.5) * area.step;
  }

  function hexA(hex, a) {
    const n = parseInt(hex.slice(1), 16);
    return 'rgba(' + ((n >> 16) & 255) + ',' + ((n >> 8) & 255) + ',' + (n & 255) + ',' + a + ')';
  }

  root.FretGeom = {
    x: x,
    bandCenter: bandCenter,
    leftBound: leftBound,
    rightBound: rightBound,
    fretFromX: fretFromX,
    yArea: yArea,
    yPx: yPx,
    hexA: hexA
  };
})(typeof self !== 'undefined' ? self : this);
