void fnMoveToRandomWaypoint()
{/*ALCODESTART::1778000001001*/
double margin = 10;
double minX = 100 + margin;
double maxX = 900 - margin;
double minY = 100 + margin;
double maxY = 500 - margin;

if (main != null && main.spaceRelease != null) {
	minX = main.spaceRelease.getX() + margin;
	maxX = main.spaceRelease.getX() + main.spaceRelease.getWidth() - margin;
	minY = main.spaceRelease.getY() + margin;
	maxY = main.spaceRelease.getY() + main.spaceRelease.getHeight() - margin;
	if (maxX < minX || maxY < minY) {
		minX = 100 + margin;
		maxX = 900 - margin;
		minY = 100 + margin;
		maxY = 500 - margin;
	}
}

double x = uniform(minX, maxX);
double y = uniform(minY, maxY);

if (main != null && varAcoMinUavSeparation > 0) {
	int attempts = 30;
	for (int attempt = 0; attempt < attempts; attempt++) {
		boolean ok = true;
		for (UAV other : main.uavs) {
			if (other == this) {
				continue;
			}
			double dist = Math.hypot(x - other.getX(), y - other.getY());
			if (dist < varAcoMinUavSeparation) {
				ok = false;
				break;
			}
		}
		if (ok) {
			break;
		}
		x = uniform(minX, maxX);
		y = uniform(minY, maxY);
	}
}

setSpeed(Math.max(0.1, varSpeed));
moveTo(x, y, Math.max(0.1, varSpeed));
varDesiredHeadingDeg = Math.toDegrees(Math.atan2(y - getY(), x - getX()));
/*ALCODEEND*/}

void fnMoveToAcoWaypoint()
{/*ALCODESTART::1778000001005*/
double margin = 10;
double minX = 100 + margin;
double maxX = 900 - margin;
double minY = 100 + margin;
double maxY = 500 - margin;

if (main != null && main.spaceRelease != null) {
	minX = main.spaceRelease.getX() + margin;
	maxX = main.spaceRelease.getX() + main.spaceRelease.getWidth() - margin;
	minY = main.spaceRelease.getY() + margin;
	maxY = main.spaceRelease.getY() + main.spaceRelease.getHeight() - margin;
	if (maxX < minX || maxY < minY) {
		minX = 100 + margin;
		maxX = 900 - margin;
		minY = 100 + margin;
		maxY = 500 - margin;
	}
}

int candidates = Math.max(4, varAcoCandidateCount);
double step = Math.max(5, varAcoStepLength);
double desiredRad = Math.toRadians(varDesiredHeadingDeg);

boolean found = false;
double bestScore = -1e9;
double bestX = getX();
double bestY = getY();
double bestAngle = desiredRad;

for (int i = 0; i < candidates; i++) {
	double angle = uniform(0, 2 * Math.PI);
	double candX = getX() + step * Math.cos(angle);
	double candY = getY() + step * Math.sin(angle);

	if (candX < minX || candX > maxX || candY < minY || candY > maxY) {
		continue;
	}

	double score = varRandomWeight * uniform(0, 1);

	if (varTargetConfidence > 0 && varTargetX >= 0 && varTargetY >= 0) {
		double dx = varTargetX - getX();
		double dy = varTargetY - getY();
		double dist = Math.hypot(dx, dy);
		if (dist > 1e-6) {
			double dirScore = (dx / dist) * Math.cos(angle) + (dy / dist) * Math.sin(angle);
			score += varHumanPullRate * varTargetConfidence * Math.max(0, dirScore);
		}
	}

	if (main != null) {
		double minDist = Double.POSITIVE_INFINITY;
		boolean tooClose = false;
		double startX = getX();
		double startY = getY();
		double segDx = candX - startX;
		double segDy = candY - startY;

		for (UAV other : main.uavs) {
			if (other == this) {
				continue;
			}
			double otherStartX = other.getX();
			double otherStartY = other.getY();
			double otherStep = Math.max(5, other.varAcoStepLength);
			double otherRad = Math.toRadians(other.varDesiredHeadingDeg);
			double otherEndX = otherStartX + otherStep * Math.cos(otherRad);
			double otherEndY = otherStartY + otherStep * Math.sin(otherRad);

			double dxEnd = candX - otherStartX;
			double dyEnd = candY - otherStartY;
			double distEnd = Math.hypot(dxEnd, dyEnd);
			if (distEnd < minDist) {
				minDist = distEnd;
			}

			if (varAcoMinUavSeparation > 0) {
				double relX = startX - otherStartX;
				double relY = startY - otherStartY;
				double relDx = segDx - (otherEndX - otherStartX);
				double relDy = segDy - (otherEndY - otherStartY);
				double denom = (relDx * relDx) + (relDy * relDy);
				double t = 0;
				if (denom > 1e-9) {
					t = -((relX * relDx + relY * relDy) / denom);
					t = Math.max(0, Math.min(1, t));
				}
				double closestX = relX + t * relDx;
				double closestY = relY + t * relDy;
				double distMoving = Math.hypot(closestX, closestY);
				if (distMoving < varAcoMinUavSeparation) {
					tooClose = true;
					break;
				}
			}
		}

		if (tooClose) {
			continue;
		}
		if (minDist < Double.POSITIVE_INFINITY) {
			double avoid = Math.max(0, (varAcoUavAvoidRadius - minDist) / Math.max(1e-6, varAcoUavAvoidRadius));
			score -= varUavPushRate * avoid;
		}
	}

	double headingScore = Math.max(0, Math.cos(angle - desiredRad));
	score += varPheromoneDepositionRate * headingScore;

	if (!found || score > bestScore) {
		found = true;
		bestScore = score;
		bestX = candX;
		bestY = candY;
		bestAngle = angle;
	}
}

if (!found) {
	fnMoveToRandomWaypoint();
	return;
}

setSpeed(Math.max(0.1, varSpeed));
moveTo(bestX, bestY, Math.max(0.1, varSpeed));
varDesiredHeadingDeg = Math.toDegrees(bestAngle);
/*ALCODEEND*/}

