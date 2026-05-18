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

void fnSenseNow()
{/*ALCODESTART::1778000001004*/
double headingRad = getRotation();
double halfAngleRad = Math.toRadians(varFovHalfAngleDeg);
boolean seen = false;
double bestX = -1;
double bestY = -1;
double bestDist = Double.MAX_VALUE;

if (main != null) {
    for (Victims v : main.victims) {
        if (v.varIsFound) continue;
        double dx = v.getX() - getX();
        double dy = v.getY() - getY();
        double dist = Math.sqrt(dx*dx + dy*dy);
        if (dist > varSensorRange) continue;
        double victimAngle = Math.atan2(dy, dx);
        double diff = Math.abs(Math.atan2(
            Math.sin(headingRad - victimAngle),
            Math.cos(headingRad - victimAngle)
        ));
        if (diff <= halfAngleRad && dist < bestDist) {
    	seen = true;
    	bestX = v.getX();
   	 	bestY = v.getY();
	    bestDist = dist;
		}
    }
}

if (seen) {
    varTargetConfidence = Math.min(1.0, varTargetConfidence + 0.35);
    varTargetX = bestX;
    varTargetY = bestY;
} else {
    varTargetConfidence = Math.max(0.0, varTargetConfidence - 0.25);
    if (varTargetConfidence <= 0.0) {
        varTargetX = -1;
        varTargetY = -1;
    }
}

if (confidenceText != null) {
    confidenceText.setText(varEnableSenseDebug
        ? String.format("c=%.2f", varTargetConfidence)
        : "");
}

if (varEnableSenseDebug) {
    traceln(String.format("UAV %d sense t=%.1f conf=%.2f target=(%.1f,%.1f)",
        getIndex(), time(), varTargetConfidence, varTargetX, varTargetY));
}
/*ALCODEEND*/}

int fnCellIndex(double x,double y)
{/*ALCODESTART::1780000003010*/
int col = (int)((x - main.varGridOriginX) / main.varGridCellSize);
int row = (int)((y - main.varGridOriginY) / main.varGridCellSize);
col = Math.max(0, Math.min(main.varGridCols - 1, col));
row = Math.max(0, Math.min(main.varGridRows - 1, row));
return row * main.varGridCols + col;
/*ALCODEEND*/}

double fnCellFreshness(int idx)
{/*ALCODESTART::1780000003020*/
if (idx < 0 || idx >= main.varGridCols * main.varGridRows) return 0.0;
double t = varCellLastSenseTime[idx];
if (t <= 0) return 0.0;
double age = time() - t;
return Math.max(0.0, 1.0 - age / varBeliefStaleSeconds);
/*ALCODEEND*/}

void fnSenseNowGrid()
{/*ALCODESTART::1780000003030*/
double uavX = getX();
double uavY = getY();
double headingRad = getRotation();
double halfAngleRad = Math.toRadians(varFovHalfAngleDeg);

int colMin = Math.max(0, (int)((uavX - varSensorRange - main.varGridOriginX) / main.varGridCellSize));
int colMax = Math.min(main.varGridCols - 1, (int)((uavX + varSensorRange - main.varGridOriginX) / main.varGridCellSize));
int rowMin = Math.max(0, (int)((uavY - varSensorRange - main.varGridOriginY) / main.varGridCellSize));
int rowMax = Math.min(main.varGridRows - 1, (int)((uavY + varSensorRange - main.varGridOriginY) / main.varGridCellSize));

for (int row = rowMin; row <= rowMax; row++) {
    for (int col = colMin; col <= colMax; col++) {
        double cellCx = main.varGridOriginX + (col + 0.5) * main.varGridCellSize;
        double cellCy = main.varGridOriginY + (row + 0.5) * main.varGridCellSize;
        double dx = cellCx - uavX;
        double dy = cellCy - uavY;
        double dist = Math.sqrt(dx*dx + dy*dy);
        if (dist > varSensorRange) continue;

        double cellAngle = Math.atan2(dy, dx);
        double diff = Math.abs(Math.atan2(
            Math.sin(headingRad - cellAngle),
            Math.cos(headingRad - cellAngle)
        ));
        if (diff > halfAngleRad) continue;

        int idx = row * main.varGridCols + col;
        varCellLastSenseTime[idx] = time();
        main.fnDepositPheromone(idx, varPheromoneDepositionRate);

        double bestConf = 0;
        for (Victims v : main.victims) {
            if (v.varIsFound) continue;
            double vdx = v.getX() - cellCx;
            double vdy = v.getY() - cellCy;
            if (Math.sqrt(vdx*vdx + vdy*vdy) <= main.varGridCellSize * 0.7) {
                bestConf = 1.0;
                break;
            }
        }
        varCellConfidence[idx] = bestConf;
    }
}
/*ALCODEEND*/}

void fnPickWaypoint()
{/*ALCODESTART::1780000005010*/
traceln("UAV " + getIndex() + " fnPickWaypoint: returnFlag=" + varReturnToLastLocation +
    ", lastCritical=(" + varLastCriticalX + "," + varLastCriticalY + 
    "), localSearch=" + varLocalSearchMode);
int n = Math.max(1, varAcoCandidateCount);
double step = varAcoStepLength;

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
}

