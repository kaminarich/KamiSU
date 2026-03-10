# KamiSU (Unofficial)

Original Author: kaminarich. Do not remove credit under GPLv2 compliance.

[![KamiSU Banner](https://raw.githubusercontent.com/kaminarich/KamiSU/refs/heads/master/docs/kamisu.jpg)](https://raw.githubusercontent.com/kaminarich/KamiSU/refs/heads/master/docs/kamisu.jpg)

### Kernel-based Android Root Solution from KamiSU

[![Telegram](https://img.shields.io/badge/Telegram-KamiSU-blue?style=for-the-badge&logo=telegram)](https://t.me/KamiSkizofrenia)

---

## Overview

KamiSU is a kernel-based root solution designed for clean integration, stability, and multi-manager compatibility. Based on [KernelSU-Next](https://github.com/KernelSU-Next/KernelSU-Next) (`dev-susfs` branch) with KamiSU-specific overlays.

The root implementation and patch system are fully functional and tested.

- **Status:** Stable
- **Type:** Kernel Root Implementation
- **Distribution:** Unofficial Open Source

---

## Integration

Use the following command to integrate KamiSU into your kernel source:

```sh
curl -LSs "https://raw.githubusercontent.com/kaminarich/KamiSU/refs/heads/master/kernel/setup.sh" | bash -s master
```

---

## Integration KPM Patch

```sh
curl -LSs "https://github.com/Kingfinik98/SukiSU_patch/raw/refs/heads/main/kpm/patch_linux" -o patch
```

---

## Manager Support

Supported managers:

- KamiSU
- KernelSU-Next
- KOWX712/KernelSU
- RapliVx/MamboSU
- rsuntk/KernelSU
- KernelSU-WILD
- ReSukiSU
- ShirkNeko/SukiSU Ultra

Full hashes are defined in [`kernel/manager_sign.h`](kernel/manager_sign.h).

---

## Features

- Kernel-based root implementation
- Fully working patch system
- Clean and modular integration
- Lightweight and optimized
- Multi-manager compatibility
- Suitable for OSS kernel builds
- **SuSFS integration** — patched on top of the dev-susfs branch
- **App Profile system** — granular root access control

---

## Hook Method

1. **SuSFS Hook (default for KamiSU builds):**
    - Enabled via `CONFIG_KSU_SUSFS_SUS_SU=y`
    - Recommended for GKI 5.10, 6.1, 6.6
2. **Syscall hook:**
    - Default hook method on GKI kernels without SuSFS
    - Requires `CONFIG_KSU_SYSCALL_HOOK=y` & `CONFIG_KPROBES=y`
3. **Manual hook:**
    - Default hook method on Non-GKI kernels
    - Requires `CONFIG_KSU_MANUAL_HOOK=y`

---

## Compatibility

- **Kernel compatibility:**
    - Android GKI 2.0 (5.10+): aarch64, x86_64
    - Android GKI 1.0 (5.4): aarch64
- **Application compatibility (ksud/manager):**
    - arm64-v8a, armeabi-v7a, x86_64

---

## Build Status

[![Build Manager](https://github.com/kaminarich/KamiSU/actions/workflows/build-manager.yml/badge.svg)](https://github.com/kaminarich/KamiSU/actions/workflows/build-manager.yml)
[![Build Kernel](https://github.com/kaminarich/KamiSU/actions/workflows/build-kernel.yml/badge.svg)](https://github.com/kaminarich/KamiSU/actions/workflows/build-kernel.yml)

---

## Credits Sources & Base

[![KernelSU](https://img.shields.io/badge/GitHub-KernelSU-black?style=for-the-badge&logo=github)](https://github.com/tiann/KernelSU)

[![KernelSU-Next](https://img.shields.io/badge/GitHub-KernelSU--Next-black?style=for-the-badge&logo=github)](https://github.com/KernelSU-Next/KernelSU-Next)

[![SukiSU-Ultra](https://img.shields.io/badge/GitHub-SukiSU--Ultra-black?style=for-the-badge&logo=github)](https://github.com/SukiSU-Ultra/SukiSU-Ultra)

[![ReSukiSU](https://img.shields.io/badge/GitHub-ReSukiSU-black?style=for-the-badge&logo=github)](https://github.com/ReSukiSU/ReSukiSU)

---

## Discussion

- Telegram Group: [@KamiSkizofrenia](https://t.me/KamiSkizofrenia)

---

## Security

For information on reporting security vulnerabilities, see [SECURITY.md](/SECURITY.md).

---

## License

- Files under the `kernel` directory are [GPL-2.0-only](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html).
- All other parts except the `kernel` directory are [GPL-3.0-or-later](https://www.gnu.org/licenses/gpl-3.0.html).

---

## Maintainer

**[@kaminarich](https://github.com/kaminarich)**
