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
if (idx < 0 || idx >= 200) return 0.0;
double age = Math.max(0, time() - varPheromoneTime[idx]);
return varPheromoneGrid[idx] * Math.exp(-varPheromoneEvapRate * age);
/*ALCODEEND*/}

void fnDepositPheromone(int idx,double amount)
{/*ALCODESTART::1780000004002*/
if (idx < 0 || idx >= 200) return;
double age = Math.max(0, time() - varPheromoneTime[idx]);
double current = varPheromoneGrid[idx] * Math.exp(-varPheromoneEvapRate * age);
varPheromoneGrid[idx] = current + amount;
varPheromoneTime[idx] = time();
/*ALCODEEND*/}

