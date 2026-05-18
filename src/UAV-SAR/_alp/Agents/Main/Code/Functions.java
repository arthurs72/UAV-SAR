void fnPlaceVictimsGMM()
{/*ALCODESTART::1777049001651*/
int nClusters = Math.max(1, varNClusters);
double margin = Math.max(0, varGmmMargin);

double minX = spaceRelease.getX();
double maxX = spaceRelease.getX() + spaceRelease.getWidth();
double minY = spaceRelease.getY();
double maxY = spaceRelease.getY() + spaceRelease.getHeight();

double centerMinX = Math.min(minX + margin, maxX);
double centerMaxX = Math.max(minX, maxX - margin);
double centerMinY = Math.min(minY + margin, maxY);
double centerMaxY = Math.max(minY, maxY - margin);

// choose cluster centers once per run
double[][] centers = new double[nClusters][2];
for (int i = 0; i < nClusters; i++) {
  centers[i][0] = uniform(centerMinX, centerMaxX);
  centers[i][1] = uniform(centerMinY, centerMaxY);
  traceln("Cluster " + i + " center = " + centers[i][0] + ", " + centers[i][1]);
}

// Normalize available weights to avoid malformed configurations.
int usableClusters = Math.min(nClusters, varGmmWeights.length);
if (usableClusters <= 0) {
  usableClusters = nClusters;
}

double[] normalizedWeights = new double[usableClusters];
double sumWeights = 0;
for (int i = 0; i < usableClusters; i++) {
  double w = i < varGmmWeights.length ? Math.max(0, varGmmWeights[i]) : 1.0;
  normalizedWeights[i] = w;
  sumWeights += w;
}
if (sumWeights <= 0) {
  for (int i = 0; i < usableClusters; i++) {
    normalizedWeights[i] = 1.0 / usableClusters;
  }
} else {
  for (int i = 0; i < usableClusters; i++) {
    normalizedWeights[i] = normalizedWeights[i] / sumWeights;
  }
}

// place victims with in-bounds rejection sampling
for (Victims v : victims) {
  double u = uniform(0, 1);
  int k = 0;
  double cumulative = 0;
  for (int i = 0; i < usableClusters; i++) {
    cumulative += normalizedWeights[i];
    if (u <= cumulative) {
      k = i;
      break;
    }
  }

  // AnyLogic normal(stdDev, mean)
  double stdX = (k < varGmmSd.length && varGmmSd[k].length > 0) ? Math.max(1e-6, varGmmSd[k][0]) : 40;
  double stdY = (k < varGmmSd.length && varGmmSd[k].length > 1) ? Math.max(1e-6, varGmmSd[k][1]) : 40;

  double x = centers[k][0];
  double y = centers[k][1];
  boolean placed = false;

  int maxAttempts = 200;
  for (int attempt = 0; attempt < maxAttempts; attempt++) {
    double candidateX = normal(stdX, centers[k][0]);
    double candidateY = normal(stdY, centers[k][1]);
    boolean inBounds = candidateX >= minX && candidateX <= maxX && candidateY >= minY && candidateY <= maxY;
    if (inBounds) {
      x = candidateX;
      y = candidateY;
      placed = true;
      break;
    }
  }

  if (!placed) {
    // Guaranteed fallback to keep simulation valid even for extreme SD settings.
    x = uniform(minX, maxX);
    y = uniform(minY, maxY);
    traceln("Warning: fallback uniform placement used for victim due to repeated out-of-bounds samples.");
  }

  v.setXY(x, y);
  traceln("Victim placed in cluster " + k + " at " + x + ", " + y);
}
/*ALCODEEND*/}

double fnGetPheromone(int idx)
{/*ALCODESTART::1780000004001*/
if (idx < 0 || idx >= varGridCols * varGridRows) return 0.0;
double age = Math.max(0, time() - varPheromoneTime[idx]);
return varPheromoneGrid[idx] * Math.exp(-varPheromoneEvapRate * age);
/*ALCODEEND*/}

void fnDepositPheromone(int idx,double amount)
{/*ALCODESTART::1780000004002*/
if (idx < 0 || idx >= varGridCols * varGridRows) return;
double age = Math.max(0, time() - varPheromoneTime[idx]);
double current = varPheromoneGrid[idx] * Math.exp(-varPheromoneEvapRate * age);
varPheromoneGrid[idx] = current + amount;
varPheromoneTime[idx] = time();
/*ALCODEEND*/}

