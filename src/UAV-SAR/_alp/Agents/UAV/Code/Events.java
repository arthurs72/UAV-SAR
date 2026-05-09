void updateFov()
{/*ALCODESTART::1778320332801*/
fovCanvas.clear();

// FOV origin inside the canvas
double cx = 150;
double cy = 150;

// FOV geometry
double rMax = varSensorRange;
double centerAngle = 0;   // 0 rad = pointing right
double halfRad = Math.toRadians(varFovHalfAngleDeg);

// Colors
boolean detected = varTargetConfidence >= varDetectionThreshold;

java.awt.Color fill = detected
    ? new java.awt.Color(255, 140, 0, 80)
    : new java.awt.Color(100, 200, 255, 60);

java.awt.Color outline = detected
    ? new java.awt.Color(255, 100, 0, 200)
    : new java.awt.Color(50, 150, 255, 180);

// Drawing resolution
double angleStep = Math.toRadians(2);
double radiusStep = 2;
double dotRadius = 2;

// Fill the FOV slice
for (double a = centerAngle - halfRad; a <= centerAngle + halfRad; a += angleStep) {
    for (double r = 0; r <= rMax; r += radiusStep) {
        double x = cx + r * Math.cos(a);
        double y = cy + r * Math.sin(a);
        fovCanvas.fillCircle(x, y, dotRadius, fill);
    }
}

// Draw the two boundary rays
double topAngle = centerAngle - halfRad;
double bottomAngle = centerAngle + halfRad;

for (double r = 0; r <= rMax; r += 2) {
    fovCanvas.fillCircle(
        cx + r * Math.cos(topAngle),
        cy + r * Math.sin(topAngle),
        2,
        outline
    );

    fovCanvas.fillCircle(
        cx + r * Math.cos(bottomAngle),
        cy + r * Math.sin(bottomAngle),
        2,
        outline
    );
}

// Draw the outer arc
for (double a = topAngle; a <= bottomAngle; a += Math.toRadians(1)) {
    fovCanvas.fillCircle(
        cx + rMax * Math.cos(a),
        cy + rMax * Math.sin(a),
        2,
        outline
    );
}

// Simple heading label
if (angleText != null) {
    angleText.setText(varEnableSenseDebug
        ? String.format("h=%.0f", varDesiredHeadingDeg)
        : "");
}

if (confidenceText != null) {
    confidenceText.setText(varEnableSenseDebug
        ? String.format("c=%.2f", varTargetConfidence)
        : "");
}
/*ALCODEEND*/}

