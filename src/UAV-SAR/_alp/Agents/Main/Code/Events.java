void eventDrawPheromones()
{/*ALCODESTART::1778310007029*/
if (pheromoneCanvas == null) return;
pheromoneCanvas.clear();

double ox = 50;
double oy = 50;

// 1. Pheromone grid tiles
for (int row = 0; row < varGridRows; row++) {
    for (int col = 0; col < varGridCols; col++) {
        int idx = row * varGridCols + col;
        double ph = fnGetPheromone(idx);
        double intensity = Math.min(1.0, ph / 5.0);
        int alpha = (int)(intensity * 220);
        if (alpha < 5) continue;
        int r, g, b;
        int[][] stops = {
    	{255,  80,  0},   // intensity 0.00 — dark orange
    	{255, 140,  0},   // intensity 0.17 — orange
    	{255, 200,  0},   // intensity 0.33 — amber
    	{255, 255,  0},   // intensity 0.50 — yellow
    	{180, 255,  0},   // intensity 0.67 — yellow-green
    	{ 80, 240,  0},   // intensity 0.83 — light green
    	{  0, 200,  0}    // intensity 1.00 — green
		};
		int seg = Math.min((int)(intensity * 6), 5);
		float t  = (float)(intensity * 6 - seg);
		r = stops[seg][0] + (int)(t * (stops[seg+1][0] - stops[seg][0]));
		g = stops[seg][1] + (int)(t * (stops[seg+1][1] - stops[seg][1]));
		b = stops[seg][2] + (int)(t * (stops[seg+1][2] - stops[seg][2]));
        double px = col * varGridCellSize;
        double py = row * varGridCellSize;
        pheromoneCanvas.fillRectangle(px, py, varGridCellSize, varGridCellSize,
            new java.awt.Color(r, g, b, alpha));
    }
}

// 2. FOV cones — drawn in Main's coordinate space, scale matches sensing exactly
for (UAV u : uavs) {
    if (u.varMovingToCharger || u.varCharging) continue;
 
    double cx          = u.getX() - ox;
    double cy          = u.getY() - oy;
    double centerAngle = u.getRotation();
    double halfRad     = Math.toRadians(u.varFovHalfAngleDeg);
    double rMax        = u.varSensorRange;

    boolean detected = u.varTargetConfidence >= u.varDetectionThreshold;
    java.awt.Color fill = detected
        ? new java.awt.Color(255, 140, 0, 60)
        : new java.awt.Color(100, 200, 255, 50);
    java.awt.Color outline = detected
        ? new java.awt.Color(255, 100, 0, 180)
        : new java.awt.Color(50, 150, 255, 160);

    // Fill slice
    for (double a = centerAngle - halfRad; a <= centerAngle + halfRad; a += Math.toRadians(2)) {
        for (double r = 0; r <= rMax; r += 2) {
            pheromoneCanvas.fillCircle(cx + r * Math.cos(a), cy + r * Math.sin(a), 2, fill);
        }
    }
    // Boundary rays
    for (double r = 0; r <= rMax; r += 2) {
        pheromoneCanvas.fillCircle(cx + r * Math.cos(centerAngle - halfRad),
                                   cy + r * Math.sin(centerAngle - halfRad), 2, outline);
        pheromoneCanvas.fillCircle(cx + r * Math.cos(centerAngle + halfRad),
                                   cy + r * Math.sin(centerAngle + halfRad), 2, outline);
    }
    // Outer arc
    for (double a = centerAngle - halfRad; a <= centerAngle + halfRad; a += Math.toRadians(1)) {
        pheromoneCanvas.fillCircle(cx + rMax * Math.cos(a), cy + rMax * Math.sin(a), 2, outline);
    }
}

// 3. Candidate waypoints
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

// 4. Found victim markers — red X
for (Victims v : victims) {
    if (!v.varIsFound) continue;
    double mx = v.getX() - ox;
    double my = v.getY() - oy;
    java.awt.Color xColor = new java.awt.Color(220, 0, 0, 255);
    for (int d = -10; d <= 10; d++) {
        pheromoneCanvas.fillCircle(mx + d, my + d, 2, xColor);
        pheromoneCanvas.fillCircle(mx + d, my - d, 2, xColor);
    }
}
/*ALCODEEND*/}

void eventSafetyStop()
{/*ALCODESTART::1778824420442*/
if (!varAllVictimsFound) {
    varConvergenceTime = Double.NaN;
    traceln("TIMEOUT not all victims found by t=" + time()
        + " found=" + varFoundVictimCount + "/" + victims.size());
    fnLogRunSummary();
    finishSimulation();
}
/*ALCODEEND*/}