void fnMarkVictimFound(double x,double y,int uavIndex)
{/*ALCODESTART::1778423584943*/
// Skip if we already tagged a victim near this location
for (Victims v : victims) {
    if (!v.varIsFound) continue;
    double dx = v.getX() - x;
    double dy = v.getY() - y;
    if (Math.sqrt(dx*dx + dy*dy) < 15) return;
}

// Find the nearest unfound victim to tag
Victims found = null;
double bestDist = Double.MAX_VALUE;
for (Victims v : victims) {
    if (v.varIsFound) continue;
    double dx = v.getX() - x;
    double dy = v.getY() - y;
    double dist = Math.sqrt(dx*dx + dy*dy);
    if (dist < bestDist) {
        bestDist = dist;
        found = v;
    }
}
if (found != null) {
    found.varIsFound = true;
    found.varFoundByUavIndex = uavIndex;
    found.varFoundAtTime = time();
    varFoundVictimCount++;
    traceln("VICTIM_FOUND victimIdx=" + found.getIndex()
        + " uavIdx=" + uavIndex
        + " time=" + time()
        + " x=" + found.getX() + " y=" + found.getY());
    fnLogVictimFound(found.getIndex(), uavIndex, time(), found.getX(), found.getY());
}

// Reset pheromone in radius around found victim
double radius = 95;
for (int row = 0; row < varGridRows; row++) {
    for (int col = 0; col < varGridCols; col++) {
        double cellCx = varGridOriginX + (col + 0.5) * varGridCellSize;
        double cellCy = varGridOriginY + (row + 0.5) * varGridCellSize;
        double dist = Math.sqrt((cellCx - x)*(cellCx - x) + (cellCy - y)*(cellCy - y));
        if (dist <= radius) {
            int idx = row * varGridCols + col;
            varPheromoneGrid[idx] = 0;
            varPheromoneTime[idx] = time();
        }
    }
}

// Convergence: all victims found
if (!varAllVictimsFound && varFoundVictimCount >= victims.size()) {
    varAllVictimsFound = true;
    varConvergenceTime = time();
    traceln("CONVERGENCE allVictimsFound=true convergenceTime=" + varConvergenceTime);
    fnLogRunSummary();
    finishSimulation();
}
/*ALCODEEND*/}

double fnLogVictimFound(int victimIdx,int uavIdx,double foundTime,double x,double y)
{/*ALCODESTART::1778831293480*/
try {
    String expDir = System.getProperty("uavsar.exp.dir", "experiments");
    java.io.File f = new java.io.File(expDir + "/victim_log.csv");

    // Synchronize on the global System class to prevent multi-thread write overlaps
    synchronized(System.class) {
        java.io.FileWriter fw = new java.io.FileWriter(f, true);
        fw.write(varSeed + "," + varSensorRange + "," + varAcoCandidateCount + ","
            + varAcoStepLength + "," + varAcoAlpha + "," + varAcoBeta + ","
            + victimIdx + "," + uavIdx + "," + foundTime + "," + x + "," + y + "\n");
        fw.close();
    }
  } catch (Exception e) {
    traceln("victim_log write error: " + e.getMessage());
  }
/*ALCODEEND*/}

double fnLogRunSummary()
{/*ALCODESTART::1778831401084*/
try {
    String expDir = System.getProperty("uavsar.exp.dir", "experiments");
    java.io.File f = new java.io.File(expDir + "/run_summary.csv");

    // Synchronize on a global object class to safe-lock multi-thread writes
    synchronized(System.class) {
        java.io.FileWriter fw = new java.io.FileWriter(f, true);
        fw.write(varSeed + "," + varSensorRange + "," + varAcoCandidateCount + ","
            + varAcoStepLength + "," + varAcoAlpha + "," + varAcoBeta + ","
            + varAllVictimsFound + "," + varConvergenceTime + "," + varFoundVictimCount + "\n");
        fw.close();
    }
  } catch (Exception e) {
    traceln("run_summary write error: " + e.getMessage());
  }
/*ALCODEEND*/}

