void fnMoveToRandomWaypoint()
{/*ALCODESTART::1778000001001*/
setSpeed(Math.max(1, varSpeed));
moveTo(uniform(110, 890), uniform(110, 490));


/*ALCODEEND*/}

void onArrival()
{/*ALCODESTART::1778000001002*/
traceln("UAV[" + getIndex() + "] arrived t=" + (int)time());
if (varUseAcoMove) {
    fnMoveAco();
} else {
    fnMoveToRandomWaypoint();
}

/*ALCODEEND*/}

void fnSenseIfOutdated()
{/*ALCODESTART::1778000001003*/

/*ALCODEEND*/}

void fnSenseNow()
{/*ALCODESTART::1778000001004*/
double bestConf = 0, bestX = -1, bestY = -1;
if (main != null) {
    for (Victims v : main.victims) {
        double d = distanceTo(v);
        if (d <= varSensorRange) {
            double conf = 1.0 - (d / Math.max(1e-6, varSensorRange));
            if (conf > bestConf) { bestConf = conf; bestX = v.getX(); bestY = v.getY(); }
        }
    }
}
varTargetConfidence = bestConf;
varTargetX = bestConf > 0 ? bestX : -1;
varTargetY = bestConf > 0 ? bestY : -1;
varLastSenseTime = time();
varBatteryLevel = Math.max(0, varBatteryLevel - varScanEnergyCost);
varEnergyUsed += varScanEnergyCost;
fnSenseNowGrid();

/*ALCODEEND*/}

int fnCellIndex()
{/*ALCODESTART::1780000003010*/

/*ALCODEEND*/}

double fnCellFreshness()
{/*ALCODESTART::1780000003020*/

/*ALCODEEND*/}

void fnSenseNowGrid()
{/*ALCODESTART::1780000003030*/

/*ALCODEEND*/}

void fnMoveAco()
{/*ALCODESTART::1780000005010*/
int n = Math.max(1, varAcoCandidateCount);
double step = varAcoStepLength;
double[] candX = new double[n];
double[] candY = new double[n];
double[] scores = new double[n];
double totalScore = 0;
for (int i = 0; i < n; i++) {
    double angle = 2.0 * Math.PI * i / n;
    double cx = Math.max(110, Math.min(890, getX() + step * Math.cos(angle)));
    double cy = Math.max(110, Math.min(490, getY() + step * Math.sin(angle)));
    candX[i] = cx; candY[i] = cy;
    int cCol = Math.max(0, Math.min(varGridCols-1, (int)((cx - varGridOriginX) / varGridCellSize)));
    int cRow = Math.max(0, Math.min(varGridRows-1, (int)((cy - varGridOriginY) / varGridCellSize)));
    int idx = cRow * varGridCols + cCol;
    double tau = 0.0;
    if (main != null && idx >= 0 && idx < 200) {
        double pAge = Math.max(0, time() - main.varPheromoneTime[idx]);
        tau = main.varPheromoneGrid[idx] * Math.exp(-main.varPheromoneEvapRate * pAge);
    }
    double tauInv = 1.0 / (1.0 + tau);
    double freshness = 0.0;
    if (idx >= 0 && idx < varGridCols * varGridRows) {
        double t = varCellLastSenseTime[idx];
        if (t > 0) freshness = Math.max(0, 1.0 - (time() - t) / varBeliefStaleSeconds);
    }
    double eta = Math.max(1e-6, 1.0 - freshness);
    double score = Math.pow(tauInv, varAcoAlpha) * Math.pow(eta, varAcoBeta);
    if (main != null) {
        for (UAV other : main.uavs) {
            if (other == this) continue;
            double dx = cx - other.getX(); double dy = cy - other.getY();
            if (Math.sqrt(dx*dx + dy*dy) < varAcoUavAvoidRadius) { score *= 0.1; break; }
        }
    }
    scores[i] = score; totalScore += score;
}
double selX = candX[0], selY = candY[0];
if (totalScore <= 0) {
    int pick = (int)(uniform(0,1) * n); if (pick >= n) pick = n-1;
    selX = candX[pick]; selY = candY[pick];
} else {
    double r = uniform(0, totalScore), cum = 0;
    for (int i = 0; i < n; i++) { cum += scores[i]; if (r <= cum) { selX = candX[i]; selY = candY[i]; break; } }
}
setSpeed(Math.max(1, varSpeed));
moveTo(selX, selY);

/*ALCODEEND*/}

