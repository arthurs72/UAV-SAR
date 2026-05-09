void eventDrawPheromones()
{/*ALCODESTART::1778310007029*/
pheromoneCanvas.clear();

for (int row = 0; row < varGridRows; row++) {
    for (int col = 0; col < varGridCols; col++) {

        int idx = row * varGridCols + col;

        double ph = fnGetPheromone(idx);
        double intensity = Math.min(1.0, ph / 5.0);

        int alpha = (int)(intensity * 200);
        if (alpha < 3) continue;

        java.awt.Color color = new java.awt.Color(0, 220, 0, alpha);

        double px = col * varGridCellSize;   // canvas is already at (varGridOriginX, varGridOriginY)
  		double py = row * varGridCellSize;
  
        pheromoneCanvas.fillRectangle(
            px,
            py,
            varGridCellSize,
            varGridCellSize,
            color
        );
    }
}

double ox = 50;
double oy = 50;
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
/*ALCODEEND*/}

