void updateFov()
{/*ALCODESTART::1778320332801*/
fovCanvas.clear();

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