void onArrival()
{/*ALCODESTART::1778000001002*/
fnSenseIfOutdated();
if (varUseAcoMove) {
	fnMoveToAcoWaypoint();
} else {
	fnMoveToRandomWaypoint();
}
/*ALCODEEND*/}

void fnSenseIfOutdated()
{/*ALCODESTART::1778000001003*/
double now = time();
double elapsed = Math.max(0, now - varLastBeliefUpdateTime);
if (elapsed > 0) {
	varTargetConfidence = Math.max(0, varTargetConfidence - (varBeliefDecayPerSecond * elapsed));
	varLastBeliefUpdateTime = now;
}

if (confidenceText != null) {
	confidenceText.setText(varEnableSenseDebug
		? String.format("c=%.2f", varTargetConfidence)
		: "");
}

boolean isStale = (varLastSenseTime < 0) || ((now - varLastSenseTime) >= varBeliefStaleSeconds);
if (!isStale) {
	return;
}

fnSenseNow();
/*ALCODEEND*/}

void fnSenseNow()
{/*ALCODESTART::1778000001004*/
double bestConfidence = 0;
double bestX = -1;
double bestY = -1;

if (main != null) {
	for (Victims v : main.victims) {
		double d = distanceTo(v);
		if (d <= varSensorRange) {
			double conf = 1.0 - (d / Math.max(1e-6, varSensorRange));
			if (conf > bestConfidence) {
				bestConfidence = conf;
				bestX = v.getX();
				bestY = v.getY();
			}
		}
	}
}

varTargetConfidence = bestConfidence;
if (bestConfidence > 0) {
	varTargetX = bestX;
	varTargetY = bestY;
} else {
	varTargetX = -1;
	varTargetY = -1;
}

varLastSenseTime = time();
varBatteryLevel = Math.max(0, varBatteryLevel - varScanEnergyCost);
varEnergyUsed += varScanEnergyCost;

if (confidenceText != null) {
	confidenceText.setText(varEnableSenseDebug
		? String.format("c=%.2f", varTargetConfidence)
		: "");
}

if (varEnableSenseDebug) {
	traceln(String.format("UAV %d sense t=%.1f conf=%.2f target=(%.1f, %.1f)",
		getIndex(), time(), varTargetConfidence, varTargetX, varTargetY));
}
/*ALCODEEND*/}

int fnCellIndex(double x,double y)
{/*ALCODESTART::1780000003010*/
int col = (int)((x - varGridOriginX) / varGridCellSize);
int row = (int)((y - varGridOriginY) / varGridCellSize);
col = Math.max(0, Math.min(varGridCols - 1, col));
row = Math.max(0, Math.min(varGridRows - 1, row));
return row * varGridCols + col;
/*ALCODEEND*/}

