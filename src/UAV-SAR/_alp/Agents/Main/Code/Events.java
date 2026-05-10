void eventDrawPheromones()
{/*ALCODESTART::1778310007029*/
pheromoneCanvas.clear();

double ox = 50;
double oy = 50;

for (int row = 0; row < varGridRows; row++) {
    for (int col = 0; col < varGridCols; col++) {
        int idx = row * varGridCols + col;
        double ph = fnGetPheromone(idx);
        double intensity = Math.min(1.0, ph / 5.0);
        int alpha = (int)(intensity * 220);
        if (alpha < 5) continue;
        int r, g, b;
        if (intensity < 0.5) {
            float t = (float)(intensity / 0.5);
            r = 255; g = (int)(255 * t); b = 0;
        } else {
            float t = (float)((intensity - 0.5) / 0.5);
            r = (int)(255 * (1 - t)); g = 255; b = 0;
        }
        double px = col * varGridCellSize;
        double py = row * varGridCellSize;
        pheromoneCanvas.fillRectangle(px, py, varGridCellSize, varGridCellSize,
            new java.awt.Color(r, g, b, alpha));
    }
}

for (UAV u : uavs) {
    if (u.varMovingToCharger || u.varCharging) continue;
    int n = Math.min(u.varAcoCandidateCount, u.varCandX.length);
    for (int i = 0; i < n; i++) {
        if (i == u.varChosenIdx) {
            pheromoneCanvas.fillCircle(u.varCandX[i] - ox, u.varCandY[i] - oy, 3,
                new java.awt.Color(255, 50, 50, 220));
        } else {
            pheromoneCanvas.fillCircle(u.varCandX[i] - ox, u.varCandY[i] - oy, 3,
                new java.awt.Color(255, 200, 0, 130));
        }
    }
}

for (int i = 0; i < varFoundVictimCount; i++) {
    double mx = varFoundVictimX[i] - ox;
    double my = varFoundVictimY[i] - oy;
    pheromoneCanvas.fillCircle(mx, my, 12, new java.awt.Color(255, 0, 0, 180));
    pheromoneCanvas.fillCircle(mx, my, 5,  new java.awt.Color(255, 255, 255, 255));
}
/*ALCODEEND*/}

