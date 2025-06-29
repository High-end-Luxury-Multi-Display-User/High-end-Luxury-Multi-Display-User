#!/bin/bash

set -e  # Exit immediately if a command fails

BASE_DIR="$(pwd)"  # Save the directory where you started

echo "Applying patches..."

# Apply vhal_map.patch
echo "Applying vhal_map.patch..."
cd "$BASE_DIR/packages/services/Car/service/src/com/android/car/hal/fakevhal"
git apply vhal_map.patch
echo "vhal_map.patch applied."

# Return to base directory
cd "$BASE_DIR"

# Apply vhal_v3.patch
echo "Applying vhal_v3.patch..."
cd "$BASE_DIR/hardware/interfaces/automotive/vehicle/aidl/impl/3"
git apply vhal_v3.patch
echo "vhal_v3.patch applied."

# Return to base directory
cd "$BASE_DIR"

# Apply vhal_curr.patch
echo "Applying vhal_curr.patch..."
cd "$BASE_DIR/hardware/interfaces/automotive/vehicle/aidl/impl/current"
git apply vhal_curr.patch
echo "vhal_curr.patch applied."

echo "All patches applied successfully!"