// PRIORITY 1: Structured 360° orbit scan — 8 points, 45° apart
if (varLocalSearchMode && varLocalSearchCenterX >= 0) {
    double scanRadius = 80.0;
    int stepIndex = 8 - varLocalSearchStepsLeft;
    double angle = stepIndex * (2.0 * Math.PI / 8.0);
    double tx = Math.max(minX, Math.min(maxX,
        varLocalSearchCenterX + scanRadius * Math.cos(angle)));
    double ty = Math.max(minY, Math.min(maxY,
        varLocalSearchCenterY + scanRadius * Math.sin(angle)));
    varCandX[0] = tx;
    varCandY[0] = ty;
    varChosenIdx = 0;
    varDesiredHeadingDeg = Math.toDegrees(Math.atan2(ty - getY(), tx - getX()));
    varLocalSearchStepsLeft--;
	if (varLocalSearchStepsLeft <= 0) {
    varLocalSearchMode = false;
    	if (varQueuedTargetX >= 0) {
        	varTargetX      = varQueuedTargetX;
        	varTargetY      = varQueuedTargetY;
        	varTargetConfidence = 1.0;
        	varQueuedTargetX = -1;
        	varQueuedTargetY = -1;
    	}
	}	
    return;
}

// PRIORITY 2: Return to last critical location (no victim spotted en route home)
if (varReturnToLastLocation && varLastCriticalX >= 0) {
    double distToLast = Math.sqrt(
        (getX() - varLastCriticalX) * (getX() - varLastCriticalX) +
        (getY() - varLastCriticalY) * (getY() - varLastCriticalY)
    );
    if (distToLast > 50) {
        double tx = Math.max(minX, Math.min(maxX, varLastCriticalX));
        double ty = Math.max(minY, Math.min(maxY, varLastCriticalY));
        varCandX[0] = tx;
        varCandY[0] = ty;
        varChosenIdx = 0;
        varDesiredHeadingDeg = Math.toDegrees(Math.atan2(
            varLastCriticalY - getY(),
            varLastCriticalX - getX()
        ));
        return;
    } else {
        varReturnToLastLocation = false;
    }
}

// PRIORITY 3: Normal ACO with optional local search boost
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
    double tau = (main != null) ? main.fnGetPheromone(idx) : 0.0;
    double tauInv = 1.0 / (1.0 + tau);
    double eta = Math.max(1e-6, 1.0 - fnCellFreshness(idx));
    double score = Math.pow(tauInv, varAcoAlpha) * Math.pow(eta, varAcoBeta);

    if (varLocalSearchMode && varLocalSearchCenterX >= 0) {
        double distCand = Math.sqrt(
            (cx - varLocalSearchCenterX) * (cx - varLocalSearchCenterX) +
            (cy - varLocalSearchCenterY) * (cy - varLocalSearchCenterY)
        );
        if (distCand <= varLocalSearchRadius) {
            score *= 5.0;
        }
    }

    if (main != null) {
        for (UAV other : main.uavs) {
            if (other == this) continue;
            double dx = cx - other.getX();
            double dy = cy - other.getY();
            if (Math.sqrt(dx*dx + dy*dy) < varAcoUavAvoidRadius) { score *= 0.1; break; }
        }
    }
    scores[i] = score;
    totalScore += score;
}

int chosenIdx = 0;
if (totalScore <= 0) {
    chosenIdx = (int) uniform(0, n);
} else {
    double r = uniform(0, totalScore);
    double cum = 0;
    for (int i = 0; i < n; i++) {
        cum += scores[i];
        if (r <= cum) { chosenIdx = i; break; }
    }
}

varAcoCandidateCount = n;
varChosenIdx = chosenIdx;
for (int i = 0; i < n && i < varCandX.length; i++) {
    varCandX[i] = candX[i];
    varCandY[i] = candY[i];
}

varDesiredHeadingDeg = Math.toDegrees(Math.atan2(
    varCandY[varChosenIdx] - getY(),
    varCandX[varChosenIdx] - getX()
));
/*ALCODEEND*/}

double function()
{/*ALCODESTART::1778331635115*/

/*ALCODEEND*/}

double fnConfirmVictim()
{/*ALCODESTART::1778423980875*/
if (varTargetX >= 0 && varTargetY >= 0) {
    double dist = Math.hypot(getX() - varTargetX, getY() - varTargetY);
    if (dist > 30.0) {
    moveTo(varTargetX, varTargetY, varSpeed);
    return;
	}
    main.fnMarkVictimFound(varTargetX, varTargetY, getIndex());
    varVictimsConfirmed++;
    varLocalSearchMode = true;
    varLocalSearchCenterX = varTargetX;
    varLocalSearchCenterY = varTargetY;
    varLocalSearchStepsLeft = 8;
    varReturnToLastLocation = false;
}
varTargetConfidence = 0.0;
varTargetX = -1;
varTargetY = -1;
/*ALCODEEND*/}

