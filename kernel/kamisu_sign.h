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
    "31c1a4fe9b7ba64858c2216db969ac47433728550fca966bfeb639b2d1e909ec"

#endif /* __KSU_H_KAMISU_SIGN */