double fnCellFreshness(int idx)
{/*ALCODESTART::1780000003020*/
if (idx < 0 || idx >= varGridCols * varGridRows) return 0.0;
double t = varCellLastSenseTime[idx];
if (t < 0) return 0.0;
double age = time() - t;
return Math.max(0.0, 1.0 - age / varBeliefStaleSeconds);
/*ALCODEEND*/}

void fnSenseNowGrid()
{/*ALCODESTART::1780000003030*/
double uavX = getX();
double uavY = getY();
int colMin = Math.max(0, (int)((uavX - varSensorRange - varGridOriginX) / varGridCellSize));
int colMax = Math.min(varGridCols - 1, (int)((uavX + varSensorRange - varGridOriginX) / varGridCellSize));
int rowMin = Math.max(0, (int)((uavY - varSensorRange - varGridOriginY) / varGridCellSize));
int rowMax = Math.min(varGridRows - 1, (int)((uavY + varSensorRange - varGridOriginY) / varGridCellSize));
for (int row = rowMin; row <= rowMax; row++) {
    for (int col = colMin; col <= colMax; col++) {
        double cellCx = varGridOriginX + (col + 0.5) * varGridCellSize;
        double cellCy = varGridOriginY + (row + 0.5) * varGridCellSize;
        double dx = uavX - cellCx;
        double dy = uavY - cellCy;
        if (Math.sqrt(dx*dx + dy*dy) > varSensorRange) continue;
        int idx = row * varGridCols + col;
        varCellLastSenseTime[idx] = time();
        if (main != null) {
            main.fnDepositPheromone(idx, varPheromoneDepositionRate);
        }
        double bestConf = 0;
        if (main != null) {
            for (Victims v : main.victims) {
                double vdx = v.getX() - cellCx;
                double vdy = v.getY() - cellCy;
                if (Math.sqrt(vdx*vdx + vdy*vdy) <= varGridCellSize * 0.7) {
                    double conf = 1.0 - (Math.sqrt(dx*dx + dy*dy) / Math.max(1e-6, varSensorRange));
                    if (conf > bestConf) bestConf = conf;
                }
            }
        }
        varCellConfidence[idx] = bestConf;
    }
}
/*ALCODEEND*/}

void fnMoveAco()
{/*ALCODESTART::1780000005010*/
int n = Math.max(1, varAcoCandidateCount);
double step = varAcoStepLength;
double minX = 110, maxX = 890, minY = 110, maxY = 490;
 
double[] candX = new double[n];
double[] candY = new double[n];
double[] scores = new double[n];
double totalScore = 0;

for (int i = 0; i < n; i++) {
    double angle = 2.0 * Math.PI * i / n;
    double cx = Math.max(minX, Math.min(maxX, getX() + step * Math.cos(angle)));
    double cy = Math.max(minY, Math.min(maxY, getY() + step * Math.sin(angle)));
    candX[i] = cx;
    candY[i] = cy;

    int idx = fnCellIndex(cx, cy);

    // τ_inv: low pheromone = unexplored = high attraction
    double tau = (main != null) ? main.fnGetPheromone(idx) : 0.0;
    double tauInv = 1.0 / (1.0 + tau);

    // η: staleness = 1 - freshness; stale cells attract exploration
    double eta = Math.max(1e-6, 1.0 - fnCellFreshness(idx));

    double score = Math.pow(tauInv, varAcoAlpha) * Math.pow(eta, varAcoBeta);

    // penalize candidates too close to other UAVs
    if (main != null) {
        for (UAV other : main.uavs) {
            if (other == this) continue;
            double dx = cx - other.getX();
            double dy = cy - other.getY();
            if (Math.sqrt(dx*dx + dy*dy) < varAcoUavAvoidRadius) {
                score *= 0.1;
                break;
            }
        }
    }

    scores[i] = score;
    totalScore += score;
}

// roulette-wheel selection
double selX = candX[0];
double selY = candY[0];
if (totalScore <= 0) {
    int pick = (int) uniform(0, n);
    selX = candX[pick];
    selY = candY[pick];
} else {
    double r = uniform(0, totalScore);
    double cum = 0;
    for (int i = 0; i < n; i++) {
        cum += scores[i];
        if (r <= cum) {
            selX = candX[i];
            selY = candY[i];
            break;
        }
    }
}

setSpeed(Math.max(0.1, varSpeed));
moveTo(selX, selY, Math.max(0.1, varSpeed));
/*ALCODEEND*/}

