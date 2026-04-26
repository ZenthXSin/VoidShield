// Updated updateLink() method to clear linkBuild first.
private fun updateLink() {
    linkBuild.clear() // Clear linkBuild before updating.
    // ... rest of the method implementation
}

// Added remove() method to clear linkBuild when removed.
private fun remove() {
    linkBuild.clear() // Clear linkBuild upon removal.
}

// Added heatSum protection against division by zero.
private fun calculateHeat() {
    if (heatSum != 0) {
        val result = totalHeat / heatSum
        // ... further calculations
    } else {
        // Handle division by zero situation
    }
}