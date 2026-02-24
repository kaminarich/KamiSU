<div align="center">
  <img src="" 
       width="100%" 
       style="border-radius: 15px;" 
       alt="KamiSU Banner" />

  <h1>KamiSU</h1>

  <p>
    <a href="https://github.com/kaminarich/KamiSU/actions/workflows/build-manager.yml"><img src="https://github.com/kaminarich/KamiSU/actions/workflows/build-manager.yml/badge.svg" alt="Build Manager"></a>
    <a href="https://github.com/kaminarich/KamiSU/blob/master/LICENSE"><img src="https://img.shields.io/github/license/kaminarich/KamiSU" alt="License"></a>
  </p>

  <p>A kernel-based root solution for Android — based on <a href="https://github.com/KernelSU-Next/KernelSU-Next">KernelSU-Next</a> (dev-susfs branch) with KamiSU customizations.</p>
</div>

<div align="center">

> [!NOTE]
> This is **KamiSU** — an unofficial fork of KernelSU-Next with customized manager signing and SuSFS support.
>
> All changes are not guaranteed stable.
>
> All rights reserved to [@tiann](https://github.com/tiann), the author of KernelSU.

</div>

## Introduction

KamiSU is a kernel-based root solution for Android GKI devices. It is based on [KernelSU-Next](https://github.com/KernelSU-Next/KernelSU-Next) (`dev-susfs` branch by [@pershoot](https://github.com/pershoot)) with KamiSU-specific overlays:

- Custom `manager_sign.h` — supports multiple unofficial managers
- Custom `apk_sign.c` / `apk_sign.h` — expanded signing support
- Custom `manager.h`
- KamiSU manager APK signing included

## Features

1. Kernel-based `su` and root access management.
2. Module system based on [Metamodule](https://kernelsu.org/guide/metamodule.html)
3. [App Profile](https://kernelsu.org/guide/app-profile.html): Lock up the root power in a cage.
4. **Multi-manager support** — KamiSU manager and others supported by default.
5. **SuSFS integration** — patched on top of the dev-susfs branch.

## How to add KamiSU kernel driver into your kernel source

**master branch**

```sh
curl -LSs "https://raw.githubusercontent.com/kaminarich/KamiSU/refs/heads/master/kernel/setup.sh" | bash -s master
```

## Supported Managers

| Manager | Size | SHA256 |
|---|---|---|
| KernelSU-Next | `0x3e6` | `79e590113c4c...` |
| pershoot/KernelSU-Next | `0x338` | `f26471a28031...` |
| KOWX712/KernelSU | `0x375` | `484fcba6e6c4...` |
| RapliVx/MamboSU | `0x384` | `a9462b8b98ea...` |
| rsuntk/KernelSU | `0x396` | `f415f4ed9435...` |
| KernelSU-WILD | `0x381` | `52d52d8c8bfb...` |
| ReSukiSU | `0x377` | `d3469712b621...` |
| ShirkNeko/SukiSU | `0x35c` | `947ae944f3de...` |
| kaminarich/KamiSU | `0x2e8` | `e1b85ab50672...` |

Full hashes are defined in [`kernel/manager_sign.h`](kernel/manager_sign.h).

## Hook method

1. **SuSFS Hook (default for KamiSU builds):**
    - Enabled via `CONFIG_KSU_SUSFS_SUS_SU=y`
    - Recommended for GKI 5.10, 6.1, 6.6
2. **Syscall hook:**
    - Default hook method on GKI kernels without SuSFS.
    - Need `CONFIG_KSU_SYSCALL_HOOK=y` & `CONFIG_KPROBES=y`
3. **Manual hook:**
    - Default hook method on Non-GKI kernels.
    - Need `CONFIG_KSU_MANUAL_HOOK=y`

## Compatibility

- **Kernel compatibility:**
    - Android GKI 2.0 (5.10+): aarch64, x86_64
    - Android GKI 1.0 (5.4): aarch64
- **Application compatibility (ksud/manager):**
    - arm64-v8a, armeabi-v7a, x86_64

## Discussion

- Telegram Group: [@KamiSkizofrenia](https://t.me/KamiSkizofrenia)

## Security

For information on reporting security vulnerabilities in KernelSU, see [SECURITY.md](/SECURITY.md).

## License

- Files under the `kernel` directory are [GPL-2.0-only](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html).
- All other parts except the `kernel` directory are [GPL-3.0-or-later](https://www.gnu.org/licenses/gpl-3.0.html).

## Credits

- [tiann](https://github.com/tiann): Original KernelSU author.
- [pershoot](https://github.com/pershoot): KernelSU-Next dev-susfs branch — the base of KamiSU.
- [KernelSU-Next](https://github.com/KernelSU-Next/KernelSU-Next): KernelSU-Next project.
- [simonpunk](https://gitlab.com/simonpunk): susfs add-on.
- [Kingfinik98](https://github.com/Kingfinik98): multi-manager support patch reference.
- [RapliVx](https://github.com/RapliVx): MamboSU — manager signing reference.
- [Magisk](https://github.com/topjohnwu/Magisk): the powerful root tool.
- [genuine](https://github.com/brevent/genuine/): apk v2 signature validation.

---

## Maintainer

**[@kaminarich](https://github.com/kaminarich)**

![kaminarich's GitHub stats](https://github-readme-stats-one-bice.vercel.app/api?username=kaminarich&show_icons=true&include_all_commits=true)