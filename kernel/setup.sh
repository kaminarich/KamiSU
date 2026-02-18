#!/bin/sh
set -eu

GKI_ROOT=$(pwd)

display_usage() {
    echo "Usage: $0 [--cleanup | <commit-or-tag>]"
    echo "  --cleanup:              Cleans up previous modifications made by the script."
    echo "  <commit-or-tag>:        Sets up or updates the KamiSU to specified tag or commit."
    echo "  -h, --help:             Displays this usage information."
    echo "  (no args):              Sets up or updates the KamiSU environment to the latest tagged version."
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

# Reverts modifications made by this script
perform_cleanup() {
    echo "[+] Cleaning up..."
    [ -L "$DRIVER_DIR/kernelsu" ] && rm "$DRIVER_DIR/kernelsu" && echo "[-] Symlink removed."
    grep -q "kernelsu" "$DRIVER_MAKEFILE" && sed -i '/kernelsu/d' "$DRIVER_MAKEFILE" && echo "[-] Makefile reverted."
    grep -q "drivers/kernelsu/Kconfig" "$DRIVER_KCONFIG" && sed -i '/drivers\/kernelsu\/Kconfig/d' "$DRIVER_KCONFIG" && echo "[-] Kconfig reverted."
    # Hapus folder KamiSU (sebelumnya KernelSU)
    if [ -d "$GKI_ROOT/KamiSU" ]; then
        rm -rf "$GKI_ROOT/KamiSU" && echo "[-] KamiSU directory deleted."
    fi
}

# Sets up or update KamiSU environment
setup_kernelsu() {
    echo "[+] Setting up KamiSU..."
    # Clone dari repo kaminarich/KamiSU ke folder KamiSU
    test -d "$GKI_ROOT/KamiSU" || git clone https://github.com/kaminarich/KamiSU KamiSU && echo "[+] Repository cloned."
    cd "$GKI_ROOT/KamiSU"
    git stash && echo "[-] Stashed current changes."
    
    # Cek branch master
    if [ "$(git status | grep -Po 'v\d+(\.\d+)*' | head -n1)" ]; then
        git checkout master && echo "[-] Switched to master branch."
    fi
    
    git pull && echo "[+] Repository updated."
    
    if [ -z "${1-}" ]; then
        # Coba checkout tag terbaru, kalau gagal fallback ke master
        if git describe --abbrev=0 --tags >/dev/null 2>&1; then
            git checkout "$(git describe --abbrev=0 --tags)" && echo "[-] Checked out latest tag."
        else
            git checkout master && echo "[-] No tags found, checked out master."
        fi
    else
        git checkout "$1" && echo "[-] Checked out $1." || echo "[-] Checkout default branch"
    fi
    
    cd "$DRIVER_DIR"
    # Symlink folder kernel dari dalam KamiSU ke drivers/kernelsu
    
    ln -sf "$(realpath --relative-to="$DRIVER_DIR" "$GKI_ROOT/KamiSU/kernel")" "kernelsu" && echo "[+] Symlink created."

    # Add entries in Makefile and Kconfig if not already existing
    grep -q "kernelsu" "$DRIVER_MAKEFILE" || printf "\nobj-\$(CONFIG_KSU) += kernelsu/\n" >> "$DRIVER_MAKEFILE" && echo "[+] Modified Makefile."
    grep -q "source \"drivers/kernelsu/Kconfig\"" "$DRIVER_KCONFIG" || sed -i "/endmenu/i\source \"drivers/kernelsu/Kconfig\"" "$DRIVER_KCONFIG" && echo "[+] Modified Kconfig."
    echo '[+] Done.'
}

# Process command-line arguments
if [ "$#" -eq 0 ]; then
    initialize_variables
    setup_kernelsu
elif [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    display_usage
elif [ "$1" = "--cleanup" ]; then
    initialize_variables
    perform_cleanup
else
    initialize_variables
    setup_kernelsu "$@"
fi
