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
    "e1b85ab506722bd67e4ac1bb6bd2f8e6216ecd3fb48bb47f053f2a971ea6a021"

#endif /* __KSU_H_KAMISU_SIGN */
