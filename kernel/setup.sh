#!/bin/sh
set -eu

GKI_ROOT=$(pwd)
KAMISU_REPO="kaminarich/KamiSU"
BASE_OWNER="pershoot"
BASE_REPO="KernelSU-Next"
BASE_BRANCH="dev-susfs"

display_usage() {
    echo "Usage: $0 [--cleanup | <commit-or-tag>]"
    echo "  --cleanup:       Cleans up previous modifications made by the script."
    echo "  <commit-or-tag>: Sets up or updates KamiSU to specified tag or commit."
    echo "  -h, --help:      Displays this usage information."
    echo "  (no args):       Sets up or updates KamiSU to the latest tagged version."
}

initialize_variables() {
    if test -d "$GKI_ROOT/common/drivers"; then
        DRIVER_DIR="$GKI_ROOT/common/drivers"
    elif test -d "$GKI_ROOT/drivers"; then
        DRIVER_DIR="$GKI_ROOT/drivers"
    else
        echo '[ERROR] "drivers/" directory not found.'
        exit 127
    fi

    DRIVER_MAKEFILE=$DRIVER_DIR/Makefile
    DRIVER_KCONFIG=$DRIVER_DIR/Kconfig
}

perform_cleanup() {
    echo "[+] Cleaning up..."
    [ -L "$DRIVER_DIR/kernelsu" ] && rm "$DRIVER_DIR/kernelsu" && echo "[-] Symlink removed."
    grep -q "kernelsu" "$DRIVER_MAKEFILE" && sed -i '/kernelsu/d' "$DRIVER_MAKEFILE" && echo "[-] Makefile reverted."
    grep -q "drivers/kernelsu/Kconfig" "$DRIVER_KCONFIG" && sed -i '/drivers\/kernelsu\/Kconfig/d' "$DRIVER_KCONFIG" && echo "[-] Kconfig reverted."
    if [ -d "$GKI_ROOT/$BASE_REPO" ]; then
        rm -rf "$GKI_ROOT/$BASE_REPO" && echo "[-] $BASE_REPO directory deleted."
    fi
}

setup_kamisu() {
    echo "[+] Setting up KamiSU (base: $BASE_REPO@$BASE_BRANCH)..."

    # 1. Clone / update base KernelSU-Next
    if ! test -d "$GKI_ROOT/$BASE_REPO"; then
        git clone "https://github.com/$BASE_OWNER/$BASE_REPO" "$GKI_ROOT/$BASE_REPO" && echo "[+] Repository cloned."
    else
        echo "[+] Repository already exists, skipping clone."
    fi

    cd "$GKI_ROOT/$BASE_REPO"
    git stash && echo "[-] Stashed current changes."
    git fetch origin && echo "[+] Fetched origin."
    git checkout "$BASE_BRANCH" && echo "[-] Checked out $BASE_BRANCH."
    git pull origin "$BASE_BRANCH" && echo "[+] Repository updated."

    # 2. Overlay KamiSU-specific files on top of KernelSU-Next
    echo "[+] Overlaying KamiSU customizations..."
    KAMISU_RAW="https://raw.githubusercontent.com/$KAMISU_REPO/master/kernel"
    for f in manager_sign.h apk_sign.c apk_sign.h manager.h; do
        curl -LSs "$KAMISU_RAW/$f" -o "$GKI_ROOT/$BASE_REPO/kernel/$f" && echo "[+] Overlaid $f"
    done

    # 3. Symlink, Makefile, Kconfig
    cd "$DRIVER_DIR"
    ln -sf "$(realpath --relative-to="$DRIVER_DIR" "$GKI_ROOT/$BASE_REPO/kernel")" "kernelsu" && echo "[+] Symlink created."
    grep -q "kernelsu" "$DRIVER_MAKEFILE" || printf "\nobj-\$(CONFIG_KSU) += kernelsu/\n" >> "$DRIVER_MAKEFILE" && echo "[+] Modified Makefile."
    grep -q "source \"drivers/kernelsu/Kconfig\"" "$DRIVER_KCONFIG" || sed -i "/endmenu/i\source \"drivers/kernelsu/Kconfig\"" "$DRIVER_KCONFIG" && echo "[+] Modified Kconfig."
    echo '[+] Done.'
}

if [ "$#" -eq 0 ]; then
    initialize_variables
    setup_kamisu
elif [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    display_usage
elif [ "$1" = "--cleanup" ]; then
    initialize_variables
    perform_cleanup
else
    initialize_variables
    setup_kamisu "$@"
fi
