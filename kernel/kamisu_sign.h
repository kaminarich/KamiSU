#ifndef __KSU_H_KAMISU_SIGN
#define __KSU_H_KAMISU_SIGN

/*
 * KamiSU Manager Signature Configuration
 *
 * This file contains the signing certificate hash for the KamiSU manager.
 * It is intentionally separated from manager_sign.h so that updating the
 * KamiSU hash does not require touching the shared multi-manager config.
 *
 * To update the KamiSU manager hash, only change this file.
 */

// kaminarich/KamiSU
#define EXPECTED_SIZE_KAMISU 0x2e8
#define EXPECTED_HASH_KAMISU \
    "13c415105fad7b798e5584798d82ea334729f0c24de7b9d4cd2f2e1b062eb566"

#endif /* __KSU_H_KAMISU_SIGN */
